package efub.awa.moamoa.auction.dto.response;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.member.entity.Member;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuctionSummaryResponse(
        Long auctionId,
        AuctionType type,
        AuctionStatus status,
        Long startPrice,
        LocalDateTime readyStartTime,
        LocalDateTime entryDeadline,
        ItemInfo itemInfo,
        OwnerInfo auctionOwner
) {

    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    public record ItemInfo(Long inventoryId, String itemName, String imageUrl) {
    }

    public record OwnerInfo(Long memberId, String nickname) {
    }

    public static AuctionSummaryResponse from(Auction auction) {
        Item item = auction.getInventory().getItem();
        //TODO: S3 이미지 URL 수정 필요 (인벤토리 목록과 동일)
        String s3ImageUrl = item.getImageUrl();

        return AuctionSummaryResponse.builder()
                .auctionId(auction.getId())
                .type(auction.getType())
                .status(auction.getStatus())
                .startPrice(auction.getStartPrice())
                .readyStartTime(auction.getReadyStartTime())
                .entryDeadline(auction.getEntryDeadline())
                .itemInfo(new ItemInfo(auction.getInventory().getId(), item.getName(), s3ImageUrl))
                .auctionOwner(toOwnerInfo(auction.getAuctionOwner()))
                .build();
    }

    //등록자가 탈퇴하면 회원 참조가 null이 되므로 "탈퇴한 사용자"로 표시
    private static OwnerInfo toOwnerInfo(Member owner) {
        if (owner == null) {
            return new OwnerInfo(null, WITHDRAWN_NICKNAME);
        }
        return new OwnerInfo(owner.getId(), owner.getNickname());
    }
}
