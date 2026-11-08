package efub.awa.moamoa.item.dto.response;

import java.util.List;

public record ShopItemListResponse(
        List<ItemResponse> itemList,
        int totalCount
) {
    public static ShopItemListResponse of (List<ItemResponse> itemResponses, int totalCount) {
        return new ShopItemListResponse(itemResponses, totalCount);
    }
}

