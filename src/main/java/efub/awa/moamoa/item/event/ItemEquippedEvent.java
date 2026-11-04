package efub.awa.moamoa.item.event;

public record ItemEquippedEvent(
        Long memberId,
        Long itemId,
        String itemImageUrl
) {
}
