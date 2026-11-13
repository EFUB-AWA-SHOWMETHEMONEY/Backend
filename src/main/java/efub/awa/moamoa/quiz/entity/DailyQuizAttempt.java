package efub.awa.moamoa.quiz.entity;

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
@Table(name = "daily_quiz_attempt",
uniqueConstraints = {
        @UniqueConstraint(columnNames = {"quiz_id", "member_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyQuizAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private DailyQuiz quiz;

    //회원 탈퇴 시 함께 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

}
