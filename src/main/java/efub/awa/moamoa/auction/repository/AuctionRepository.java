package efub.awa.moamoa.auction.repository;

import efub.awa.moamoa.auction.entity.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :auctionId")
    Optional<Auction> findByIdForUpdate(@Param("auctionId") Long auctionId);
}
