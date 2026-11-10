package efub.awa.moamoa.auth.service;

import efub.awa.moamoa.auth.dto.AuthResponse;
import efub.awa.moamoa.auth.dto.AuthResult;
import efub.awa.moamoa.auth.dto.IssuedTokens;
import efub.awa.moamoa.auth.dto.LoginRequest;
import efub.awa.moamoa.auth.dto.SignUpRequest;
import efub.awa.moamoa.auth.dto.SocialSignUpRequest;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.global.security.JwtProvider;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.entity.SelfMember;
import efub.awa.moamoa.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    //OAuth2 Client의 registrationId (현재 지원하는 소셜 로그인)
    private static final String KAKAO_REGISTRATION_ID = "kakao";

    private final MemberService memberService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthResult signUp(SignUpRequest request) {
        //아이디와 닉네임이 모두 중복이면 아이디를 먼저 검사
        if (memberService.existsByLoginId(request.loginId())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (memberService.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Member member = createSelfMember(request);

        IssuedTokens tokens = tokenService.issueTokens(member.getId());
        log.info("회원가입 성공 - memberId: {}", member.getId());

        return new AuthResult(AuthResponse.of(member.getId(), member.getNickname(), tokens), tokens.refreshToken());
    }

    public AuthResult login(LoginRequest request) {
        //존재하지 않는 아이디(카카오 회원 포함)와 비밀번호 불일치는 같은 응답
        SelfMember selfMember = memberService.findSelfMemberByLoginId(request.loginId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), selfMember.getLoginPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        Member member = selfMember.getMember();
        IssuedTokens tokens = tokenService.issueTokens(member.getId());
        log.info("로그인 성공 - memberId: {}", member.getId());

        return new AuthResult(AuthResponse.of(member.getId(), member.getNickname(), tokens), tokens.refreshToken());
    }

    //Refresh Token으로 Access Token 재발급 (Refresh Token도 새로 발급하는 회전 방식)
    public IssuedTokens reissue(String refreshToken) {
        Long memberId = tokenService.validateRefreshToken(refreshToken);

        //회원은 삭제됐는데 Refresh Token만 남아 있는 경우
        if (!memberService.existsById(memberId)) {
            tokenService.deleteRefreshToken(memberId);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        IssuedTokens tokens = tokenService.issueTokens(memberId);
        log.info("토큰 재발급 성공 - memberId: {}", memberId);

        return tokens;
    }

    //저장된 Refresh Token 삭제 (이미 없어도 성공)
    public void logout(Long memberId) {
        tokenService.deleteRefreshToken(memberId);
        log.info("로그아웃 성공 - memberId: {}", memberId);
    }

    //소셜 로그인에서 신규 회원으로 확인된 사용자의 추가 가입 (registerToken은 쿠키로 받는다)
    public AuthResult socialSignUp(String registerToken, SocialSignUpRequest request) {
        //쿠키 없음, 위조, 만료, 지원하지 않는 소셜 종류는 모두 같은 응답
        JwtProvider.RegisterTokenClaims claims = jwtProvider.parseRegisterToken(registerToken)
                .filter(c -> KAKAO_REGISTRATION_ID.equals(c.provider()))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REGISTER_TOKEN));

        if (memberService.existsByOauthId(claims.oauthId())) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED);
        }
        if (memberService.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Member member = createKakaoMember(claims.oauthId(), request);

        IssuedTokens tokens = tokenService.issueTokens(member.getId());
        log.info("소셜 회원가입 성공 - memberId: {}", member.getId());

        return new AuthResult(AuthResponse.of(member.getId(), member.getNickname(), tokens), tokens.refreshToken());
    }

    private Member createSelfMember(SignUpRequest request) {
        try {
            return memberService.createSelfMember(
                    request.loginId(),
                    passwordEncoder.encode(request.password()),
                    request.name(),
                    request.nickname(),
                    request.age());
        } catch (DataIntegrityViolationException e) {
            //사전 검사를 통과한 뒤 동시에 같은 값으로 가입한 경우: 어떤 값이 중복인지 다시 확인
            if (memberService.existsByLoginId(request.loginId())) {
                throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
            }
            if (memberService.existsByNickname(request.nickname())) {
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
            }
            throw e;
        }
    }

    private Member createKakaoMember(String oauthId, SocialSignUpRequest request) {
        try {
            return memberService.createKakaoMember(oauthId, request.name(), request.nickname(), request.age());
        } catch (DataIntegrityViolationException e) {
            //사전 검사를 통과한 뒤 동시에 가입한 경우: 어떤 값이 중복인지 다시 확인
            if (memberService.existsByOauthId(oauthId)) {
                throw new CustomException(ErrorCode.ALREADY_REGISTERED);
            }
            if (memberService.existsByNickname(request.nickname())) {
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
            }
            throw e;
        }
    }
}
