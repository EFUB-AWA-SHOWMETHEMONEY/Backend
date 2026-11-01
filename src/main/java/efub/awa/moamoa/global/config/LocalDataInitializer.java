package efub.awa.moamoa.global.config;

import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.enums.Gender;
import efub.awa.moamoa.member.enums.MemberType;
import efub.awa.moamoa.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

//local 환경 테스트용 데이터. DB가 비어 있을 때만 삽입
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;

    @Override
    public void run(String... args) {
        if (memberRepository.count() > 0) return;

        for (int i = 1; i <= 3; i++) {
            memberRepository.save(Member.builder()
                    .nickname("테스터" + i)
                    .age(20 + i)
                    .type(MemberType.SELF)
                    .capital(100000L)
                    .gender(i % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                    .build());
        }
        log.info("local 테스트 멤버 3명 생성 완료");
    }
}
