package efub.awa.moamoa.websocket.plaza.listener;

import efub.awa.moamoa.websocket.plaza.entity.ParticipantState;
import efub.awa.moamoa.websocket.plaza.enums.PlazaConstants;
import efub.awa.moamoa.websocket.plaza.dto.response.PlazaEvent;
import efub.awa.moamoa.websocket.plaza.repository.PlazaParticipantRegistry;
import efub.awa.moamoa.websocket.plaza.service.PlazaService;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class PlazaEventListener {

    private static final String PLAZA_SUB_ID = "plazaSubscriptionId";

    private final PlazaParticipantRegistry registry;
    private final SimpMessagingTemplate messagingTemplate;
    private final PlazaService plazaService;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!PlazaConstants.TOPIC.equals(accessor.getDestination())) return;

        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long memberId = memberIdOf(attrs);
        if (memberId == null) return;

        attrs.put(PLAZA_SUB_ID, accessor.getSubscriptionId());

        ParticipantState state = plazaService.join(memberId, accessor.getSessionId());
        log.info("광장 입장 memberId={} sessionId={}", memberId, accessor.getSessionId());
        messagingTemplate.convertAndSend(PlazaConstants.TOPIC, PlazaEvent.UserJoined.from(state));
    }

    @EventListener
    public void onUnSubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long memberId = memberIdOf(attrs);
        if (memberId == null) return;

        //UNSUBSCRIBE 프레임에는 destination이 없어, 구독 때 저장한 id와 비교
        Object plazaSubId = attrs.get(PLAZA_SUB_ID);
        if (plazaSubId == null || !plazaSubId.equals(accessor.getSubscriptionId())) return;

        attrs.remove(PLAZA_SUB_ID);
        leave(memberId, accessor.getSessionId());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long memberId = memberIdOf(accessor.getSessionAttributes());
        if (memberId == null) return;

        leave(memberId, event.getSessionId());
    }

    private void leave(Long memberId, String sessionId) {
        if (registry.leave(memberId, sessionId)) {
            log.info("광장 퇴장 memberId={} sessionId={}", memberId, sessionId);
            messagingTemplate.convertAndSend(PlazaConstants.TOPIC, PlazaEvent.UserLeft.of(memberId));
        }
    }

    private Long memberIdOf(Map<String, Object> attrs) {
        return attrs != null && attrs.get("memberId") instanceof Long id ? id : null;
    }
}
