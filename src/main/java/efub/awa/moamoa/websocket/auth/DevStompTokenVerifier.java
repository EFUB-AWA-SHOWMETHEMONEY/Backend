package efub.awa.moamoa.websocket.auth;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
//TODO: JWT PR 머지 후 삭제하고 JwtStompTokenVerifier로 교체
public class DevStompTokenVerifier implements StompTokenVerifier {
    @Override
    public Long verify (String token) {
        try {
            return Long.parseLong(token); //토큰 칸에 1,2 같은 숫자 넣어서 테스트
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
        }
    }
}
