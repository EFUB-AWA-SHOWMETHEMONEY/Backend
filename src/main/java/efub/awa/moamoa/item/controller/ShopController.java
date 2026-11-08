package efub.awa.moamoa.item.controller;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.item.dto.response.BuyItemResponse;
import efub.awa.moamoa.item.dto.response.ShopItemListResponse;
import efub.awa.moamoa.item.service.ShopService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop/items")
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShopItemListResponse>> getShopItemList(@AuthenticationPrincipal Long memberId) {
        log.info("상점 아이템 목록 조회 API - memberId: {}", memberId);

        ShopItemListResponse response = shopService.getShopItemList();
        return ResponseEntity.ok(ApiResponse.success("상점 판매 아이템 목록 조회 요청이 성공했습니다.", response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ShopItemListResponse>> searchShopItemList(@AuthenticationPrincipal Long memberId,
                                                                                @RequestParam("keyword") @NotBlank String keyword) {
        log.info("상점 아이템 검색 API - memberId: {}, keyword: {}", memberId, keyword);

        ShopItemListResponse response = shopService.searchShopItemList(keyword);
        return ResponseEntity.ok(ApiResponse.success("상점 아이템 검색 요청이 성공했습니다.", response));
    }

    @PostMapping("/{itemId}/purchase")
    public ResponseEntity<ApiResponse<BuyItemResponse>> buyShopItem(@AuthenticationPrincipal Long memberId,
                                                                    @PathVariable Long itemId) {
        log.info("상점 아이템 구매 API - memberId: {}, itemId: {}", memberId, itemId);

        BuyItemResponse response = shopService.buyShopItem(memberId, itemId);
        return ResponseEntity.ok(ApiResponse.success("상점 아이템 구매에 성공했습니다.", response));
    }
}
