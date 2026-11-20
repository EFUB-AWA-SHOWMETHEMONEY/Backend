package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.request.PrivateBidCommitRequest;
import efub.awa.moamoa.auction.dto.request.PrivateBidRevealRequest;
import efub.awa.moamoa.auction.dto.response.PrivateBidCommitResponse;
import efub.awa.moamoa.auction.dto.response.PrivateBidRevealResponse;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.entity.PrivateBid;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.auction.repository.PrivateBidRepository;
import efub.awa.moamoa.auction.support.CommitHasher;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateBidService {

    private final AuctionParticipantChecker auctionParticipantChecker;
    private final AuctionRepository auctionRepository;
    private final PrivateBidRepository privateBidRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PrivateBidCommitResponse commit(Long bidderId, Long auctionId, PrivateBidCommitRequest request) {
        Member bidder = findByMemberId(bidderId);
        Auction auction = findByAuctionId(auctionId);

        auction.validatePrivate();

        if(!auctionParticipantChecker.isParticipant(auctionId, bidderId)) throw new CustomException(ErrorCode.NOT_AUCTION_PARTICIPANT);

        LocalDateTime now = LocalDateTime.now();
        auction.validateCommitPhase(now);

        boolean invalidated = privateBidRepository.findByAuctionIdAndBidderIdAndActiveTrue(auctionId, bidderId)
                .map(prev -> {
                    prev.invalidate();
                    return true;})
                .orElse(false);

        PrivateBid saved = PrivateBid.builder()
                .auction(auction)
                .bidder(bidder)
                .commitHash(request.commitHash())
                .committedAt(now)
                .build();
        privateBidRepository.save(saved);

        log.info("Private 경매 입찰 커밋 제출 성공 - bidderId:{}, auctionId:{}", bidderId, auction);
        return PrivateBidCommitResponse.from(saved, invalidated);
    }

    @Transactional
    public PrivateBidRevealResponse reveal(Long bidderId, Long auctionId, PrivateBidRevealRequest request) {
        Member bidder = findByMemberId(bidderId);
        Auction auction = findByAuctionId(auctionId);
        auction.validatePrivate();

        LocalDateTime now = LocalDateTime.now();
        auction.validateRevealPhase(now);

        //auctionId + bidderId로 유효한 커밋 조회 (isActive로 가장 latest commit 조회)
        PrivateBid bid = privateBidRepository.findByAuctionIdAndBidderIdAndActiveTrue(auctionId, bidderId)
                .orElseThrow(() -> {
                    log.info("Private 경매 입찰 커밋 reveal 실패 (원인: 유효한 커밋 부재):  bidderId - {}, auctionId - {}", bidderId, auctionId);
                    return new CustomException(ErrorCode.NO_ACTIVE_COMMIT);
                });
        //DB의 verified = true 필드로 중복 reveal 방지
        if (bid.isVerified()) {
            log.info("Private 경매 입찰 커밋 reveal 실패 (원인: 중복 reveal):  bidderId - {}, auctionId - {}", bidderId, auctionId);
            throw new CustomException(ErrorCode.ALREADY_REVEALED);
        }

        //salt + amount를 해시해서, DB에 저장해둔 hash와 이리치하는지 체크하는 로직
        if (!CommitHasher.matches(request.amount(), request.salt(), bid.getCommitHash()))  {
            log.info("Private 경매 입찰 커밋 reveal 실패 (원인: 해시 불일치):  bidderId - {}, auctionId - {}", bidderId, auctionId);
            throw new CustomException(ErrorCode.COMMIT_HASH_MISMATCH);
        }

        if (bidder.getCapital() < request.amount()) {
            log.info("Private 경매 입찰 커밋 reveal 실패 (원인: 잔액 부족):  bidderId - {}, auctionId - {}", bidderId, auctionId);
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        bid.reveal(request.amount(), now);
        log.info("Private 경매 입찰 커밋 reveal 성공: bidderId - {}, auctionId - {}", bidderId, auctionId);
        return PrivateBidRevealResponse.from(bid);
    }

    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원: memberId-{}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }
    private Auction findByAuctionId(Long auctionId) {
        return auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 경매: auctionId-{}", auctionId);
                    return new CustomException(ErrorCode.AUCTION_NOT_FOUND);
                });
    }
}
