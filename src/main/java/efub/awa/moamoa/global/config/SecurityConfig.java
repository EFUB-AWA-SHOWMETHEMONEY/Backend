package efub.awa.moamoa.global.config;

import efub.awa.moamoa.global.security.JwtAuthenticationEntryPoint;
import efub.awa.moamoa.global.security.JwtAuthenticationFilter;
import efub.awa.moamoa.global.security.JwtProvider;
import efub.awa.moamoa.global.security.oauth2.CustomOAuth2UserService;
import efub.awa.moamoa.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import efub.awa.moamoa.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    //인증이 필요한 API(로그아웃)만 막고 나머지는 열어 둠 (다른 도메인 API의 인증 적용은 협의 후 수정)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtProvider jwtProvider,
                                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                                           CustomOAuth2UserService customOAuth2UserService,
                                           OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                                           OAuth2AuthenticationFailureHandler oAuth2FailureHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/ws/**",
                                "/api/auth/signup", "/api/auth/login",
                                "/api/auth/social/**", "/api/auth/reissue").permitAll()
                        //닉네임 중복확인: 회원가입/소셜 추가가입 중에는 Access Token이 없으므로 공개 API
                        //(닉네임은 광장 등에서 이미 공개되는 정보이므로 새로 노출되는 정보 없음)
                        .requestMatchers(HttpMethod.GET, "/api/members/nickname-check").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                //@Component로 등록하면 서블릿 필터로 중복 등록되므로 여기서 직접 생성
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
