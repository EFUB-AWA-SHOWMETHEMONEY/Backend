package efub.awa.moamoa.trade.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.trade.enums.TradeStatus;
import jakarta.persistence.*;
import lombok.Builder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Table(name = "trade")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    //회원 탈퇴 시 기록은 유지하고 회원 참조만 null로 변경 (다른 회원에게는 "탈퇴한 사용자"로 표시)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "participant1_id")
    private Member participant1;

    //회원 탈퇴 시 기록은 유지하고 회원 참조만 null로 변경 (다른 회원에게는 "탈퇴한 사용자"로 표시)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "participant2_id")
    private Member participant2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory1_id")
    private Inventory inventory1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory2_id")
    private Inventory inventory2;

    @Column(name = "price1")
    private Long price1;

    @Column(name = "price2")
    private Long price2;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TradeStatus status;

    @ColumnDefault("false")
    @Column(name = "p1_confirmed", nullable = false)
    private boolean p1Confirmed;

    @ColumnDefault("false")
    @Column(name = "p2_confirmed", nullable = false)
    private boolean p2Confirmed;

    @Builder
    public Trade (Member participant1, Member participant2) {
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.status = TradeStatus.PENDING;
        this.p1Confirmed = false;
        this.p2Confirmed = false;
    }
}
