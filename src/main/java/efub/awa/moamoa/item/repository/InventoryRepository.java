package efub.awa.moamoa.item.repository;

import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.entity.Item;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByIdAndMemberId(Long inventoryId, Long memberId);
    List<Inventory> findAllByMemberId(Long memberId);
    List<Inventory> findAllByMemberIdAndItemNameContainingOrderById(Long memberId, String keyword);
    Optional<Inventory> findByMemberIdAndItemId(Long memberId, Long itemId);

    //경매 등록으로 수량이 0이 된 행은 삭제하지 않고 유지하므로, 인벤토리 목록/검색에서는 수량이 0보다 큰 행만 조회
    List<Inventory> findAllByMemberIdAndQuantityGreaterThan(Long memberId, Long quantity);
    List<Inventory> findAllByMemberIdAndQuantityGreaterThanAndItemNameContainingOrderById(Long memberId, Long quantity, String keyword);

    //경매 등록 시 같은 아이템을 동시에 두 번 올려 수량이 중복 차감되는 것을 막기 위해 행을 잠그고 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inv from Inventory inv where inv.id = :inventoryId and inv.member.id = :memberId")
    Optional<Inventory> findByIdAndMemberIdForUpdate(@Param("inventoryId") Long inventoryId, @Param("memberId") Long memberId);

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
