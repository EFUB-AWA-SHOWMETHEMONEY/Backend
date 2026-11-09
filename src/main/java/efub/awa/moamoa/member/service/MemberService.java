package efub.awa.moamoa.member.service;

import efub.awa.moamoa.member.entity.KakaoMember;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.entity.SelfMember;
import efub.awa.moamoa.member.enums.MemberType;
import efub.awa.moamoa.member.repository.KakaoMemberRepository;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.member.repository.SelfMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    //가입 시 지급하는 초기 자본
    private static final long INITIAL_CAPITAL = 100L;

    private final MemberRepository memberRepository;
    private final SelfMemberRepository selfMemberRepository;
    private final KakaoMemberRepository kakaoMemberRepository;

    @Transactional(readOnly = true)
    public boolean existsByLoginId(String loginId) {
        return selfMemberRepository.existsByLoginId(loginId);
    }

    @Transactional(readOnly = true)
    public boolean existsByNickname(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long memberId) {
        return memberRepository.existsById(memberId);
    }

    @Transactional(readOnly = true)
    public boolean existsByOauthId(String oauthId) {
        return kakaoMemberRepository.existsByOauthId(oauthId);
    }

    @Transactional(readOnly = true)
    public Optional<SelfMember> findSelfMemberByLoginId(String loginId) {
        return selfMemberRepository.findByLoginId(loginId);
    }

    @Transactional(readOnly = true)
    public Optional<KakaoMember> findKakaoMemberByOauthId(String oauthId) {
        return kakaoMemberRepository.findByOauthId(oauthId);
    }

    //Member + SelfMember를 한 트랜잭션으로 저장 (중복 제약 위반 시 DataIntegrityViolationException 발생)
    @Transactional
    public Member createSelfMember(String loginId, String encodedPassword, String name, String nickname,
                                   Integer age) {
        Member member = saveMember(name, nickname, age, MemberType.SELF);

        selfMemberRepository.save(SelfMember.builder()
                .member(member)
                .loginId(loginId)
                .loginPassword(encodedPassword)
                .build());

        log.info("일반 회원 생성 성공 - memberId: {}", member.getId());
        return member;
    }

    //Member + KakaoMember를 한 트랜잭션으로 저장 (중복 제약 위반 시 DataIntegrityViolationException 발생)
    @Transactional
    public Member createKakaoMember(String oauthId, String name, String nickname, Integer age) {
        Member member = saveMember(name, nickname, age, MemberType.KAKAO);

        kakaoMemberRepository.save(KakaoMember.builder()
                .oauthId(oauthId)
                .member(member)
                .build());

        log.info("카카오 회원 생성 성공 - memberId: {}", member.getId());
        return member;
    }

    private Member saveMember(String name, String nickname, Integer age, MemberType type) {
        return memberRepository.save(Member.builder()
                .name(name)
                .nickname(nickname)
                .age(age)
                .type(type)
                .capital(INITIAL_CAPITAL)
                .build());
    }
}
