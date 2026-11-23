package efub.awa.moamoa.auction.dto.response;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;

import java.time.LocalDateTime;

//경매 상태 전환 결과 (서비스 반환값): 입장 처리, 스케줄러가 전환 결과를 확인하는 데 쓴다.
public record AuctionTransitionResult(
        Long auctionId,
        AuctionStatus status,
        LocalDateTime openStartTime,
        LocalDateTime commitDeadline,
        LocalDateTime revealDeadline,
        Long winnerId,
        Long finalPrice
) {
    //winnerId, finalPrice는 CLOSED일 때만 값이 있고 그 외에는 null
    public static AuctionTransitionResult of(Auction auction, Long winnerId, Long finalPrice) {
        return new AuctionTransitionResult(
                auction.getId(),
                auction.getStatus(),
                auction.getOpenStartTime(),
                auction.getCommitDeadline(),
                auction.getRevealDeadline(),
                winnerId,
                finalPrice
        );
    }
}
