package efub.awa.moamoa.global.websocket.exception;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        //인터셉터 예외는 MessageDeliveryException으로 감싸져 오기도 해 원인 예외 꺼냄
        Throwable cause = (ex instanceof MessageDeliveryException && ex.getCause() != null)
                ? ex.getCause() : ex;

        if (cause instanceof CustomException ce) {
            return buildError(ce.getErrorCode());
        }
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    private Message<byte[]> buildError(ErrorCode code) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(code.name());
        accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
        accessor.setLeaveMutable(true);

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(
                    ApiResponse.fail(code)
            );
        } catch (JacksonException e){
            body = new byte[0];
        }
        return MessageBuilder.createMessage(body, accessor.getMessageHeaders());
    }
}
