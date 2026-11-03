package efub.awa.moamoa.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

//Redis에 회원당 하나의 Refresh Token을 원문으로 저장 (key: refresh:{memberId})
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    //기존 토큰이 있으면 덮어쓴다
    public void save(Long memberId, String refreshToken, long ttlSeconds) {
        redisTemplate.opsForValue().set(KEY_PREFIX + memberId, refreshToken, Duration.ofSeconds(ttlSeconds));
    }

    public Optional<String> find(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + memberId));
    }

    //키가 없어도 예외 없이 끝난다
    public void delete(Long memberId) {
        redisTemplate.delete(KEY_PREFIX + memberId);
    }
}
