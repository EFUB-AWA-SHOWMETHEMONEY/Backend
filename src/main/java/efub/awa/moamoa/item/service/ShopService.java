package efub.awa.moamoa.item.service;

import efub.awa.moamoa.balance.enums.BalanceChangeType;
import efub.awa.moamoa.balance.enums.ReferenceType;
import efub.awa.moamoa.balance.service.BalanceService;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.item.dto.response.BuyItemResponse;
import efub.awa.moamoa.item.dto.response.ShopItemListResponse;
import efub.awa.moamoa.item.dto.response.ItemResponse;
import efub.awa.moamoa.item.entity.Inventory;
import efub.awa.moamoa.item.entity.Item;
import efub.awa.moamoa.item.repository.InventoryRepository;
import efub.awa.moamoa.item.repository.ItemRepository;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final InventoryRepository inventoryRepository;
    private final BalanceService balanceService;

    @Transactional(readOnly = true)
    public ShopItemListResponse getShopItemList() {
        List<Item> items = itemRepository.findAllSaleItems();

        List<ItemResponse> itemResponses = items.stream()
                .map(item -> {
                    //TODO: S3 이미지 URL 수정 필요
                    String s3ImageUrl = item.getImageUrl();
                    return ItemResponse.of(item, s3ImageUrl);
                })
                .toList();
        return ShopItemListResponse.of(itemResponses, itemResponses.size());
    }

    @Transactional(readOnly = true)
    public ShopItemListResponse searchShopItemList(String keyword) {
        List<Item> items = itemRepository.search(keyword);

        List<ItemResponse> itemResponses = items.stream()
                .map(item -> {
                    //TODO: S3 이미지 URL 수정 필요
                    String s3ImageUrl = item.getImageUrl();
                    return ItemResponse.of(item, s3ImageUrl);
                })
                .toList();
        return ShopItemListResponse.of(itemResponses, itemResponses.size());
    }

    @Transactional
    public BuyItemResponse buyShopItem(Long memberId, Long itemId) {
        Item item = getItemById(itemId);

        if (!item.isForSale()) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOR_SALE);
        }

        Long afterBalance = balanceService.withdraw(memberId, item.getPrice(), BalanceChangeType.SHOP_PURCHASE,
                ReferenceType.SHOP_ITEM, item.getId() );

        Member member = getMemberById(memberId);
        Inventory inventory = getInventory(memberId, itemId)
                .map(inv -> {
                    inv.increaseQuantity();
                    return inv;
                })
                .orElseGet(() -> saveNewInventory(member, item));

        return BuyItemResponse.of(memberId, afterBalance, inventory);
    }

    private Inventory saveNewInventory(Member member, Item item) {
        Inventory newInventory = Inventory.builder()
                .member(member)
                .item(item)
                .quantity(1L)
                .isEquipped(false)
                .build();
        return inventoryRepository.save(newInventory);
    }

    private Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원 - memberId: {}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    private Item getItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 아이템 - itemId: {}", itemId);
                    return new CustomException(ErrorCode.ITEM_NOT_FOUND);
                });
    }

    private Optional<Inventory> getInventory(Long memberId, Long itemId) {
        return inventoryRepository.findByMemberIdAndItemId(memberId, itemId);
    }
}
