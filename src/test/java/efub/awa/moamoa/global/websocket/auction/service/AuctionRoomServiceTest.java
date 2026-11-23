package efub.awa.moamoa.global.websocket.auction.service;

import efub.awa.moamoa.auction.entity.Auction;
import efub.awa.moamoa.auction.enums.AuctionStatus;
import efub.awa.moamoa.auction.enums.AuctionType;
import efub.awa.moamoa.auction.repository.AuctionEntryListRepository;
import efub.awa.moamoa.auction.repository.AuctionRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.service.InventoryService;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import efub.awa.moamoa.websocket.auction.dto.response.AuctionEvent;
import efub.awa.moamoa.websocket.auction.entity.AuctionParticipantState;
import efub.awa.moamoa.websocket.auction.repository.AuctionParticipantRegistry;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import efub.awa.moamoa.websocket.plaza.dto.request.ChatRequestDto;
import efub.awa.moamoa.websocket.plaza.support.ChatFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuctionRoomServiceTest {

    private static final long AUCTION = 10L;
    private static final long MEMBER = 1L;

    private AuctionParticipantRegistry registry;
    private MemberRepository memberRepository;
    private InventoryService inventoryService;
    private AuctionRepository auctionRepository;
    private AuctionRoomService service;

    @BeforeEach
    void setUp() {
        registry = new AuctionParticipantRegistry();
        memberRepository = mock(MemberRepository.class);
        inventoryService = mock(InventoryService.class);
        auctionRepository = mock(AuctionRepository.class);
        //[변경 내용] AuctionRoomService에 입장 명단 저장소(AuctionEntryListRepository)가 추가되어 생성자 인자를 추가했다.
        //[변경 이유] 경매 입장 API 개발로 validateJoinable이 OPEN 이후 참가자/퇴장 명단을 확인하게 되었다. (mock은 명단에 없음으로 응답)
        service = new AuctionRoomService(registry, memberRepository, inventoryService,
                new ChatFilter(), auctionRepository, mock(AuctionEntryListRepository.class));

        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn("테스터");
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
        when(inventoryService.findEquippedItems(anyLong())).thenReturn(List.of());
    }

    private Auction auctionWith(AuctionStatus status) {
        Auction auction = Auction.builder()
                .type(AuctionType.PUBLIC)
                .startPrice(1000L)
                .readyStartTime(LocalDateTime.now())
                //[변경 내용] 정원(maxParticipants)을 추가했다.
                //[변경 이유] validateJoinable이 READY에서 정원 초과를 검사하게 되어 정원 값이 필요하다. 기존 테스트의 기대 결과는 그대로다.
                .maxParticipants(20)
                .build();
        ReflectionTestUtils.setField(auction, "status", status);   // 생성 시 READY 고정이라 리플렉션으로 변경
        when(auctionRepository.findById(AUCTION)).thenReturn(Optional.of(auction));
        return auction;
    }

    private void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    // ---------- join ----------

    @Test
    @DisplayName("입장하면 새로 만든 상태를 반환하고 registry에 등록된다 (null 아님)")
    void joinReturnsNewState() {
        AuctionParticipantState state = service.join(AUCTION, MEMBER, "s1");

        assertThat(state).isNotNull();
        assertThat(state.memberId()).isEqualTo(MEMBER);
        assertThat(state.nickname()).isEqualTo("테스터");
        assertThat(state.sessionId()).isEqualTo("s1");
        assertThat(state.auctionId()).isEqualTo(AUCTION);
        assertThat(registry.find(AUCTION, MEMBER)).isEqualTo(state);
    }

    @Test
    @DisplayName("같은 회원이 새 세션으로 다시 입장하면 새 세션이 반환·등록된다")
    void rejoinReturnsNewSession() {
        service.join(AUCTION, MEMBER, "s1");

        AuctionParticipantState state = service.join(AUCTION, MEMBER, "s2");

        assertThat(state.sessionId()).isEqualTo("s2");
        assertThat(registry.find(AUCTION, MEMBER).sessionId()).isEqualTo("s2");
    }

    @Test
    @DisplayName("존재하지 않는 회원은 MEMBER_NOT_FOUND")
    void joinMemberNotFound() {
        when(memberRepository.findById(MEMBER)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.join(AUCTION, MEMBER, "s1"), ErrorCode.MEMBER_NOT_FOUND);
        assertThat(registry.find(AUCTION, MEMBER)).isNull();
    }

    // ---------- chat ----------

    @Test
    @DisplayName("방에 없으면 NOT_IN_AUCTION_ROOM (NPE 아님)")
    void chatNotInRoom() {
        assertErrorCode(() -> service.chat(AUCTION, MEMBER, "s1", new ChatRequestDto("안녕")),
                ErrorCode.NOT_IN_AUCTION_ROOM);
    }

    @Test
    @DisplayName("다른 세션(이전 탭)의 채팅은 거부된다")
    void chatFromStaleSessionRejected() {
        service.join(AUCTION, MEMBER, "s2");   // 새 탭으로 재접속

        assertErrorCode(() -> service.chat(AUCTION, MEMBER, "s1", new ChatRequestDto("안녕")),
                ErrorCode.NOT_IN_AUCTION_ROOM);
    }

    @Test
    @DisplayName("다른 경매방에서는 채팅할 수 없다")
    void chatInOtherAuctionRejected() {
        service.join(AUCTION, MEMBER, "s1");

        assertErrorCode(() -> service.chat(99L, MEMBER, "s1", new ChatRequestDto("안녕")),
                ErrorCode.NOT_IN_AUCTION_ROOM);
    }

    @Test
    @DisplayName("채팅은 앞뒤 공백을 제거하고 금지어를 마스킹한다")
    void chatTrimsAndMasks() {
        service.join(AUCTION, MEMBER, "s1");

        AuctionEvent.AuctionChat event = service.chat(AUCTION, MEMBER, "s1",
                new ChatRequestDto("  fuck 하지마  "));

        assertThat(event.type()).isEqualTo("auction_chat");
        assertThat(event.memberId()).isEqualTo(MEMBER);
        assertThat(event.nickname()).isEqualTo("테스터");
        assertThat(event.text()).isEqualTo("**** 하지마");
    }

    // ---------- validateJoinable ----------

    @Test
    @DisplayName("존재하지 않는 경매는 AUCTION_NOT_FOUND")
    void validateAuctionNotFound() {
        when(auctionRepository.findById(AUCTION)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.validateJoinable(AUCTION, MEMBER), ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("READY 경매는 입장 가능")
    void validateReadyOk() {
        auctionWith(AuctionStatus.READY);

        assertThatCode(() -> service.validateJoinable(AUCTION, MEMBER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("종료(CLOSED)·유찰(FAILED) 경매는 ALREADY_ENDED")
    void validateEnded() {
        auctionWith(AuctionStatus.CLOSED);
        assertErrorCode(() -> service.validateJoinable(AUCTION, MEMBER), ErrorCode.ALREADY_ENDED);

        auctionWith(AuctionStatus.FAILED);
        assertErrorCode(() -> service.validateJoinable(AUCTION, MEMBER), ErrorCode.ALREADY_ENDED);
    }

    @Test
    @DisplayName("OPEN 경매에 방에 없던 사람은 ALREADY_STARTED")
    void validateOpenNewcomerRejected() {
        auctionWith(AuctionStatus.OPEN);

        assertErrorCode(() -> service.validateJoinable(AUCTION, MEMBER), ErrorCode.ALREADY_STARTED);
    }

    @Test
    @DisplayName("OPEN 경매에 이미 방에 있는 사람(다른 탭 재구독)은 통과")
    void validateOpenExistingMemberOk() {
        auctionWith(AuctionStatus.OPEN);
        service.join(AUCTION, MEMBER, "s1");

        assertThatCode(() -> service.validateJoinable(AUCTION, MEMBER)).doesNotThrowAnyException();
    }
}
