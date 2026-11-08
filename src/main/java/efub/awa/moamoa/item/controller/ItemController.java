package efub.awa.moamoa.item.controller;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.item.dto.response.ItemDetailResponse;
import efub.awa.moamoa.item.service.ItemService;
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
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemDetailResponse>> getItem(@AuthenticationPrincipal Long memberId, @PathVariable Long itemId) {
        log.info("아이템 조회 API 호출 - itemId:{}", itemId);

        ItemDetailResponse response = itemService.getItem(memberId, itemId);
        return ResponseEntity.ok(ApiResponse.success("아이템 상세조회 요청이 성공했습니다", response));
    }

}
