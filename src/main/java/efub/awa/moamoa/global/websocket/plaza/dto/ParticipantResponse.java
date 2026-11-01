package efub.awa.moamoa.global.websocket.plaza.dto;

import efub.awa.moamoa.global.websocket.plaza.enums.ParticipantState;

import java.util.List;
import java.util.Map;

public record ParticipantResponse(
        Long memberId,
        String nickname,
        Map<String, Object> appearance,
        int x,
        int y

) {
    public static ParticipantResponse from (ParticipantState state) {
        return new ParticipantResponse(
                state.memberId(),
                state.nickname(),
                state.appearance(),
                state.x(),
                state.y()
        );
    }
}