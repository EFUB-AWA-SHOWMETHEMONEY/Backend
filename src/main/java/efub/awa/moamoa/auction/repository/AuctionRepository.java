package efub.awa.moamoa.auction.repository;

import efub.awa.moamoa.auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
