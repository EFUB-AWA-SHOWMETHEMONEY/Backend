package efub.awa.moamoa.global.security.oauth2;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

//소셜 로그인 결과: memberId가 null이면 아직 가입하지 않은 신규 회원
public class CustomOAuth2User implements OAuth2User {

    private final Map<String, Object> attributes;
    private final String oauthId;
    private final Long memberId;

    public CustomOAuth2User(Map<String, Object> attributes, String oauthId, Long memberId) {
        this.attributes = attributes;
        this.oauthId = oauthId;
        this.memberId = memberId;
    }

    public String getOauthId() {
        return oauthId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public boolean isNewUser() {
        return memberId == null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //이후 API는 JWT로 인증하므로 여기서는 권한을 부여하지 않는다
        return List.of();
    }

    @Override
    public String getName() {
        return oauthId;
    }
}
