package efub.awa.moamoa.auction.dto.response;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.member.entity.Member;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record HighestBidResponse(
        Long auctionId,
        Long currentPrice,
        Long highestBidderId,
        String highestBidderNickname,
        int bidCount,
        LocalDateTime lastBidAt,
        LocalDateTime commitDeadline
) {
    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    public static HighestBidResponse of(Auction auction, long bidCount, LocalDateTime lastBidAt) {
        Member highestBidder = auction.getCurrentBidder();

        return HighestBidResponse.builder()
                .auctionId(auction.getId())
                .currentPrice(auction.getCurrentPrice())
                .highestBidderId(highestBidder != null ? highestBidder.getId() : null)
                .highestBidderNickname(nicknameOf(highestBidder, bidCount))
                .bidCount((int) bidCount)
                .lastBidAt(lastBidAt)
                //마감 시각은 OPEN일 때만 내려준다 (READY는 아직 정해지지 않음)
                .commitDeadline(auction.getStatus() == AuctionStatus.OPEN ? auction.getCommitDeadline() : null)
                .build();
    }

    //입찰이 없으면 null, 입찰은 있는데 최고 입찰자가 없으면(회원 탈퇴로 null) "탈퇴한 사용자"
    private static String nicknameOf(Member highestBidder, long bidCount) {
        if (highestBidder != null) return highestBidder.getNickname();
        return bidCount > 0 ? WITHDRAWN_NICKNAME : null;
    }
}
