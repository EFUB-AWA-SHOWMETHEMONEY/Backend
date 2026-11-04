package efub.awa.moamoa.global.security.oauth2;

import java.util.Map;

//카카오 사용자 정보 응답: 최상위 "id"가 회원번호
public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id == null ? null : String.valueOf(id);
    }
}
