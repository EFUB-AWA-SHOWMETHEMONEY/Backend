package efub.awa.moamoa.item.repository;

import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByIdAndMemberId(Long inventoryId, Long memberId);
    List<Inventory> findAllByMemberId(Long memberId);
    List<Inventory> findAllByMemberIdAndItemNameContainingOrderById(Long memberId, String keyword);

    @Query("""
            select inv from Inventory inv
            where inv.member.id = :memberId
                and inv.isEquipped = true
    """)
    List<Inventory> findEquippedItem(Long memberId);
    @Query("""
            select inv.item from Inventory inv
            where inv.member.id = :memberId
                and inv.isEquipped = true
            order by inv.id desc
            """)
    List<Item> findEquippedItems(@Param("memberId") Long memberId);
}
