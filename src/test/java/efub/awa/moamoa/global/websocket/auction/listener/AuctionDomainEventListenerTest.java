package efub.awa.moamoa.global.websocket.auction.listener;

import efub.awa.moamoa.auction.event.AuctionClosedEvent;
import efub.awa.moamoa.auction.event.BidPlacedEvent;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.listener.AuctionDomainEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * AFTER_COMMIT 시점 제어(트랜잭션 연동)는 스프링이 맡는 부분이라 여기서 다루지 않고,
 * 이벤트를 받았을 때 올바른 토픽에 올바른 페이로드를 보내는지만 확인한다.
 */
class AuctionDomainEventListenerTest {

    private SimpMessagingTemplate template;
    private AuctionDomainEventListener listener;

    @BeforeEach
    void setUp() {
        template = mock(SimpMessagingTemplate.class);
        listener = new AuctionDomainEventListener(template);
    }

    private Object sentTo(String topic) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq(topic), payload.capture());
        return payload.getValue();
    }

    @Test
    @DisplayName("입찰 이벤트는 해당 경매 토픽에 bid_accepted로 전송된다")
    void bidPlacedBroadcast() {
        listener.onBidPlaced(new BidPlacedEvent(10L, 2L, "입찰자", 5000L, 1_700_000_000_000L));

        AuctionEvent.PublicBidAccepted event = (AuctionEvent.PublicBidAccepted) sentTo("/topic/auctions/10");
        assertThat(event.type()).isEqualTo("bid_accepted");
        assertThat(event.bidderId()).isEqualTo(2L);
        assertThat(event.nickname()).isEqualTo("입찰자");
        assertThat(event.price()).isEqualTo(5000L);
        assertThat(event.newDeadline()).isEqualTo(1_700_000_000_000L);
    }

    @Test
    @DisplayName("낙찰이면 auction_closed SUCCESS로 전송된다")
    void closedWithWinner() {
        listener.onAuctionClosed(new AuctionClosedEvent(10L, 2L, 7000L));

        AuctionEvent.AuctionClosed event = (AuctionEvent.AuctionClosed) sentTo("/topic/auctions/10");
        assertThat(event.type()).isEqualTo("auction_closed");
        assertThat(event.result()).isEqualTo("SUCCESS");
        assertThat(event.winnerId()).isEqualTo(2L);
        assertThat(event.finalPrice()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("낙찰자가 없으면 auction_closed FAILED(유찰)로 전송된다")
    void closedWithoutWinner() {
        listener.onAuctionClosed(new AuctionClosedEvent(10L, null, null));

        AuctionEvent.AuctionClosed event = (AuctionEvent.AuctionClosed) sentTo("/topic/auctions/10");
        assertThat(event.result()).isEqualTo("FAILED");
        assertThat(event.winnerId()).isNull();
        assertThat(event.finalPrice()).isNull();
    }
}
