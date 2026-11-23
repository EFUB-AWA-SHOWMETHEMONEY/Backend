package efub.awa.moamoa.auction.controller;

import efub.awa.moamoa.auction.dto.request.PublicBidRequest;
import efub.awa.moamoa.auction.dto.response.HighestBidResponse;
import efub.awa.moamoa.auction.dto.response.PublicBidResponse;
import efub.awa.moamoa.auction.service.PublicBidService;
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
@RequestMapping("/api/auctions/{auctionId}")
@RequiredArgsConstructor
public class PublicBidController {

    private final PublicBidService publicBidService;

    @PostMapping("/public/bids")
    public ResponseEntity<ApiResponse<PublicBidResponse>> placeBid(@AuthenticationPrincipal Long memberId,
                                                                   @PathVariable("auctionId") Long auctionId,
                                                                   @Valid @RequestBody PublicBidRequest request) {
        log.info("PUBLIC 입찰 제출 API - memberId: {}, auctionId: {}, amount: {}", memberId, auctionId, request.amount());
        PublicBidResponse response = publicBidService.placeBid(memberId, auctionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("입찰 제출에 성공했습니다.", response));
    }

    @GetMapping("/public/bids/highest")
    public ResponseEntity<ApiResponse<HighestBidResponse>> getHighestBid(@AuthenticationPrincipal Long memberId,
                                                                         @PathVariable("auctionId") Long auctionId) {
        log.info("PUBLIC 최고 입찰 조회 API - memberId: {}, auctionId: {}", memberId, auctionId);
        HighestBidResponse response = publicBidService.getHighestBid(memberId, auctionId);
        return ResponseEntity.ok(ApiResponse.success("최고 입찰 내역 조회 요청이 성공했습니다.", response));
    }
}
