package efub.awa.moamoa.item.dto.response;

import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.enums.ItemGrade;
import lombok.Builder;

@Builder
public record InventoryResponse (
        Long inventoryId,
        Long itemId,
        String name,
        String imageUrl,
        ItemGrade grade,
        Boolean isEquipped
) {
    public static InventoryResponse of (Long inventoryId, Item item, String s3ImageUrl, boolean isEquipped) {
        return InventoryResponse.builder()
                .inventoryId(inventoryId)
                .itemId(item.getId())
                .name(item.getName())
                .imageUrl(s3ImageUrl)
                .grade(item.getGrade())
                .isEquipped(isEquipped)
                .build();
    }
}
