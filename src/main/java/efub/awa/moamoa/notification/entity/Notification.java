package efub.awa.moamoa.notification.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Getter
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationType type;

    @Column(nullable = false)
    private boolean expired;

    @Column(name = "is_read", nullable = false)   // read는 MySQL 예약어라서 is_read로 변경함
    private boolean read;

    @Builder
    public Notification(Member member, String content, NotificationType type) {
        this.member = member;
        this.content = content;
        this.type = type;
        this.expired = false;
        this.read = false;
    }
}
