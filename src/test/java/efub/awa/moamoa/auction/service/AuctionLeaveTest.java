package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.repository.AuctionEntryListRepository;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import efub.awa.moamoa.websocket.plaza.support.ChatFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//경매 퇴장 API (AuctionService.leaveAuction)
class AuctionLeaveTest {

    private static final long AUCTION = 15L;
    private static final long SELLER = 1L;
    private static final long MEMBER = 2L;
    private static final String TOPIC = "/topic/auctions/" + AUCTION;

    private AuctionRepository auctionRepository;
    private AuctionEntryListRepository entryListRepository;
    private AuctionParticipantRegistry registry;
    private SimpMessagingTemplate messagingTemplate;
    private AuctionService auctionService;

    private Member seller;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        entryListRepository = mock(AuctionEntryListRepository.class);
        registry = new AuctionParticipantRegistry();
        messagingTemplate = mock(SimpMessagingTemplate.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        InventoryService inventoryService = mock(InventoryService.class);

        AuctionRoomService roomService = new AuctionRoomService(registry, memberRepository, inventoryService,
                new ChatFilter(), auctionRepository, entryListRepository);
        auctionService = new AuctionService(auctionRepository, memberRepository, inventoryService,
                roomService, registry, entryListRepository, messagingTemplate);

        seller = mock(Member.class);
        when(seller.getId()).thenReturn(SELLER);
    }

    // ---------- 픽스처 ----------

    private Auction auction(AuctionType type, AuctionStatus status) {
        LocalDateTime readyStartTime = LocalDateTime.now().minusMinutes(15);
        Auction auction = Auction.builder()
                .inventory(mock(Inventory.class))
                .auctionOwner(seller)
                .type(type)
                .startPrice(1000L)
                .readyStartTime(readyStartTime)
                .maxParticipants(20)
                .entryDeadline(readyStartTime.plusMinutes(10))
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION);
        if (status == AuctionStatus.OPEN) {
            auction.open(LocalDateTime.now().minusMinutes(1));
        } else {
            ReflectionTestUtils.setField(auction, "status", status);
        }
        when(auctionRepository.findById(AUCTION)).thenReturn(Optional.of(auction));
        return auction;
    }

    private void enter(long memberId) {
        registry.join(AUCTION, new AuctionParticipantState(memberId, "s" + memberId, "n" + memberId, null, AUCTION));
    }

    //OPEN 순간 참가자 명단에 있었던 회원 (연결이 끊겨 지금 방에는 없을 수 있음)
    private void inParticipantList(long memberId) {
        when(entryListRepository.isInParticipantList(AUCTION, memberId)).thenReturn(true);
    }

    private void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private void assertUserLeftSent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(TOPIC), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(AuctionEvent.UserLeft.class);
        assertThat(((AuctionEvent.UserLeft) captor.getValue()).memberId()).isEqualTo(MEMBER);
    }

    private void assertUserLeftNotSent() {
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    // ---------- 공통 ----------

    @Test
    @DisplayName("존재하지 않는 경매는 AUCTION_NOT_FOUND")
    void notFound() {
        when(auctionRepository.findById(AUCTION)).thenReturn(Optional.empty());

        assertErrorCode(() -> auctionService.leaveAuction(MEMBER, AUCTION), ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("CLOSED/FAILED 경매는 방에 있어도 ALREADY_ENDED")
    void endedAuction() {
        enter(MEMBER);

        auction(AuctionType.PUBLIC, AuctionStatus.CLOSED);
        assertErrorCode(() -> auctionService.leaveAuction(MEMBER, AUCTION), ErrorCode.ALREADY_ENDED);

        auction(AuctionType.PUBLIC, AuctionStatus.FAILED);
        assertErrorCode(() -> auctionService.leaveAuction(MEMBER, AUCTION), ErrorCode.ALREADY_ENDED);

        assertThat(registry.find(AUCTION, MEMBER)).isNotNull();
        assertUserLeftNotSent();
    }

    // ---------- READY ----------

    @Test
    @DisplayName("READY: 방에 있으면 퇴장, user_left 발행, 퇴장 명단에는 기록하지 않는다")
    void readyLeave() {
        auction(AuctionType.PUBLIC, AuctionStatus.READY);
        enter(MEMBER);

        auctionService.leaveAuction(MEMBER, AUCTION);

        assertThat(registry.find(AUCTION, MEMBER)).isNull();
        assertUserLeftSent();
        verify(entryListRepository, never()).addLeft(anyLong(), anyLong());
    }

    @Test
    @DisplayName("READY: 방에 없으면(이미 퇴장, 입장한 적 없음, 판매자) ALREADY_LEFT")
    void readyNotInRoom() {
        auction(AuctionType.PUBLIC, AuctionStatus.READY);

        assertErrorCode(() -> auctionService.leaveAuction(MEMBER, AUCTION), ErrorCode.ALREADY_LEFT);
        assertErrorCode(() -> auctionService.leaveAuction(SELLER, AUCTION), ErrorCode.ALREADY_LEFT);
        verifyNoInteractions(entryListRepository);   // READY는 Redis 명단을 보지 않는다
    }

    @Test
    @DisplayName("READY: 퇴장 후 다시 호출하면 ALREADY_LEFT")
    void readyLeaveTwice() {
        auction(AuctionType.PUBLIC, AuctionStatus.READY);
        enter(MEMBER);

        auctionService.leaveAuction(MEMBER, AUCTION);

        assertErrorCode(() -> auctionService.leaveAuction(MEMBER, AUCTION), ErrorCode.ALREADY_LEFT);
    }

    @Test
    @DisplayName("다른 회원은 방에 그대로 남는다")
    void otherMembersStay() {
        auction(AuctionType.PUBLIC, AuctionStatus.READY);
        enter(MEMBER);
        enter(3L);

        auctionService.leaveAuction(MEMBER, AUCTION);

        assertThat(registry.find(AUCTION, 3L)).isNotNull();
        assertThat(registry.count(AUCTION)).isEqualTo(1);
    }

    // ---------- OPEN (PUBLIC) ----------

    @Test
    @DisplayName("OPEN PUBLIC: 방에 있으면 퇴장 명단 기록 후 퇴장, user_left 발행")
    void openPublicLeave() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN);
        enter(MEMBER);

        auctionService.leaveAuction(MEMBER, AUCTION);

        verify(entryListRepository).addLeft(AUCTION, MEMBER);
        assertThat(registry.find(AUCTION, MEMBER)).isNull();
        assertUserLeftSent();
    }

    @Test
    @DisplayName("OPEN PUBLIC: 연결이 끊긴 참가자도 퇴장 명단에 기록되고, user_left는 다시 발행하지 않는다")
    void openPublicDisconnectedLeave() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN);
        inParticipantList(MEMBER);

        auctionService.leaveAuction(MEMBER, AUCTION);

        verify(entryListRepository).addLeft(AUCTION, MEMBER);
        assertUserLeftNotSent();
    }

    @Test
    @DisplayName("OPEN PUBLIC: 이미 퇴장 명단에 있으면 ALREADY_LEFT")
    void openPublicAlreadyLeft() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN);
        inParticipantList(MEMBER);
        when(entryListRepository.isInLeftList(AUCTION, MEMBER)).thenReturn(true);

        assertErrorCode(() -> auctionService.leaveAuction(MEMBER, AUCTION), ErrorCode.ALREADY_LEFT);
        verify(entryListRepository, never()).addLeft(anyLong(), anyLong());
    }

    @Test
    @DisplayName("OPEN: 방에도 없고 참가자 명단에도 없으면 ALREADY_LEFT")
    void openNotAttending() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN);

        assertErrorCode(() -> auctionService.leaveAuction(MEMBER, AUCTION), ErrorCode.ALREADY_LEFT);
    }

    @Test
    @DisplayName("OPEN PUBLIC: 퇴장 명단 기록이 실패하면 예외가 전파되고 방에서는 제거하지 않는다")
    void openPublicLeftListFailure() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN);
        enter(MEMBER);
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(entryListRepository).addLeft(AUCTION, MEMBER);

        assertThatThrownBy(() -> auctionService.leaveAuction(MEMBER, AUCTION))
                .isInstanceOf(RedisConnectionFailureException.class);
        assertThat(registry.find(AUCTION, MEMBER)).isNotNull();
        assertUserLeftNotSent();
    }

    // ---------- OPEN (PRIVATE) ----------

    @Test
    @DisplayName("OPEN PRIVATE: 방에 있으면 퇴장, user_left 발행, 퇴장 명단에는 기록하지 않는다")
    void openPrivateLeave() {
        auction(AuctionType.PRIVATE, AuctionStatus.OPEN);
        enter(MEMBER);

        auctionService.leaveAuction(MEMBER, AUCTION);

        assertThat(registry.find(AUCTION, MEMBER)).isNull();
        assertUserLeftSent();
        verify(entryListRepository, never()).addLeft(anyLong(), anyLong());
    }

    @Test
    @DisplayName("OPEN PRIVATE: 연결이 끊긴 참가자는 다시 호출해도 200 (기록하지 않으므로 계속 참가 중)")
    void openPrivateDisconnectedLeaveRepeatable() {
        auction(AuctionType.PRIVATE, AuctionStatus.OPEN);
        inParticipantList(MEMBER);

        auctionService.leaveAuction(MEMBER, AUCTION);
        auctionService.leaveAuction(MEMBER, AUCTION);

        verify(entryListRepository, never()).addLeft(anyLong(), anyLong());
        assertUserLeftNotSent();
    }

    // ---------- 레지스트리 ----------

    @Test
    @DisplayName("퇴장 후 구독 해제로 기존 leave가 호출되어도 false라 user_left가 중복 발행되지 않는다")
    void sessionLeaveAfterApiLeaveIsNoop() {
        auction(AuctionType.PUBLIC, AuctionStatus.READY);
        enter(MEMBER);

        auctionService.leaveAuction(MEMBER, AUCTION);

        assertThat(registry.leave(MEMBER, AUCTION, "s" + MEMBER)).isFalse();
    }
}
