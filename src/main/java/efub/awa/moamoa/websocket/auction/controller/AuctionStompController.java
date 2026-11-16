package efub.awa.moamoa.websocket.auction.controller;

import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.enums.AuctionConstants;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import efub.awa.moamoa.websocket.auth.StompPrincipal;
import efub.awa.moamoa.websocket.plaza.dto.request.ChatRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuctionStompController {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuctionRoomService auctionRoomService;

    @MessageMapping("/auctions/{auctionId}/chat")
    public void auctionChat(@DestinationVariable Long auctionId, @Valid @Payload ChatRequestDto request,
                            SimpMessageHeaderAccessor accessor, Principal principal) {
        Long memberId = ((StompPrincipal) principal).memberId();
        String sessionId = accessor.getSessionId();

        AuctionEvent.AuctionChat event = auctionRoomService.chat(auctionId, memberId, sessionId, request);
        messagingTemplate.convertAndSend(AuctionConstants.topic(auctionId), event);
    }
}
