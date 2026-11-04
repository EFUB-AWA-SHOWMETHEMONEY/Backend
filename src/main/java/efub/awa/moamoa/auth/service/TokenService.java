package efub.awa.moamoa.auth.service;

import efub.awa.moamoa.auth.dto.IssuedTokens;
import efub.awa.moamoa.auth.repository.RefreshTokenRepository;
import efub.awa.moamoa.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    //Access/Refresh Token 발급 + Refresh Token 저장 (회원당 하나, 기존 토큰은 덮어씀)
    public IssuedTokens issueTokens(Long memberId) {
        JwtProvider.AccessToken accessToken = jwtProvider.createAccessToken(memberId);
        String refreshToken = jwtProvider.createRefreshToken(memberId);

        refreshTokenRepository.save(memberId, refreshToken, jwtProvider.getRefreshTokenExpirationSeconds());

        log.info("토큰 발급 성공 - memberId: {}", memberId);
        return new IssuedTokens(accessToken.token(), accessToken.expiresIn(), accessToken.expiresAt(), refreshToken);
    }
}
