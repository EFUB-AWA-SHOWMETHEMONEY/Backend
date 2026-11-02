package efub.awa.moamoa.websocket.plaza.service;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.websocket.plaza.dto.request.MoveRequestDto;
import efub.awa.moamoa.websocket.plaza.dto.response.PlazaEvent.PositionUpdate;
import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.websocket.plaza.entity.ParticipantState;
import efub.awa.moamoa.websocket.plaza.enums.PlazaConstants;
import efub.awa.moamoa.websocket.plaza.repository.PlazaParticipantRegistry;
import efub.awa.moamoa.item.repository.InventoryRepository;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlazaService {

    private final PlazaParticipantRegistry registry;
    private final MemberRepository memberRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public ParticipantState join(Long memberId, String sessionId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원 - memberId: {}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
        EquippedItem equippedItem = inventoryRepository.findEquippedItems(memberId)
                .stream().findFirst()
                .map(EquippedItem::from)
                .orElse(null);

        ParticipantState state = new ParticipantState(
                memberId, sessionId, member.getNickname(), equippedItem,
                PlazaConstants.SPAWN_X, PlazaConstants.SPAWN_Y, 0L
        );

        registry.join(state).ifPresent(prev ->
                log.info("중복 접속: memberId - {}, 이전 세션 - {}", memberId, prev.sessionId()));

        return state;
    }
    public Optional<PositionUpdate> move(Long memberId, String sessionId, MoveRequestDto request) {
        if (registry.find(memberId).isEmpty()) {
            throw new CustomException(ErrorCode.NOT_IN_ROOM);
        }

        int x = Math.min(Math.max(request.x(), 0), PlazaConstants.WIDTH-1);
        int y = Math.min(Math.max(request.y(), 0), PlazaConstants.HEIGHT-1);
        return registry.updatePosition(memberId, sessionId, x, y, request.seq())
                .map(PositionUpdate::from);
    }

}
