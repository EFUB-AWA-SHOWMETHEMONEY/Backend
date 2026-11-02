package efub.awa.moamoa.websocket.plaza.dto.response;

import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.websocket.plaza.entity.ParticipantState;

public record ParticipantResponse(
        Long memberId,
        String nickname,
        EquippedItem equippedItem,
        int x,
        int y

) {
    public static ParticipantResponse from (ParticipantState state) {
        return new ParticipantResponse(
                state.memberId(),
                state.nickname(),
                state.equippedItem(),
                state.x(),
                state.y()
        );
    }
}
