package efub.awa.moamoa.auction.entity;

import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.policy.AuctionPolicy;
import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static efub.awa.moamoa.global.exception.ErrorCode.*;

@Entity
@Getter
@Table(name = "auction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    //회원 탈퇴 시 기록은 유지하고 회원 참조만 null로 변경 (다른 회원에게는 "탈퇴한 사용자"로 표시)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "current_bidder_id")
    private Member currentBidder;

    //회원 탈퇴 시 기록은 유지하고 회원 참조만 null로 변경 (다른 회원에게는 "탈퇴한 사용자"로 표시)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "auction_owner")
    private Member auctionOwner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuctionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuctionStatus status;

    @Column(nullable = false)
    private Long startPrice;

    @Column(nullable = false)
    private LocalDateTime readyStartTime;

    //입장 정원 (경매 생성 시 확정, 정원이 차면 READY에서 OPEN으로 바로 전환)
    @Column(nullable = false)
    private Integer maxParticipants;

    //입장 대기 최대 마감 시각 (readyStartTime + 입장 대기 시간, 경매 생성 시 계산해서 저장)
    @Column(nullable = false)
    private LocalDateTime entryDeadline;

    private LocalDateTime openStartTime;

    @Column(nullable = false)
    private Long currentPrice;

    @Version
    private Long version;

    // 비공개 경매 전용
    private LocalDateTime commitDeadline;
    private LocalDateTime revealDeadline;

    private Long finalPrice;

    @Builder
    public Auction(Inventory inventory, Member auctionOwner, AuctionType type, Long startPrice,
                   LocalDateTime readyStartTime, Integer maxParticipants, LocalDateTime entryDeadline,
                   LocalDateTime openStartTime, LocalDateTime commitDeadline, LocalDateTime revealDeadline) {
        this.inventory = inventory;
        this.auctionOwner = auctionOwner;
        this.type = type;
        this.startPrice = startPrice;
        this.readyStartTime = readyStartTime;
        this.maxParticipants = maxParticipants;
        this.entryDeadline = entryDeadline;
        this.openStartTime = openStartTime;
        this.commitDeadline = commitDeadline;
        this.revealDeadline = revealDeadline;
        this.status = AuctionStatus.READY;
        this.currentPrice = startPrice;
    }

    //READY -> OPEN: 시작 시각과 마감 시각을 계산한다. (입찰/커밋 10분, PRIVATE은 리빌 3분을 추가)
    public void open(LocalDateTime now) {
        this.status = AuctionStatus.OPEN;
        this.openStartTime = now;
        this.commitDeadline = now.plusMinutes(AuctionPolicy.COMMIT_MINUTES);
        if (this.type == AuctionType.PRIVATE) {
            this.revealDeadline = this.commitDeadline.plusMinutes(AuctionPolicy.REVEAL_MINUTES);
        }
    }

    //READY/OPEN -> FAILED: 인원 미달 또는 유효 낙찰자 없음
    public void fail() {
        this.status = AuctionStatus.FAILED;
    }

    //OPEN -> CLOSED (PUBLIC): 현재가가 낙찰가이고 현재 최고 입찰자(currentBidder)가 낙찰자이다.
    public void close() {
        this.status = AuctionStatus.CLOSED;
        this.finalPrice = this.currentPrice;
    }

    public void validatePrivate() {
        if (this.type != AuctionType.PRIVATE) throw new CustomException(NOT_PRIVATE_AUCTION);
    }

    public void validateCommitPhase(LocalDateTime now) {
        if (status != AuctionStatus.OPEN || commitDeadline == null || now.isAfter(commitDeadline)) throw new CustomException(INVALID_AUCTION_STATE);
    }

    public void validateRevealPhase(LocalDateTime now) {
        if (status != AuctionStatus.OPEN || commitDeadline == null || revealDeadline == null) throw new CustomException(INVALID_REVEAL_STATE);
        if (!now.isAfter(commitDeadline) || now.isAfter(revealDeadline)) throw new CustomException(INVALID_REVEAL_STATE);
    }
}
