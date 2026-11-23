package efub.awa.moamoa.auction.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublicBidRequest(
        @NotNull(message = "입찰가는 필수입니다.")
        @Min(value = 1, message = "입찰가는 1 이상이어야 합니다.")
        Long amount
) {
}
