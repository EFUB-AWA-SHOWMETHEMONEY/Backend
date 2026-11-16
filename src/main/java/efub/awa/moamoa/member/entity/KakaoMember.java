package efub.awa.moamoa.member.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "kakao_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KakaoMember extends BaseEntity {

    @Id
    @Column(name = "oauth_id", length = 25)
    private String oauthId;

    //회원 탈퇴 시 함께 삭제
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Builder
    public KakaoMember(String oauthId, Member member) {
        this.oauthId = oauthId;
        this.member = member;
    }
}
