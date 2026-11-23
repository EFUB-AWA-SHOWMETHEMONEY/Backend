package efub.awa.moamoa.websocket.auction.repository;

import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AuctionParticipantRegistry {
    private final Map<Long, Map<Long, AuctionParticipantState>> rooms = new ConcurrentHashMap<>();

    public AuctionParticipantState join (Long auctionId, AuctionParticipantState state) {
        AtomicReference<AuctionParticipantState> previous = new AtomicReference<>();
        rooms.compute(auctionId, (id, room) -> {
            if (room == null) room = new ConcurrentHashMap<>();
            previous.set(room.put(state.memberId(), state));
            return room;
        });

        return previous.get();
    }

    public boolean leave (Long memberId, Long auctionId, String sessionId) {
        if (memberId == null || auctionId == null || sessionId == null) return false;

        AtomicBoolean removed = new AtomicBoolean();
        rooms.computeIfPresent(auctionId, (id, room) -> {
            AuctionParticipantState current = room.get(memberId);

            if (current != null && current.sessionId().equals(sessionId)) {
                room.remove(memberId);
                removed.set(true);
            }
            return room.isEmpty() ? null : room;
        });
        return removed.get();
    }

    public AuctionParticipantState find(Long auctionId, Long memberId) {
        if (auctionId == null || memberId == null) return null;

        Map<Long, AuctionParticipantState> room = rooms.get(auctionId);
        return room == null ? null : room.get(memberId);
    }

    public AuctionParticipantState findBySession(Long auctionId, Long memberId, String sessionId) {
        AuctionParticipantState state = find(auctionId, memberId);
        return (state != null && state.sessionId().equals(sessionId)) ? state : null;
    }

    //[변경 내용] 경매방에 현재 입장해 있는 인원 수를 돌려주는 count 메서드를 추가했다.
    //[변경 이유] 경매 상태 전환(AuctionTransitionService)에서 정원(20명) 도달과 최소 인원(5명) 충족 여부를
    //            판단하려면 방 인원 수가 필요한데, 기존에는 인원 수를 셀 방법이 없었다. 기존 메서드는 수정하지 않았다.
    public int count(Long auctionId) {
        if (auctionId == null) return 0;

        Map<Long, AuctionParticipantState> room = rooms.get(auctionId);
        return room == null ? 0 : room.size();
    }
}
