package efub.awa.moamoa.websocket.auction.dto.response;

import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;

public record AuctionParticipantResponse(
        Long memberId,
        String nickname,
        EquippedItem equippedItem
) {
    public static AuctionParticipantResponse from(AuctionParticipantState state) {
        return new AuctionParticipantResponse(
                state.memberId(),
                state.nickname(),
                state.equippedItem()
        );
    }
}
