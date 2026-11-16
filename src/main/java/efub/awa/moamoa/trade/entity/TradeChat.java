package efub.awa.moamoa.trade.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.member.entity.Member;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "trade_chat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeChat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //회원 탈퇴 시 기록은 유지하고 회원 참조만 null로 변경 (다른 회원에게는 "탈퇴한 사용자"로 표시)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "sender_id")
    private Member sender;

    //회원 탈퇴 시 기록은 유지하고 회원 참조만 null로 변경 (다른 회원에게는 "탈퇴한 사용자"로 표시)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "receiver_id")
    private Member receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @Column(name = "message", nullable = false, updatable = false)
    private String message;
}
