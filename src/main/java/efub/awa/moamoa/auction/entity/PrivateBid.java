package efub.awa.moamoa.auction.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "private_bid")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivateBid extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id", nullable = false)
    private Member bidder;

    @Column(nullable = false, length = 64)   //SHA-256 hex 사용
    private String commitHash;

    private Long revealedAmount;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private LocalDateTime committedAt;

    private LocalDateTime revealedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder
    public PrivateBid(Auction auction, Member bidder, String commitHash, LocalDateTime committedAt) {
        this.auction = auction;
        this.bidder = bidder;
        this.commitHash = commitHash;
        this.committedAt = committedAt;
        this.verified = false;
        this.active = false;
    }
}
