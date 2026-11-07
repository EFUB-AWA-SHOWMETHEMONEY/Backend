package efub.awa.moamoa.balance.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.balance.enums.BalanceChangeType;
import efub.awa.moamoa.balance.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "balance_history",
indexes = {
        @Index(name = "idx_balance_history_member_created",
        columnList = "member_id, created_at"),
        @Index(name = "idx_balance_history_reference", columnList = "reference_type, reference_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BalanceHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "amount", nullable = false, updatable = false)
    private Long amount;

    @Column(name ="balance_after", nullable = false, updatable = false)
    private Long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private BalanceChangeType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", updatable = false)
    private ReferenceType referenceType;

    @Column(name = "reference_id", updatable = false)
    private Long referenceId;
}
