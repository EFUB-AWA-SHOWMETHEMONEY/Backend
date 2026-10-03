package efub.awa.moamoa.bank.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "bank_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    //precision: 유효숫자 자릿수 (5자리)
    //scale: 소수점 자릿수 (4자리)
    //예: 0.035 저장 -> 3.5% 이자율 (0.01% 단위 까지 저장 가능 (소수 4자리니까)
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "description")
    private String description;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;
}
