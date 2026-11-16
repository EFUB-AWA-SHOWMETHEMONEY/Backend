package efub.awa.moamoa.websocket.auction.enums;

import java.util.regex.Pattern;

public class AuctionConstants {
    private AuctionConstants() {}

    public static final String TOPIC_PREFIX = "/topic/auctions/";
    public static final String TOPIC_TEMPLATE = TOPIC_PREFIX + "{auctionId}";
    public static final Pattern TOPIC_PATTERN = Pattern.compile("^/topic/auctions/(\\d+)$");

    public static String topic(Long auctionId) {
        return TOPIC_PREFIX + auctionId;
    }
}
