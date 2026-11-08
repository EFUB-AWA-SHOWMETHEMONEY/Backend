package efub.awa.moamoa.item.controller;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.item.dto.response.InventoryListResponse;
import efub.awa.moamoa.item.dto.response.InventoryResponse;
import efub.awa.moamoa.item.service.InventoryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<InventoryListResponse>> getInventoryList(@AuthenticationPrincipal Long memberId) {
        log.info("인벤토리 목록 조회 API - memberId: {}", memberId);

        InventoryListResponse response = inventoryService.getInventoryList(memberId);
        return ResponseEntity.ok(ApiResponse.success("인벤토리 조회 요청이 성공했습니다.", response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<InventoryListResponse>> searchInventory(@AuthenticationPrincipal Long memberId,
                                                                              @RequestParam("keyword") @NotBlank String keyword) {
        log.info("인벤토리 검색 API - memberId: {}, keyword: {}", memberId, keyword);

        InventoryListResponse response = inventoryService.searchInventory(memberId, keyword);
        return ResponseEntity.ok(ApiResponse.success("인벤토리 검색 요청이 성공했습니다.", response));
    }

    @PatchMapping("/{inventoryId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> equipInventoryItem(@AuthenticationPrincipal Long memberId, @PathVariable Long inventoryId) {
        log.info ("인벤토리 장착/장착 해제 API - memberId: {}, inventoryId: {}", memberId, inventoryId);

        InventoryResponse response = inventoryService.toggleEquipped(memberId, inventoryId);
        return ResponseEntity.ok(ApiResponse.success("아이템 장착/장착 해제 요청이 성공했습니다.", response));
    }

}
