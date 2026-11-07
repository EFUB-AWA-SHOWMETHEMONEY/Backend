package efub.awa.moamoa.websocket.plaza.entity;

import efub.awa.moamoa.item.entity.Item;

public record EquippedItem(
        Long itemId,
        String itemImageUrl
) {
    public static EquippedItem from (Item item) {
        return new EquippedItem(item.getId(), item.getImageUrl());
    }
}
