package efub.awa.moamoa.auction.service;

import efub.awa.moamoa.auction.dto.response.JoinAuctionResponse;
import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.repository.AuctionEntryListRepository;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import efub.awa.moamoa.websocket.plaza.support.ChatFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//경매 입장 API(AuctionService.joinAuction)와 입장 검증(AuctionRoomService.validateJoinable)
class AuctionJoinTest {

    private static final long AUCTION = 15L;
    private static final long SELLER = 1L;
    private static final long MEMBER = 2L;

    private AuctionRepository auctionRepository;
    private AuctionEntryListRepository entryListRepository;
    private AuctionParticipantRegistry registry;
    private AuctionService auctionService;

    private Member seller;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        entryListRepository = mock(AuctionEntryListRepository.class);
        registry = new AuctionParticipantRegistry();
        MemberRepository memberRepository = mock(MemberRepository.class);
        InventoryService inventoryService = mock(InventoryService.class);

        AuctionRoomService roomService = new AuctionRoomService(registry, memberRepository, inventoryService,
                new ChatFilter(), auctionRepository, entryListRepository);
        auctionService = new AuctionService(auctionRepository, memberRepository, inventoryService,
                roomService, registry, entryListRepository, mock(SimpMessagingTemplate.class));

        seller = mock(Member.class);
        when(seller.getId()).thenReturn(SELLER);
    }

    // ---------- 픽스처 ----------

    //readyStartTime은 현재 시각 기준 offset(분) 이후 (음수면 이미 지난 상태)
    private Auction auction(AuctionType type, AuctionStatus status, long readyStartOffsetMinutes) {
        LocalDateTime readyStartTime = LocalDateTime.now().plusMinutes(readyStartOffsetMinutes);
        Auction auction = Auction.builder()
                .inventory(mock(Inventory.class))
                .auctionOwner(seller)
                .type(type)
                .startPrice(1000L)
                .readyStartTime(readyStartTime)
                .maxParticipants(20)
                .entryDeadline(readyStartTime.plusMinutes(10))
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION);
        if (status == AuctionStatus.OPEN) {
            auction.open(LocalDateTime.now().minusMinutes(1));
        } else {
            ReflectionTestUtils.setField(auction, "status", status);
        }
        when(auctionRepository.findById(AUCTION)).thenReturn(Optional.of(auction));
        return auction;
    }

    private Auction readyPublic() {
        return auction(AuctionType.PUBLIC, AuctionStatus.READY, -1);
    }

    private void enter(long memberId) {
        registry.join(AUCTION, new AuctionParticipantState(memberId, "s" + memberId, "n" + memberId, null, AUCTION));
    }

    //MEMBER(2)와 SELLER(1)를 제외한 회원으로 방을 채운다
    private void fillRoomWithOthers(int count) {
        for (int i = 0; i < count; i++) {
            enter(100L + i);
        }
    }

    private void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    // ---------- 공통 ----------

    @Test
    @DisplayName("존재하지 않는 경매는 AUCTION_NOT_FOUND")
    void notFound() {
        when(auctionRepository.findById(AUCTION)).thenReturn(Optional.empty());

        assertErrorCode(() -> auctionService.joinAuction(MEMBER, AUCTION), ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("CLOSED/FAILED 경매는 ALREADY_ENDED (판매자 검사보다 먼저)")
    void endedAuction() {
        auction(AuctionType.PUBLIC, AuctionStatus.CLOSED, -20);
        assertErrorCode(() -> auctionService.joinAuction(SELLER, AUCTION), ErrorCode.ALREADY_ENDED);

        auction(AuctionType.PUBLIC, AuctionStatus.FAILED, -20);
        assertErrorCode(() -> auctionService.joinAuction(MEMBER, AUCTION), ErrorCode.ALREADY_ENDED);
    }

    @Test
    @DisplayName("판매자 본인은 SELLER_CANNOT_JOIN")
    void sellerCannotJoin() {
        readyPublic();

        assertErrorCode(() -> auctionService.joinAuction(SELLER, AUCTION), ErrorCode.SELLER_CANNOT_JOIN);
    }

    @Test
    @DisplayName("판매자가 탈퇴해 등록자가 없어도 입장 검증은 정상 동작한다")
    void withdrawnSeller() {
        Auction auction = readyPublic();
        ReflectionTestUtils.setField(auction, "auctionOwner", null);

        JoinAuctionResponse response = auctionService.joinAuction(MEMBER, AUCTION);

        assertThat(response.status()).isEqualTo(AuctionStatus.READY);
    }

    // ---------- READY ----------

    @Test
    @DisplayName("READY: 입장 가능하면 방 정보를 돌려준다 (PUBLIC은 현재가, READY라 입장 대기 마감만 있음)")
    void readyPublicOk() {
        Auction auction = readyPublic();
        fillRoomWithOthers(7);

        JoinAuctionResponse response = auctionService.joinAuction(MEMBER, AUCTION);

        assertThat(response.auctionId()).isEqualTo(AUCTION);
        assertThat(response.type()).isEqualTo(AuctionType.PUBLIC);
        assertThat(response.status()).isEqualTo(AuctionStatus.READY);
        assertThat(response.startPrice()).isEqualTo(1000L);
        assertThat(response.currentPrice()).isEqualTo(1000L);
        assertThat(response.currentParticipants()).isEqualTo(7);
        assertThat(response.maxParticipants()).isEqualTo(20);
        assertThat(response.readyStartTime()).isEqualTo(auction.getReadyStartTime());
        assertThat(response.entryDeadline()).isEqualTo(auction.getEntryDeadline());
        assertThat(response.commitDeadline()).isNull();
        assertThat(response.revealDeadline()).isNull();
        verifyNoInteractions(entryListRepository);   // READY는 Redis 명단을 보지 않는다
    }

    @Test
    @DisplayName("READY: PRIVATE은 현재가를 내려주지 않는다")
    void readyPrivateHidesCurrentPrice() {
        auction(AuctionType.PRIVATE, AuctionStatus.READY, -1);

        JoinAuctionResponse response = auctionService.joinAuction(MEMBER, AUCTION);

        assertThat(response.currentPrice()).isNull();
    }

    @Test
    @DisplayName("READY: readyStartTime 이전이면 ENTRY_NOT_STARTED")
    void readyBeforeStartTime() {
        auction(AuctionType.PUBLIC, AuctionStatus.READY, 5);

        assertErrorCode(() -> auctionService.joinAuction(MEMBER, AUCTION), ErrorCode.ENTRY_NOT_STARTED);
    }

    @Test
    @DisplayName("READY: 정원(20명)이 차 있으면 ROOM_FULL")
    void readyRoomFull() {
        readyPublic();
        fillRoomWithOthers(20);

        assertErrorCode(() -> auctionService.joinAuction(MEMBER, AUCTION), ErrorCode.ROOM_FULL);
    }

    @Test
    @DisplayName("READY: 이미 방에 있는 사람은 정원이 차 있어도 통과한다 (멱등)")
    void readyAlreadyInRoomIsIdempotent() {
        readyPublic();
        fillRoomWithOthers(19);
        enter(MEMBER);

        JoinAuctionResponse response = auctionService.joinAuction(MEMBER, AUCTION);

        assertThat(response.currentParticipants()).isEqualTo(20);
    }

    // ---------- OPEN ----------

    @Test
    @DisplayName("OPEN: 참가자 명단에 없으면 ALREADY_STARTED")
    void openNewcomerRejected() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN, -15);

        assertErrorCode(() -> auctionService.joinAuction(MEMBER, AUCTION), ErrorCode.ALREADY_STARTED);
    }

    @Test
    @DisplayName("OPEN: 참가자 명단에 있고 퇴장 명단에 없으면 재접속 허용, 입찰/커밋 마감을 내려준다")
    void openReconnectAllowed() {
        Auction auction = auction(AuctionType.PUBLIC, AuctionStatus.OPEN, -15);
        when(entryListRepository.isInParticipantList(AUCTION, MEMBER)).thenReturn(true);

        JoinAuctionResponse response = auctionService.joinAuction(MEMBER, AUCTION);

        assertThat(response.status()).isEqualTo(AuctionStatus.OPEN);
        assertThat(response.entryDeadline()).isNull();
        assertThat(response.commitDeadline()).isEqualTo(auction.getCommitDeadline());
        assertThat(response.revealDeadline()).isNull();
    }

    @Test
    @DisplayName("OPEN: PUBLIC 퇴장 명단에 있으면 REENTRY_NOT_ALLOWED")
    void openPublicLeftRejected() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN, -15);
        when(entryListRepository.isInParticipantList(AUCTION, MEMBER)).thenReturn(true);
        when(entryListRepository.isInLeftList(AUCTION, MEMBER)).thenReturn(true);

        assertErrorCode(() -> auctionService.joinAuction(MEMBER, AUCTION), ErrorCode.REENTRY_NOT_ALLOWED);
    }

    @Test
    @DisplayName("OPEN: 지금 방에 있으면 명단을 보지 않고 통과한다")
    void openAlreadyInRoom() {
        auction(AuctionType.PUBLIC, AuctionStatus.OPEN, -15);
        enter(MEMBER);

        JoinAuctionResponse response = auctionService.joinAuction(MEMBER, AUCTION);

        assertThat(response.currentParticipants()).isEqualTo(1);
        verifyNoInteractions(entryListRepository);
    }

    @Test
    @DisplayName("OPEN: PRIVATE은 리빌 마감까지 내려준다")
    void openPrivateHasRevealDeadline() {
        Auction auction = auction(AuctionType.PRIVATE, AuctionStatus.OPEN, -15);
        when(entryListRepository.isInParticipantList(AUCTION, MEMBER)).thenReturn(true);

        JoinAuctionResponse response = auctionService.joinAuction(MEMBER, AUCTION);

        assertThat(response.currentPrice()).isNull();
        assertThat(response.commitDeadline()).isEqualTo(auction.getCommitDeadline());
        assertThat(response.revealDeadline()).isEqualTo(auction.getRevealDeadline());
    }

    @Test
    @DisplayName("구독 시점 검증(auctionId로 호출)도 같은 규칙을 쓴다")
    void subscribeValidationSharesRules() {
        AuctionRoomService roomService = new AuctionRoomService(registry, mock(MemberRepository.class),
                mock(InventoryService.class), new ChatFilter(), auctionRepository, entryListRepository);
        readyPublic();
        fillRoomWithOthers(20);

        assertErrorCode(() -> roomService.validateJoinable(AUCTION, MEMBER), ErrorCode.ROOM_FULL);
        assertErrorCode(() -> roomService.validateJoinable(AUCTION, SELLER), ErrorCode.SELLER_CANNOT_JOIN);
    }
}
