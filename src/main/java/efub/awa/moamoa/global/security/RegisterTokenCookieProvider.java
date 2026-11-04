package efub.awa.moamoa.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RegisterTokenCookieProvider {

    public static final String COOKIE_NAME = "registerToken";

    //소셜 추가 가입 API에만 쿠키가 전송되도록 경로를 한정
    private static final String COOKIE_PATH = "/api/auth/social/signup";

    private final JwtProvider jwtProvider;

    public ResponseCookie create(String registerToken) {
        return ResponseCookie.from(COOKIE_NAME, registerToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(jwtProvider.getRegisterTokenExpirationSeconds()))
                .build();
    }

    //가입 완료 후 쿠키 삭제 (Max-Age=0)
    public ResponseCookie expire() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
