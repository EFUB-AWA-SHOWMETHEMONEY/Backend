package efub.awa.moamoa.global.websocket.plaza.service;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.websocket.plaza.dto.request.MoveRequestDto;
import efub.awa.moamoa.websocket.plaza.dto.response.PlazaEvent.PositionUpdate;
import efub.awa.moamoa.websocket.plaza.entity.ParticipantState;
import efub.awa.moamoa.websocket.plaza.enums.PlazaConstants;
import efub.awa.moamoa.websocket.plaza.repository.PlazaParticipantRegistry;
import efub.awa.moamoa.websocket.plaza.service.PlazaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * move()는 registry만 쓰므로 회원·인벤토리 레포지토리는 주입하지 않는다.
 * join()은 DB가 필요해 여기서 다루지 않는다.
 */
class PlazaServiceTest {

    private static final long MEMBER = 1L;
    private static final String SESSION = "sess-A";

    private PlazaParticipantRegistry registry;
    private PlazaService service;

    @BeforeEach
    void setUp() {
        registry = new PlazaParticipantRegistry();
        service = new PlazaService(registry, null, null);
    }

    private void joinPlaza() {
        registry.join(new ParticipantState(
                MEMBER, SESSION, "테스터", null,
                PlazaConstants.SPAWN_X, PlazaConstants.SPAWN_Y, 0L));
    }

    private MoveRequestDto move(int x, int y, long seq) {
        return new MoveRequestDto(x, y, seq);
    }

    @Test
    @DisplayName("광장에 없으면 NOT_IN_ROOM")
    void throwsWhenNotInPlaza() {
        assertThatThrownBy(() -> service.move(MEMBER, SESSION, move(10, 10, 1L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_IN_ROOM);
    }

    @Test
    @DisplayName("정상 이동은 좌표 그대로 반영된다")
    void appliesValidMove() {
        joinPlaza();

        Optional<PositionUpdate> event = service.move(MEMBER, SESSION, move(100, 200, 1L));

        assertThat(event).isPresent();
        assertThat(event.get().type()).isEqualTo("position_update");
        assertThat(event.get().memberId()).isEqualTo(MEMBER);
        assertThat(event.get().x()).isEqualTo(100);
        assertThat(event.get().y()).isEqualTo(200);
        assertThat(event.get().seq()).isEqualTo(1L);
    }

    @Test
    @DisplayName("범위를 넘는 좌표는 최대값으로 보정된다")
    void clampsUpperBound() {
        joinPlaza();

        Optional<PositionUpdate> event = service.move(MEMBER, SESSION, move(9999, 9999, 1L));

        assertThat(event).isPresent();
        assertThat(event.get().x()).isEqualTo(PlazaConstants.WIDTH - 1);
        assertThat(event.get().y()).isEqualTo(PlazaConstants.HEIGHT - 1);
    }

    @Test
    @DisplayName("음수 좌표는 0으로 보정된다")
    void clampsLowerBound() {
        joinPlaza();

        Optional<PositionUpdate> event = service.move(MEMBER, SESSION, move(-50, -1, 1L));

        assertThat(event).isPresent();
        assertThat(event.get().x()).isZero();
        assertThat(event.get().y()).isZero();
    }

    @Test
    @DisplayName("경계값은 보정 없이 통과한다")
    void keepsBoundaryValues() {
        joinPlaza();

        Optional<PositionUpdate> event = service.move(
                MEMBER, SESSION, move(PlazaConstants.WIDTH - 1, PlazaConstants.HEIGHT - 1, 1L));

        assertThat(event).isPresent();
        assertThat(event.get().x()).isEqualTo(PlazaConstants.WIDTH - 1);
        assertThat(event.get().y()).isEqualTo(PlazaConstants.HEIGHT - 1);
    }

    @Test
    @DisplayName("낡은 seq는 이벤트를 만들지 않는다 (브로드캐스트 안 함)")
    void staleSeqProducesNoEvent() {
        joinPlaza();
        service.move(MEMBER, SESSION, move(100, 100, 5L));

        assertThat(service.move(MEMBER, SESSION, move(1, 1, 5L))).isEmpty();
        assertThat(service.move(MEMBER, SESSION, move(2, 2, 3L))).isEmpty();

        ParticipantState current = registry.find(MEMBER).orElseThrow();
        assertThat(current.x()).isEqualTo(100);
        assertThat(current.seq()).isEqualTo(5L);
    }

    @Test
    @DisplayName("구 세션의 이동은 이벤트를 만들지 않는다")
    void staleSessionProducesNoEvent() {
        joinPlaza();

        assertThat(service.move(MEMBER, "sess-OLD", move(300, 200, 1L))).isEmpty();
        assertThat(registry.find(MEMBER).orElseThrow().x()).isEqualTo(PlazaConstants.SPAWN_X);
    }
}
