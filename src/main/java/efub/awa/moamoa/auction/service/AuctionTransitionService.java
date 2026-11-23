package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.response.AuctionTransitionResult;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.event.AuctionClosedEvent;
import efub.awa.moamoa.auction.policy.AuctionPolicy;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.auction.repository.PublicBidRepository;
import efub.awa.moamoa.balance.enums.BalanceChangeType;
import efub.awa.moamoa.balance.enums.ReferenceType;
import efub.awa.moamoa.balance.service.BalanceService;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

//경매 상태 전환 (외부에 공개하는 REST 엔드포인트 없음: 입장 처리(정원 도달 시)와 스케줄러가 직접 호출한다)
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionTransitionService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AuctionRepository auctionRepository;
    private final PublicBidRepository publicBidRepository;
    private final AuctionParticipantRegistry participantRegistry;
    private final BalanceService balanceService;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    //현재 상태, 시각, 참가 인원을 조회해서 전환할 수 있으면 한 단계만 전환한다.
    //전환 조건을 만족하지 않으면 AUCTION_CONDITION_NOT_MET
    @Transactional
    public AuctionTransitionResult transition(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 경매 전환 시도 - auctionId: {}", auctionId);
                    return new CustomException(ErrorCode.AUCTION_NOT_FOUND);
                });
        LocalDateTime now = LocalDateTime.now(KST);

        return switch (auction.getStatus()) {
            case READY -> transitionFromReady(auction, now);
            case OPEN -> transitionFromOpen(auction, now);
            case CLOSED, FAILED -> {
                log.warn("이미 종료된 경매 전환 시도 - auctionId: {}, status: {}", auctionId, auction.getStatus());
                throw new CustomException(ErrorCode.ALREADY_ENDED);
            }
        };
    }

    //READY -> OPEN: 정원 도달, 또는 입장 대기 시간 경과 + 최소 인원 충족
    //READY -> FAILED: 입장 대기 시간 경과 + 최소 인원 미달
    private AuctionTransitionResult transitionFromReady(Auction auction, LocalDateTime now) {
        int participantCount = participantRegistry.count(auction.getId());
        boolean full = participantCount >= auction.getMaxParticipants();
        boolean entryClosed = now.isAfter(auction.getEntryDeadline());

        if (full || (entryClosed && participantCount >= AuctionPolicy.MIN_PARTICIPANTS)) {
            auction.open(now);
            saveAndFlush(auction);

            log.info("경매 시작 - auctionId: {}, 참가 인원: {}", auction.getId(), participantCount);
            return AuctionTransitionResult.of(auction, null, null);
        }

        if (entryClosed) {
            log.info("인원 미달로 경매 유찰 - auctionId: {}, 참가 인원: {}", auction.getId(), participantCount);
            return failAuction(auction);
        }

        throw new CustomException(ErrorCode.AUCTION_CONDITION_NOT_MET);
    }

    private AuctionTransitionResult transitionFromOpen(Auction auction, LocalDateTime now) {
        if (auction.getType() == AuctionType.PRIVATE) {
            return transitionPrivateOpen(auction, now);
        }
        return transitionPublicOpen(auction, now);
    }

    //OPEN -> CLOSED / FAILED (PUBLIC)
    //종료 조건: 마감(commitDeadline, 연장 반영) 시각이 지났거나, 마지막 입찰로부터 30초 동안 새 입찰이 없을 때
    private AuctionTransitionResult transitionPublicOpen(Auction auction, LocalDateTime now) {
        LocalDateTime lastBidAt = publicBidRepository.findLastBidTime(auction.getId()).orElse(null);

        boolean deadlinePassed = now.isAfter(auction.getCommitDeadline());
        boolean idleTimeout = lastBidAt != null
                && now.isAfter(lastBidAt.plusSeconds(AuctionPolicy.PUBLIC_IDLE_CLOSE_SECONDS));
        if (!deadlinePassed && !idleTimeout) {
            throw new CustomException(ErrorCode.AUCTION_CONDITION_NOT_MET);
        }

        //PUBLIC은 입찰을 startPrice 이상만 받으므로 최고 입찰자가 있으면 곧 유효 낙찰자이다.
        Member winner = auction.getCurrentBidder();
        if (winner == null) {
            log.info("낙찰자 없이 경매 유찰 - auctionId: {}", auction.getId());
            return failAuction(auction);
        }

        return settlePublic(auction, winner);
    }

    //낙찰 정산 (PUBLIC, 한 트랜잭션): 낙찰금을 판매자에게 지급하고 낙찰자에게 아이템을 지급한다.
    //낙찰자의 돈은 입찰할 때 이미 차감되어 있고, 다른 입찰자의 돈도 밀려날 때 이미 반환되었으므로 추가로 차감/반환하지 않는다.
    private AuctionTransitionResult settlePublic(Auction auction, Member winner) {
        Member seller = auction.getAuctionOwner();
        if (seller == null) {
            //진행 중인 경매가 있으면 탈퇴할 수 없으므로 발생하면 안 되는 상태: 낙찰금이 사라지지 않도록 정산하지 않고 롤백한다.
            log.error("경매 등록자가 없어 정산할 수 없음 - auctionId: {}", auction.getId());
            throw new IllegalStateException("경매 등록자가 없습니다. auctionId=" + auction.getId());
        }

        Long finalPrice = auction.getCurrentPrice();
        auction.close();
        saveAndFlush(auction);

        balanceService.deposit(seller.getId(), finalPrice, BalanceChangeType.AUCTION_SALE,
                ReferenceType.AUCTION, auction.getId());
        inventoryService.grantAuctionItem(winner, auction.getInventory().getItem());
        eventPublisher.publishEvent(new AuctionClosedEvent(auction.getId(), winner.getId(), finalPrice));

        log.info("경매 낙찰 - auctionId: {}, winnerId: {}, finalPrice: {}", auction.getId(), winner.getId(), finalPrice);
        return AuctionTransitionResult.of(auction, winner.getId(), finalPrice);
    }

    //PRIVATE 경매의 마감(낙찰자 판정과 정산)은 이번 범위에서 제외한다.
    //이유: PRIVATE 입찰금을 원본 금액 공개(리빌) 때 차감하는지가 확정되지 않아 환불/차감 방식을 정할 수 없다.
    //      (리빌 API는 다른 팀원이 개발했으므로 그 처리 방식이 확정되면 이 부분을 구현한다)
    private AuctionTransitionResult transitionPrivateOpen(Auction auction, LocalDateTime now) {
        if (!now.isAfter(auction.getRevealDeadline())) {
            throw new CustomException(ErrorCode.AUCTION_CONDITION_NOT_MET);
        }
        throw new UnsupportedOperationException(
                "PRIVATE 경매 마감 처리는 아직 지원하지 않습니다. auctionId=" + auction.getId());
    }

    //FAILED 처리: 판매자에게 아이템을 돌려주고 종료 이벤트를 발행한다. (winnerId가 null이면 유찰)
    private AuctionTransitionResult failAuction(Auction auction) {
        auction.fail();
        saveAndFlush(auction);

        inventoryService.restoreForAuction(auction.getInventory().getId());
        eventPublisher.publishEvent(new AuctionClosedEvent(auction.getId(), null, null));

        return AuctionTransitionResult.of(auction, null, null);
    }

    //경매 상태를 가장 먼저 DB에 반영(flush)해서 낙관적 락(version) 충돌을 이후의 돈/아이템 처리보다 앞에서 감지한다.
    //같은 경매를 동시에 처리해 충돌하면 RESOURCE_BUSY로 실패시키고(전체 롤백) 호출한 쪽이 다음 주기에 다시 시도한다.
    private void saveAndFlush(Auction auction) {
        try {
            auctionRepository.saveAndFlush(auction);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            log.warn("경매 상태 전환 충돌 - auctionId: {}", auction.getId());
            throw new CustomException(ErrorCode.RESOURCE_BUSY);
        }
    }
}
