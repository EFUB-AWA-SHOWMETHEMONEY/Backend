package efub.awa.moamoa.auction.repository;

import efub.awa.moamoa.auction.entity.PrivateBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrivateBidRepository extends JpaRepository<PrivateBid, Long> {
    Optional<PrivateBid> findByAuctionIdAndBidderIdAndActiveTrue(Long auctionId, Long bidderId);
}
