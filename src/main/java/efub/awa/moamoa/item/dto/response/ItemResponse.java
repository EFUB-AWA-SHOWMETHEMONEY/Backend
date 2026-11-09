package efub.awa.moamoa.item.dto.response;

import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.enums.ItemGrade;
import lombok.Builder;

@Builder
public record ItemResponse(
        Long itemId,
        String name,
        String imageUrl,
        ItemGrade grade,
        Long price
) {
    public static ItemResponse of (Item item, String s3ImageUrl) {
        return ItemResponse.builder()
                .itemId(item.getId())
                .name(item.getName())
                .imageUrl(s3ImageUrl)
                .grade(item.getGrade())
                .price(item.getPrice())
                .build();
    }
}