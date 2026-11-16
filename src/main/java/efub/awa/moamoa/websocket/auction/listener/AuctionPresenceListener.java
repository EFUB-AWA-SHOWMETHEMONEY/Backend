package efub.awa.moamoa.websocket.auction.listener;

import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.auction.enums.AuctionConstants;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.regex.Matcher;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionPresenceListener {

    private static final String AUCTION_SUB_ID = "auctionSubscriptionId";
    private static final String AUCTION_ID = "auctionId";

    private final AuctionParticipantRegistry registry;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuctionRoomService auctionRoomService;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String destination = accessor.getDestination();
        if (destination == null) return;

        Matcher matcher = AuctionConstants.TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) return;

        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long memberId = memberIdOf(attrs);
        if (memberId == null) return;

        Long auctionId = Long.parseLong(matcher.group(1));

        //이전에 참여한 경매에서는 떠나는 처리
        Long prevAuctionId = auctionIdOf(attrs);
        if (prevAuctionId != null && !prevAuctionId.equals(auctionId)) {
            leave(memberId, prevAuctionId, accessor.getSessionId());
        }

        attrs.put(AUCTION_SUB_ID, accessor.getSubscriptionId());
        attrs.put(AUCTION_ID, auctionId);

        AuctionParticipantState state = auctionRoomService.join(auctionId, memberId, accessor.getSessionId());

        messagingTemplate.convertAndSend(
                AuctionConstants.topic(auctionId),
                AuctionEvent.UserJoined.from(state)
        );
    }

    @EventListener
    public void onUnSubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> attrs = accessor.getSessionAttributes();

        Long memberId = memberIdOf(attrs);
        if (memberId == null) return;

        Long auctionId = auctionIdOf(attrs);
        if (auctionId == null) return;

        Object auctionSubId = attrs.get(AUCTION_SUB_ID);
        if (auctionSubId == null || !auctionSubId.equals(accessor.getSubscriptionId())) return;

        attrs.remove(AUCTION_SUB_ID);
        attrs.remove(AUCTION_ID);
        leave(memberId, auctionId, accessor.getSessionId());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;

        Long memberId = memberIdOf(attrs);
        Long auctionId = auctionIdOf(attrs);
        if (memberId == null || auctionId == null) return;

        leave(memberId, auctionId, event.getSessionId());
    }

    private void leave(Long memberId, Long auctionId, String sessionId) {
        if (registry.leave(memberId, auctionId, sessionId)) {
            log.info("경매 퇴장: memberId-{}", memberId);
            messagingTemplate.convertAndSend(
                    AuctionConstants.topic(auctionId),
                    AuctionEvent.UserLeft.of(memberId)
                    );
        }
    }

    private Long memberIdOf(Map<String, Object> attrs) {
        return attrs != null && attrs.get("memberId") instanceof Long id ? id : null;
    }

    private Long auctionIdOf(Map<String, Object> attrs) {
        return attrs != null && attrs.get("auctionId") instanceof Long id ? id : null;
    }
}
