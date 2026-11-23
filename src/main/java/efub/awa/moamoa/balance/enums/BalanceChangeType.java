package efub.awa.moamoa.balance.enums;

public enum BalanceChangeType {
    SHOP_PURCHASE,
    SHOP_SELL,
    AUCTION_WIN,
    AUCTION_SALE,
    //[변경 내용] PUBLIC 경매 입찰금 차감(AUCTION_BID)과 반환(AUCTION_BID_REFUND) 타입을 추가했다.
    //[변경 이유] PUBLIC은 입찰할 때 입찰금을 바로 차감하고, 더 높은 입찰에 밀려나면 바로 돌려준다.
    //            차감 후 밀려나면 낙찰이 아니므로 AUCTION_WIN 대신 별도 타입으로 기록한다. 기존 값은 그대로다.
    AUCTION_BID,
    AUCTION_BID_REFUND,
    TRADE_SEND,
    TRADE_RECEIVE,
    ATTENDANCE_REWARD,
    QUIZ_REWARD,
    BANK_DEPOSIT,
    BANK_WITHDRAW,
    ADMIN_GRANT
}
