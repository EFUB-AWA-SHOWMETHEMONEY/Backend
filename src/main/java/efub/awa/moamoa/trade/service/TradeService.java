package efub.awa.moamoa.trade.service;

import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.trade.dto.request.TradeCreateRequest;
import efub.awa.moamoa.trade.dto.response.TradeCreateResponse;
import efub.awa.moamoa.trade.entity.Trade;
import efub.awa.moamoa.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public TradeCreateResponse createTrade(Long participant1Id, TradeCreateRequest request) {
        if (participant1Id.equals(request.counterpartyId())) {
            log.warn("스스로에게 거래 요청 오류 - memberId: {}", participant1Id);
            throw new CustomException(ErrorCode.SELF_TRADE_NOT_ALLOWED);
        }
        Member p1 = findByMemberId(participant1Id);
        Member p2 = findByMemberId(request.counterpartyId());

        Trade trade = Trade.builder()
                .participant1(p1)
                .participant2(p2)
                .build();
        tradeRepository.save(trade);
        return TradeCreateResponse.from(trade);

    }

    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원 - memberId: {}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }
}
