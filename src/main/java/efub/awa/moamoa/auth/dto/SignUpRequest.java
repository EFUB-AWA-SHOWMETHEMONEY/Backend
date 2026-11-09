package efub.awa.moamoa.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

        @NotNull(message = "아이디는 필수입니다.")
        @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "아이디는 4~20자의 영문, 숫자여야 합니다.")
        String loginId,

        //8~20자, 영문 1자 이상 + 특수문자 1자 이상 포함, 숫자 허용, 공백 불가 (영문/숫자/ASCII 특수문자만 허용)
        @NotNull(message = "비밀번호는 필수입니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[!-/:-@\\[-`{-~])[A-Za-z0-9!-/:-@\\[-`{-~]{8,20}$", message = "영문, 특수문자를 포함한 8~20자여야 합니다.")
        String password,

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
