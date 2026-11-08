package efub.awa.moamoa.item.service;

import efub.awa.moamoa.item.dto.response.InventoryListResponse;
import efub.awa.moamoa.item.dto.response.InventoryResponse;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.event.ItemEquippedEvent;
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
        List<Inventory> inventoryList = inventoryRepository.findAllByMemberId(memberId);
        List<InventoryResponse> inventoryResponse = toInventoryResponses(memberId, inventoryList);

        log.info("인벤토리 목록 조회 성공 - memberId: {}", memberId);
        return InventoryListResponse.of(inventoryResponse, inventoryResponse.size());

    }

    @Transactional(readOnly = true)
    public InventoryListResponse searchInventory(Long memberId, String keyword) {
        List<Inventory> inventoryList = inventoryRepository.findAllByMemberIdAndItemNameContainingOrderById(memberId, keyword);
        List<InventoryResponse> inventoryResponse = toInventoryResponses(memberId, inventoryList);

        log.info("인벤토리 검색 성공 - memberId: {}, keyword: {}", memberId, keyword);
        return InventoryListResponse.of(inventoryResponse, inventoryResponse.size());
    }

    //TODO: 추후 아이템 장착 구현
    @Transactional
    public void equip(Long memberId, Long inventoryId) {

//        applicationEventPublisher.publishEvent(new ItemEquippedEvent(memberId, item.getId(), item.getImageUrl()));
    }

    public List<Item> findEquippedItems(Long memberId) {
        return inventoryRepository.findEquippedItems(memberId);
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
