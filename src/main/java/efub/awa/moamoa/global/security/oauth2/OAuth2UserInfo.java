package efub.awa.moamoa.global.security.oauth2;

//소셜 제공자별 사용자 정보 응답 구조 차이를 감추는 인터페이스
public interface OAuth2UserInfo {

    //소셜 제공자가 부여한 회원 고유 번호
    String getProviderId();
}
