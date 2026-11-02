package efub.awa.moamoa.websocket.plaza.dto.response;

import java.util.List;

public record PlazaSnapshotResponse(
        long snapshotAt,
        int participantCount,
        List<ParticipantResponse> participants
) {
    //participantCount는 목록에서 파생 -> 둘이 어긋날 여지를 없앰
    public static PlazaSnapshotResponse of(long snapshotAt, List<ParticipantResponse> participants) {
        return new PlazaSnapshotResponse(snapshotAt, participants.size(), participants);
    }
}
