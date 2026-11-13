package efub.awa.moamoa.websocket.auth;

import efub.awa.moamoa.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtStompTokenVerifier implements StompTokenVerifier {
    private final JwtProvider jwtProvider;

    //Access Token 검증 후 memberId 반환 (만료/위조 시 JwtProvider가 CustomException을 던짐)
    @Override
    public Long verify(String token) {
        return jwtProvider.parseAccessToken(token);
    }
}
