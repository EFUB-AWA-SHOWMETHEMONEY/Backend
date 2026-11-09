package efub.awa.moamoa.websocket.plaza.listener;

import efub.awa.moamoa.websocket.plaza.dto.response.PlazaEvent;
import efub.awa.moamoa.websocket.plaza.entity.EquippedItem;
import efub.awa.moamoa.websocket.plaza.enums.PlazaConstants;
import efub.awa.moamoa.websocket.plaza.repository.PlazaParticipantRegistry;
import efub.awa.moamoa.item.event.ItemEquipmentChangeEvent;
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
    public void onItemEquipped(ItemEquipmentChangeEvent e) {
        //TODO s3 이미지 url로 교체 필요
        EquippedItem equippedItem = e.itemId() == null ? null : new EquippedItem(e.itemId(), e.itemImageUrl());
        registry.updateEquippedItem(e.memberId(), equippedItem)
                .ifPresent(state -> messagingTemplate.convertAndSend(
                        PlazaConstants.TOPIC, PlazaEvent.EquipmentUpdate.from(state)
                ));
    }
}
