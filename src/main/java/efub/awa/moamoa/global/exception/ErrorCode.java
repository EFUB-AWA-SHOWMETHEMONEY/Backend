package efub.awa.moamoa.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    //Default
    INTERNAL_SERVER_ERROR(500, "서버 내부 에러가 발생했습니다."),
    BAD_REQUEST(400, "잘못된 요청입니다.");

    private final int status;
    private final String message;
}
