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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bidder_id")   //입찰 전에는 null
    private Member currentBidder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_owner", nullable = false)
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

    @Version   //낙관적 락
    private Long version;

    private LocalDateTime commitDeadline;   //비공개 경매 전용

    private LocalDateTime revealDeadline;   //비공개 경매 전용

    private Long finalPrice;   //낙찰 전에는 null

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
