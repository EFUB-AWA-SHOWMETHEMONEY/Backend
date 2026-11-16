package efub.awa.moamoa.auction.controller;

import efub.awa.moamoa.auction.dto.request.PrivateBidCommitRequest;
import efub.awa.moamoa.auction.dto.response.PrivateBidCommitResponse;
import efub.awa.moamoa.auction.service.PrivateBidService;
import efub.awa.moamoa.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class PrivateBidController {

    private final PrivateBidService privateBidService;

    @PostMapping("/{auctionId}/private/bids")
    public ResponseEntity<ApiResponse<PrivateBidCommitResponse>> privateBidCommit(@AuthenticationPrincipal Long memberId,
                                                                                  @PathVariable("auctionId") Long auctionId,
                                                                                  @Valid @RequestBody PrivateBidCommitRequest request) {

        log.info("Private 경매 입찰 커밋 제출: memberId-{}, auctionId-{}", memberId, auctionId);
        PrivateBidCommitResponse response = privateBidService.commit(memberId, auctionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("입찰 커밋 제출에 성공했습니다.", response));
    }
}
