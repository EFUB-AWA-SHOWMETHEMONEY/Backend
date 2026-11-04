package efub.awa.moamoa.global.websocket.plaza.entity;

public record ParticipantState (
        Long memberId,
        String sessionId,
        String nickname,
        EquippedItem equippedItem,
        int x,
        int y,
        long seq
)
{
    public ParticipantState withPosition(int x, int y, long seq) {
        return new ParticipantState(memberId, sessionId, nickname, equippedItem, x, y, seq);
    }

    public ParticipantState withEquippedItem(EquippedItem item) {
        return new ParticipantState(memberId, sessionId, nickname, item, x, y, seq);
    }
}
