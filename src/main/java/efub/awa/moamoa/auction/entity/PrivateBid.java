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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
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

    //회원 탈퇴 시 기록은 유지하고 회원 참조만 null로 변경 (다른 회원에게는 "탈퇴한 사용자"로 표시)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "bidder_id")
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
