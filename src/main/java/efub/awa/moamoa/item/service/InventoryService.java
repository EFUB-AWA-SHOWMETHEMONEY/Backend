package efub.awa.moamoa.item.service;

import efub.awa.moamoa.item.event.ItemEquippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ApplicationEventPublisher applicationEventPublisher;

    //TODO: 추후 아이템 장착 구현
    @Transactional
    public void equip(Long memberId, Long inventoryId) {

//        applicationEventPublisher.publishEvent(new ItemEquippedEvent(memberId, item.getId(), item.getImageUrl()));
    }
}
