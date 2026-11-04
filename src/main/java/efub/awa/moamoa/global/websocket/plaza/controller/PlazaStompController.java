package efub.awa.moamoa.global.websocket.plaza.controller;

import efub.awa.moamoa.global.websocket.auth.StompPrincipal;
import efub.awa.moamoa.global.websocket.plaza.enums.PlazaConstants;
import efub.awa.moamoa.global.websocket.plaza.dto.request.MoveRequestDto;
import efub.awa.moamoa.global.websocket.plaza.service.PlazaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class PlazaStompController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PlazaService plazaService;

    @MessageMapping("/plaza/move")
    public void move(@Valid @Payload MoveRequestDto request, Principal principal, SimpMessageHeaderAccessor accessor) {
        Long memberId = ((StompPrincipal) principal).memberId();
        String sessionId = accessor.getSessionId();

        plazaService.move(memberId, sessionId, request)
                .ifPresent(event -> messagingTemplate.convertAndSend(PlazaConstants.TOPIC, event));
    }
}
