package efub.awa.moamoa.auction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PrivateBidCommitRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9a-f]{64}$")
        String commitHash
) {
}
