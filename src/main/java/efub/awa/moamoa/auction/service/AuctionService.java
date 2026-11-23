package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.request.CreateAuctionRequest;
import efub.awa.moamoa.auction.dto.response.AuctionListResponse;
import efub.awa.moamoa.auction.dto.response.AuctionSummaryResponse;
import efub.awa.moamoa.auction.dto.response.CreateAuctionResponse;
import efub.awa.moamoa.auction.dto.response.JoinAuctionResponse;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.policy.AuctionPolicy;
import efub.awa.moamoa.auction.repository.AuctionEntryListRepository;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.enums.AuctionConstants;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AuctionRepository auctionRepository;
    private final MemberRepository memberRepository;
    private final InventoryService inventoryService;
    private final AuctionRoomService auctionRoomService;
    private final AuctionParticipantRegistry participantRegistry;
    private final AuctionEntryListRepository entryListRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public CreateAuctionResponse createAuction(Long memberId, CreateAuctionRequest request) {
        validateReadyStartTime(request.readyStartTime());

        Member member = getMember(memberId);

        //본인 소유/장착 여부/수량 검증 후 수량 1 차감 (같은 트랜잭션에서 경매 저장까지 처리)
        Inventory inventory = inventoryService.reserveForAuction(memberId, request.inventoryId());

        Auction auction = Auction.builder()
                .inventory(inventory)
                .auctionOwner(member)
                .type(request.type())
                .startPrice(request.startPrice())
                .readyStartTime(request.readyStartTime())
                .maxParticipants(AuctionPolicy.MAX_PARTICIPANTS)
                .entryDeadline(request.readyStartTime().plusMinutes(AuctionPolicy.ENTRY_WAIT_MINUTES))
                .build();
        Auction saved = auctionRepository.save(auction);

        log.info("경매 생성 성공 - auctionId: {}, memberId: {}, inventoryId: {}, type: {}",
                saved.getId(), memberId, inventory.getId(), saved.getType());
        return CreateAuctionResponse.from(saved);
    }

    //입장 가능한(READY) 경매와 진행 중인(OPEN) 경매를 입장 시작 시각 순으로 조회
    @Transactional(readOnly = true)
    public AuctionListResponse getAuctionList() {
        List<AuctionSummaryResponse> auctions = auctionRepository
                .findAllByStatusInWithItemAndOwner(List.of(AuctionStatus.READY, AuctionStatus.OPEN))
                .stream()
                .map(AuctionSummaryResponse::from)
                .toList();

        log.info("경매 목록 조회 성공 - totalCount: {}", auctions.size());
        return AuctionListResponse.of(auctions, auctions.size());
    }

    //입장 가능 여부만 검증하고 방 정보를 돌려준다. 실제 입장(참가자 등록, user_joined)은 구독 시점에 일어나며 같은 검증을 쓴다.
    @Transactional(readOnly = true)
    public JoinAuctionResponse joinAuction(Long memberId, Long auctionId) {
        Auction auction = getAuction(auctionId);
        auctionRoomService.validateJoinable(auction, memberId);

        int currentParticipants = participantRegistry.count(auctionId);

        log.info("경매 입장 검증 성공 - auctionId: {}, memberId: {}, status: {}, 현재 인원: {}",
                auctionId, memberId, auction.getStatus(), currentParticipants);
        return JoinAuctionResponse.of(auction, currentParticipants);
    }

    //[경매 퇴장하기] 버튼. 연결 끊김은 퇴장으로 보지 않는다.
    //처리 순서: 참가 중 확인 -> (OPEN + PUBLIC) 퇴장 명단 기록 -> 방에서 제거 -> 실제로 제거됐으면 user_left 발행
    //퇴장 명단 기록이 실패하면 예외(500)로 끝나고 방에서는 제거하지 않아 다시 시도할 수 있다.
    @Transactional(readOnly = true)
    public void leaveAuction(Long memberId, Long auctionId) {
        Auction auction = getAuction(auctionId);
        AuctionStatus status = auction.getStatus();

        if (status == AuctionStatus.CLOSED || status == AuctionStatus.FAILED) {
            log.warn("종료된 경매 퇴장 시도 - auctionId: {}, memberId: {}, status: {}", auctionId, memberId, status);
            throw new CustomException(ErrorCode.ALREADY_ENDED);
        }
        if (!isAttending(auction, memberId)) {
            log.warn("참가 중이 아닌 회원의 경매 퇴장 시도 - auctionId: {}, memberId: {}", auctionId, memberId);
            throw new CustomException(ErrorCode.ALREADY_LEFT);
        }

        //OPEN 이후 PUBLIC 퇴장만 기록한다. (READY 퇴장과 PRIVATE 퇴장은 재입장할 수 있다)
        if (status == AuctionStatus.OPEN && auction.getType() == AuctionType.PUBLIC) {
            entryListRepository.addLeft(auctionId, memberId);
        }

        //연결이 끊겨 이미 방에서 빠진 사람은 user_left가 이미 발행되었으므로 다시 보내지 않는다.
        if (participantRegistry.leaveByMember(auctionId, memberId)) {
            messagingTemplate.convertAndSend(AuctionConstants.topic(auctionId), AuctionEvent.UserLeft.of(memberId));
        }

        log.info("경매 퇴장 성공 - auctionId: {}, memberId: {}, status: {}, type: {}",
                auctionId, memberId, status, auction.getType());
    }

    //참가 중: 지금 방에 있거나, OPEN 이후 참가자 명단에 있고 퇴장 명단에는 없는 경우 (연결이 끊긴 참가자 포함)
    private boolean isAttending(Auction auction, Long memberId) {
        Long auctionId = auction.getId();
        if (participantRegistry.find(auctionId, memberId) != null) return true;
        if (auction.getStatus() != AuctionStatus.OPEN) return false;

        return entryListRepository.isInParticipantList(auctionId, memberId)
                && !entryListRepository.isInLeftList(auctionId, memberId);
    }

    private void validateReadyStartTime(LocalDateTime readyStartTime) {
        if (!readyStartTime.isAfter(LocalDateTime.now(KST))) {
            log.warn("현재 시각 이전의 경매 입장 시작 시각 - readyStartTime: {}", readyStartTime);
            throw new CustomException(ErrorCode.INVALID_READY_START_TIME);
        }
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원 - memberId: {}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    private Auction getAuction(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 경매 - auctionId: {}", auctionId);
                    return new CustomException(ErrorCode.AUCTION_NOT_FOUND);
                });
    }
}
