package efub.awa.moamoa.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SocialSignUpRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 20, message = "이름은 1~20자여야 합니다.")
        String name,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 10, message = "닉네임은 1~10자여야 합니다.")
        String nickname,

        @NotNull(message = "나이는 필수입니다.")
        @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
        Integer age
) {
}
