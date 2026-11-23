package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.response.AuctionTransitionResult;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.event.AuctionClosedEvent;
import efub.awa.moamoa.auction.repository.AuctionEntryListRepository;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.auction.repository.PublicBidRepository;
import efub.awa.moamoa.balance.enums.BalanceChangeType;
import efub.awa.moamoa.balance.enums.ReferenceType;
import efub.awa.moamoa.balance.service.BalanceService;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuctionTransitionServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long AUCTION = 10L;
    private static final long SELLER = 1L;
    private static final long WINNER = 2L;
    private static final long INVENTORY = 100L;
    private static final long PRICE = 5000L;

    private AuctionRepository auctionRepository;
    private PublicBidRepository publicBidRepository;
    private AuctionParticipantRegistry registry;
    private BalanceService balanceService;
    private InventoryService inventoryService;
    private ApplicationEventPublisher eventPublisher;
    private AuctionEntryListRepository entryListRepository;
    private AuctionTransitionService service;

    private Member seller;
    private Member winner;
    private Item item;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        publicBidRepository = mock(PublicBidRepository.class);
        registry = new AuctionParticipantRegistry();
        balanceService = mock(BalanceService.class);
        inventoryService = mock(InventoryService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        entryListRepository = mock(AuctionEntryListRepository.class);
        service = new AuctionTransitionService(auctionRepository, publicBidRepository, registry,
                balanceService, inventoryService, eventPublisher, entryListRepository);

        seller = mock(Member.class);
        when(seller.getId()).thenReturn(SELLER);
        winner = mock(Member.class);
        when(winner.getId()).thenReturn(WINNER);
        item = mock(Item.class);

        when(auctionRepository.saveAndFlush(any(Auction.class))).then(returnsFirstArg());
        when(publicBidRepository.findLastBidTime(AUCTION)).thenReturn(Optional.empty());
    }

    // ---------- 픽스처 ----------

    //READY 경매를 만든다. entryDeadline은 현재 시각 기준 offset(분) 이후 (음수면 이미 지난 상태)
    private Auction readyAuction(AuctionType type, long entryDeadlineOffsetMinutes) {
        LocalDateTime now = LocalDateTime.now(KST);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getId()).thenReturn(INVENTORY);
        when(inventory.getItem()).thenReturn(item);

        Auction auction = Auction.builder()
                .inventory(inventory)
                .auctionOwner(seller)
                .type(type)
                .startPrice(1000L)
                .readyStartTime(now.minusMinutes(1))
                .maxParticipants(20)
                .entryDeadline(now.plusMinutes(entryDeadlineOffsetMinutes))
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION);
        when(auctionRepository.findById(AUCTION)).thenReturn(Optional.of(auction));
        return auction;
    }

    //OPEN 경매를 만든다. commitDeadline은 현재 시각 기준 offset(분) 이후, PRIVATE이면 리빌 마감은 commitDeadline + 3분
    private Auction openAuction(AuctionType type, long commitDeadlineOffsetMinutes) {
        Auction auction = readyAuction(type, -5);
        LocalDateTime commitDeadline = LocalDateTime.now(KST).plusMinutes(commitDeadlineOffsetMinutes);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.OPEN);
        ReflectionTestUtils.setField(auction, "openStartTime", commitDeadline.minusMinutes(10));
        ReflectionTestUtils.setField(auction, "commitDeadline", commitDeadline);
        if (type == AuctionType.PRIVATE) {
            ReflectionTestUtils.setField(auction, "revealDeadline", commitDeadline.plusMinutes(3));
        }
        return auction;
    }

    private void withBidder(Auction auction) {
        ReflectionTestUtils.setField(auction, "currentBidder", winner);
        ReflectionTestUtils.setField(auction, "currentPrice", PRICE);
    }

    private void fillRoom(int count) {
        for (int i = 1; i <= count; i++) {
            registry.join(AUCTION, new AuctionParticipantState((long) i, "s" + i, "n" + i, null, AUCTION));
        }
    }

    private void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private void assertNothingHappened() {
        verify(auctionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(balanceService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(entryListRepository);
        verify(inventoryService, never()).restoreForAuction(any());
        verify(inventoryService, never()).grantAuctionItem(any(), any());
    }

    // ---------- 공통 ----------

    @Test
    @DisplayName("존재하지 않는 경매면 AUCTION_NOT_FOUND")
    void notFound() {
        when(auctionRepository.findById(AUCTION)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.transition(AUCTION), ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("CLOSED 경매는 ALREADY_ENDED")
    void closedIsAlreadyEnded() {
        Auction auction = readyAuction(AuctionType.PUBLIC, 5);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.CLOSED);

        assertErrorCode(() -> service.transition(AUCTION), ErrorCode.ALREADY_ENDED);
        assertNothingHappened();
    }

    @Test
    @DisplayName("FAILED 경매는 ALREADY_ENDED")
    void failedIsAlreadyEnded() {
        Auction auction = readyAuction(AuctionType.PUBLIC, 5);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.FAILED);

        assertErrorCode(() -> service.transition(AUCTION), ErrorCode.ALREADY_ENDED);
        assertNothingHappened();
    }

    // ---------- READY ----------

    @Test
    @DisplayName("READY: 정원(20명)이 차면 대기 시간이 남아도 OPEN, PUBLIC은 리빌 마감이 없다")
    void readyFullOpensPublic() {
        Auction auction = readyAuction(AuctionType.PUBLIC, 5);
        fillRoom(20);

        AuctionTransitionResult result = service.transition(AUCTION);

        assertThat(result.status()).isEqualTo(AuctionStatus.OPEN);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.OPEN);
        assertThat(auction.getOpenStartTime()).isNotNull();
        assertThat(auction.getCommitDeadline()).isEqualTo(auction.getOpenStartTime().plusMinutes(10));
        assertThat(auction.getRevealDeadline()).isNull();
        assertThat(result.commitDeadline()).isEqualTo(auction.getCommitDeadline());
        assertThat(result.revealDeadline()).isNull();
        verify(auctionRepository).saveAndFlush(auction);
        verifyNoInteractions(eventPublisher);   // OPEN 전환은 이벤트를 발행하지 않는다
    }

    @Test
    @DisplayName("READY: PRIVATE이 OPEN되면 입찰/커밋 10분 + 리빌 3분이 계산된다")
    void readyFullOpensPrivate() {
        Auction auction = readyAuction(AuctionType.PRIVATE, 5);
        fillRoom(20);

        service.transition(AUCTION);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.OPEN);
        assertThat(auction.getCommitDeadline()).isEqualTo(auction.getOpenStartTime().plusMinutes(10));
        assertThat(auction.getRevealDeadline()).isEqualTo(auction.getCommitDeadline().plusMinutes(3));
    }

    @Test
    @DisplayName("READY: 대기 시간이 남아 있고 정원 미달이면 AUCTION_CONDITION_NOT_MET")
    void readyConditionNotMet() {
        Auction auction = readyAuction(AuctionType.PUBLIC, 5);
        fillRoom(19);

        assertErrorCode(() -> service.transition(AUCTION), ErrorCode.AUCTION_CONDITION_NOT_MET);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.READY);
        assertNothingHappened();
    }

    @Test
    @DisplayName("READY: 대기 시간이 지났고 최소 인원(5명)이면 OPEN")
    void readyDeadlinePassedWithMinParticipantsOpens() {
        Auction auction = readyAuction(AuctionType.PUBLIC, -1);
        fillRoom(5);

        AuctionTransitionResult result = service.transition(AUCTION);

        assertThat(result.status()).isEqualTo(AuctionStatus.OPEN);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.OPEN);
    }

    @Test
    @DisplayName("READY: 대기 시간이 지났고 인원이 4명이면 FAILED, 판매자 수량 복구, 종료 이벤트(낙찰자 없음) 발행")
    void readyDeadlinePassedBelowMinFails() {
        Auction auction = readyAuction(AuctionType.PUBLIC, -1);
        fillRoom(4);

        AuctionTransitionResult result = service.transition(AUCTION);

        assertThat(result.status()).isEqualTo(AuctionStatus.FAILED);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.FAILED);
        verify(inventoryService).restoreForAuction(INVENTORY);

        ArgumentCaptor<AuctionClosedEvent> captor = ArgumentCaptor.forClass(AuctionClosedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new AuctionClosedEvent(AUCTION, null, null));
        verifyNoInteractions(balanceService);
    }

    @Test
    @DisplayName("READY: 대기 시간이 지났고 입장자가 없어도 FAILED")
    void readyDeadlinePassedEmptyRoomFails() {
        readyAuction(AuctionType.PUBLIC, -1);

        AuctionTransitionResult result = service.transition(AUCTION);

        assertThat(result.status()).isEqualTo(AuctionStatus.FAILED);
        verify(inventoryService).restoreForAuction(INVENTORY);
    }

    // ---------- OPEN (PUBLIC) ----------

    @Test
    @DisplayName("OPEN PUBLIC: 마감 시각이 지나고 최고 입찰자가 있으면 CLOSED, 판매자 입금 -> 낙찰자 아이템 지급 -> 종료 이벤트")
    void publicDeadlinePassedWithBidderCloses() {
        Auction auction = openAuction(AuctionType.PUBLIC, -1);
        withBidder(auction);

        AuctionTransitionResult result = service.transition(AUCTION);

        assertThat(result.status()).isEqualTo(AuctionStatus.CLOSED);
        assertThat(result.winnerId()).isEqualTo(WINNER);
        assertThat(result.finalPrice()).isEqualTo(PRICE);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CLOSED);
        assertThat(auction.getFinalPrice()).isEqualTo(PRICE);

        //경매 상태를 가장 먼저 반영해야 낙관적 락 충돌을 돈/아이템 처리보다 앞에서 감지한다.
        InOrder order = inOrder(auctionRepository, balanceService, inventoryService, eventPublisher);
        order.verify(auctionRepository).saveAndFlush(auction);
        order.verify(balanceService).deposit(SELLER, PRICE, BalanceChangeType.AUCTION_SALE,
                ReferenceType.AUCTION, AUCTION);
        order.verify(inventoryService).grantAuctionItem(winner, item);
        order.verify(eventPublisher).publishEvent(new AuctionClosedEvent(AUCTION, WINNER, PRICE));
        verify(inventoryService, never()).restoreForAuction(any());
    }

    @Test
    @DisplayName("OPEN PUBLIC: 마감 시각이 지났는데 입찰자가 없으면 FAILED, 판매자 수량 복구")
    void publicDeadlinePassedWithoutBidderFails() {
        Auction auction = openAuction(AuctionType.PUBLIC, -1);

        AuctionTransitionResult result = service.transition(AUCTION);

        assertThat(result.status()).isEqualTo(AuctionStatus.FAILED);
        assertThat(auction.getFinalPrice()).isNull();
        verify(inventoryService).restoreForAuction(INVENTORY);
        verify(eventPublisher).publishEvent(new AuctionClosedEvent(AUCTION, null, null));
        verifyNoInteractions(balanceService);
        verify(inventoryService, never()).grantAuctionItem(any(), any());
    }

    @Test
    @DisplayName("OPEN PUBLIC: 마감 전이라도 마지막 입찰 후 31초가 지나면 CLOSED")
    void publicIdleTimeoutCloses() {
        Auction auction = openAuction(AuctionType.PUBLIC, 5);
        withBidder(auction);
        when(publicBidRepository.findLastBidTime(AUCTION))
                .thenReturn(Optional.of(LocalDateTime.now(KST).minusSeconds(31)));

        AuctionTransitionResult result = service.transition(AUCTION);

        assertThat(result.status()).isEqualTo(AuctionStatus.CLOSED);
        assertThat(result.winnerId()).isEqualTo(WINNER);
    }

    @Test
    @DisplayName("OPEN PUBLIC: 마지막 입찰 후 10초밖에 안 지났고 마감 전이면 AUCTION_CONDITION_NOT_MET")
    void publicRecentBidNotMet() {
        Auction auction = openAuction(AuctionType.PUBLIC, 5);
        withBidder(auction);
        when(publicBidRepository.findLastBidTime(AUCTION))
                .thenReturn(Optional.of(LocalDateTime.now(KST).minusSeconds(10)));

        assertErrorCode(() -> service.transition(AUCTION), ErrorCode.AUCTION_CONDITION_NOT_MET);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.OPEN);
        assertNothingHappened();
    }

    @Test
    @DisplayName("OPEN PUBLIC: 입찰이 없고 마감 전이면 AUCTION_CONDITION_NOT_MET (무입찰은 마감 시각에만 종료)")
    void publicNoBidBeforeDeadlineNotMet() {
        openAuction(AuctionType.PUBLIC, 5);

        assertErrorCode(() -> service.transition(AUCTION), ErrorCode.AUCTION_CONDITION_NOT_MET);
        assertNothingHappened();
    }

    @Test
    @DisplayName("OPEN PUBLIC: 판매자가 없으면 정산하지 않고 IllegalStateException (트랜잭션 롤백)")
    void publicWithoutSellerThrows() {
        Auction auction = openAuction(AuctionType.PUBLIC, -1);
        withBidder(auction);
        ReflectionTestUtils.setField(auction, "auctionOwner", null);

        assertThatThrownBy(() -> service.transition(AUCTION)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(balanceService);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("낙관적 락 충돌이면 RESOURCE_BUSY, 돈/아이템 처리와 이벤트는 일어나지 않는다")
    void optimisticLockConflictIsResourceBusy() {
        Auction auction = openAuction(AuctionType.PUBLIC, -1);
        withBidder(auction);
        when(auctionRepository.saveAndFlush(any(Auction.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Auction.class, AUCTION));

        assertErrorCode(() -> service.transition(AUCTION), ErrorCode.RESOURCE_BUSY);
        verifyNoInteractions(balanceService);
        verifyNoInteractions(eventPublisher);
        verify(inventoryService, never()).grantAuctionItem(any(), any());
    }

    // ---------- OPEN (PRIVATE) ----------

    @Test
    @DisplayName("OPEN PRIVATE: 리빌 마감 전이면 AUCTION_CONDITION_NOT_MET")
    void privateBeforeRevealDeadlineNotMet() {
        openAuction(AuctionType.PRIVATE, -1);   // commitDeadline은 지났지만 revealDeadline(+3분)은 남음

        assertErrorCode(() -> service.transition(AUCTION), ErrorCode.AUCTION_CONDITION_NOT_MET);
        assertNothingHappened();
    }

    @Test
    @DisplayName("OPEN PRIVATE: 리빌 마감이 지나면 아직 지원하지 않으므로 UnsupportedOperationException, 아무것도 변경하지 않는다")
    void privateAfterRevealDeadlineUnsupported() {
        Auction auction = openAuction(AuctionType.PRIVATE, -10);

        assertThatThrownBy(() -> service.transition(AUCTION)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.OPEN);
        assertNothingHappened();
    }

    // ---------- OPEN 전환 시 참가자 명단 ----------

    @Test
    @DisplayName("READY -> OPEN: 그 순간 방에 있던 회원을 참가자 명단으로 저장한다")
    @SuppressWarnings("unchecked")
    void openSavesParticipantList() {
        readyAuction(AuctionType.PUBLIC, -1);
        fillRoom(6);

        service.transition(AUCTION);

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(entryListRepository).saveParticipants(eq(AUCTION), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    @DisplayName("READY -> FAILED는 참가자 명단을 저장하지 않는다")
    void failDoesNotSaveParticipantList() {
        readyAuction(AuctionType.PUBLIC, -1);
        fillRoom(4);

        service.transition(AUCTION);

        verifyNoInteractions(entryListRepository);
    }

    @Test
    @DisplayName("참가자 명단 저장에 실패하면 예외가 전파되어 OPEN 전환이 롤백된다")
    void participantListFailurePropagates() {
        readyAuction(AuctionType.PUBLIC, 5);
        fillRoom(20);
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(entryListRepository).saveParticipants(anyLong(), any());

        assertThatThrownBy(() -> service.transition(AUCTION)).isInstanceOf(RedisConnectionFailureException.class);
        verifyNoInteractions(eventPublisher);
    }
}
