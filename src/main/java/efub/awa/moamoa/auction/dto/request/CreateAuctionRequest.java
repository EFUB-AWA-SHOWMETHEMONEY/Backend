package efub.awa.moamoa.auction.dto.request;

import efub.awa.moamoa.auction.enums.AuctionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateAuctionRequest(

        @NotNull(message = "인벤토리 아이디는 필수입니다.")
        Long inventoryId,

        @NotNull(message = "경매 타입은 필수입니다.")
        AuctionType type,

        @NotNull(message = "시작가는 필수입니다.")
        @Min(value = 1, message = "시작가는 1 이상이어야 합니다.")
        Long startPrice,

        //현재 시각 이후인지는 서비스에서 검증 (INVALID_READY_START_TIME)
        @NotNull(message = "경매 입장 시작 시각은 필수입니다.")
        LocalDateTime readyStartTime
) {
}
