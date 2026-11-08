package efub.awa.moamoa.item.repository;

import efub.awa.moamoa.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
