package efub.awa.moamoa.auction.dto.response;

import efub.awa.moamoa.auction.entity.PrivateBid;

import java.time.LocalDateTime;

public record PrivateBidCommitResponse(
        Long bidId,
        Long auctionId,
        Long bidderId,
        LocalDateTime committedAt,
        boolean previousBidInvalidated
) {
    public static PrivateBidCommitResponse from(PrivateBid privateBid, boolean previousBidInvalidated) {
        return new PrivateBidCommitResponse(
                privateBid.getId(),
                privateBid.getAuction().getId(),
                privateBid.getBidder().getId(),
                privateBid.getCommittedAt(),
                previousBidInvalidated
        );
    }
}
