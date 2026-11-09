package efub.awa.moamoa.item.dto.response;

import efub.awa.moamoa.item.entity.Inventory;

public record BuyItemResponse(
        Long memberId,
        Long money,
        Long inventoryId
) {
    public static BuyItemResponse of (Long memberId, Long afterBalance, Inventory inventory) {
        return new BuyItemResponse(memberId, afterBalance, inventory.getId());
    }
}
