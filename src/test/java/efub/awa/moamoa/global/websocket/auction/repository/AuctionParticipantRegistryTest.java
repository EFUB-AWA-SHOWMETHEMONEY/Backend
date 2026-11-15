package efub.awa.moamoa.global.websocket.auction.repository;

import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionParticipantRegistryTest {

    private static final long AUCTION = 10L;
    private static final long MEMBER = 1L;

    private AuctionParticipantRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AuctionParticipantRegistry();
    }

    private AuctionParticipantState state(long memberId, String sessionId, long auctionId) {
        return new AuctionParticipantState(memberId, sessionId, "닉" + memberId, null, auctionId);
    }

    @Test
    @DisplayName("처음 입장하면 이전 상태가 없어 null을 반환한다")
    void firstJoinReturnsNull() {
        assertThat(registry.join(AUCTION, state(MEMBER, "s1", AUCTION))).isNull();
        assertThat(registry.find(AUCTION, MEMBER)).isNotNull();
    }

    @Test
    @DisplayName("같은 회원이 다시 입장하면 이전 상태를 반환하고 새 세션으로 덮어쓴다")
    void rejoinReplacesAndReturnsPrevious() {
        registry.join(AUCTION, state(MEMBER, "s1", AUCTION));

        AuctionParticipantState prev = registry.join(AUCTION, state(MEMBER, "s2", AUCTION));

        assertThat(prev.sessionId()).isEqualTo("s1");
        assertThat(registry.find(AUCTION, MEMBER).sessionId()).isEqualTo("s2");
    }

    @Test
    @DisplayName("같은 세션일 때만 퇴장된다")
    void leaveOnlyWithSameSession() {
        registry.join(AUCTION, state(MEMBER, "s2", AUCTION));

        assertThat(registry.leave(MEMBER, AUCTION, "s1")).isFalse();   // 이전 세션의 퇴장 요청
        assertThat(registry.find(AUCTION, MEMBER)).isNotNull();

        assertThat(registry.leave(MEMBER, AUCTION, "s2")).isTrue();
        assertThat(registry.find(AUCTION, MEMBER)).isNull();
    }

    @Test
    @DisplayName("없는 방이나 없는 회원의 퇴장은 false")
    void leaveUnknownReturnsFalse() {
        assertThat(registry.leave(MEMBER, AUCTION, "s1")).isFalse();

        registry.join(AUCTION, state(MEMBER, "s1", AUCTION));
        assertThat(registry.leave(99L, AUCTION, "s1")).isFalse();
    }

    @Test
    @DisplayName("null 인자는 예외 없이 false/null")
    void nullArgumentsAreSafe() {
        assertThat(registry.leave(null, AUCTION, "s1")).isFalse();
        assertThat(registry.leave(MEMBER, null, "s1")).isFalse();
        assertThat(registry.leave(MEMBER, AUCTION, null)).isFalse();
        assertThat(registry.find(null, MEMBER)).isNull();
        assertThat(registry.find(AUCTION, null)).isNull();
    }

    @Test
    @DisplayName("마지막 사람이 나가도 다른 방은 영향이 없다")
    void roomsAreIsolated() {
        registry.join(10L, state(1L, "s1", 10L));
        registry.join(20L, state(1L, "s1", 20L));

        registry.leave(1L, 10L, "s1");

        assertThat(registry.find(10L, 1L)).isNull();
        assertThat(registry.find(20L, 1L)).isNotNull();
    }

    @Test
    @DisplayName("방이 비면 정리되고, 다시 입장해도 정상 동작한다")
    void emptyRoomIsCleanedUp() {
        registry.join(AUCTION, state(MEMBER, "s1", AUCTION));
        registry.leave(MEMBER, AUCTION, "s1");

        assertThat(registry.join(AUCTION, state(MEMBER, "s2", AUCTION))).isNull();
        assertThat(registry.find(AUCTION, MEMBER).sessionId()).isEqualTo("s2");
    }

    @Test
    @DisplayName("findBySession: 방에 없으면 null (NPE 아님)")
    void findBySessionNullWhenAbsent() {
        assertThat(registry.findBySession(AUCTION, MEMBER, "s1")).isNull();
    }

    @Test
    @DisplayName("findBySession: 세션이 다르면 null, 같으면 상태 반환")
    void findBySessionChecksSession() {
        registry.join(AUCTION, state(MEMBER, "s2", AUCTION));

        assertThat(registry.findBySession(AUCTION, MEMBER, "s1")).isNull();
        assertThat(registry.findBySession(AUCTION, MEMBER, "s2")).isNotNull();
    }
}
