package efub.awa.moamoa.member.dto.response;

import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.member.entity.Member;

import java.util.List;

public record MemberInfoResponse(
        Long memberId,
        String name,
        String nickname,
        Integer age,
        List<EquippedItemResponse> equippedItemList
) {
    public static MemberInfoResponse of(Member member, List<EquippedItemResponse> equippedItemList) {
        return new MemberInfoResponse(
                member.getId(),
                member.getName(),
                member.getNickname(),
                member.getAge(),
                equippedItemList
        );
    }

    public record EquippedItemResponse(
            Long itemId,
            String imageUrl
    ) {
        public static EquippedItemResponse of(Item item, String imageUrl) {
            return new EquippedItemResponse(item.getId(), imageUrl);
        }
    }
}
