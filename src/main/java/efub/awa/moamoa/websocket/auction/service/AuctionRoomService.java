package efub.awa.moamoa.websocket.auction.service;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.repository.AuctionEntryListRepository;
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

import java.time.LocalDateTime;
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
    //[변경 내용] OPEN 이후 입장 판단에 쓰는 참가자 명단/퇴장 명단(Redis) 저장소를 추가했다. (아래 validateJoinable 참고)
    private final AuctionEntryListRepository entryListRepository;

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

        log.info("경매 웹소켓 입장 성공 - memberId: {}, auctionId: {}", memberId, auctionId);
        return state;
    }

    public AuctionEvent.AuctionChat chat(Long auctionId, Long memberId, String sessionId, ChatRequestDto request) {
        AuctionParticipantState state = registry.findBySession(auctionId, memberId, sessionId);
        if (state == null) {
            log.warn("경매에 채팅 거부 - memberId: {}, auctionId: {}. sessionId: {}", memberId, auctionId, sessionId);
            throw new CustomException(ErrorCode.NOT_IN_AUCTION_ROOM);
        }
        String text = chatFilter.mask(request.text().trim());

        log.info("경매 웹소켓 채팅 성공 - memberId: {}, auctionId: {}", memberId, auctionId);
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

        log.info("경매 스냅샷 조회 API 요청 성공 - memberId: {}, auctionId: {}", memberId, auctionId);
        return AuctionSnapshotResponse.of(auction, auctionItem, participants, snapshotAt);
    }

    //[변경 내용] 입장 검증을 경매 입장 명세서의 검사 순서대로 확장했다.
    //  종료 확인 -> 판매자 본인 차단(SELLER_CANNOT_JOIN) -> 이미 방에 있으면 통과(멱등)
    //  -> READY: 입장 시작 전(ENTRY_NOT_STARTED), 정원 초과(ROOM_FULL)
    //  -> OPEN: 참가자 명단에 없으면 ALREADY_STARTED, PUBLIC 퇴장 명단에 있으면 REENTRY_NOT_ALLOWED, 그 외는 재접속 허용
    //  REST 입장 API(AuctionService.joinAuction)도 같은 검증을 쓰도록 Auction을 받는 메서드를 추가했다.
    //[변경 이유] 기존에는 READY면 항상 입장할 수 있었고, OPEN 중 연결이 끊긴 참가자는 다시 들어올 수 없었다.
    //            정원/판매자/입장 시작 시각 검사를 넣고, 웹소켓 컨트랙트의 재연결 규칙에 맞춰 OPEN 참가자의 재접속을 허용한다.
    @Transactional(readOnly = true)
    public void validateJoinable(Long auctionId, Long memberId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 경매 접속: auctionId - {}", auctionId);
                    return new CustomException(ErrorCode.AUCTION_NOT_FOUND);
                });
        validate(auction, auctionId, memberId);
    }

    public void validateJoinable(Auction auction, Long memberId) {
        validate(auction, auction.getId(), memberId);
    }

    //구독 시점 검증은 요청으로 받은 auctionId를 그대로 써서 방(레지스트리)과 명단을 조회한다. (기존 동작과 동일)
    private void validate(Auction auction, Long auctionId, Long memberId) {
        AuctionStatus status = auction.getStatus();

        if (status == AuctionStatus.CLOSED || status == AuctionStatus.FAILED) {
            throw new CustomException(ErrorCode.ALREADY_ENDED);
        }
        if (isSeller(auction, memberId)) {
            log.warn("판매자 본인 경매 입장 시도: memberId - {}, auctionId - {}", memberId, auctionId);
            throw new CustomException(ErrorCode.SELLER_CANNOT_JOIN);
        }
        //이미 방에 있으면(다른 탭 재구독, 입장 API 재호출) 정원과 상관없이 통과
        if (registry.find(auctionId, memberId) != null) return;

        if (status == AuctionStatus.READY) {
            if (LocalDateTime.now().isBefore(auction.getReadyStartTime())) {
                throw new CustomException(ErrorCode.ENTRY_NOT_STARTED);
            }
            //정원이 차면 스케줄러가 곧 OPEN으로 전환한다. 전환 전까지 들어오는 입장은 막는다.
            if (registry.count(auctionId) >= auction.getMaxParticipants()) {
                throw new CustomException(ErrorCode.ROOM_FULL);
            }
            return;
        }

        //OPEN: 시작 순간 방에 있던 사람만 다시 들어올 수 있다. (연결이 끊긴 참가자의 재접속)
        if (!entryListRepository.isInParticipantList(auctionId, memberId)) {
            throw new CustomException(ErrorCode.ALREADY_STARTED);
        }
        //PUBLIC은 시작 후 퇴장 버튼으로 나가면 다시 들어올 수 없다. (퇴장 명단에는 PUBLIC 퇴장만 기록된다)
        if (entryListRepository.isInLeftList(auctionId, memberId)) {
            throw new CustomException(ErrorCode.REENTRY_NOT_ALLOWED);
        }
    }

    //판매자가 탈퇴하면 경매 등록자가 null이 될 수 있다.
    private boolean isSeller(Auction auction, Long memberId) {
        Member owner = auction.getAuctionOwner();
        return owner != null && owner.getId().equals(memberId);
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
