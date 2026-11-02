package efub.awa.moamoa.global.websocket.exception;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(CustomException.class)
    @SendToUser("/queue/errors")
    public ApiResponse<Void> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("WebSocket CustomException 발생: errorCode - {}", errorCode);

        return ApiResponse.fail(errorCode);
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ApiResponse<Void> handleValidation (MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                        .collect(Collectors.joining(", "));
        log.warn("WebSocket 검증 실패 발생: message - {}", message);

        return ApiResponse.fail(ErrorCode.BAD_REQUEST, message);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ApiResponse<Void> handleUnexpected(Exception e) {
        log.error("WebSocket Unexpected 에러 발생", e);

        return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
