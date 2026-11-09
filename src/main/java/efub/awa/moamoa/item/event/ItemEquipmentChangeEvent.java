package efub.awa.moamoa.item.event;

public record ItemEquipmentChangeEvent(
        Long memberId,
        Long itemId,
        String itemImageUrl
) {
}
