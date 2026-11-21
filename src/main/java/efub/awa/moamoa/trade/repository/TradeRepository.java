package efub.awa.moamoa.trade.repository;

import efub.awa.moamoa.trade.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
