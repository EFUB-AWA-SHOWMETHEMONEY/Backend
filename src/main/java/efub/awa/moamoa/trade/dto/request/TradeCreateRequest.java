package efub.awa.moamoa.trade.dto.request;

import jakarta.validation.constraints.NotNull;

public record TradeCreateRequest(
        @NotNull
        Long counterpartyId
) {
}
