package efub.awa.moamoa.item.service;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.dto.response.ItemDetailResponse;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final InventoryService inventoryService;

    public ItemDetailResponse getItem(Long memberId, Long itemId) {
        Item item = getItemById(itemId);
        String s3ImageUrl = item.getImageUrl();
        boolean isEquipped = inventoryService.findEquippedItems(memberId).contains(item);

        log.info("아이템 상세조회 성공 - memberId: {}, itemId: {}", memberId, itemId);
        return ItemDetailResponse.from(item, s3ImageUrl, isEquipped);
    }

    private Item getItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 아이템 - itemId: {}", itemId);
                    return new CustomException(ErrorCode.ITEM_NOT_FOUND);
                });
    }

}
