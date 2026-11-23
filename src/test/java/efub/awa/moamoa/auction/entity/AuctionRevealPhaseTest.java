package efub.awa.moamoa.auction.entity;

import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionRevealPhaseTest {

    private static final LocalDateTime COMMIT_DEADLINE = LocalDateTime.of(2026, 11, 20, 12, 0, 0);
    private static final LocalDateTime REVEAL_DEADLINE = LocalDateTime.of(2026, 11, 20, 12, 10, 0);

    private Auction auction(AuctionStatus status, LocalDateTime commitDeadline, LocalDateTime revealDeadline) {
        Auction auction = Auction.builder()
                .type(AuctionType.PRIVATE)
                .startPrice(1000L)
                .readyStartTime(COMMIT_DEADLINE.minusHours(1))
                .commitDeadline(commitDeadline)
                .revealDeadline(revealDeadline)
                .build();
        ReflectionTestUtils.setField(auction, "status", status);
        return auction;
    }

    private void assertInvalid(Auction auction, LocalDateTime now) {
        assertThatThrownBy(() -> auction.validateRevealPhase(now))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REVEAL_STATE);
    }

    @Test
    @DisplayName("commit 마감 직후 ~ reveal 마감 시각까지는 통과한다")
    void insideWindow() {
        Auction auction = auction(AuctionStatus.OPEN, COMMIT_DEADLINE, REVEAL_DEADLINE);

        assertThatCode(() -> auction.validateRevealPhase(COMMIT_DEADLINE.plusNanos(1))).doesNotThrowAnyException();
        assertThatCode(() -> auction.validateRevealPhase(COMMIT_DEADLINE.plusMinutes(5))).doesNotThrowAnyException();
        assertThatCode(() -> auction.validateRevealPhase(REVEAL_DEADLINE)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("commit 마감 시각 정각까지는 아직 commit 구간이라 reveal 불가 (commit 검증과 경계가 겹치지 않는다)")
    void commitDeadlineBoundary() {
        Auction auction = auction(AuctionStatus.OPEN, COMMIT_DEADLINE, REVEAL_DEADLINE);

        assertInvalid(auction, COMMIT_DEADLINE.minusSeconds(1));
        assertInvalid(auction, COMMIT_DEADLINE);   // validateCommitPhase는 이 시각에 아직 통과
        assertThatCode(() -> auction.validateCommitPhase(COMMIT_DEADLINE)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reveal 마감이 1나노초라도 지나면 불가")
    void afterRevealDeadline() {
        assertInvalid(auction(AuctionStatus.OPEN, COMMIT_DEADLINE, REVEAL_DEADLINE), REVEAL_DEADLINE.plusNanos(1));
    }

    @Test
    @DisplayName("OPEN이 아닌 상태는 시간이 맞아도 불가")
    void statusNotOpen() {
        LocalDateTime now = COMMIT_DEADLINE.plusMinutes(5);

        for (AuctionStatus status : AuctionStatus.values()) {
            if (status == AuctionStatus.OPEN) continue;
            assertInvalid(auction(status, COMMIT_DEADLINE, REVEAL_DEADLINE), now);
        }
    }

    @Test
    @DisplayName("commitDeadline 또는 revealDeadline이 null이면 NPE 없이 INVALID_REVEAL_STATE")
    void nullDeadlines() {
        LocalDateTime now = COMMIT_DEADLINE.plusMinutes(5);

        assertInvalid(auction(AuctionStatus.OPEN, null, REVEAL_DEADLINE), now);
        assertInvalid(auction(AuctionStatus.OPEN, COMMIT_DEADLINE, null), now);
    }
}
