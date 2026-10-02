package efub.awa.moamoa.bank.entity;

import efub.awa.moamoa.bank.enums.AccountStatus;
import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "bank_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_product_id", nullable = false)
    private BankProduct bankProduct;

    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDateTime startDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDateTime maturityDate;

    @Column(name = "start_money", nullable = false, updatable = false)
    private Long startMoney;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "expected_interest", nullable = false)
    private Long expectedInterest;

    //계좌 상태 업데이트 메소드
    public void updateStatus(AccountStatus status) {
        this.status = status;
    }

}
