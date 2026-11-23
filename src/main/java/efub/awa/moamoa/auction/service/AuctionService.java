package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.request.CreateAuctionRequest;
import efub.awa.moamoa.auction.dto.response.AuctionListResponse;
import efub.awa.moamoa.auction.dto.response.AuctionSummaryResponse;
import efub.awa.moamoa.auction.dto.response.CreateAuctionResponse;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.policy.AuctionPolicy;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
