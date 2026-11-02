package efub.awa.moamoa.global.websocket.plaza.dto.response;

import efub.awa.moamoa.global.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.global.websocket.plaza.entity.ParticipantState;

public sealed interface PlazaEvent {

    String type();
    long ts();

    //join: user_join
    record UserJoined(String type, Long memberId, String nickname, EquippedItem equippedItem,
                      int x, int y, long ts) implements PlazaEvent {
        public static UserJoined from (ParticipantState state) {
            return new UserJoined(
                    "user_joined", state.memberId(), state.nickname(), state.equippedItem(),
                    state.x(), state.y(), System.currentTimeMillis()
            );
        }
    }

    //leave: user_left
    record UserLeft(String type, Long memberId, long ts) implements PlazaEvent {

        public static UserLeft of (Long memberId) {
            return new UserLeft(
                    "user_left", memberId, System.currentTimeMillis()
            );
        }
    }

    //위치 업데이트: position_update
    record PositionUpdate(String type, Long memberId, int x, int y, long seq, long ts) implements PlazaEvent {
        public static PositionUpdate from (ParticipantState state) {
            return new PositionUpdate(
                    "position_update", state.memberId(), state.x(), state.y(), state.seq(), System.currentTimeMillis()
            );
        }
    }

    record EquipmentUpdate(String type, Long memberId, EquippedItem equippedItem, long ts) implements PlazaEvent {
        public static EquipmentUpdate from (ParticipantState state) {
            return new EquipmentUpdate(
                    "equipment_update", state.memberId(), state.equippedItem(), System.currentTimeMillis()
            );
        }
    }
}
