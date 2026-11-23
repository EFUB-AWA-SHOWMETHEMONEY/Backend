package efub.awa.moamoa.auction.dto.response;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.entity.PublicBid;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PublicBidResponse(
        Long bidId,
        Long auctionId,
        Long bidderId,
        Long amount,
        Long currentPrice,
        LocalDateTime commitDeadline
) {
    public static PublicBidResponse of(PublicBid bid, Auction auction) {
        return PublicBidResponse.builder()
                .bidId(bid.getId())
                .auctionId(auction.getId())
                .bidderId(bid.getBidder().getId())
                .amount(bid.getAmount())
                .currentPrice(auction.getCurrentPrice())
                .commitDeadline(auction.getCommitDeadline())
                .build();
    }
}
