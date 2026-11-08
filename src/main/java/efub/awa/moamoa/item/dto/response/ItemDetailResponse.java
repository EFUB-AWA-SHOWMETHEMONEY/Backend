package efub.awa.moamoa.item.dto.response;

import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.enums.ItemGrade;
import lombok.Builder;

@Builder
public record ItemDetailResponse(
        Long itemId,
        String name,
        String imageUrl,
        Long price,
        String description,
        ItemGrade grade,
        Boolean isEquipped,
        Boolean forSale
) {
    public static ItemDetailResponse from(Item item, String s3ImageUrl, boolean isEquipped) {
        return ItemDetailResponse.builder()
                .itemId(item.getId())
                .name(item.getName())
                .imageUrl(s3ImageUrl)
                .price(item.getPrice())
                .description(item.getDescription())
                .grade(item.getGrade())
                .isEquipped(isEquipped)
                .forSale(item.isForSale())
                .build();
    }
}
