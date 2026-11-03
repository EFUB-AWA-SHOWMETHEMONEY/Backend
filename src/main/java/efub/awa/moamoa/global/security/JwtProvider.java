package efub.awa.moamoa.global.security;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
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

    //Access Token: sub(memberId), type=access, iat, exp
    public AccessToken createAccessToken(Long memberId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(accessTokenExpirationSeconds);

        String token = Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new AccessToken(token, accessTokenExpirationSeconds,
                LocalDateTime.ofInstant(expiresAt, ZONE_ID).truncatedTo(ChronoUnit.SECONDS));
    }

    //Refresh Token: sub(memberId), type=refresh, iat, exp, jti(발급마다 달라지도록)
    public String createRefreshToken(Long memberId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(refreshTokenExpirationSeconds);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
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

    //Access Token 검증 후 memberId 반환 (만료: EXPIRED_ACCESS_TOKEN, 위조·형식 오류·종류 불일치: INVALID_ACCESS_TOKEN)
    public Long parseAccessToken(String token) {
        try {
            return parseMemberId(token, TYPE_ACCESS);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    //Refresh Token 검증 후 memberId 반환 (만료: EXPIRED_REFRESH_TOKEN, 위조·형식 오류·종류 불일치: INVALID_REFRESH_TOKEN)
    public Long parseRefreshToken(String token) {
        try {
            return parseMemberId(token, TYPE_REFRESH);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    //서명, 만료, 토큰 종류(type)를 확인하고 sub(memberId)를 꺼낸다
    private Long parseMemberId(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("토큰 종류가 올바르지 않습니다.");
        }
        return Long.valueOf(claims.getSubject());
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
