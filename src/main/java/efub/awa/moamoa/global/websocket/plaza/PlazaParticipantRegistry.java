package efub.awa.moamoa.global.websocket.plaza;

import efub.awa.moamoa.global.websocket.plaza.enums.ParticipantState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PlazaParticipantRegistry {

    private final Map<Long, ParticipantState> participants = new ConcurrentHashMap<>();

    //입장: 같은 유저의 기존 상태 있으면 반환 (중복 접속 감지용)
    public Optional<ParticipantState> join(ParticipantState state) {
        return Optional.ofNullable(participants.put(state.memberId(), state));
    }

    //퇴장: sessionId가 현재 등록된 세션과 같을 때만 제거
    public boolean leave(Long memberId, String sessionId) {
        ParticipantState current = participants.get(memberId);
        if (current == null || !current.sessionId().equals(sessionId)) return false;

        return participants.remove(memberId, current);
    }

    public Optional<ParticipantState> updatePosition(Long memberId, int x, int y) {
        return Optional.ofNullable(
                participants.computeIfPresent(memberId, (id, s) -> s.withPosition(x, y))
        );
    }

    public List<ParticipantState> findAll() {
        return List.copyOf(participants.values());
    }
}
