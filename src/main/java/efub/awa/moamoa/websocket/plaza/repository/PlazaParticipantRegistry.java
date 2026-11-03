package efub.awa.moamoa.websocket.plaza.repository;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.websocket.plaza.entity.ParticipantState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class PlazaParticipantRegistry {

    private final Map<Long, ParticipantState> participants = new ConcurrentHashMap<>();

    //입장: 같은 유저의 기존 상태 있으면 반환 (중복 접속 감지용)
    public Optional<ParticipantState> join(ParticipantState state) {
        return Optional.ofNullable(participants.put(state.memberId(), state));
    }

    //퇴장: sessionId가 현재 등록된 세션과 같을 때만 제거
    public boolean leave(Long memberId, String sessionId) {
        AtomicBoolean removed = new AtomicBoolean(false);
        participants.computeIfPresent(memberId, (id, state) -> {
            if (!state.sessionId().equals(sessionId)) return state;
            removed.set(true);
            return null;
        });
        return removed.get();
    }

    public Optional<ParticipantState> updatePosition(Long memberId, String sessionId, int x, int y, long seq) {
        AtomicReference<ParticipantState> applied = new AtomicReference<>();
        participants.computeIfPresent(memberId, (id, s) -> {
            //구 세션이거나 낡은 프레임 -> 무시
            if (!s.sessionId().equals(sessionId) || seq <= s.seq()) {
                return s;
            }
            ParticipantState next = s.withPosition(x, y, seq);
            applied.set(next);

            return next;
        });

        return Optional.ofNullable(applied.get());
    }

    public Optional<ParticipantState> updateEquippedItem(Long memberId, EquippedItem item) {
        AtomicReference<ParticipantState> applied = new AtomicReference<>();
        participants.computeIfPresent(memberId, (id, s) -> {
            ParticipantState next = s.withEquippedItem(item);
            applied.set(next);
            return next;
        });
        return Optional.ofNullable(applied.get());
    }

    public List<ParticipantState> findAll() {
        return List.copyOf(participants.values());
    }

    public Optional<ParticipantState> find(Long memberId) {
        return Optional.ofNullable(participants.get(memberId));
    }

    public Optional<ParticipantState> findBySession(Long memberId, String sessionId) {
        return find(memberId)
                .filter(state ->
                        state.sessionId().equals(sessionId));
    }
}
