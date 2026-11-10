package efub.awa.moamoa.member.dto.response;

import efub.awa.moamoa.member.entity.Member;

public record MemberMoneyResponse(
        Long memberId,
        //회원이 소유한 돈 (Member.capital)
        Long money
) {
    public static MemberMoneyResponse of(Member member) {
        return new MemberMoneyResponse(member.getId(), member.getCapital());
    }
}
