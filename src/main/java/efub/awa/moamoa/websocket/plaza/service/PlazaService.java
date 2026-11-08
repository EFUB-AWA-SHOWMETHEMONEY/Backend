package efub.awa.moamoa.websocket.plaza.service;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.websocket.plaza.dto.request.ChatRequestDto;
import efub.awa.moamoa.websocket.plaza.dto.request.MoveRequestDto;
import efub.awa.moamoa.websocket.plaza.dto.response.PlazaEvent;
import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.websocket.plaza.entity.ParticipantState;
import efub.awa.moamoa.websocket.plaza.enums.PlazaConstants;
import efub.awa.moamoa.websocket.plaza.repository.PlazaParticipantRegistry;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.websocket.plaza.support.ChatFilter;
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
    private final InventoryService inventoryService;
    private final ChatFilter chatFilter;

    @Transactional(readOnly = true)
    public ParticipantState join(Long memberId, String sessionId) {
        Member member = findMemberById(memberId);
        EquippedItem equippedItem = inventoryService.findEquippedItems(memberId)
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
    public Optional<PlazaEvent.PositionUpdate> move(Long memberId, String sessionId, MoveRequestDto request) {
        isPresent(memberId);

        int x = Math.min(Math.max(request.x(), 0), PlazaConstants.WIDTH-1);
        int y = Math.min(Math.max(request.y(), 0), PlazaConstants.HEIGHT-1);
        return registry.updatePosition(memberId, sessionId, x, y, request.seq())
                .map(PlazaEvent.PositionUpdate::from);
    }

    public PlazaEvent.PlazaChat chat(Long memberId, String sessionId, ChatRequestDto request) {
        ParticipantState state = registry.findBySession(memberId, sessionId)
                .orElseThrow(() -> {
                    log.warn("광장에 채팅 거부 - memberId: {}, sessionId: {}", memberId, sessionId);
                    return new CustomException(ErrorCode.NOT_IN_ROOM);
                });
        String text = chatFilter.mask(request.text().trim());

        return PlazaEvent.PlazaChat.of(state, text);
    }

    private void isPresent(Long memberId) {
        if (registry.find(memberId).isEmpty()) {
            throw new CustomException(ErrorCode.NOT_IN_ROOM);
        }
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원 - memberId: {}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }
}
