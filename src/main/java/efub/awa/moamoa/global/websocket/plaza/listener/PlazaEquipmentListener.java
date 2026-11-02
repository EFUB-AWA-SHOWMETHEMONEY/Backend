package efub.awa.moamoa.global.websocket.plaza.listener;

import efub.awa.moamoa.global.websocket.plaza.dto.response.PlazaEvent;
import efub.awa.moamoa.global.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.global.websocket.plaza.enums.PlazaConstants;
import efub.awa.moamoa.global.websocket.plaza.repository.PlazaParticipantRegistry;
import efub.awa.moamoa.item.event.ItemEquippedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PlazaEquipmentListener {

    private final PlazaParticipantRegistry registry;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItemEquipped(ItemEquippedEvent e) {
        registry.updateEquippedItem(e.memberId(), new EquippedItem(e.itemId(), e.itemImageUrl()))
                .ifPresent(state -> messagingTemplate.convertAndSend(
                        PlazaConstants.TOPIC, PlazaEvent.EquipmentUpdate.from(state)
                ));
    }
}
