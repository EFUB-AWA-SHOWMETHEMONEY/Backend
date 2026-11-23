package efub.awa.moamoa.auction.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;

//경매 OPEN 이후 입장 판단에 쓰는 명단 (Redis SET, 값: memberId)
// - 참가자 명단 (key: auction:participants:{auctionId}): OPEN으로 전환되는 순간 방에 있던 회원
// - 퇴장 명단 (key: auction:left:{auctionId}): OPEN 이후 PUBLIC 경매에서 퇴장 버튼으로 나간 회원 (퇴장 API에서 기록)
//경매가 끝나면 쓰지 않으므로 처음 기록할 때 1일 뒤 만료되게 한다.
@Repository
@RequiredArgsConstructor
public class AuctionEntryListRepository {

    private static final String PARTICIPANTS_KEY_PREFIX = "auction:participants:";
    private static final String LEFT_KEY_PREFIX = "auction:left:";
    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    //참가자 명단을 통째로 저장한다. (OPEN 전환을 다시 시도하는 경우를 위해 기존 명단을 지우고 새로 저장)
    public void saveParticipants(Long auctionId, Collection<Long> memberIds) {
        String key = participantsKey(auctionId);
        redisTemplate.delete(key);
        if (memberIds.isEmpty()) return;

        redisTemplate.opsForSet().add(key, memberIds.stream().map(String::valueOf).toArray(String[]::new));
        redisTemplate.expire(key, TTL);
    }

    //퇴장 명단에 추가한다. 만료 시간은 처음 기록할 때만 건다. (이후 퇴장이 만료를 늘리지 않도록)
    public void addLeft(Long auctionId, Long memberId) {
        String key = leftKey(auctionId);
        redisTemplate.opsForSet().add(key, String.valueOf(memberId));

        Long ttl = redisTemplate.getExpire(key);
        if (ttl == null || ttl < 0) {
            redisTemplate.expire(key, TTL);
        }
    }

    public boolean isInParticipantList(Long auctionId, Long memberId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(participantsKey(auctionId), String.valueOf(memberId)));
    }

    public boolean isInLeftList(Long auctionId, Long memberId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(leftKey(auctionId), String.valueOf(memberId)));
    }

    private String participantsKey(Long auctionId) {
        return PARTICIPANTS_KEY_PREFIX + auctionId;
    }

    private String leftKey(Long auctionId) {
        return LEFT_KEY_PREFIX + auctionId;
    }
}
