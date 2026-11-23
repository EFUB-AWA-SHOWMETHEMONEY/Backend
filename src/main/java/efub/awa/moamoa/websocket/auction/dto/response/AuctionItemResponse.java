package efub.awa.moamoa.websocket.auction.dto.response;

import efub.awa.moamoa.auction.entity.Auction;

public record AuctionItemResponse(
        Long itemId,
        String itemImageUrl,
        Long startPrice,
        Long currentPrice
) {
    public static AuctionItemResponse of (Auction auction, String itemImageUrl) {
        return new AuctionItemResponse(
                auction.getInventory().getItem().getId(),
                itemImageUrl,
                auction.getStartPrice(),
                auction.getCurrentPrice()
        );
    }
}
