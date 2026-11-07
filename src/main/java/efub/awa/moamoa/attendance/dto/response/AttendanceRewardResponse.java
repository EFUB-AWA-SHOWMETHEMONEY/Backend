package efub.awa.moamoa.attendance.dto.response;

import lombok.Builder;

@Builder
public record AttendanceRewardResponse(
        Long memberId,
        Long balance

) {}
