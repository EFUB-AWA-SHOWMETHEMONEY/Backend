package efub.awa.moamoa.global.config;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.websocket.auction.enums.AuctionConstants;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
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

import java.util.Map;
import java.util.regex.Matcher;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {
    private final StompTokenVerifier tokenVerifier;
    private final AuctionRoomService auctionRoomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        StompCommand command = accessor.getCommand();

        //CONNECT 시점에만 인증 (이후 메시지는 세션 유지)
        if (StompCommand.CONNECT.equals(command)) {
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

        //SEND: 서버 로직을 거치는 /app/**만 허용 (/topic 직접 발행으로 이벤트를 위조하는 것 방지)
        if (StompCommand.SEND.equals(command)) {
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith("/app/")) {
                log.warn("허용되지 않은 SEND destination: sessionId-{}, destination-{}",
                        accessor.getSessionId(), destination);
                throw new CustomException(ErrorCode.INVALID_STOMP_DESTINATION);
            }
        }

        //SUBSCRIBE: 허용된 destination만 구독 가능, 경매방은 입장 가능 여부까지 검증
        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination == null) {
                log.warn("destination 없는 SUBSCRIBE: sessionId-{}", accessor.getSessionId());
                throw new CustomException(ErrorCode.INVALID_STOMP_DESTINATION);
            }

            Matcher auctionMatcher = AuctionConstants.TOPIC_PATTERN.matcher(destination);
            if (auctionMatcher.matches()) {
                Long auctionId = parseAuctionId(auctionMatcher.group(1));
                auctionRoomService.validateJoinable(auctionId, memberIdOf(accessor));
            } else if (!destination.equals("/topic/plaza") && !destination.startsWith("/user/queue/")) {
                log.warn("허용되지 않은 SUBSCRIBE: sessionId-{}, destination-{}",
                        accessor.getSessionId(), destination);
                throw new CustomException(ErrorCode.INVALID_STOMP_DESTINATION);
            }
        }

        return message;
    }

    //Long 범위를 넘는 숫자는 존재할 수 없는 경매이므로 잘못된 destination으로 처리
    private Long parseAuctionId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_STOMP_DESTINATION);
        }
    }

    private Long memberIdOf(StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        return attrs != null && attrs.get("memberId") instanceof Long id ? id : null;
    }
}
