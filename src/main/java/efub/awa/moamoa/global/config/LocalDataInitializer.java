package efub.awa.moamoa.global.config;

import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.enums.Gender;
import efub.awa.moamoa.member.enums.MemberType;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//local 환경 테스트용 데이터. ddl-auto가 create라 매 기동 시 DB가 비어 있는 상태로 시작한다
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    //Postman 테스트용 자체 로그인 계정 (POST /api/auth/login 으로 Access Token 발급)
    //비밀번호는 가입 API의 정책(영문+특수문자 8~20자)을 통과하지 못하는 값이라 여기서만 직접 심는다
    private static final String TEST_LOGIN_ID = "member1";
    private static final String TEST_PASSWORD = "pw1";
    private static final String TEST_NICKNAME = "member1";
    private static final int TEST_AGE = 20;

    //memberId를 키에 포함하는 Redis 데이터 (DB 재생성 시 다른 회원을 가리키게 되므로 함께 비운다)
    private static final List<String> MEMBER_SCOPED_KEY_PATTERNS = List.of("refresh:*", "attendance:*");

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) {
        clearMemberScopedRedisKeys();
        initSelfLoginMember();
        initPlazaTestMembers();
    }

    //DB가 매 기동 시 재생성되면서 auto_increment도 1부터 다시 시작한다.
    //남아 있는 refresh 토큰/출석 기록은 엉뚱한 회원에게 적용되므로 비운다.
    //keys()는 운영에서 쓸 명령이 아니지만 local 데이터 규모에서는 문제되지 않는다.
    private void clearMemberScopedRedisKeys() {
        Set<String> keys = new HashSet<>();
        MEMBER_SCOPED_KEY_PATTERNS.forEach(pattern -> keys.addAll(redisTemplate.keys(pattern)));

        if (keys.isEmpty()) return;

        redisTemplate.delete(keys);
        log.info("local Redis 키 {}개 정리 완료", keys.size());
    }

    //로그인 가능한 계정 (Member + SelfMember)
    private void initSelfLoginMember() {
        if (memberService.existsByLoginId(TEST_LOGIN_ID)) {
            log.info("local 테스트 계정 이미 존재 - loginId: {}", TEST_LOGIN_ID);
            return;
        }
        //닉네임이 유니크라 선점돼 있으면 저장이 실패한다 -> 서버 기동을 막지 않도록 건너뛴다
        if (memberService.existsByNickname(TEST_NICKNAME)) {
            log.warn("닉네임 '{}'이 이미 사용 중이라 local 테스트 계정을 만들지 못했습니다", TEST_NICKNAME);
            return;
        }

        Member member = memberService.createSelfMember(
                TEST_LOGIN_ID,
                passwordEncoder.encode(TEST_PASSWORD),
                TEST_NICKNAME,
                TEST_AGE,
                Gender.FEMALE);

        log.info("local 테스트 계정 생성 완료 - loginId: {}, memberId: {}", TEST_LOGIN_ID, member.getId());
    }

    //광장 테스트용 멤버 (로그인 자격 없음, memberId로만 사용)
    private void initPlazaTestMembers() {
        for (int i = 1; i <= 3; i++) {
            String nickname = "테스터" + i;
            if (memberRepository.existsByNickname(nickname)) continue;

            Member member = memberRepository.save(Member.builder()
                    .nickname(nickname)
                    .age(20 + i)
                    .type(MemberType.SELF)
                    .capital(100000L)
                    .gender(i % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                    .build());
            log.info("local 테스트 멤버 생성 완료 - nickname: {}, memberId: {}", nickname, member.getId());
        }
    }
}
