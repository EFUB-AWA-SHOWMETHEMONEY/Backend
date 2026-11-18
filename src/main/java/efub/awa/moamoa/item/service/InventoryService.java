package efub.awa.moamoa.item.service;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.dto.response.InventoryListResponse;
import efub.awa.moamoa.item.dto.response.InventoryResponse;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.event.ItemEquipmentChangeEvent;
import efub.awa.moamoa.item.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventoryListResponse getInventoryList(Long memberId) {
        //수량이 0인 아이템(경매에 올려 소진된 아이템)은 목록에서 제외 (경매 등록 시 인벤토리 행은 삭제하지 않고 수량만 차감하기 때문)
        List<Inventory> inventoryList = inventoryRepository.findAllByMemberIdAndQuantityGreaterThan(memberId, 0L);
        List<InventoryResponse> inventoryResponse = toInventoryResponses(memberId, inventoryList);

        log.info("인벤토리 목록 조회 성공 - memberId: {}", memberId);
        return InventoryListResponse.of(inventoryResponse, inventoryResponse.size());

    }

    @Transactional(readOnly = true)
    public InventoryListResponse searchInventory(Long memberId, String keyword) {
        //수량이 0인 아이템은 검색 결과에서도 제외 (목록 조회와 같은 이유)
        List<Inventory> inventoryList = inventoryRepository.findAllByMemberIdAndQuantityGreaterThanAndItemNameContainingOrderById(memberId, 0L, keyword);
        List<InventoryResponse> inventoryResponse = toInventoryResponses(memberId, inventoryList);

        log.info("인벤토리 검색 성공 - memberId: {}, keyword: {}", memberId, keyword);
        return InventoryListResponse.of(inventoryResponse, inventoryResponse.size());
    }

    @Transactional
    public InventoryResponse toggleEquipped(Long memberId, Long inventoryId) {
        Inventory inventory = getInventory(memberId, inventoryId);
        //수량이 0인 아이템(경매에 올려 소진됨)은 인벤토리에서 보이지 않으므로 장착 대상에서도 제외
        if (inventory.getQuantity() <= 0) {
            log.warn("수량이 0인 인벤토리 아이템 장착 시도 - inventoryId: {}", inventoryId);
            throw new CustomException(ErrorCode.INVENTORY_NOT_FOUND);
        }
        Item item = inventory.getItem();
        //TODO: S3 이미지 수정 필요
        String s3ImageUrl = item.getImageUrl();

        if (!inventory.isEquipped()) {
            //이미 장착한 아이템 해제
            findEquippedInventory(memberId).forEach(Inventory::toggleEquipped);
            inventory.toggleEquipped();
            applicationEventPublisher.publishEvent(new ItemEquipmentChangeEvent(memberId, item.getId(), s3ImageUrl));
        }
        else {
            inventory.toggleEquipped();
            applicationEventPublisher.publishEvent(new ItemEquipmentChangeEvent(memberId, null, null));
        }

        return InventoryResponse.of(inventoryId, item, s3ImageUrl, inventory.isEquipped());

    }

    //경매 등록용: 본인 소유 인벤토리 행을 잠그고 조회해 장착 여부를 검증한 뒤 수량을 1 차감한다.
    //(같은 아이템을 동시에 두 번 올리는 것을 막기 위해 행 잠금을 사용하며, 호출하는 쪽의 트랜잭션에 포함된다)
    @Transactional
    public Inventory reserveForAuction(Long memberId, Long inventoryId) {
        Inventory inventory = inventoryRepository.findByIdAndMemberIdForUpdate(inventoryId, memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않거나 본인 소유가 아닌 인벤토리 아이템 - memberId: {}, inventoryId: {}", memberId, inventoryId);
                    return new CustomException(ErrorCode.INVENTORY_NOT_FOUND);
                });

        if (inventory.isEquipped()) {
            log.warn("장착 중인 아이템 경매 등록 시도 - memberId: {}, inventoryId: {}", memberId, inventoryId);
            throw new CustomException(ErrorCode.ITEM_EQUIPPED);
        }

        inventory.decreaseQuantity();
        return inventory;
    }

    public List<Inventory> findEquippedInventory(Long memberId) {
        return inventoryRepository.findEquippedItem(memberId);
    }

    public List<Item> findEquippedItems(Long memberId) {
        return inventoryRepository.findEquippedItems(memberId);
    }

    private Inventory getInventory(Long memberId, Long inventoryId) {
        return inventoryRepository.findByIdAndMemberId(inventoryId, memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 인벤토리 아이템 - inventoryId: {}", inventoryId);
                    return new CustomException(ErrorCode.INVENTORY_NOT_FOUND);
                });
    }

    private List<InventoryResponse> toInventoryResponses(Long memberId, List<Inventory> inventoryList) {
        List<Item> equippedItems = findEquippedItems(memberId);
        return inventoryList.stream()
                .map(inventory -> {
                    Item item = inventory.getItem();
                    //TODO: S3 이미지 링크 받아오는걸로 수정
                    String s3ImageUrl = item.getImageUrl();
                    return InventoryResponse.of (inventory.getId(), item, s3ImageUrl, inventory.isEquipped());
                })
                .toList();
    }
}
