package efub.awa.moamoa.global.config;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.global.websocket.StompTestMessages;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import efub.awa.moamoa.websocket.auth.StompPrincipal;
import efub.awa.moamoa.websocket.auth.StompTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StompAuthInterceptorTest {

    private StompTokenVerifier tokenVerifier;
    private AuctionRoomService auctionRoomService;
    private StompAuthInterceptor interceptor;
    private Map<String, Object> attrs;

    @BeforeEach
    void setUp() {
        tokenVerifier = mock(StompTokenVerifier.class);
        auctionRoomService = mock(AuctionRoomService.class);
        interceptor = new StompAuthInterceptor(tokenVerifier, auctionRoomService);

        attrs = new HashMap<>();
        attrs.put("memberId", 1L);
    }

    private Message<?> run(Message<?> message) {
        return interceptor.preSend(message, null);
    }

    private void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    // ---------- CONNECT ----------

    @Test
    @DisplayName("Authorization 헤더가 없으면 INVALID_STOMP_AUTH_HEADER")
    void connectWithoutHeader() {
        assertErrorCode(() -> run(StompTestMessages.connect(null, new HashMap<>())),
                ErrorCode.INVALID_STOMP_AUTH_HEADER);
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 INVALID_STOMP_AUTH_HEADER")
    void connectWithNonBearerHeader() {
        assertErrorCode(() -> run(StompTestMessages.connect("Basic abc", new HashMap<>())),
                ErrorCode.INVALID_STOMP_AUTH_HEADER);
    }

    @Test
    @DisplayName("유효한 토큰이면 세션에 memberId와 Principal이 저장된다")
    void connectWithValidToken() {
        when(tokenVerifier.verify("tok")).thenReturn(7L);
        Map<String, Object> sessionAttrs = new HashMap<>();

        Message<?> result = run(StompTestMessages.connect("Bearer tok", sessionAttrs));

        assertThat(result).isNotNull();
        assertThat(sessionAttrs.get("memberId")).isEqualTo(7L);
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor.getUser()).isEqualTo(new StompPrincipal(7L));
    }

    @Test
    @DisplayName("토큰 검증 실패 예외는 그대로 전파된다")
    void connectWithInvalidToken() {
        when(tokenVerifier.verify("bad"))
                .thenThrow(new CustomException(ErrorCode.INVALID_ACCESS_TOKEN));

        assertErrorCode(() -> run(StompTestMessages.connect("Bearer bad", new HashMap<>())),
                ErrorCode.INVALID_ACCESS_TOKEN);
    }

    // ---------- SEND ----------

    @Test
    @DisplayName("/app/** SEND는 통과한다")
    void sendToAppAllowed() {
        Message<?> message = StompTestMessages.send("/app/plaza/chat", attrs);

        assertThat(run(message)).isSameAs(message);
    }

    @Test
    @DisplayName("/topic 직접 SEND는 INVALID_STOMP_DESTINATION (클라이언트가 이벤트를 위조하는 것 방지)")
    void sendToTopicBlocked() {
        assertErrorCode(() -> run(StompTestMessages.send("/topic/plaza", attrs)),
                ErrorCode.INVALID_STOMP_DESTINATION);
        assertErrorCode(() -> run(StompTestMessages.send("/topic/auctions/10", attrs)),
                ErrorCode.INVALID_STOMP_DESTINATION);
    }

    @Test
    @DisplayName("destination이 없는 SEND도 INVALID_STOMP_DESTINATION")
    void sendWithoutDestinationBlocked() {
        assertErrorCode(() -> run(StompTestMessages.send(null, attrs)),
                ErrorCode.INVALID_STOMP_DESTINATION);
    }

    // ---------- SUBSCRIBE ----------

    @Test
    @DisplayName("허용되지 않은 destination 구독은 INVALID_STOMP_DESTINATION")
    void subscribeDisallowed() {
        assertErrorCode(() -> run(StompTestMessages.subscribe("s1", "sub-1", "/topic/other", attrs)),
                ErrorCode.INVALID_STOMP_DESTINATION);
        assertErrorCode(() -> run(StompTestMessages.subscribe("s1", "sub-1", "/topic/auctions/abc", attrs)),
                ErrorCode.INVALID_STOMP_DESTINATION);
    }

    @Test
    @DisplayName("destination이 없는 구독은 INVALID_STOMP_DESTINATION")
    void subscribeWithoutDestination() {
        assertErrorCode(() -> run(StompTestMessages.subscribe("s1", "sub-1", null, attrs)),
                ErrorCode.INVALID_STOMP_DESTINATION);
    }

    @Test
    @DisplayName("광장·개인 큐 구독은 통과하고 경매 검증은 호출하지 않는다")
    void subscribePlazaAndUserQueueAllowed() {
        assertThat(run(StompTestMessages.subscribe("s1", "sub-1", "/topic/plaza", attrs))).isNotNull();
        assertThat(run(StompTestMessages.subscribe("s1", "sub-2", "/user/queue/errors", attrs))).isNotNull();

        verifyNoInteractions(auctionRoomService);
    }

    @Test
    @DisplayName("경매 토픽 구독 시 입장 가능 여부를 검증한다 (auctionId, memberId 전달)")
    void subscribeAuctionValidatesJoinable() {
        Message<?> message = StompTestMessages.subscribe("s1", "sub-1", "/topic/auctions/10", attrs);

        assertThat(run(message)).isSameAs(message);
        verify(auctionRoomService).validateJoinable(10L, 1L);
    }

    @Test
    @DisplayName("입장 불가 경매면 검증 예외가 그대로 전파되어 구독이 거부된다")
    void subscribeAuctionRejectedWhenNotJoinable() {
        doThrow(new CustomException(ErrorCode.ALREADY_STARTED))
                .when(auctionRoomService).validateJoinable(10L, 1L);

        assertErrorCode(() -> run(StompTestMessages.subscribe("s1", "sub-1", "/topic/auctions/10", attrs)),
                ErrorCode.ALREADY_STARTED);
    }
}
