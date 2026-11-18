package efub.awa.moamoa.auction.dto.response;

import java.util.List;

public record AuctionListResponse(
        List<AuctionSummaryResponse> auctions,
        int totalCount
) {
    public static AuctionListResponse of(List<AuctionSummaryResponse> auctions, int totalCount) {
        return new AuctionListResponse(auctions, totalCount);
    }
}
