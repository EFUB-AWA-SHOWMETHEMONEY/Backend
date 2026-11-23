package efub.awa.moamoa.auction.service;

public interface AuctionParticipantChecker {
    boolean isParticipant(Long auctionId, Long memberId);
}
