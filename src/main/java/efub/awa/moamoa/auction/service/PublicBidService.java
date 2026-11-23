package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.request.PublicBidRequest;
import efub.awa.moamoa.auction.dto.response.PublicBidResponse;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.entity.PublicBid;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.event.BidPlacedEvent;
import efub.awa.moamoa.auction.policy.AuctionPolicy;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.auction.repository.PublicBidRepository;
import efub.awa.moamoa.balance.enums.BalanceChangeType;
import efub.awa.moamoa.balance.enums.ReferenceType;
import efub.awa.moamoa.balance.service.BalanceService;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicBidService {

    private final AuctionRepository auctionRepository;
    private final PublicBidRepository publicBidRepository;
    private final MemberRepository memberRepository;
    private final BalanceService balanceService;
    private final AuctionService auctionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PublicBidResponse placeBid(Long bidderId, Long auctionId, PublicBidRequest request) {
        //같은 경매의 입찰을 한 건씩 차례로 처리하도록 경매 행을 잠근다. (상태 전환, Private 커밋/리빌과 같은 방식)
        //뒤에 온 입찰은 앞 입찰이 반영된 현재가로 판단하므로 BID_TOO_LOW 등 실제 이유로 실패한다.
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 경매 입찰 - auctionId: {}, bidderId: {}", auctionId, bidderId);
                    return new CustomException(ErrorCode.AUCTION_NOT_FOUND);
                });
        LocalDateTime now = LocalDateTime.now();

        //마지막 입찰 시각: 30초 무입찰 종료 판단과 첫 입찰 여부 판단에 함께 쓴다. (입찰이 없으면 null)
        LocalDateTime lastBidAt = publicBidRepository.findLastBidTime(auctionId).orElse(null);
        validateBiddable(auction, now, lastBidAt);

        if (!auctionService.isAttending(auction, bidderId)) {
            log.warn("참가 중이 아닌 회원의 입찰 - auctionId: {}, bidderId: {}", auctionId, bidderId);
            throw new CustomException(ErrorCode.NOT_AUCTION_PARTICIPANT);
        }

        Member previousBidder = auction.getCurrentBidder();
        if (previousBidder != null && previousBidder.getId().equals(bidderId)) {
            throw new CustomException(ErrorCode.ALREADY_HIGHEST_BIDDER);
        }

        Long amount = request.amount();
        validateAmount(auction, amount, lastBidAt);

        //입찰금 처리: 입찰자에게서 차감하고 이전 최고 입찰자에게 돌려준다. (같은 트랜잭션, 잔액 부족이면 전체 실패)
        Member bidder = lockMembers(bidderId, previousBidder);
        Long previousPrice = auction.getCurrentPrice();
        balanceService.withdraw(bidderId, amount, BalanceChangeType.AUCTION_BID, ReferenceType.AUCTION, auctionId);
        if (previousBidder != null) {
            balanceService.deposit(previousBidder.getId(), previousPrice, BalanceChangeType.AUCTION_BID_REFUND,
                    ReferenceType.AUCTION, auctionId);
        }

        LocalDateTime deadline = extendDeadline(auction, now);
        auction.placeBid(bidder, amount, deadline);

        PublicBid bid = publicBidRepository.save(PublicBid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(amount)
                .build());

        //트랜잭션 커밋 후 웹소켓이 bid_accepted로 브로드캐스트한다.
        eventPublisher.publishEvent(new BidPlacedEvent(auctionId, bidderId, bidder.getNickname(), amount,
                toEpochMillis(deadline)));

        log.info("PUBLIC 입찰 성공 - auctionId: {}, bidderId: {}, amount: {}, commitDeadline: {}",
                auctionId, bidderId, amount, deadline);
        return PublicBidResponse.of(bid, auction);
    }

    //입찰 가능한 경매: PUBLIC + OPEN + 종료 조건 미충족
    //종료 조건(마감 시각 경과, 마지막 입찰 후 30초 경과)은 상태 전환과 같은 기준이라, 스케줄러가 처리하기 전이어도 입찰을 받지 않는다.
    private void validateBiddable(Auction auction, LocalDateTime now, LocalDateTime lastBidAt) {
        if (auction.getType() != AuctionType.PUBLIC) {
            throw new CustomException(ErrorCode.NOT_PUBLIC_AUCTION);
        }

        AuctionStatus status = auction.getStatus();
        if (status == AuctionStatus.CLOSED || status == AuctionStatus.FAILED) {
            throw new CustomException(ErrorCode.ALREADY_ENDED);
        }
        if (status != AuctionStatus.OPEN) {
            throw new CustomException(ErrorCode.AUCTION_NOT_BIDDABLE);
        }

        boolean deadlinePassed = now.isAfter(auction.getCommitDeadline());
        boolean idleTimeout = lastBidAt != null
                && now.isAfter(lastBidAt.plusSeconds(AuctionPolicy.PUBLIC_IDLE_CLOSE_SECONDS));
        if (deadlinePassed || idleTimeout) {
            log.info("종료 조건이 충족된 경매 입찰 - auctionId: {}", auction.getId());
            throw new CustomException(ErrorCode.AUCTION_NOT_BIDDABLE);
        }
    }

    //입찰이 없으면 startPrice 이상, 있으면 currentPrice 초과. 최소 호가 단위는 없다.
    //입찰 유무는 입찰 기록으로 판단한다. (최고 입찰자가 탈퇴해 currentBidder가 null이어도 입찰은 있었던 것)
    private void validateAmount(Auction auction, Long amount, LocalDateTime lastBidAt) {
        boolean firstBid = lastBidAt == null;
        boolean tooLow = firstBid ? amount < auction.getStartPrice() : amount <= auction.getCurrentPrice();
        if (tooLow) {
            throw new CustomException(ErrorCode.BID_TOO_LOW);
        }
    }

    //입찰자와 이전 최고 입찰자의 회원 행을 id 오름차순으로 잠가 교착을 막는다. (다른 경매에서 서로 반대로 입찰하는 경우)
    private Member lockMembers(Long bidderId, Member previousBidder) {
        List<Long> ids = (previousBidder == null)
                ? List.of(bidderId)
                : Stream.of(bidderId, previousBidder.getId()).sorted().toList();

        Member bidder = null;
        for (Long id : ids) {
            Member member = memberRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> {
                        log.warn("존재하지 않는 회원 - memberId: {}", id);
                        return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                    });
            if (id.equals(bidderId)) bidder = member;
        }
        return bidder;
    }

    //소프트클로즈: 마감까지 10초 이하로 남았으면 10초 늘린다. 단 openStartTime + 13분을 넘지 않는다.
    private LocalDateTime extendDeadline(Auction auction, LocalDateTime now) {
        LocalDateTime deadline = auction.getCommitDeadline();
        boolean closing = !now.plusSeconds(AuctionPolicy.SOFT_CLOSE_THRESHOLD_SECONDS).isBefore(deadline);
        if (!closing) return deadline;

        LocalDateTime extended = deadline.plusSeconds(AuctionPolicy.SOFT_CLOSE_EXTENSION_SECONDS);
        LocalDateTime limit = auction.getOpenStartTime().plusMinutes(AuctionPolicy.PUBLIC_MAX_OPEN_MINUTES);
        return extended.isAfter(limit) ? limit : extended;
    }

    //bid_accepted.newDeadline은 epoch ms (서버 시간대 기준)
    private long toEpochMillis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
