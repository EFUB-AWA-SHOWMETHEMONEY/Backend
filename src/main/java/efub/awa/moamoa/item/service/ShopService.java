package efub.awa.moamoa.item.service;

import efub.awa.moamoa.item.dto.response.ShopItemListResponse;
import efub.awa.moamoa.item.dto.response.ItemResponse;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ItemRepository itemRepository;
    @Transactional(readOnly = true)
    public ShopItemListResponse getShopItemList() {
        List<Item> items = itemRepository.findAllSaleItems();

        List<ItemResponse> itemResponses = items.stream()
                .map(item -> {
                    //TODO: S3 이미지 URL 수정 필요
                    String s3ImageUrl = item.getImageUrl();
                    return ItemResponse.of(item, s3ImageUrl);
                })
                .toList();
        return ShopItemListResponse.of(itemResponses, itemResponses.size());
    }

    @Transactional(readOnly = true)
    public ShopItemListResponse searchShopItemList(String keyword) {
        List<Item> items = itemRepository.search(keyword);

        List<ItemResponse> itemResponses = items.stream()
                .map(item -> {
                    //TODO: S3 이미지 URL 수정 필요
                    String s3ImageUrl = item.getImageUrl();
                    return ItemResponse.of(item, s3ImageUrl);
                })
                .toList();
        return ShopItemListResponse.of(itemResponses, itemResponses.size());
    }
}
