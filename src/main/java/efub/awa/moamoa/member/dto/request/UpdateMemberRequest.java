package efub.awa.moamoa.member.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

//부분 수정: null인 항목은 수정하지 않음 (name 등 정의되지 않은 필드는 무시)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateMemberRequest(

        //null 허용, 빈 문자열/공백만 입력/10자 초과는 거부 (공백만 입력 검사는 null/빈 문자열과 메시지가 겹치지 않도록 Pattern으로 분리)
        @Size(min = 1, max = 10, message = "닉네임은 1~10자여야 합니다.")
        @Pattern(regexp = "^(?!\\s+$)[\\s\\S]*$", message = "닉네임은 1~10자여야 합니다.")
        String nickname,

        @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
        Integer age
) {
    //수정할 값이 하나도 없는지 여부
    public boolean hasNoUpdate() {
        return nickname == null && age == null;
    }
}
