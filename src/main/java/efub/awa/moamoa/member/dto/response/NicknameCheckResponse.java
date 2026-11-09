package efub.awa.moamoa.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NicknameCheckResponse(
        //true: 이미 사용 중, false: 사용 가능
        @JsonProperty("isDuplicated")
        boolean isDuplicated
) {
    public static NicknameCheckResponse of(boolean isDuplicated) {
        return new NicknameCheckResponse(isDuplicated);
    }
}
