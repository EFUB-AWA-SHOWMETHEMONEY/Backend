package efub.awa.moamoa.auction.policy;

//경매 확정값. 생성뿐 아니라 이후 상태 전환·마감 처리에서도 같은 값을 쓰도록 한 곳에 둔다.
public final class AuctionPolicy {

    private AuctionPolicy() {}

    //입장 정원
    public static final int MAX_PARTICIPANTS = 20;

    //입장 대기 시간(분): readyStartTime부터 이 시간이 지나면 READY에서 OPEN/FAILED로 판단
    public static final long ENTRY_WAIT_MINUTES = 10;

    //경매 시작(OPEN)에 필요한 최소 인원
    public static final int MIN_PARTICIPANTS = 5;

    //입찰/커밋 시간(분): openStartTime부터 commitDeadline까지
    public static final long COMMIT_MINUTES = 10;

    //PRIVATE 리빌 시간(분): commitDeadline부터 revealDeadline까지
    public static final long REVEAL_MINUTES = 3;

    //PUBLIC 경매는 마지막 입찰 후 이 시간(초) 동안 새 입찰이 없으면 종료
    public static final long PUBLIC_IDLE_CLOSE_SECONDS = 30;

    //PUBLIC 소프트클로즈: 마감까지 이 시간(초) 이하로 남았을 때 입찰이 들어오면
    public static final long SOFT_CLOSE_THRESHOLD_SECONDS = 10;

    //마감 시각을 이 시간(초)만큼 늘린다
    public static final long SOFT_CLOSE_EXTENSION_SECONDS = 10;

    //단, 마감 시각은 openStartTime + 이 시간(분)을 넘지 않는다
    public static final long PUBLIC_MAX_OPEN_MINUTES = 13;
}
