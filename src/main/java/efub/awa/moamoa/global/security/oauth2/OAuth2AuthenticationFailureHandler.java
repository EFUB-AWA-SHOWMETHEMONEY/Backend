package efub.awa.moamoa.global.security.oauth2;

import efub.awa.moamoa.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//소셜 로그인 실패 시 프론트 실패 페이지로 리다이렉트: ?error={errorCode}
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final FrontendRedirectUrls frontendRedirectUrls;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String oauthErrorCode = exception instanceof OAuth2AuthenticationException oauthException
                ? oauthException.getError().getErrorCode()
                : "";
        ErrorCode errorCode = resolveErrorCode(oauthErrorCode);

        if (errorCode == ErrorCode.INTERNAL_SERVER_ERROR) {
            //사용자가 해결할 수 없는 설정 문제일 가능성이 높다
            log.error("소셜 로그인 실패 - oauthError: {}", oauthErrorCode, exception);
        } else {
            log.warn("소셜 로그인 실패 - errorCode: {}, oauthError: {}", errorCode.name(), oauthErrorCode);
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(frontendRedirectUrls.loginFailureUrl(errorCode.name()));
    }

    private ErrorCode resolveErrorCode(String oauthErrorCode) {
        return switch (oauthErrorCode) {
            //소셜 서버와 통신하지 못함
            case "invalid_token_response", "invalid_user_info_response", "server_error", "temporarily_unavailable" ->
                    ErrorCode.OAUTH_SERVER_ERROR;
            //서버 설정 문제 (클라이언트 ID/시크릿, 스코프, 사용자 정보 설정 등) 또는 알 수 없는 예외
            case "", "invalid_client", "unauthorized_client", "invalid_scope",
                 "missing_user_info_uri", "missing_user_name_attribute" -> ErrorCode.INTERNAL_SERVER_ERROR;
            //사용자 취소, 인가코드 오류/만료/재사용, state 불일치 등 인증 실패
            default -> ErrorCode.INVALID_AUTHORIZATION_CODE;
        };
    }
}
