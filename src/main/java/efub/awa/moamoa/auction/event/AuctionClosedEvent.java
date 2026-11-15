package efub.awa.moamoa.auction.event;

//TODO: 경매 종료 시 이벤트 발생 필요
public record AuctionClosedEvent(
        Long auctionId,
        Long winnerId,
        Long finalPrice)
{}