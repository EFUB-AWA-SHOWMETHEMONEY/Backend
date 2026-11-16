package efub.awa.moamoa.websocket.auction.dto.response;

import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;

public sealed interface AuctionEvent {

    String type();
    long ts();

    //join: user_join
    record UserJoined(String type, Long memberId, String nickname, EquippedItem equippedItem,
                      long ts) implements AuctionEvent{
        public static UserJoined from (AuctionParticipantState state) {
            return new UserJoined(
                    "user_joined", state.memberId(), state.nickname(),
                    state.equippedItem(), System.currentTimeMillis()
            );
        }
    }

    //left: user_left
    record UserLeft(String type, Long memberId, long ts) implements AuctionEvent{
        public static UserLeft of (Long memberId) {
            return new UserLeft("user_left", memberId, System.currentTimeMillis());
        }
    }

    //chat: auction_chat
    record AuctionChat(String type, Long memberId, String nickname, String text, long ts) implements AuctionEvent {
        public static AuctionChat of (AuctionParticipantState state, String text) {
            return new AuctionChat(
                    "auction_chat", state.memberId(), state.nickname(), text, System.currentTimeMillis()
            );
        }
    }

    //bid: bid_accepted
    record PublicBidAccepted(String type, Long bidderId, String nickname, Long price, long newDeadline, long ts) implements AuctionEvent {
        public static PublicBidAccepted of(Long bidderId, String nickname, Long price, long newDeadline) {
            return new PublicBidAccepted(
                    "bid_accepted", bidderId, nickname, price, newDeadline, System.currentTimeMillis()
            );
        }
    }

    //close: auction_closed
    record AuctionClosed(String type, String result, Long winnerId, Long finalPrice, long ts) implements AuctionEvent {
        public static AuctionClosed success(Long winnerId, Long finalPrice) {
            return new AuctionClosed(
                    "auction_closed", "SUCCESS", winnerId, finalPrice, System.currentTimeMillis()
            );
        }

        public static AuctionClosed failed() {
            return new AuctionClosed(
                    "auction_closed", "FAILED", null, null, System.currentTimeMillis()
            );
        }
    }
}
