package efub.awa.moamoa.auction.dto.response;

import efub.awa.moamoa.auction.entity.PrivateBid;

import java.time.LocalDateTime;

public record PrivateBidRevealResponse(
        Long bidId,
        Long auctionId,
        Long bidderId,
        Long revealedAmount,
        LocalDateTime revealedAt
) {
    public static PrivateBidRevealResponse from (PrivateBid bid) {
        return new PrivateBidRevealResponse(
                bid.getId(),
                bid.getAuction().getId(),
                bid.getBidder().getId(),
                bid.getRevealedAmount(),
                bid.getRevealedAt()
        );
    }
}
