package efub.awa.moamoa.auction.policy;

//경매 확정값. 생성뿐 아니라 이후 상태 전환·마감 처리에서도 같은 값을 쓰도록 한 곳에 둔다.
public final class AuctionPolicy {

    private AuctionPolicy() {}

    //입장 정원
    public static final int MAX_PARTICIPANTS = 20;

    //입장 대기 시간(분): readyStartTime부터 이 시간이 지나면 READY에서 OPEN/FAILED로 판단
    public static final long ENTRY_WAIT_MINUTES = 10;
}
