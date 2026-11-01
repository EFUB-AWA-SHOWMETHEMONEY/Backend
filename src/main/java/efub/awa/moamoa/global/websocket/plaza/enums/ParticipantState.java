package efub.awa.moamoa.global.websocket.plaza.enums;

import java.util.Map;

public record ParticipantState (Long memberId, String nickname, Map<String, Object> appearance,
                                int x, int y, String sessionId) {
    public ParticipantState withPosition(int x, int y) {
        return new ParticipantState(memberId, nickname, appearance, x, y, sessionId);
    }
}
