package efub.awa.moamoa.member.enums;

public enum BalanceChangeType {
    ATTENDANCE, //출석 보상 (+)
    QUIZ, //퀴즈 보상 (+)
    SHOP_PURCHASE, //상점 구매 (-)
    BANK_DEPOSIT, //예금 가입(차감) (-)
    BANK_INTEREST, //이자 지급 (+)
    AUCTION_BID, //입찰 보증금 (-)
    AUCTION_WIN, //낙찰 (-)
    AUCTION_REFUND, //입찰 환불 (+)
    TRADE_BUY, //거래_구매 (-)
    TRADE_SELL //거래_판매 (+)
}
