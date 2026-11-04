package efub.awa.moamoa.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    //Default
    INTERNAL_SERVER_ERROR(500, "서버 내부 에러가 발생했습니다."),
    BAD_REQUEST(400, "잘못된 요청입니다."),

    //Auth - 401
    INVALID_CREDENTIALS(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_AUTHORIZATION_CODE(401, "유효하지 않은 인가코드입니다."),
    INVALID_REGISTER_TOKEN(401, "유효하지 않거나 만료된 가입 토큰입니다."),
    REFRESH_TOKEN_REQUIRED(401, "Refresh Token이 필요합니다."),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(401, "만료된 Refresh Token입니다."),
    ACCESS_TOKEN_REQUIRED(401, "Access Token이 필요합니다."),
    INVALID_ACCESS_TOKEN(401, "유효하지 않은 Access Token입니다."),
    EXPIRED_ACCESS_TOKEN(401, "만료된 Access Token입니다."),

    //Auth - 409
    DUPLICATE_LOGIN_ID(409, "이미 사용 중인 아이디입니다."),
    DUPLICATE_NICKNAME(409, "이미 사용 중인 닉네임입니다."),
    ALREADY_REGISTERED(409, "이미 가입된 소셜 계정입니다."),

    //Auth - 502
    OAUTH_SERVER_ERROR(502, "소셜 서버와 통신하지 못했습니다."),

    //WebSocket - 401
    INVALID_STOMP_AUTH_HEADER(401, "STOMP 인증 헤더가 필요합니다."),
    INVALID_AUTH_TOKEN(401, "유효하지 않은 인증 토큰입니다."),
    EXPIRED_AUTH_TOKEN(401, "만료된 인증 토큰입니다."),

    //Member - 404
    MEMBER_NOT_FOUND(404, "존재하지 않는 회원입니다."),

    //Plaza - 400
    NOT_IN_ROOM(400, "광장에 존재하지 않는 회원입니다.");

    private final int status;
    private final String message;
}
