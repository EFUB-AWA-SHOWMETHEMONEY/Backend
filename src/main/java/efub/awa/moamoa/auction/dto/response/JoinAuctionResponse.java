package efub.awa.moamoa.auction.dto.response;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record JoinAuctionResponse(
        Long auctionId,
        AuctionType type,
        AuctionStatus status,
        Long startPrice,
        Long currentPrice,
        int currentParticipants,
        Integer maxParticipants,
        LocalDateTime readyStartTime,
        LocalDateTime entryDeadline,
        LocalDateTime commitDeadline,
        LocalDateTime revealDeadline
) {
    public static JoinAuctionResponse of(Auction auction, int currentParticipants) {
        boolean ready = auction.getStatus() == AuctionStatus.READY;
        boolean open = auction.getStatus() == AuctionStatus.OPEN;
        boolean isPublic = auction.getType() == AuctionType.PUBLIC;

        return JoinAuctionResponse.builder()
                .auctionId(auction.getId())
                .type(auction.getType())
                .status(auction.getStatus())
                .startPrice(auction.getStartPrice())
                //PRIVATE은 입찰 금액을 마감 전까지 공개하지 않으므로 현재가를 내려주지 않는다
                .currentPrice(isPublic ? auction.getCurrentPrice() : null)
                .currentParticipants(currentParticipants)
                .maxParticipants(auction.getMaxParticipants())
                .readyStartTime(auction.getReadyStartTime())
                //입장 대기 마감은 READY일 때만, 입찰/커밋 마감은 OPEN일 때만, 리빌 마감은 PRIVATE이고 OPEN일 때만 내려준다
                .entryDeadline(ready ? auction.getEntryDeadline() : null)
                .commitDeadline(open ? auction.getCommitDeadline() : null)
                .revealDeadline(open && !isPublic ? auction.getRevealDeadline() : null)
                .build();
    }
}
