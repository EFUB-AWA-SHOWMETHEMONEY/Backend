package efub.awa.moamoa.auction.event;

public record BidPlacedEvent(
        Long auctionId,
        Long bidderId,
        String nickname,
        Long price,
        long newDeadline) {}