package efub.awa.moamoa.auction.controller;

import efub.awa.moamoa.auction.dto.request.CreateAuctionRequest;
import efub.awa.moamoa.auction.dto.response.AuctionListResponse;
import efub.awa.moamoa.auction.dto.response.CreateAuctionResponse;
import efub.awa.moamoa.auction.dto.response.JoinAuctionResponse;
import efub.awa.moamoa.auction.service.AuctionService;
import efub.awa.moamoa.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateAuctionResponse>> createAuction(@AuthenticationPrincipal Long memberId,
                                                                            @Valid @RequestBody CreateAuctionRequest request) {
        log.info("경매 생성 API - memberId: {}, inventoryId: {}, type: {}", memberId, request.inventoryId(), request.type());

        CreateAuctionResponse response = auctionService.createAuction(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("경매 생성 요청이 성공했습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AuctionListResponse>> getAuctionList(@AuthenticationPrincipal Long memberId) {
        log.info("경매 목록 조회 API - memberId: {}", memberId);

        AuctionListResponse response = auctionService.getAuctionList();
        return ResponseEntity.ok(ApiResponse.success("경매 목록 조회 요청이 성공했습니다.", response));
    }

    @PostMapping("/{auctionId}/join")
    public ResponseEntity<ApiResponse<JoinAuctionResponse>> joinAuction(@AuthenticationPrincipal Long memberId,
                                                                        @PathVariable("auctionId") Long auctionId) {
        log.info("경매 입장 API - memberId: {}, auctionId: {}", memberId, auctionId);
        JoinAuctionResponse response = auctionService.joinAuction(memberId, auctionId);
        return ResponseEntity.ok(ApiResponse.success("경매 입장 요청이 성공했습니다.", response));
    }
}
