package efub.awa.moamoa.auction.entity;

import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.global.common.entity.BaseEntity;
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
                   LocalDateTime readyStartTime, LocalDateTime openStartTime,
                   LocalDateTime commitDeadline, LocalDateTime revealDeadline) {
        this.inventory = inventory;
        this.auctionOwner = auctionOwner;
        this.type = type;
        this.startPrice = startPrice;
        this.readyStartTime = readyStartTime;
        this.openStartTime = openStartTime;
        this.commitDeadline = commitDeadline;
        this.revealDeadline = revealDeadline;
        this.status = AuctionStatus.READY;
        this.currentPrice = startPrice;
    }
}
