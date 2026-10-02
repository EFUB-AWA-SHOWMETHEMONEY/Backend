package efub.awa.moamoa.quiz.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "daily_quiz")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyQuiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", nullable = false)
    private String question;

    @Column(name = "answer", nullable = false)
    private String answer;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;
}
