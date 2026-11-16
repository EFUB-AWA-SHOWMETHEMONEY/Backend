package efub.awa.moamoa.websocket.auction.service;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionItemResponse;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionParticipantResponse;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionSnapshotResponse;
import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import efub.awa.moamoa.websocket.plaza.dto.request.ChatRequestDto;
import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.websocket.plaza.support.ChatFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionRoomService {

    private final AuctionParticipantRegistry registry;
    private final MemberRepository memberRepository;
    private final InventoryService inventoryService;
    private final ChatFilter chatFilter;
    private final AuctionRepository auctionRepository;

    @Transactional(readOnly = true)
    public AuctionParticipantState join(Long auctionId, Long memberId, String sessionId) {
        Member member = findMemberById(memberId);
        EquippedItem equippedItem = inventoryService.findEquippedItems(memberId)
                .stream().findFirst()
                .map(EquippedItem::from)
                .orElse(null);

        AuctionParticipantState state = new AuctionParticipantState(
                memberId, sessionId, member.getNickname(), equippedItem,
                auctionId
        );

        //registry.join은 이전 상태를 반환하므로(중복 접속 감지용) 브로드캐스트에는 새 state를 사용
        AuctionParticipantState prev = registry.join(auctionId, state);
        if (prev != null) {
            log.info("경매 중복 접속: memberId - {}, auctionId - {}, 이전 세션 - {}", memberId, auctionId, prev.sessionId());
        }

        return state;
    }

    public AuctionEvent.AuctionChat chat(Long auctionId, Long memberId, String sessionId, ChatRequestDto request) {
        AuctionParticipantState state = registry.findBySession(auctionId, memberId, sessionId);
        if (state == null) {
            log.warn("경매에 채팅 거부 - memberId: {}, auctionId: {}. sessionId: {}", memberId, auctionId, sessionId);
            throw new CustomException(ErrorCode.NOT_IN_AUCTION_ROOM);
        }
        String text = chatFilter.mask(request.text().trim());

        return AuctionEvent.AuctionChat.of(state, text);
    }

    @Transactional(readOnly = true)
    public AuctionSnapshotResponse getSnapshot(Long memberId, Long auctionId) {
        long snapshotAt = System.currentTimeMillis();

        Auction auction = findAuctionById(auctionId);
        if(registry.find(auctionId, memberId) == null) {
            throw new CustomException(ErrorCode.NOT_IN_AUCTION_ROOM);
        }

        //TODO: 아이템 이미지 -> S3 다운로드 및 조회 메소드 호출 필요
        String itemS3ImageUrl = auction.getInventory().getItem().getImageUrl();
        AuctionItemResponse auctionItem = AuctionItemResponse.of(auction, itemS3ImageUrl);
        List<AuctionParticipantResponse> participants = registry.findAllByAuctionId(auctionId)
                .stream().map(AuctionParticipantResponse::from).toList();

        return AuctionSnapshotResponse.of(auction, auctionItem, participants, snapshotAt);
    }

    @Transactional(readOnly = true)
    public void validateJoinable(Long auctionId, Long memberId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 경매 접속: auctionId - {}", auctionId);
                    return new CustomException(ErrorCode.AUCTION_NOT_FOUND);
                });

        switch(auction.getStatus()) {
            case READY -> {}
            case CLOSED, FAILED -> throw new CustomException(ErrorCode.ALREADY_ENDED);
            case OPEN -> {
                if (registry.find(auctionId, memberId) != null) return;
                throw new CustomException(ErrorCode.ALREADY_STARTED);
            }
        }
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원 - memberId: {}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    private Auction findAuctionById(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 경매 - auctionId: {}", auctionId);
                    return new CustomException(ErrorCode.AUCTION_NOT_FOUND);
                });
    }
}
