package efub.awa.moamoa.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    //Default
    INTERNAL_SERVER_ERROR(500, "서버 내부 에러가 발생했습니다."),
    BAD_REQUEST(400, "잘못된 요청입니다."),
    INVALID_STOMP_AUTH_HEADER(401, "STOMP 인증 헤더가 필요합니다."),
    INVALID_AUTH_TOKEN(401, "유효하지 않은 인증 토큰입니다."),
    EXPIRED_AUTH_TOKEN(401, "만료된 인증 토큰입니다."),
    MEMBER_NOT_FOUND(404, "존재하지 않는 회원입니다."),
    NOT_IN_ROOM(400, "광장에 존재하지 않는 회원입니다.");

    private final int status;
    private final String message;
}
