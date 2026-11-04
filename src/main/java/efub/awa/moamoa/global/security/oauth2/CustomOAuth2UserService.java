package efub.awa.moamoa.global.security.oauth2;

import efub.awa.moamoa.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberService memberService;

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

    //소셜 사용자 정보를 조회해 기존 회원인지 확인만 한다 (신규 회원은 여기서 만들지 않는다)
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = switch (registrationId) {
            case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_request", "지원하지 않는 소셜 로그인입니다.", null));
        };

        String oauthId = userInfo.getProviderId();
        if (oauthId == null || oauthId.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info_response", "소셜 회원번호를 확인할 수 없습니다.", null));
        }

        Long memberId = memberService.findKakaoMemberByOauthId(oauthId)
                .map(kakaoMember -> kakaoMember.getMember().getId())
                .orElse(null);

        log.info("소셜 사용자 정보 조회 성공 - provider: {}, 신규 회원 여부: {}", registrationId, memberId == null);
        return new CustomOAuth2User(oAuth2User.getAttributes(), oauthId, memberId);
    }
}
