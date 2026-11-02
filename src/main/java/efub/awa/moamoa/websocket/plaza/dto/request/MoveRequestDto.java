package efub.awa.moamoa.websocket.plaza.dto.request;

import jakarta.validation.constraints.NotNull;

public record MoveRequestDto(
        @NotNull
        Integer x,
        @NotNull
        Integer y,
        @NotNull
        Long seq
) {}
