package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.request.PrivateBidRevealRequest;
import efub.awa.moamoa.auction.dto.response.PrivateBidRevealResponse;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.entity.PrivateBid;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.auction.repository.PrivateBidRepository;
import efub.awa.moamoa.auction.support.CommitHasher;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrivateBidRevealServiceTest {

    private static final long AUCTION = 16L;
    private static final long BIDDER = 42L;
    private static final long AMOUNT = 5000L;
    private static final String SALT = "abcdefghijklmnop";   // 16자
    // `printf '5000:abcdefghijklmnop' | shasum -a 256` 로 구한 값 (구현과 독립된 테스트 벡터)
    private static final String HASH = "afe20295a762141440392d2d179100995892b30dfe31aafdfb3493f519044dd8";

    private AuctionRepository auctionRepository;
    private PrivateBidRepository privateBidRepository;
    private MemberRepository memberRepository;
    private PrivateBidService service;

    private Member bidder;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        privateBidRepository = mock(PrivateBidRepository.class);
        memberRepository = mock(MemberRepository.class);
        service = new PrivateBidService(mock(AuctionParticipantChecker.class), auctionRepository,
                privateBidRepository, memberRepository);

        bidder = mock(Member.class);
        when(bidder.getId()).thenReturn(BIDDER);
        when(bidder.getCapital()).thenReturn(10_000L);
        when(memberRepository.findById(BIDDER)).thenReturn(Optional.of(bidder));
    }

    private Auction auction(AuctionType type, AuctionStatus status,
                            LocalDateTime commitDeadline, LocalDateTime revealDeadline) {
        Auction auction = Auction.builder()
                .type(type)
                .startPrice(1000L)
                .readyStartTime(LocalDateTime.now().minusHours(1))
                .commitDeadline(commitDeadline)
                .revealDeadline(revealDeadline)
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION);
        ReflectionTestUtils.setField(auction, "status", status);
        when(auctionRepository.findByIdForUpdate(AUCTION)).thenReturn(Optional.of(auction));
        return auction;
    }

    /** commit 마감은 지났고 reveal 마감은 아직인 정상 reveal 구간의 PRIVATE 경매 */
    private Auction revealPhaseAuction() {
        return auction(AuctionType.PRIVATE, AuctionStatus.OPEN,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
    }

    private PrivateBid activeCommit(Auction auction, String hash) {
        PrivateBid bid = PrivateBid.builder()
                .auction(auction)
                .bidder(bidder)
                .commitHash(hash)
                .committedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        ReflectionTestUtils.setField(bid, "id", 34L);
        when(privateBidRepository.findByAuctionIdAndBidderIdAndActiveTrue(AUCTION, BIDDER))
                .thenReturn(Optional.of(bid));
        return bid;
    }

    private PrivateBidRevealRequest request() {
        return new PrivateBidRevealRequest(AMOUNT, SALT);
    }

    private void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    // ---------- 성공 ----------

    @Test
    @DisplayName("올바른 금액·salt면 reveal 되고 verified=true, 금액·시각이 기록된다")
    void revealSuccess() {
        PrivateBid bid = activeCommit(revealPhaseAuction(), HASH);

        PrivateBidRevealResponse response = service.reveal(BIDDER, AUCTION, request());

        assertThat(response.bidId()).isEqualTo(34L);
        assertThat(response.auctionId()).isEqualTo(AUCTION);
        assertThat(response.bidderId()).isEqualTo(BIDDER);
        assertThat(response.revealedAmount()).isEqualTo(AMOUNT);
        assertThat(response.revealedAt()).isNotNull();

        assertThat(bid.isVerified()).isTrue();
        assertThat(bid.getRevealedAmount()).isEqualTo(AMOUNT);
        assertThat(bid.getRevealedAt()).isNotNull();
        assertThat(bid.isActive()).isTrue();
    }

    @Test
    @DisplayName("잔액이 금액과 정확히 같아도 reveal 된다 (경계값)")
    void revealWithExactBalance() {
        when(bidder.getCapital()).thenReturn(AMOUNT);
        PrivateBid bid = activeCommit(revealPhaseAuction(), HASH);

        service.reveal(BIDDER, AUCTION, request());

        assertThat(bid.isVerified()).isTrue();
    }

    @Test
    @DisplayName("테스트 벡터 해시가 CommitHasher 구현과 일치한다 (amount:salt → SHA-256 소문자 hex)")
    void hashVectorMatchesImplementation() {
        assertThat(CommitHasher.hash(AMOUNT, SALT)).isEqualTo(HASH);
        assertThat(CommitHasher.matches(AMOUNT, SALT, HASH)).isTrue();
    }

    // ---------- 실패 ----------

    @Test
    @DisplayName("회원이 없으면 MEMBER_NOT_FOUND")
    void memberNotFound() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.reveal(999L, AUCTION, request()), ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("경매가 없으면 AUCTION_NOT_FOUND")
    void auctionNotFound() {
        when(auctionRepository.findByIdForUpdate(AUCTION)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, request()), ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("PRIVATE 경매가 아니면 NOT_PRIVATE_AUCTION")
    void notPrivate() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, request()), ErrorCode.NOT_PRIVATE_AUCTION);
    }

    @Test
    @DisplayName("commit 마감 전이면 INVALID_REVEAL_STATE")
    void beforeCommitDeadline() {
        auction(AuctionType.PRIVATE, AuctionStatus.OPEN,
                LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusMinutes(10));

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, request()), ErrorCode.INVALID_REVEAL_STATE);
    }

    @Test
    @DisplayName("reveal 마감 후면 INVALID_REVEAL_STATE")
    void afterRevealDeadline() {
        auction(AuctionType.PRIVATE, AuctionStatus.OPEN,
                LocalDateTime.now().minusMinutes(10), LocalDateTime.now().minusSeconds(1));

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, request()), ErrorCode.INVALID_REVEAL_STATE);
    }

    @Test
    @DisplayName("경매가 OPEN 상태가 아니면 INVALID_REVEAL_STATE")
    void notOpen() {
        auction(AuctionType.PRIVATE, AuctionStatus.CLOSED,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, request()), ErrorCode.INVALID_REVEAL_STATE);
    }

    @Test
    @DisplayName("유효한 커밋이 없으면 NO_ACTIVE_COMMIT")
    void noActiveCommit() {
        revealPhaseAuction();
        when(privateBidRepository.findByAuctionIdAndBidderIdAndActiveTrue(AUCTION, BIDDER))
                .thenReturn(Optional.empty());

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, request()), ErrorCode.NO_ACTIVE_COMMIT);
    }

    @Test
    @DisplayName("이미 reveal 한 커밋이면 ALREADY_REVEALED이고 기록된 금액은 바뀌지 않는다")
    void alreadyRevealed() {
        PrivateBid bid = activeCommit(revealPhaseAuction(), HASH);
        LocalDateTime firstRevealedAt = LocalDateTime.now().minusSeconds(30);
        bid.reveal(AMOUNT, firstRevealedAt);

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, request()), ErrorCode.ALREADY_REVEALED);

        assertThat(bid.getRevealedAmount()).isEqualTo(AMOUNT);
        assertThat(bid.getRevealedAt()).isEqualTo(firstRevealedAt);
    }

    @Test
    @DisplayName("이미 reveal 한 뒤에는 해시가 틀려도 해시 검사보다 ALREADY_REVEALED가 우선한다")
    void alreadyRevealedBeatsMismatch() {
        PrivateBid bid = activeCommit(revealPhaseAuction(), HASH);
        bid.reveal(AMOUNT, LocalDateTime.now().minusSeconds(30));

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, new PrivateBidRevealRequest(9999L, SALT)),
                ErrorCode.ALREADY_REVEALED);
    }

    @Test
    @DisplayName("금액이 다르면 COMMIT_HASH_MISMATCH이고 reveal 되지 않는다")
    void wrongAmount() {
        PrivateBid bid = activeCommit(revealPhaseAuction(), HASH);

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, new PrivateBidRevealRequest(AMOUNT + 1, SALT)),
                ErrorCode.COMMIT_HASH_MISMATCH);

        assertThat(bid.isVerified()).isFalse();
        assertThat(bid.getRevealedAmount()).isNull();
    }

    @Test
    @DisplayName("salt가 다르면 COMMIT_HASH_MISMATCH이고 reveal 되지 않는다")
    void wrongSalt() {
        PrivateBid bid = activeCommit(revealPhaseAuction(), HASH);

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, new PrivateBidRevealRequest(AMOUNT, "ABCDEFGHIJKLMNOP")),
                ErrorCode.COMMIT_HASH_MISMATCH);

        assertThat(bid.isVerified()).isFalse();
    }

    @Test
    @DisplayName("잔액이 금액보다 1이라도 적으면 INSUFFICIENT_BALANCE이고 reveal 되지 않는다")
    void insufficientBalance() {
        when(bidder.getCapital()).thenReturn(AMOUNT - 1);
        PrivateBid bid = activeCommit(revealPhaseAuction(), HASH);

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, request()), ErrorCode.INSUFFICIENT_BALANCE);

        assertThat(bid.isVerified()).isFalse();
        assertThat(bid.getRevealedAmount()).isNull();
    }

    @Test
    @DisplayName("해시 검사가 잔액 검사보다 먼저다 (해시가 틀리면 잔액이 부족해도 COMMIT_HASH_MISMATCH)")
    void hashCheckedBeforeBalance() {
        when(bidder.getCapital()).thenReturn(0L);
        activeCommit(revealPhaseAuction(), HASH);

        assertErrorCode(() -> service.reveal(BIDDER, AUCTION, new PrivateBidRevealRequest(AMOUNT + 1, SALT)),
                ErrorCode.COMMIT_HASH_MISMATCH);
    }
}
