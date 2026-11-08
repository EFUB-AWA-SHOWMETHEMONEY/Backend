package efub.awa.moamoa.item.dto.response;

import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.enums.ItemGrade;
import lombok.Builder;

import java.util.List;

public record InventoryListResponse(
        List<InventoryResponse> inventoryList,
        int totalCount
) {
    public static InventoryListResponse of (List<InventoryResponse> inventoryList, int totalCount) {
        return new InventoryListResponse(inventoryList, totalCount);
    }
}
