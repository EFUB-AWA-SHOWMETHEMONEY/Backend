package efub.awa.moamoa.auth.dto;

import java.time.LocalDateTime;

//서버 내부에서 발급 결과를 전달하는 용도 (응답에 그대로 노출하지 않음)
public record IssuedTokens(
        String accessToken,
        long expiresIn,
        LocalDateTime expiresAt,
        String refreshToken
) {
}
