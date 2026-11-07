package efub.awa.moamoa.websocket.plaza.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequestDto(
        @NotBlank
        @Size(max=100)
        String text
) {
}
