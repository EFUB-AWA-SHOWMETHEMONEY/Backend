package efub.awa.moamoa.global.security.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

//소셜 로그인 후 리다이렉트할 프론트 주소 (app.frontend.* 설정)
@Component
public class FrontendRedirectUrls {

    private final String baseUrl;
    private final String loginSuccessPath;
    private final String signupPath;
    private final String loginFailurePath;

    public FrontendRedirectUrls(@Value("${app.frontend.base-url}") String baseUrl,
                                @Value("${app.frontend.login-success-path}") String loginSuccessPath,
                                @Value("${app.frontend.signup-path}") String signupPath,
                                @Value("${app.frontend.login-failure-path}") String loginFailurePath) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.loginSuccessPath = loginSuccessPath;
        this.signupPath = signupPath;
        this.loginFailurePath = loginFailurePath;
    }

    public String loginSuccessUrl() {
        return baseUrl + loginSuccessPath;
    }

    public String signupUrl() {
        return baseUrl + signupPath;
    }

    public String loginFailureUrl(String errorCode) {
        return UriComponentsBuilder.fromUriString(baseUrl + loginFailurePath)
                .queryParam("error", errorCode)
                .build()
                .toUriString();
    }
}
