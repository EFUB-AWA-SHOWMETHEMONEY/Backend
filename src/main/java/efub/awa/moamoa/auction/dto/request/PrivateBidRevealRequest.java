package efub.awa.moamoa.auction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PrivateBidRevealRequest(
        @NotNull @Positive Long amount,
        @NotBlank @Size(min = 16, max = 64) String salt
) {
}
