package efub.awa.moamoa.trade.controller;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.trade.dto.request.TradeCreateRequest;
import efub.awa.moamoa.trade.dto.response.TradeCreateResponse;
import efub.awa.moamoa.trade.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradeCreateResponse>> createTrade(@AuthenticationPrincipal Long memberId,
                                                                        @Valid @RequestBody TradeCreateRequest request){

        log.info("거래 생성 요청 - memberId: {}", memberId);
        TradeCreateResponse response = tradeService.createTrade(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("거래 요청 생성에 성공했습니다.", response));
    }
}
