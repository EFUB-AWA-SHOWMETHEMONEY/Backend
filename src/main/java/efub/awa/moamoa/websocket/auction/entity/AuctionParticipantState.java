package efub.awa.moamoa.websocket.auction.entity;

import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;

public record AuctionParticipantState(
        Long memberId,
        String sessionId,
        String nickname,
        EquippedItem equippedItem,
        Long auctionId
) {
}
