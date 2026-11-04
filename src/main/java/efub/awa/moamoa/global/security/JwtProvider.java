package efub.awa.moamoa.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_PROVIDER = "provider";
    private static final String TYPE_REGISTER = "register";

    private final SecretKey key;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;
    private final long registerTokenExpirationSeconds;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-expiration}") long accessTokenExpirationSeconds,
                       @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationSeconds,
                       @Value("${jwt.register-token-expiration}") long registerTokenExpirationSeconds) {
        //HS256은 32바이트 이상 키가 필요 (부족하면 서버 시작 시 예외 발생)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.registerTokenExpirationSeconds = registerTokenExpirationSeconds;
    }

    //Access Token: sub(memberId), iat, exp
    public AccessToken createAccessToken(Long memberId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(accessTokenExpirationSeconds);

        String token = Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new AccessToken(token, accessTokenExpirationSeconds,
                LocalDateTime.ofInstant(expiresAt, ZONE_ID).truncatedTo(ChronoUnit.SECONDS));
    }

    //Refresh Token: sub(memberId), iat, exp, jti(발급마다 달라지도록)
    public String createRefreshToken(Long memberId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(refreshTokenExpirationSeconds);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    //Register Token: sub(소셜 회원번호), provider, type=register, iat, exp
    //소셜 로그인에서 신규 회원으로 확인된 사용자가 추가 가입을 마칠 때까지만 쓰는 단기 토큰
    public String createRegisterToken(String provider, String oauthId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(registerTokenExpirationSeconds);

        return Jwts.builder()
                .subject(oauthId)
                .claim(CLAIM_PROVIDER, provider)
                .claim(CLAIM_TYPE, TYPE_REGISTER)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    //위조, 형식 오류, 만료, 토큰 종류 불일치는 모두 빈 값으로 돌려준다
    public Optional<RegisterTokenClaims> parseRegisterToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_REGISTER.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            String provider = claims.get(CLAIM_PROVIDER, String.class);
            String oauthId = claims.getSubject();
            if (provider == null || oauthId == null) {
                return Optional.empty();
            }
            return Optional.of(new RegisterTokenClaims(provider, oauthId));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    public long getRegisterTokenExpirationSeconds() {
        return registerTokenExpirationSeconds;
    }

    public record AccessToken(String token, long expiresIn, LocalDateTime expiresAt) {
    }

    public record RegisterTokenClaims(String provider, String oauthId) {
    }
}
