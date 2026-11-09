package efub.awa.moamoa.member.controller;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.member.dto.response.MemberInfoResponse;
import efub.awa.moamoa.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(@AuthenticationPrincipal Long memberId) {
        log.info("내 정보 조회 API - memberId: {}", memberId);

        MemberInfoResponse response = memberService.getMyInfo(memberId);
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회 요청이 성공했습니다.", response));
    }
}
