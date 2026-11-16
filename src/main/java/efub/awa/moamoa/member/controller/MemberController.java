package efub.awa.moamoa.member.controller;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.global.security.RefreshTokenCookieProvider;
import efub.awa.moamoa.member.dto.request.UpdateMemberRequest;
import efub.awa.moamoa.member.dto.response.MemberInfoResponse;
import efub.awa.moamoa.member.dto.response.MemberMoneyResponse;
import efub.awa.moamoa.member.dto.response.NicknameCheckResponse;
import efub.awa.moamoa.member.service.MemberService;
import efub.awa.moamoa.member.service.MemberWithdrawalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberWithdrawalService memberWithdrawalService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(@AuthenticationPrincipal Long memberId) {
        log.info("내 정보 조회 API - memberId: {}", memberId);

        MemberInfoResponse response = memberService.getMyInfo(memberId);
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회 요청이 성공했습니다.", response));
    }

    @GetMapping("/me/money")
    public ResponseEntity<ApiResponse<MemberMoneyResponse>> getMyMoney(@AuthenticationPrincipal Long memberId) {
        log.info("소유 금액 조회 API - memberId: {}", memberId);

        MemberMoneyResponse response = memberService.getMyMoney(memberId);
        return ResponseEntity.ok(ApiResponse.success("소유 금액 조회 요청이 성공했습니다.", response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMyInfo(@AuthenticationPrincipal Long memberId,
                                                                        @Valid @RequestBody UpdateMemberRequest request) {
        log.info("내 정보 수정 API - memberId: {}", memberId);

        MemberInfoResponse response = memberService.updateMyInfo(memberId, request);
        return ResponseEntity.ok(ApiResponse.success("내 정보 수정 요청이 성공했습니다.", response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal Long memberId) {
        log.info("회원 탈퇴 API - memberId: {}", memberId);

        memberWithdrawalService.withdraw(memberId);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
                .body(ApiResponse.success("회원 탈퇴 요청이 성공했습니다."));
    }

    //회원가입/소셜 추가가입 중에는 토큰이 없으므로 인증 없이 호출 가능 (SecurityConfig에서 permitAll)
    @GetMapping("/nickname-check")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(
            @RequestParam("nickname") @NotBlank @Size(max = 10) String nickname) {
        log.info("닉네임 중복확인 API - nickname: {}", nickname);

        NicknameCheckResponse response = memberService.checkNickname(nickname);
        return ResponseEntity.ok(ApiResponse.success("닉네임 중복확인 요청이 성공했습니다.", response));
    }
}
