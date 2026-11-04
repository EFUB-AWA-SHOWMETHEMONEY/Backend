package efub.awa.moamoa.item.repository;

import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("""
            select inv.item from Inventory inv
            where inv.member.id = :memberId
                and inv.isEquipped = true
            order by inv.id desc
            """)
    List<Item> findEquippedItems(@Param("memberId") Long memberId);
}
