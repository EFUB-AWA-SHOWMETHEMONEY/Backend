package efub.awa.moamoa.global.security.oauth2;

import efub.awa.moamoa.auth.dto.IssuedTokens;
import efub.awa.moamoa.auth.service.TokenService;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.global.security.JwtProvider;
import efub.awa.moamoa.global.security.RefreshTokenCookieProvider;
import efub.awa.moamoa.global.security.RegisterTokenCookieProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//소셜 로그인 성공 후: 기존 회원이면 Refresh Token 쿠키, 신규 회원이면 registerToken 쿠키를 내려주고 프론트로 리다이렉트
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;
    private final RegisterTokenCookieProvider registerTokenCookieProvider;
    private final FrontendRedirectUrls frontendRedirectUrls;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String redirectUrl;
        try {
            redirectUrl = handle(response, authentication);
        } catch (RuntimeException e) {
            log.error("소셜 로그인 후처리 중 예외 발생", e);
            redirectUrl = frontendRedirectUrls.loginFailureUrl(ErrorCode.INTERNAL_SERVER_ERROR.name());
        }

        //이후 API는 JWT로만 인증하므로 소셜 로그인 과정에서 만들어진 세션 인증 정보는 지운다
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(redirectUrl);
    }

    private String handle(HttpServletResponse response, Authentication authentication) {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        if (oAuth2User.isNewUser()) {
            String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            String registerToken = jwtProvider.createRegisterToken(registrationId, oAuth2User.getOauthId());
            response.addHeader(HttpHeaders.SET_COOKIE, registerTokenCookieProvider.create(registerToken).toString());

            log.info("소셜 로그인 - 신규 회원, 추가 가입 페이지로 이동");
            return frontendRedirectUrls.signupUrl();
        }

        IssuedTokens tokens = tokenService.issueTokens(oAuth2User.getMemberId());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(tokens.refreshToken()).toString());

        log.info("소셜 로그인 성공 - memberId: {}", oAuth2User.getMemberId());
        return frontendRedirectUrls.loginSuccessUrl();
    }
}
