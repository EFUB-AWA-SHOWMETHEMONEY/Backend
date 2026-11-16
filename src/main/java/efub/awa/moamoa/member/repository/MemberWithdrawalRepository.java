package efub.awa.moamoa.member.repository;

import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.trade.enums.TradeStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

//회원 탈퇴 가능 여부 판단용 조회 (경매/거래 도메인의 repository를 수정하지 않도록 member 쪽에서 직접 조회)
@Repository
@RequiredArgsConstructor
public class MemberWithdrawalRepository {

    //진행 중인 경매: 준비 중, 진행 중
    private static final List<AuctionStatus> IN_PROGRESS_AUCTION_STATUSES = List.of(AuctionStatus.READY, AuctionStatus.OPEN);
    //진행 중인 거래: 대기, 수락
    private static final List<TradeStatus> IN_PROGRESS_TRADE_STATUSES = List.of(TradeStatus.PENDING, TradeStatus.ACCEPTED);

    private final EntityManager entityManager;

    //등록자, 최고 입찰자, 공개/비공개 입찰자로 참여 중인 진행 중 경매가 있는지
    public boolean existsInProgressAuction(Long memberId) {
        return exists("""
                select a.id from Auction a
                where a.status in :statuses
                    and (a.auctionOwner.id = :memberId or a.currentBidder.id = :memberId)
                """, memberId, IN_PROGRESS_AUCTION_STATUSES)
                || exists("""
                select b.id from PublicBid b
                where b.bidder.id = :memberId and b.auction.status in :statuses
                """, memberId, IN_PROGRESS_AUCTION_STATUSES)
                || exists("""
                select b.id from PrivateBid b
                where b.bidder.id = :memberId and b.auction.status in :statuses
                """, memberId, IN_PROGRESS_AUCTION_STATUSES);
    }

    //판매자 또는 구매자로 참여 중인 진행 중 거래가 있는지
    public boolean existsInProgressTrade(Long memberId) {
        return exists("""
                select t.id from Trade t
                where t.status in :statuses
                    and (t.seller.id = :memberId or t.buyer.id = :memberId)
                """, memberId, IN_PROGRESS_TRADE_STATUSES);
    }

    private boolean exists(String jpql, Long memberId, List<?> statuses) {
        return !entityManager.createQuery(jpql, Long.class)
                .setParameter("memberId", memberId)
                .setParameter("statuses", statuses)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }
}
