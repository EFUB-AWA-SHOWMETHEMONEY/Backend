package efub.awa.moamoa.global.config;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.websocket.auth.StompPrincipal;
import efub.awa.moamoa.websocket.auth.StompTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {
    private final StompTokenVerifier tokenVerifier;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        //CONNECT 시점에만 인증 (이후 메시지는 세션 유지)
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("CONNECT 인증 실패 - header 문제");
                throw new CustomException(ErrorCode.INVALID_STOMP_AUTH_HEADER);
            }

            String token = authHeader.substring(7);
            Long memberId = tokenVerifier.verify(token);

            accessor.getSessionAttributes().put("memberId", memberId);
            accessor.setUser(new StompPrincipal(memberId));
        }

        return message;
    }
}
