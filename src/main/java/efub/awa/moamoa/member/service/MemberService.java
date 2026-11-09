package efub.awa.moamoa.member.service;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.dto.response.MemberInfoResponse;
import efub.awa.moamoa.member.dto.response.MemberInfoResponse.EquippedItemResponse;
import efub.awa.moamoa.member.dto.response.NicknameCheckResponse;
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

import java.util.Comparator;
import java.util.List;
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
    private final InventoryService inventoryService;

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

    //내 정보 조회 (회원 기본 정보 + 장착 아이템 목록, 장착 아이템은 itemId 오름차순)
    @Transactional(readOnly = true)
    public MemberInfoResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원 - memberId: {}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });

        //TODO: S3 이미지 링크 받아오는걸로 수정 (다른 아이템 API와 동일하게 Item.imageUrl 사용)
        List<EquippedItemResponse> equippedItemList = inventoryService.findEquippedItems(memberId).stream()
                .sorted(Comparator.comparing(Item::getId))
                .map(item -> EquippedItemResponse.of(item, item.getImageUrl()))
                .toList();

        log.info("내 정보 조회 성공 - memberId: {}", memberId);
        return MemberInfoResponse.of(member, equippedItemList);
    }

    //닉네임 중복확인 (회원가입의 닉네임 중복 검사와 같은 existsByNickname 사용: 대소문자만 다른 닉네임도 중복)
    //닉네임을 예약하지 않으므로 확인 이후 가입/수정 시 중복이 생기면 해당 API에서 DUPLICATE_NICKNAME 응답
    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String nickname) {
        boolean isDuplicated = existsByNickname(nickname);

        log.info("닉네임 중복확인 성공 - nickname: {}, isDuplicated: {}", nickname, isDuplicated);
        return NicknameCheckResponse.of(isDuplicated);
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
