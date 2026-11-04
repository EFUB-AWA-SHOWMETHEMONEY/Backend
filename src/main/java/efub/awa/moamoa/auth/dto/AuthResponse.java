package efub.awa.moamoa.auth.dto;

import java.time.LocalDateTime;

public record AuthResponse(
        Long memberId,
        String nickname,
        String accessToken,
        Long expiresIn,
        LocalDateTime expiresAt
) {
    public static AuthResponse of(Long memberId, String nickname, IssuedTokens tokens) {
        return new AuthResponse(memberId, nickname, tokens.accessToken(), tokens.expiresIn(), tokens.expiresAt());
    }
}
