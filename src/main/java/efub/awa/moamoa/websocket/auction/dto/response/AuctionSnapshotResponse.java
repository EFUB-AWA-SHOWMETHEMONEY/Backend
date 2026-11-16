package efub.awa.moamoa.websocket.auction.dto.response;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;

import java.util.List;

public record AuctionSnapshotResponse(
        long snapshotAt,
        Long auctionId,
        AuctionItemResponse auctionItem,
        int participantCount,
        List<AuctionParticipantResponse> participants
) {
    public static AuctionSnapshotResponse of(Auction auction, AuctionItemResponse auctionItem, List<AuctionParticipantResponse> participants, long snapshotAt){
        return new AuctionSnapshotResponse(
                snapshotAt,
                auction.getId(),
                auctionItem,
                participants.size(),
                participants
        );
    }
}
