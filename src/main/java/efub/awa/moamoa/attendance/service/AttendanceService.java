package efub.awa.moamoa.attendance.service;

import efub.awa.moamoa.balance.enums.BalanceChangeType;
import efub.awa.moamoa.balance.enums.ReferenceType;
import efub.awa.moamoa.balance.service.BalanceService;
import efub.awa.moamoa.attendance.dto.response.AttendanceRewardResponse;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final Long ATTENDANCE_REWARD = 500L;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final RedisTemplate<String, String> redisTemplate;
    private final BalanceService balanceService;

    public AttendanceRewardResponse claimReward (Long memberId) {
        String key = attendanceKey(memberId);

        if (!markAttended(key)) {
            throw new CustomException(ErrorCode.ALREADY_RECEIVED_REWARD);
        }

        try {
            Long balanceAfter = attendanceReward(memberId);
            return AttendanceRewardResponse.builder()
                    .memberId(memberId)
                    .balance(balanceAfter)
                    .build();
        } catch (RuntimeException e) {
            redisTemplate.delete(key);
            throw e;
        }
    }

    private boolean markAttended(String key) {
        Duration ttl = Duration.between(
                ZonedDateTime.now(KST),
                LocalDate.now(KST).plusDays(1).atStartOfDay(KST).plusHours(1));

        Boolean firstVisit = redisTemplate.opsForValue().setIfAbsent(key, "true", ttl);
        return Boolean.TRUE.equals(firstVisit);
    }
    private String attendanceKey(Long memberId) {
        return "attendance:" + LocalDate.now(KST) + ":" + memberId;
    }

    private Long attendanceReward(Long memberId) {
        return balanceService.deposit(memberId, ATTENDANCE_REWARD, BalanceChangeType.ATTENDANCE_REWARD,
                ReferenceType.ATTENDANCE, null);
    }

}
