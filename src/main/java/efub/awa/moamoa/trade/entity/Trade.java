package efub.awa.moamoa.trade.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.trade.enums.TradeStatus;
import jakarta.persistence.*;
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
    @JoinColumn(name = "seller_id")
    private Member seller;

    //회원 탈퇴 시 기록은 유지하고 회원 참조만 null로 변경 (다른 회원에게는 "탈퇴한 사용자"로 표시)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "buyer_id")
    private Member buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TradeStatus status;

    @ColumnDefault("false")
    @Column(name = "buyer_confirmed", nullable = false)
    private boolean buyerConfirmed;

    @ColumnDefault("false")
    @Column(name = "seller_confirmed", nullable = false)
    private boolean sellerConfirmed;
}
