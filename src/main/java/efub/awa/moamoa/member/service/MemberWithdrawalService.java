package efub.awa.moamoa.member.service;

import efub.awa.moamoa.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

//회원 탈퇴: DB 삭제(트랜잭션)가 끝난 뒤 Redis에 남은 회원 데이터를 정리
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

    //출석 기록 key: attendance:{날짜}:{memberId} (AttendanceService)
    private static final String ATTENDANCE_KEY_PATTERN_FORMAT = "attendance:*:%d";

    private final MemberService memberService;
    private final TokenService tokenService;
    private final StringRedisTemplate redisTemplate;

    public void withdraw(Long memberId) {
        memberService.deleteMember(memberId);

        //회원은 이미 삭제됐으므로 Redis 정리에 실패해도 탈퇴는 성공으로 처리 (회원이 없으면 남은 값은 쓰이지 않음)
        deleteRefreshToken(memberId);
        deleteAttendanceKeys(memberId);

        log.info("회원 탈퇴 성공 - memberId: {}", memberId);
    }

    private void deleteRefreshToken(Long memberId) {
        try {
            tokenService.deleteRefreshToken(memberId);
        } catch (RuntimeException e) {
            log.error("탈퇴 후 Refresh Token 삭제 실패 - memberId: {}", memberId, e);
        }
    }

    private void deleteAttendanceKeys(Long memberId) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(ATTENDANCE_KEY_PATTERN_FORMAT.formatted(memberId))
                .count(100)
                .build();

        try {
            List<String> keys = new ArrayList<>();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException e) {
            log.error("탈퇴 후 출석 기록 삭제 실패 - memberId: {}", memberId, e);
        }
    }
}
