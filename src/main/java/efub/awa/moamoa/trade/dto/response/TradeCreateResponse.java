package efub.awa.moamoa.trade.dto.response;

import efub.awa.moamoa.trade.entity.Trade;
import efub.awa.moamoa.trade.enums.TradeStatus;

public record TradeCreateResponse(
        Long tradeId,
        Long memberId,
        String nickname,
        Long counterpartyId,
        String counterpartyNickname,
        TradeStatus status,
        boolean myConfirmed,
        boolean counterpartyConfirmed
) {
    public static TradeCreateResponse from (Trade trade) {
        return new TradeCreateResponse(
                trade.getId(),
                trade.getParticipant1().getId(),
                trade.getParticipant1().getNickname(),
                trade.getParticipant2().getId(),
                trade.getParticipant2().getNickname(),
                trade.getStatus(),
                trade.isP1Confirmed(),
                trade.isP2Confirmed()
        );
    }
}
