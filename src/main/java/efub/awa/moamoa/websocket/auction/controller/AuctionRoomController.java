package efub.awa.moamoa.websocket.auction.controller;

import efub.awa.moamoa.websocket.auction.dto.response.AuctionSnapshotResponse;
import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.websocket.auction.service.AuctionRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionRoomController {

    private final AuctionRoomService auctionRoomService;

    @GetMapping("/{auctionId}/participants")
    public ResponseEntity<ApiResponse<AuctionSnapshotResponse>> getSnapshot(@AuthenticationPrincipal Long memberId, @PathVariable("auctionId") Long auctionId) {

        log.info("경매 스냅샷 조회 API 요청 - memberId: {}, auctionId: {}", memberId, auctionId);

        AuctionSnapshotResponse response = auctionRoomService.getSnapshot(memberId, auctionId);
        return ResponseEntity.ok(ApiResponse.success("경매방 참여자 조회 요청이 성공했습니다.", response));
    }
}
