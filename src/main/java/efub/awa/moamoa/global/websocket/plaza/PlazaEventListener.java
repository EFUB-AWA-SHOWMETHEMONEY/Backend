package efub.awa.moamoa.global.websocket.plaza;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.global.websocket.plaza.enums.ParticipantState;
import efub.awa.moamoa.global.websocket.plaza.enums.PlazaConstants;
import efub.awa.moamoa.global.websocket.plaza.enums.PlazaEvent;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!PlazaConstants.TOPIC.equals(accessor.getDestination())) return;

        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long memberId = memberIdOf(attrs);
        if (memberId == null) return;

        attrs.put(PLAZA_SUB_ID, accessor.getSubscriptionId());

        ParticipantState state = new ParticipantState(
                memberId, nicknameOf(memberId), Map.of(),
                PlazaConstants.SPAWN_X, PlazaConstants.SPAWN_Y, accessor.getSessionId()
        );
        registry.join(state).ifPresent(prev ->
                log.info("중복 접속: memberId - {}. 이전 세션 - {}", memberId, prev.sessionId()));
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

    private String nicknameOf(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                        log.warn("존재하지 않는 회원입니다 - memberId: {}", memberId);
                        return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
        return member.getNickname();
    }
}
