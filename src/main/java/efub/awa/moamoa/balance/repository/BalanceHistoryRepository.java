package efub.awa.moamoa.balance.repository;

import efub.awa.moamoa.balance.entity.BalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory, Long> {
}
