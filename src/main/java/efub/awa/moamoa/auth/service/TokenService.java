package efub.awa.moamoa.auth.service;

import efub.awa.moamoa.auth.dto.IssuedTokens;
import efub.awa.moamoa.auth.repository.RefreshTokenRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
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

    //쿠키로 받은 Refresh Token 검증: 없음, 위조·형식 오류·만료, 서버 저장값과 불일치(폐기된 토큰 포함) 확인 후 memberId 반환
    public Long validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        Long memberId = jwtProvider.parseRefreshToken(refreshToken);

        String savedToken = refreshTokenRepository.find(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!savedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return memberId;
    }

    //저장된 Refresh Token 삭제 (이미 없어도 성공)
    public void deleteRefreshToken(Long memberId) {
        refreshTokenRepository.delete(memberId);
    }
}
