package efub.awa.moamoa.global.websocket.plaza.entity;

import efub.awa.moamoa.item.entity.Item;

public record EquippedItem(
        Long itemId,
        String itemImageUrl
) {
    public static EquippedItem from (Item item) {
        return new EquippedItem(item.getId(), item.getImageUrl());
    }
}
