package efub.awa.moamoa.auth.dto;

//서비스 -> 컨트롤러 전달용: 응답 Body와 쿠키로 내려줄 Refresh Token
public record AuthResult(
        AuthResponse response,
        String refreshToken
) {
}
