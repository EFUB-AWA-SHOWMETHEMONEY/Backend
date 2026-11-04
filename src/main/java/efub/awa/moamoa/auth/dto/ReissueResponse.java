package efub.awa.moamoa.auth.dto;

import java.time.LocalDateTime;

public record ReissueResponse(
        String accessToken,
        Long expiresIn,
        LocalDateTime expiresAt
) {
    public static ReissueResponse of(IssuedTokens tokens) {
        return new ReissueResponse(tokens.accessToken(), tokens.expiresIn(), tokens.expiresAt());
    }
}
