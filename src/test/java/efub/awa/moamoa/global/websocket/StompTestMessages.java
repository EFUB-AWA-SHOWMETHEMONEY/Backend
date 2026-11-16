package efub.awa.moamoa.global.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

/**
 * 테스트용 STOMP 메시지 생성기.
 * leaveMutable(true)를 켜야 StompHeaderAccessor.getAccessor(message, ...)가 accessor를 돌려준다.
 * 안 켜면 인터셉터가 accessor == null로 보고 모든 메시지를 그대로 통과시켜, 테스트가 의미 없이 통과한다.
 */
public final class StompTestMessages {

    private StompTestMessages() {}

    public static Message<byte[]> connect(String authHeader, Map<String, Object> sessionAttrs) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        return build(accessor, sessionAttrs);
    }

    public static Message<byte[]> send(String destination, Map<String, Object> sessionAttrs) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        return build(accessor, sessionAttrs);
    }

    public static Message<byte[]> subscribe(String sessionId, String subscriptionId, String destination,
                                            Map<String, Object> sessionAttrs) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setDestination(destination);
        return build(accessor, sessionAttrs);
    }

    public static Message<byte[]> unsubscribe(String sessionId, String subscriptionId,
                                              Map<String, Object> sessionAttrs) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        return build(accessor, sessionAttrs);
    }

    public static Message<byte[]> disconnect(String sessionId, Map<String, Object> sessionAttrs) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId(sessionId);
        return build(accessor, sessionAttrs);
    }

    private static Message<byte[]> build(StompHeaderAccessor accessor, Map<String, Object> sessionAttrs) {
        accessor.setSessionAttributes(sessionAttrs);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
