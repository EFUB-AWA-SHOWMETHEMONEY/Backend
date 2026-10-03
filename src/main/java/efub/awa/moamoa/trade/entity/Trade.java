package efub.awa.moamoa.trade.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.trade.enums.TradeStatus;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
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
