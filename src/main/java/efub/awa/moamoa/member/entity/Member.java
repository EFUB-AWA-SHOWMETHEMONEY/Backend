package efub.awa.moamoa.member.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.member.enums.MemberType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String nickname;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberType type;

    @Column(nullable = false)
    private Long capital;

    @Column(nullable = false)
    private boolean alarm;

    @Builder
    public Member(String name, String nickname, Integer age, MemberType type, Long capital) {
        this.name = name;
        this.nickname = nickname;
        this.age = age;
        this.type = type;
        this.capital = capital;
        this.alarm = true;
    }

    //닉네임, 나이 부분 수정 (null인 항목은 수정하지 않음)
    public void updateProfile(String nickname, Integer age) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (age != null) {
            this.age = age;
        }
    }

    //Member의 withdraw/deposit의 파라미터는 항상 양수
    public Long withdraw(Long amount) {
        validateBalance(amount);
        if(this.capital < amount) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.capital -= amount;

        return this.capital;
    }

    public Long deposit(Long amount) {
        validateBalance(amount);
        this.capital += amount;

        return this.capital;
    }

    private void validateBalance(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_AMOUNT);
        }
    }
}
