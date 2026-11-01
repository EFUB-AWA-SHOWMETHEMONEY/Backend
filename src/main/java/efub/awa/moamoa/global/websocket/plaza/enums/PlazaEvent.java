package efub.awa.moamoa.global.websocket.plaza.enums;

import java.util.Map;

public sealed interface PlazaEvent {

    String type();
    long ts();

    record UserJoined(String type, Long memberId, String nickname, Map<String, Object> appearance,
                      int x, int y, long ts) implements PlazaEvent {
        public static UserJoined from (ParticipantState state) {
            return new UserJoined(
                    "user_joined", state.memberId(), state.nickname(), state.appearance(),
                    state.x(), state.y(), System.currentTimeMillis()
            );
        }
    }

    record UserLeft(String type, Long memberId, long ts) implements PlazaEvent {

        public static UserLeft of (Long memberId) {
            return new UserLeft(
                    "user_left", memberId, System.currentTimeMillis()
            );
        }
    }
}
