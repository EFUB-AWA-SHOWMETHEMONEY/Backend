package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.request.PrivateBidCommitRequest;
import efub.awa.moamoa.auction.dto.response.PrivateBidCommitResponse;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.entity.PrivateBid;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.auction.repository.PrivateBidRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrivateBidServiceTest {

    private static final long AUCTION = 16L;
    private static final long BIDDER = 42L;
    private static final String HASH = "a".repeat(64);

    private AuctionParticipantChecker participantChecker;
    private AuctionRepository auctionRepository;
    private PrivateBidRepository privateBidRepository;
    private MemberRepository memberRepository;
    private PrivateBidService service;

    private Member bidder;

    @BeforeEach
    void setUp() {
        participantChecker = mock(AuctionParticipantChecker.class);
        auctionRepository = mock(AuctionRepository.class);
        privateBidRepository = mock(PrivateBidRepository.class);
        memberRepository = mock(MemberRepository.class);
        service = new PrivateBidService(participantChecker, auctionRepository, privateBidRepository, memberRepository);

        bidder = mock(Member.class);
        when(bidder.getId()).thenReturn(BIDDER);
        when(memberRepository.findById(BIDDER)).thenReturn(Optional.of(bidder));

        // 기본: 방에 있고, 기존 active 커밋 없음, save 시 id 부여
        when(participantChecker.isParticipant(AUCTION, BIDDER)).thenReturn(true);
        when(privateBidRepository.findByAuctionIdAndBidderIdAndActiveTrue(AUCTION, BIDDER)).thenReturn(Optional.empty());
        when(privateBidRepository.save(any(PrivateBid.class))).thenAnswer(inv -> {
            PrivateBid bid = inv.getArgument(0);
            ReflectionTestUtils.setField(bid, "id", 34L);
            return bid;
        });
    }

    private Auction auction(AuctionType type, AuctionStatus status, LocalDateTime commitDeadline) {
        Auction auction = Auction.builder()
                .type(type)
                .startPrice(1000L)
                .readyStartTime(LocalDateTime.now().minusHours(1))
                .commitDeadline(commitDeadline)
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION);
        ReflectionTestUtils.setField(auction, "status", status);   // 생성 시 READY 고정이라 리플렉션으로 변경
        when(auctionRepository.findByIdForUpdate(AUCTION)).thenReturn(Optional.of(auction));
        return auction;
    }

    private Auction openPrivateAuction() {
        return auction(AuctionType.PRIVATE, AuctionStatus.OPEN, LocalDateTime.now().plusMinutes(10));
    }

    private PrivateBidCommitRequest request() {
        return new PrivateBidCommitRequest(HASH);
    }

    private void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    // ---------- 성공 ----------

    @Test
    @DisplayName("첫 커밋: 저장되고 previousBidInvalidated=false")
    void firstCommit() {
        openPrivateAuction();

        PrivateBidCommitResponse response = service.commit(BIDDER, AUCTION, request());

        assertThat(response.bidId()).isEqualTo(34L);
        assertThat(response.auctionId()).isEqualTo(AUCTION);
        assertThat(response.bidderId()).isEqualTo(BIDDER);
        assertThat(response.committedAt()).isNotNull();
        assertThat(response.previousBidInvalidated()).isFalse();

        ArgumentCaptor<PrivateBid> saved = ArgumentCaptor.forClass(PrivateBid.class);
        verify(privateBidRepository).save(saved.capture());
        assertThat(saved.getValue().getCommitHash()).isEqualTo(HASH);
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(saved.getValue().isVerified()).isFalse();
    }

    @Test
    @DisplayName("재제출: 이전 커밋은 무효화되고 새 커밋만 active, previousBidInvalidated=true")
    void resubmitInvalidatesPrevious() {
        openPrivateAuction();
        PrivateBid previous = PrivateBid.builder()
                .auction(auction(AuctionType.PRIVATE, AuctionStatus.OPEN, LocalDateTime.now().plusMinutes(10)))
                .bidder(bidder)
                .commitHash("b".repeat(64))
                .committedAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(privateBidRepository.findByAuctionIdAndBidderIdAndActiveTrue(AUCTION, BIDDER))
                .thenReturn(Optional.of(previous));

        PrivateBidCommitResponse response = service.commit(BIDDER, AUCTION, request());

        assertThat(previous.isActive()).isFalse();
        assertThat(response.previousBidInvalidated()).isTrue();

        ArgumentCaptor<PrivateBid> saved = ArgumentCaptor.forClass(PrivateBid.class);
        verify(privateBidRepository).save(saved.capture());
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(saved.getValue().getCommitHash()).isEqualTo(HASH);
    }

    @Test
    @DisplayName("경매는 동시 커밋 방지를 위해 락 조회(findByIdForUpdate)로 가져온다")
    void usesLockingLookup() {
        openPrivateAuction();

        service.commit(BIDDER, AUCTION, request());

        verify(auctionRepository).findByIdForUpdate(AUCTION);
        verify(auctionRepository, never()).findById(any());
    }

    // ---------- 조회 실패 ----------

    @Test
    @DisplayName("존재하지 않는 회원은 MEMBER_NOT_FOUND")
    void memberNotFound() {
        openPrivateAuction();
        when(memberRepository.findById(BIDDER)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.commit(BIDDER, AUCTION, request()), ErrorCode.MEMBER_NOT_FOUND);
        verify(privateBidRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 경매는 AUCTION_NOT_FOUND")
    void auctionNotFound() {
        when(auctionRepository.findByIdForUpdate(AUCTION)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.commit(BIDDER, AUCTION, request()), ErrorCode.AUCTION_NOT_FOUND);
        verify(privateBidRepository, never()).save(any());
    }

    // ---------- 경매 조건 ----------

    @Test
    @DisplayName("PRIVATE 경매가 아니면 NOT_PRIVATE_AUCTION")
    void notPrivateAuction() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN, LocalDateTime.now().plusMinutes(10));

        assertErrorCode(() -> service.commit(BIDDER, AUCTION, request()), ErrorCode.NOT_PRIVATE_AUCTION);
        verify(privateBidRepository, never()).save(any());
    }

    @Test
    @DisplayName("OPEN이 아닌 경매(READY/CLOSED/FAILED)는 INVALID_AUCTION_STATE")
    void notOpenStatus() {
        for (AuctionStatus status : new AuctionStatus[]{AuctionStatus.READY, AuctionStatus.CLOSED, AuctionStatus.FAILED}) {
            auction(AuctionType.PRIVATE, status, LocalDateTime.now().plusMinutes(10));

            assertErrorCode(() -> service.commit(BIDDER, AUCTION, request()), ErrorCode.INVALID_AUCTION_STATE);
        }
        verify(privateBidRepository, never()).save(any());
    }

    @Test
    @DisplayName("커밋 마감 이후에는 INVALID_AUCTION_STATE")
    void afterCommitDeadline() {
        auction(AuctionType.PRIVATE, AuctionStatus.OPEN, LocalDateTime.now().minusSeconds(1));

        assertErrorCode(() -> service.commit(BIDDER, AUCTION, request()), ErrorCode.INVALID_AUCTION_STATE);
        verify(privateBidRepository, never()).save(any());
    }

    @Test
    @DisplayName("commitDeadline이 없으면 NPE 없이 INVALID_AUCTION_STATE")
    void nullCommitDeadline() {
        auction(AuctionType.PRIVATE, AuctionStatus.OPEN, null);

        assertErrorCode(() -> service.commit(BIDDER, AUCTION, request()), ErrorCode.INVALID_AUCTION_STATE);
    }

    // ---------- 참여자 검증 ----------

    @Test
    @DisplayName("경매 참여자가 아니면 NOT_AUCTION_PARTICIPANT(403)이고 저장·무효화되지 않는다")
    void notParticipant() {
        openPrivateAuction();
        when(participantChecker.isParticipant(AUCTION, BIDDER)).thenReturn(false);
        PrivateBid existing = PrivateBid.builder()
                .auction(openPrivateAuction())
                .bidder(bidder)
                .commitHash("b".repeat(64))
                .committedAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(privateBidRepository.findByAuctionIdAndBidderIdAndActiveTrue(AUCTION, BIDDER))
                .thenReturn(Optional.of(existing));

        assertErrorCode(() -> service.commit(BIDDER, AUCTION, request()), ErrorCode.NOT_AUCTION_PARTICIPANT);

        assertThat(existing.isActive()).isTrue();
        verify(privateBidRepository, never()).save(any());
        assertThat(ErrorCode.NOT_AUCTION_PARTICIPANT.getStatus()).isEqualTo(403);
    }
}
