package efub.awa.moamoa.websocket.auction.listener;

import efub.awa.moamoa.auction.event.AuctionClosedEvent;
import efub.awa.moamoa.auction.event.BidPlacedEvent;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.enums.AuctionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionDomainEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBidPlaced(BidPlacedEvent e) {
        messagingTemplate.convertAndSend(
                AuctionConstants.topic(e.auctionId()),
                AuctionEvent.PublicBidAccepted.of(e.bidderId(), e.nickname(), e.price(),
                        e.newDeadline()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionClosed(AuctionClosedEvent e) {
        AuctionEvent event = (e.winnerId() == null)
                ? AuctionEvent.AuctionClosed.failed()
                : AuctionEvent.AuctionClosed.success(e.winnerId(), e.finalPrice());
        messagingTemplate.convertAndSend(AuctionConstants.topic(e.auctionId()), event);
    }
}
