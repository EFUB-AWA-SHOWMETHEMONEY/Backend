package efub.awa.moamoa.global.websocket.plaza.dto;

import java.util.List;

public record PlazaSnapshotResponse(
        long snapshotAt,
        List<ParticipantResponse> participants
) {}
