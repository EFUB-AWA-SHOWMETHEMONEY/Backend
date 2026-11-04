package efub.awa.moamoa.auth.controller;

import efub.awa.moamoa.auth.dto.AuthResponse;
import efub.awa.moamoa.auth.dto.AuthResult;
import efub.awa.moamoa.auth.dto.LoginRequest;
import efub.awa.moamoa.auth.dto.SignUpRequest;
import efub.awa.moamoa.auth.dto.SocialSignUpRequest;
import efub.awa.moamoa.auth.service.AuthService;
import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.global.security.RefreshTokenCookieProvider;
import efub.awa.moamoa.global.security.RegisterTokenCookieProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;
    private final RegisterTokenCookieProvider registerTokenCookieProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("회원가입 요청");
        AuthResult result = authService.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(result.refreshToken()).toString())
                .body(ApiResponse.success("회원가입 요청이 성공했습니다.", result.response()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청");
        AuthResult result = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(result.refreshToken()).toString())
                .body(ApiResponse.success("로그인 요청이 성공했습니다.", result.response()));
    }

    @PostMapping("/social/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> socialSignUp(
            @CookieValue(name = RegisterTokenCookieProvider.COOKIE_NAME, required = false) String registerToken,
            @Valid @RequestBody SocialSignUpRequest request) {
        log.info("소셜 회원가입 요청");
        AuthResult result = authService.socialSignUp(registerToken, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(result.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, registerTokenCookieProvider.expire().toString())
                .body(ApiResponse.success("소셜 회원가입 요청이 성공했습니다.", result.response()));
    }
}
