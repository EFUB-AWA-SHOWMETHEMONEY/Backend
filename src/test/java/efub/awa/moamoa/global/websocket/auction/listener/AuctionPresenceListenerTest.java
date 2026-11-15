package efub.awa.moamoa.global.websocket.auction.listener;

import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.global.websocket.StompTestMessages;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.listener.AuctionPresenceListener;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import efub.awa.moamoa.websocket.plaza.support.ChatFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuctionPresenceListenerTest {

    private static final String SESSION = "s1";
    private static final String TOPIC_10 = "/topic/auctions/10";
    private static final String TOPIC_20 = "/topic/auctions/20";

    private AuctionParticipantRegistry registry;
    private SimpMessagingTemplate template;
    private AuctionPresenceListener listener;
    private Map<String, Object> attrs;

    @BeforeEach
    void setUp() {
        registry = new AuctionParticipantRegistry();
        template = mock(SimpMessagingTemplate.class);

        MemberRepository memberRepository = mock(MemberRepository.class);
        InventoryService inventoryService = mock(InventoryService.class);
        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn("테스터");
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
        when(inventoryService.findEquippedItems(anyLong())).thenReturn(List.of());

        AuctionRoomService roomService = new AuctionRoomService(
                registry, memberRepository, inventoryService, new ChatFilter(), mock(AuctionRepository.class));
        listener = new AuctionPresenceListener(registry, template, roomService);

        attrs = new HashMap<>();
        attrs.put("memberId", 1L);
    }

    private void subscribe(String subId, String destination) {
        listener.onSubscribe(new SessionSubscribeEvent(this,
                StompTestMessages.subscribe(SESSION, subId, destination, attrs), null));
    }

    private void unsubscribe(String subId) {
        listener.onUnSubscribe(new SessionUnsubscribeEvent(this,
                StompTestMessages.unsubscribe(SESSION, subId, attrs), null));
    }

    private void disconnect() {
        listener.onDisconnect(new SessionDisconnectEvent(this,
                StompTestMessages.disconnect(SESSION, attrs), SESSION, CloseStatus.NORMAL));
    }

    @Test
    @DisplayName("경매 토픽 구독 = 입장: registry 등록 + user_joined 브로드캐스트")
    void subscribeJoinsRoom() {
        subscribe("sub-1", TOPIC_10);

        assertThat(registry.find(10L, 1L)).isNotNull();
        assertThat(attrs.get("auctionId")).isEqualTo(10L);
        verify(template).convertAndSend(eq(TOPIC_10), isA(AuctionEvent.UserJoined.class));
    }

    @Test
    @DisplayName("경매가 아닌 토픽 구독(광장 등)은 무시한다")
    void nonAuctionSubscribeIgnored() {
        subscribe("sub-1", "/topic/plaza");

        assertThat(registry.find(10L, 1L)).isNull();
        assertThat(attrs).doesNotContainKey("auctionId");
        verifyNoInteractions(template);
    }

    @Test
    @DisplayName("memberId가 없는 세션은 입장 처리하지 않는다")
    void subscribeWithoutMemberIgnored() {
        attrs.remove("memberId");

        subscribe("sub-1", TOPIC_10);

        assertThat(registry.find(10L, 1L)).isNull();
        verifyNoInteractions(template);
    }

    @Test
    @DisplayName("다른 경매방을 구독하면 이전 방에서 자동 퇴장한다")
    void switchingRoomLeavesPreviousRoom() {
        subscribe("sub-1", TOPIC_10);
        subscribe("sub-2", TOPIC_20);

        assertThat(registry.find(10L, 1L)).isNull();
        assertThat(registry.find(20L, 1L)).isNotNull();
        assertThat(attrs.get("auctionId")).isEqualTo(20L);
        verify(template).convertAndSend(eq(TOPIC_10), isA(AuctionEvent.UserLeft.class));
        verify(template).convertAndSend(eq(TOPIC_20), isA(AuctionEvent.UserJoined.class));
    }

    @Test
    @DisplayName("방을 옮긴 뒤 이전 방 구독을 뒤늦게 해제해도 중복 퇴장이 없다")
    void staleUnsubscribeAfterSwitchIgnored() {
        subscribe("sub-1", TOPIC_10);
        subscribe("sub-2", TOPIC_20);

        unsubscribe("sub-1");   // 구 구독 ID라 무시되어야 함

        assertThat(registry.find(20L, 1L)).isNotNull();
        verify(template, times(1)).convertAndSend(eq(TOPIC_10), isA(AuctionEvent.UserLeft.class));
    }

    @Test
    @DisplayName("같은 방을 다시 구독해도 퇴장 처리되지 않는다")
    void resubscribeSameRoomDoesNotLeave() {
        subscribe("sub-1", TOPIC_10);
        subscribe("sub-2", TOPIC_10);

        assertThat(registry.find(10L, 1L)).isNotNull();
        verify(template, times(0)).convertAndSend(eq(TOPIC_10), isA(AuctionEvent.UserLeft.class));
    }

    @Test
    @DisplayName("구독 해제 = 퇴장: user_left 한 번, 세션의 경매 정보도 지워진다")
    void unsubscribeLeavesAndClearsAttributes() {
        subscribe("sub-1", TOPIC_10);

        unsubscribe("sub-1");

        assertThat(registry.find(10L, 1L)).isNull();
        assertThat(attrs).doesNotContainKey("auctionId");
        assertThat(attrs).doesNotContainKey("auctionSubscriptionId");
        verify(template, times(1)).convertAndSend(eq(TOPIC_10), isA(AuctionEvent.UserLeft.class));
    }

    @Test
    @DisplayName("해제 뒤 연결이 끊겨도 user_left가 중복 전송되지 않는다")
    void disconnectAfterUnsubscribeDoesNotDuplicateLeft() {
        subscribe("sub-1", TOPIC_10);
        unsubscribe("sub-1");

        disconnect();

        verify(template, times(1)).convertAndSend(eq(TOPIC_10), isA(AuctionEvent.UserLeft.class));
    }

    @Test
    @DisplayName("다른 구독 ID의 해제는 무시한다")
    void unsubscribeOtherSubscriptionIgnored() {
        subscribe("sub-1", TOPIC_10);

        unsubscribe("sub-other");

        assertThat(registry.find(10L, 1L)).isNotNull();
    }

    @Test
    @DisplayName("연결이 끊기면 퇴장 처리된다")
    void disconnectLeavesRoom() {
        subscribe("sub-1", TOPIC_10);

        disconnect();

        assertThat(registry.find(10L, 1L)).isNull();
        verify(template).convertAndSend(eq(TOPIC_10), isA(AuctionEvent.UserLeft.class));
    }

    @Test
    @DisplayName("경매에 입장한 적 없는 세션(광장 전용)이 끊겨도 예외 없이 무시한다")
    void disconnectWithoutAuctionIsSafe() {
        assertThatCode(this::disconnect).doesNotThrowAnyException();

        verifyNoInteractions(template);
    }

    @Test
    @DisplayName("세션 attribute가 아예 없어도 끊김 처리에서 예외가 나지 않는다")
    void disconnectWithNullAttributesIsSafe() {
        SessionDisconnectEvent event = new SessionDisconnectEvent(this,
                StompTestMessages.disconnect(SESSION, null), SESSION, CloseStatus.NORMAL);

        assertThatCode(() -> listener.onDisconnect(event)).doesNotThrowAnyException();
    }
}
