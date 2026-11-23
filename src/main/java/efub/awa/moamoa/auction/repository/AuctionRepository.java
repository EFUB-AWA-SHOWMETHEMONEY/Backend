package efub.awa.moamoa.auction.repository;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    //경매 목록 조회: 아이템과 등록자를 fetch join으로 한 번에 가져와 목록 건수만큼 쿼리가 늘어나는 것(N+1)을 막는다.
    //등록자는 회원 탈퇴 시 null이 될 수 있어 left join
    @Query("""
            select a from Auction a
            join fetch a.inventory inv
            join fetch inv.item
            left join fetch a.auctionOwner
            where a.status in :statuses
            order by a.readyStartTime asc, a.id asc
            """)
    List<Auction> findAllByStatusInWithItemAndOwner(@Param("statuses") List<AuctionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :auctionId")
    Optional<Auction> findByIdForUpdate(@Param("auctionId") Long auctionId);
}
