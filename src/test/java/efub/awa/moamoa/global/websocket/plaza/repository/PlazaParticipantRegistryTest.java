package efub.awa.moamoa.global.websocket.plaza.repository;

import efub.awa.moamoa.global.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.global.websocket.plaza.entity.ParticipantState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 광장 참가자 등록소 단위 테스트.
 * Spring 컨텍스트도 DB도 필요 없는 순수 POJO라 빠르게 돈다.
 */
class PlazaParticipantRegistryTest {

    private static final long MEMBER = 1L;
    private static final String SESSION = "sess-A";
    private static final String OTHER_SESSION = "sess-B";

    private PlazaParticipantRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PlazaParticipantRegistry();
    }

    private ParticipantState state(long memberId, String sessionId) {
        return new ParticipantState(memberId, sessionId, "테스터" + memberId, null, 240, 135, 0L);
    }

    @Nested
    @DisplayName("입장")
    class Join {

        @Test
        @DisplayName("처음 입장하면 이전 상태가 없다")
        void firstJoinHasNoPrevious() {
            assertThat(registry.join(state(MEMBER, SESSION))).isEmpty();
            assertThat(registry.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("같은 멤버가 다시 입장하면 이전 세션 상태를 돌려준다")
        void rejoinReturnsPreviousState() {
            registry.join(state(MEMBER, SESSION));

            Optional<ParticipantState> prev = registry.join(state(MEMBER, OTHER_SESSION));

            assertThat(prev).isPresent();
            assertThat(prev.get().sessionId()).isEqualTo(SESSION);
            //덮어쓰기이므로 참가자 수는 늘지 않는다
            assertThat(registry.findAll()).hasSize(1);
            assertThat(registry.find(MEMBER).orElseThrow().sessionId()).isEqualTo(OTHER_SESSION);
        }
    }

    @Nested
    @DisplayName("퇴장")
    class Leave {

        @Test
        @DisplayName("현재 세션이면 제거된다")
        void leaveWithCurrentSession() {
            registry.join(state(MEMBER, SESSION));

            assertThat(registry.leave(MEMBER, SESSION)).isTrue();
            assertThat(registry.find(MEMBER)).isEmpty();
        }

        @Test
        @DisplayName("다른 세션의 퇴장 요청은 무시된다")
        void leaveWithStaleSessionIsIgnored() {
            registry.join(state(MEMBER, OTHER_SESSION));

            assertThat(registry.leave(MEMBER, SESSION)).isFalse();
            assertThat(registry.find(MEMBER)).isPresent();
        }

        @Test
        @DisplayName("없는 멤버의 퇴장은 false")
        void leaveAbsentMember() {
            assertThat(registry.leave(999L, SESSION)).isFalse();
        }

        @Test
        @DisplayName("이동으로 상태 객체가 교체된 뒤에도 퇴장이 유실되지 않는다")
        void leaveSurvivesPositionUpdate() {
            //record라 이동 시 새 객체로 교체된다.
            //값 비교 CAS(remove(key, value))로 구현하면 여기서 퇴장이 유실된다.
            registry.join(state(MEMBER, SESSION));
            registry.updatePosition(MEMBER, SESSION, 10, 20, 1L);
            registry.updatePosition(MEMBER, SESSION, 30, 40, 2L);

            assertThat(registry.leave(MEMBER, SESSION)).isTrue();
            assertThat(registry.find(MEMBER)).isEmpty();
        }
    }

    @Nested
    @DisplayName("위치 갱신")
    class UpdatePosition {

        @BeforeEach
        void join() {
            registry.join(state(MEMBER, SESSION));
        }

        @Test
        @DisplayName("seq가 크면 수락하고 갱신된 상태를 돌려준다")
        void acceptsIncreasingSeq() {
            Optional<ParticipantState> applied = registry.updatePosition(MEMBER, SESSION, 100, 200, 1L);

            assertThat(applied).isPresent();
            assertThat(applied.get().x()).isEqualTo(100);
            assertThat(applied.get().y()).isEqualTo(200);
            assertThat(applied.get().seq()).isEqualTo(1L);
        }

        @Test
        @DisplayName("seq가 같거나 작으면 거부하고 좌표를 바꾸지 않는다")
        void rejectsStaleSeq() {
            registry.updatePosition(MEMBER, SESSION, 100, 200, 5L);

            assertThat(registry.updatePosition(MEMBER, SESSION, 1, 1, 5L)).isEmpty();
            assertThat(registry.updatePosition(MEMBER, SESSION, 2, 2, 4L)).isEmpty();

            ParticipantState current = registry.find(MEMBER).orElseThrow();
            assertThat(current.x()).isEqualTo(100);
            assertThat(current.y()).isEqualTo(200);
            assertThat(current.seq()).isEqualTo(5L);
        }

        @Test
        @DisplayName("구 세션의 이동은 거부되고 현재 세션 좌표를 건드리지 않는다")
        void rejectsStaleSession() {
            assertThat(registry.updatePosition(MEMBER, OTHER_SESSION, 999, 999, 1L)).isEmpty();

            ParticipantState current = registry.find(MEMBER).orElseThrow();
            assertThat(current.x()).isEqualTo(240);
            assertThat(current.y()).isEqualTo(135);
            assertThat(current.seq()).isZero();
        }

        @Test
        @DisplayName("퇴장한 멤버의 이동은 거부되고 되살아나지 않는다")
        void rejectsAfterLeave() {
            registry.leave(MEMBER, SESSION);

            assertThat(registry.updatePosition(MEMBER, SESSION, 50, 50, 1L)).isEmpty();
            assertThat(registry.find(MEMBER)).isEmpty();
        }
    }

    @Nested
    @DisplayName("장착 아이템 갱신")
    class UpdateEquippedItem {

        @Test
        @DisplayName("광장에 있으면 교체되고 좌표·seq는 유지된다")
        void replacesItemKeepingPosition() {
            registry.join(state(MEMBER, SESSION));
            registry.updatePosition(MEMBER, SESSION, 11, 22, 3L);

            Optional<ParticipantState> applied =
                    registry.updateEquippedItem(MEMBER, new EquippedItem(7L, "http://img/7.png"));

            assertThat(applied).isPresent();
            assertThat(applied.get().equippedItem().itemId()).isEqualTo(7L);
            assertThat(applied.get().x()).isEqualTo(11);
            assertThat(applied.get().y()).isEqualTo(22);
            assertThat(applied.get().seq()).isEqualTo(3L);
        }

        @Test
        @DisplayName("광장에 없으면 아무 일도 일어나지 않는다")
        void noOpWhenAbsent() {
            assertThat(registry.updateEquippedItem(MEMBER, new EquippedItem(7L, "http://img/7.png")))
                    .isEmpty();
            assertThat(registry.find(MEMBER)).isEmpty();
        }
    }

    @Nested
    @DisplayName("조회")
    class Query {

        @Test
        @DisplayName("findAll은 호출 시점의 복사본이라 이후 변경에 영향받지 않는다")
        void findAllIsSnapshot() {
            registry.join(state(MEMBER, SESSION));
            List<ParticipantState> snapshot = registry.findAll();

            registry.join(state(2L, "sess-C"));

            assertThat(snapshot).hasSize(1);
            assertThat(registry.findAll()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("동시성")
    class Concurrency {

        @Test
        @DisplayName("같은 멤버에 이동이 몰려도 최종 좌표는 최대 seq의 것이다")
        void highestSeqWins() throws Exception {
            registry.join(state(MEMBER, SESSION));

            int threads = 16;
            int perThread = 100;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger accepted = new AtomicInteger();

            for (int t = 0; t < threads; t++) {
                final int base = t * perThread;
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 1; i <= perThread; i++) {
                            long seq = base + i;
                            //seq를 좌표에 그대로 실어 최종 상태의 일관성을 확인한다
                            int coord = (int) (seq % 480);
                            if (registry.updatePosition(MEMBER, SESSION, coord, coord, seq).isPresent()) {
                                accepted.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
            pool.shutdownNow();

            ParticipantState finalState = registry.find(MEMBER).orElseThrow();
            //수락된 갱신이 하나라도 있고, 최종 좌표는 최종 seq와 짝이 맞아야 한다 (찢어진 상태 없음)
            assertThat(accepted.get()).isPositive();
            assertThat(finalState.x()).isEqualTo((int) (finalState.seq() % 480));
            assertThat(finalState.y()).isEqualTo(finalState.x());
        }

        @Test
        @DisplayName("이동과 퇴장이 경쟁해도 퇴장은 정확히 한 번만 성공한다")
        void leaveSucceedsExactlyOnce() throws Exception {
            int rounds = 300;
            AtomicInteger doubleLeave = new AtomicInteger();

            for (int r = 0; r < rounds; r++) {
                PlazaParticipantRegistry reg = new PlazaParticipantRegistry();
                reg.join(state(MEMBER, SESSION));

                ExecutorService pool = Executors.newFixedThreadPool(3);
                CountDownLatch start = new CountDownLatch(1);
                AtomicInteger leaveWins = new AtomicInteger();

                pool.submit(() -> {
                    awaitQuietly(start);
                    for (long seq = 1; seq <= 50; seq++) {
                        reg.updatePosition(MEMBER, SESSION, (int) seq, (int) seq, seq);
                    }
                });
                for (int i = 0; i < 2; i++) {
                    pool.submit(() -> {
                        awaitQuietly(start);
                        if (reg.leave(MEMBER, SESSION)) leaveWins.incrementAndGet();
                    });
                }

                start.countDown();
                pool.shutdown();
                assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

                //퇴장 요청 2개 중 정확히 하나만 true여야 user_left가 한 번만 나간다
                if (leaveWins.get() != 1) doubleLeave.incrementAndGet();
                assertThat(reg.find(MEMBER)).isEmpty();
            }

            assertThat(doubleLeave.get()).isZero();
        }

        private void awaitQuietly(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
