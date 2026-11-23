package efub.awa.moamoa.auction.dto.response;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CreateAuctionResponse(
        Long auctionId,
        Long inventoryId,
        AuctionType type,
        AuctionStatus status,
        Long startPrice,
        Long currentPrice,
        Integer maxParticipants,
        LocalDateTime readyStartTime,
        LocalDateTime entryDeadline,
        LocalDateTime openStartTime,
        LocalDateTime commitDeadline,
        LocalDateTime revealDeadline,
        Long version
) {
    public static CreateAuctionResponse from(Auction auction) {
        return CreateAuctionResponse.builder()
                .auctionId(auction.getId())
                .inventoryId(auction.getInventory().getId())
                .type(auction.getType())
                .status(auction.getStatus())
                .startPrice(auction.getStartPrice())
                //PRIVATE은 입찰 금액을 마감 전까지 공개하지 않으므로 현재가를 내려주지 않는다 (DB에는 시작가로 저장됨)
                .currentPrice(auction.getType() == AuctionType.PUBLIC ? auction.getCurrentPrice() : null)
                .maxParticipants(auction.getMaxParticipants())
                .readyStartTime(auction.getReadyStartTime())
                .entryDeadline(auction.getEntryDeadline())
                .openStartTime(auction.getOpenStartTime())
                .commitDeadline(auction.getCommitDeadline())
                .revealDeadline(auction.getRevealDeadline())
                .version(auction.getVersion())
                .build();
    }
}
