package efub.awa.moamoa.balance.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransferType {
    TRADE(BalanceChangeType.TRADE_SEND, BalanceChangeType.TRADE_RECEIVE, ReferenceType.TRADE),
    AUCTION(BalanceChangeType.AUCTION_WIN, BalanceChangeType.AUCTION_SALE, ReferenceType.AUCTION);

    private final BalanceChangeType sendType;
    private final BalanceChangeType receiveType;
    private final ReferenceType referenceType;
}
