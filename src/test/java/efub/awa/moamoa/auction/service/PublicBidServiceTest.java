package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.request.PublicBidRequest;
import efub.awa.moamoa.auction.dto.response.PublicBidResponse;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.entity.PublicBid;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.event.BidPlacedEvent;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.auction.repository.PublicBidRepository;
import efub.awa.moamoa.balance.enums.BalanceChangeType;
import efub.awa.moamoa.balance.enums.ReferenceType;
import efub.awa.moamoa.balance.service.BalanceService;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicBidServiceTest {

    private static final long AUCTION = 15L;
    private static final long BIDDER = 5L;
    private static final long PREVIOUS = 3L;
    private static final long START_PRICE = 1000L;

    private AuctionRepository auctionRepository;
    private PublicBidRepository publicBidRepository;
    private MemberRepository memberRepository;
    private BalanceService balanceService;
    private AuctionService auctionService;
    private ApplicationEventPublisher eventPublisher;
    private PublicBidService service;

    private Member bidder;
    private Member previous;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        publicBidRepository = mock(PublicBidRepository.class);
        memberRepository = mock(MemberRepository.class);
        balanceService = mock(BalanceService.class);
        auctionService = mock(AuctionService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new PublicBidService(auctionRepository, publicBidRepository, memberRepository,
                balanceService, auctionService, eventPublisher);

        bidder = member(BIDDER, "입찰자");
        previous = member(PREVIOUS, "이전입찰자");

        when(publicBidRepository.findLastBidTime(AUCTION)).thenReturn(Optional.empty());
        when(publicBidRepository.save(any(PublicBid.class))).thenAnswer(inv -> {
            PublicBid bid = inv.getArgument(0);
            ReflectionTestUtils.setField(bid, "id", 101L);
            return bid;
        });
        when(auctionService.isAttending(any(Auction.class), eq(BIDDER))).thenReturn(true);
    }

    // ---------- 픽스처 ----------

    private Member member(long id, String nickname) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        when(member.getNickname()).thenReturn(nickname);
        when(memberRepository.findByIdForUpdate(id)).thenReturn(Optional.of(member));
        return member;
    }

    //OPEN 경매. openStartTime은 현재 시각 기준 openedMinutesAgo분 전, commitDeadline은 그로부터 10분 뒤
    private Auction openAuction(AuctionType type, long openedMinutesAgo) {
        Auction auction = Auction.builder()
                .inventory(mock(Inventory.class))
                .auctionOwner(mock(Member.class))
                .type(type)
                .startPrice(START_PRICE)
                .readyStartTime(LocalDateTime.now().minusMinutes(openedMinutesAgo + 10))
                .maxParticipants(20)
                .entryDeadline(LocalDateTime.now().minusMinutes(openedMinutesAgo))
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION);
        auction.open(LocalDateTime.now().minusMinutes(openedMinutesAgo));
        when(auctionRepository.findByIdForUpdate(AUCTION)).thenReturn(Optional.of(auction));
        return auction;
    }

    private Auction openPublic() {
        return openAuction(AuctionType.PUBLIC, 1);
    }

    //이전 최고 입찰이 있는 상태 (lastBidSecondsAgo초 전 입찰)
    private void withPreviousBid(Auction auction, Member highest, long price, long lastBidSecondsAgo) {
        ReflectionTestUtils.setField(auction, "currentBidder", highest);
        ReflectionTestUtils.setField(auction, "currentPrice", price);
        when(publicBidRepository.findLastBidTime(AUCTION))
                .thenReturn(Optional.of(LocalDateTime.now().minusSeconds(lastBidSecondsAgo)));
    }

    private void setCommitDeadline(Auction auction, LocalDateTime deadline) {
        ReflectionTestUtils.setField(auction, "commitDeadline", deadline);
    }

    private PublicBidRequest bid(long amount) {
        return new PublicBidRequest(amount);
    }

    private void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private void assertNothingChanged() {
        verifyNoInteractions(balanceService);
        verifyNoInteractions(eventPublisher);
        verify(publicBidRepository, never()).save(any());
    }

    // ---------- 경매 상태 ----------

    @Test
    @DisplayName("존재하지 않는 경매는 AUCTION_NOT_FOUND")
    void notFound() {
        when(auctionRepository.findByIdForUpdate(AUCTION)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(2000)), ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("PRIVATE 경매는 NOT_PUBLIC_AUCTION")
    void privateAuction() {
        openAuction(AuctionType.PRIVATE, 1);

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(2000)), ErrorCode.NOT_PUBLIC_AUCTION);
        assertNothingChanged();
    }

    @Test
    @DisplayName("CLOSED/FAILED 경매는 ALREADY_ENDED")
    void endedAuction() {
        Auction auction = openPublic();
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.CLOSED);
        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(2000)), ErrorCode.ALREADY_ENDED);

        ReflectionTestUtils.setField(auction, "status", AuctionStatus.FAILED);
        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(2000)), ErrorCode.ALREADY_ENDED);
        assertNothingChanged();
    }

    @Test
    @DisplayName("READY 경매는 AUCTION_NOT_BIDDABLE")
    void readyAuction() {
        Auction auction = openPublic();
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.READY);

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(2000)), ErrorCode.AUCTION_NOT_BIDDABLE);
        assertNothingChanged();
    }

    @Test
    @DisplayName("마감 시각이 지난 경매는 스케줄러 처리 전이어도 AUCTION_NOT_BIDDABLE")
    void deadlinePassed() {
        Auction auction = openPublic();
        setCommitDeadline(auction, LocalDateTime.now().minusSeconds(1));

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(2000)), ErrorCode.AUCTION_NOT_BIDDABLE);
        assertNothingChanged();
    }

    @Test
    @DisplayName("마지막 입찰 후 30초가 지난 경매는 AUCTION_NOT_BIDDABLE")
    void idleTimeout() {
        Auction auction = openPublic();
        withPreviousBid(auction, previous, 2000L, 31);

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(3000)), ErrorCode.AUCTION_NOT_BIDDABLE);
        assertNothingChanged();
    }

    // ---------- 참가자 / 최고 입찰자 / 입찰가 ----------

    @Test
    @DisplayName("참가 중이 아니면(퇴장한 PUBLIC 사용자, 판매자 등) NOT_AUCTION_PARTICIPANT")
    void notAttending() {
        openPublic();
        when(auctionService.isAttending(any(Auction.class), eq(BIDDER))).thenReturn(false);

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(2000)), ErrorCode.NOT_AUCTION_PARTICIPANT);
        assertNothingChanged();
    }

    @Test
    @DisplayName("현재 최고 입찰자가 다시 입찰하면 ALREADY_HIGHEST_BIDDER")
    void alreadyHighestBidder() {
        Auction auction = openPublic();
        withPreviousBid(auction, bidder, 2000L, 5);

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(3000)), ErrorCode.ALREADY_HIGHEST_BIDDER);
        assertNothingChanged();
    }

    @Test
    @DisplayName("첫 입찰이 startPrice 미만이면 BID_TOO_LOW")
    void firstBidBelowStartPrice() {
        openPublic();

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(START_PRICE - 1)), ErrorCode.BID_TOO_LOW);
        assertNothingChanged();
    }

    @Test
    @DisplayName("이후 입찰이 currentPrice 이하이면 BID_TOO_LOW (같은 금액도 불가)")
    void laterBidNotHigher() {
        Auction auction = openPublic();
        withPreviousBid(auction, previous, 2000L, 5);

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(2000)), ErrorCode.BID_TOO_LOW);
        assertNothingChanged();
    }

    @Test
    @DisplayName("잔액이 부족하면 INSUFFICIENT_BALANCE, 반환·저장·이벤트·경매 갱신이 일어나지 않는다")
    void insufficientBalance() {
        Auction auction = openPublic();
        withPreviousBid(auction, previous, 2000L, 5);
        when(balanceService.withdraw(eq(BIDDER), eq(3000L), any(), any(), anyLong()))
                .thenThrow(new CustomException(ErrorCode.INSUFFICIENT_BALANCE));

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(3000)), ErrorCode.INSUFFICIENT_BALANCE);
        verify(balanceService, never()).deposit(anyLong(), anyLong(), any(), any(), anyLong());
        verify(publicBidRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
        assertThat(auction.getCurrentPrice()).isEqualTo(2000L);
        assertThat(auction.getCurrentBidder()).isEqualTo(previous);
    }

    // ---------- 성공 ----------

    @Test
    @DisplayName("첫 입찰(startPrice와 같은 금액): 차감만 하고 반환은 없다. 경매 갱신, 입찰 저장, 이벤트 발행")
    void firstBidSuccess() {
        Auction auction = openPublic();
        LocalDateTime deadline = auction.getCommitDeadline();

        PublicBidResponse response = service.placeBid(BIDDER, AUCTION, bid(START_PRICE));

        verify(balanceService).withdraw(BIDDER, START_PRICE, BalanceChangeType.AUCTION_BID, ReferenceType.AUCTION, AUCTION);
        verify(balanceService, never()).deposit(anyLong(), anyLong(), any(), any(), anyLong());
        assertThat(auction.getCurrentBidder()).isEqualTo(bidder);
        assertThat(auction.getCurrentPrice()).isEqualTo(START_PRICE);
        assertThat(auction.getCommitDeadline()).isEqualTo(deadline);   // 마감까지 여유가 있어 연장하지 않음

        assertThat(response.bidId()).isEqualTo(101L);
        assertThat(response.auctionId()).isEqualTo(AUCTION);
        assertThat(response.bidderId()).isEqualTo(BIDDER);
        assertThat(response.amount()).isEqualTo(START_PRICE);
        assertThat(response.currentPrice()).isEqualTo(START_PRICE);
        assertThat(response.commitDeadline()).isEqualTo(deadline);

        ArgumentCaptor<BidPlacedEvent> captor = ArgumentCaptor.forClass(BidPlacedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new BidPlacedEvent(AUCTION, BIDDER, "입찰자", START_PRICE,
                deadline.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
    }

    @Test
    @DisplayName("이전 최고 입찰자가 있으면 입찰자 차감 후 이전 입찰자에게 이전 현재가를 반환한다")
    void refundPreviousBidder() {
        Auction auction = openPublic();
        withPreviousBid(auction, previous, 2000L, 5);

        service.placeBid(BIDDER, AUCTION, bid(3000));

        InOrder order = inOrder(balanceService);
        order.verify(balanceService).withdraw(BIDDER, 3000L, BalanceChangeType.AUCTION_BID, ReferenceType.AUCTION, AUCTION);
        order.verify(balanceService).deposit(PREVIOUS, 2000L, BalanceChangeType.AUCTION_BID_REFUND, ReferenceType.AUCTION, AUCTION);
        assertThat(auction.getCurrentBidder()).isEqualTo(bidder);
        assertThat(auction.getCurrentPrice()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("회원 행은 id 오름차순으로 잠근다 (입찰자 5, 이전 입찰자 3 -> 3, 5 순서)")
    void locksMembersInIdOrder() {
        Auction auction = openPublic();
        withPreviousBid(auction, previous, 2000L, 5);

        service.placeBid(BIDDER, AUCTION, bid(3000));

        InOrder order = inOrder(memberRepository, balanceService);
        order.verify(memberRepository).findByIdForUpdate(PREVIOUS);
        order.verify(memberRepository).findByIdForUpdate(BIDDER);
        order.verify(balanceService).withdraw(eq(BIDDER), anyLong(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("이전 최고 입찰자가 탈퇴해 없으면 반환 없이, 현재가보다 높은 금액만 받는다")
    void previousBidderWithdrawn() {
        Auction auction = openPublic();
        withPreviousBid(auction, null, 2000L, 5);

        assertErrorCode(() -> service.placeBid(BIDDER, AUCTION, bid(1500)), ErrorCode.BID_TOO_LOW);

        service.placeBid(BIDDER, AUCTION, bid(2500));
        verify(balanceService, never()).deposit(anyLong(), anyLong(), any(), any(), anyLong());
        assertThat(auction.getCurrentPrice()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("마지막 입찰 후 10초밖에 안 지났으면 입찰을 받는다")
    void recentBidStillBiddable() {
        Auction auction = openPublic();
        withPreviousBid(auction, previous, 2000L, 10);

        PublicBidResponse response = service.placeBid(BIDDER, AUCTION, bid(2500));

        assertThat(response.currentPrice()).isEqualTo(2500L);
    }

    // ---------- 소프트클로즈 ----------

    @Test
    @DisplayName("소프트클로즈: 마감까지 10초 이하로 남았으면 마감을 10초 늘린다")
    void softCloseExtends() {
        Auction auction = openAuction(AuctionType.PUBLIC, 5);
        LocalDateTime deadline = LocalDateTime.now().plusSeconds(5);
        setCommitDeadline(auction, deadline);

        PublicBidResponse response = service.placeBid(BIDDER, AUCTION, bid(START_PRICE));

        assertThat(response.commitDeadline()).isEqualTo(deadline.plusSeconds(10));
        assertThat(auction.getCommitDeadline()).isEqualTo(deadline.plusSeconds(10));
    }

    @Test
    @DisplayName("소프트클로즈: 마감까지 15초 남았으면 연장하지 않는다")
    void softCloseNotNeeded() {
        Auction auction = openAuction(AuctionType.PUBLIC, 5);
        LocalDateTime deadline = LocalDateTime.now().plusSeconds(15);
        setCommitDeadline(auction, deadline);

        PublicBidResponse response = service.placeBid(BIDDER, AUCTION, bid(START_PRICE));

        assertThat(response.commitDeadline()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("소프트클로즈: 연장해도 openStartTime + 13분을 넘지 않는다")
    void softCloseCapped() {
        Auction auction = openAuction(AuctionType.PUBLIC, 5);
        //이미 상한(openStartTime + 13분)까지 연장되어 마감이 4초 남은 상태 -> 10초 늘리면 상한을 넘으므로 상한 그대로
        ReflectionTestUtils.setField(auction, "openStartTime", LocalDateTime.now().minusMinutes(13).plusSeconds(4));
        LocalDateTime limit = auction.getOpenStartTime().plusMinutes(13);
        setCommitDeadline(auction, limit);

        PublicBidResponse response = service.placeBid(BIDDER, AUCTION, bid(START_PRICE));

        assertThat(response.commitDeadline()).isEqualTo(limit);
    }
}
