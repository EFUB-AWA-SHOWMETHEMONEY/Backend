package efub.awa.moamoa.attendance.controller;

import efub.awa.moamoa.attendance.dto.response.AttendanceRewardResponse;
import efub.awa.moamoa.attendance.service.AttendanceService;
import efub.awa.moamoa.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<ApiResponse<AttendanceRewardResponse>> claimReward(@AuthenticationPrincipal Long memberId) {
        AttendanceRewardResponse response = attendanceService.claimReward(memberId);
        return ResponseEntity.ok(ApiResponse.success("출석 보상 지급 성공", response));

    }
}
