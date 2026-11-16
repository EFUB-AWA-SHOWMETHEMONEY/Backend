package efub.awa.moamoa.item.entity;


import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.member.entity.Member;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "inventory",
uniqueConstraints =  {
        @UniqueConstraint(columnNames = {"member_id", "item_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    //회원 탈퇴 시 인벤토리 행은 유지하고 소유 회원만 null로 변경 (다른 회원의 거래·경매 기록에 아이템 정보가 남도록 함)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "is_equipped", nullable = false)
    private boolean isEquipped;

    @Builder
    private Inventory(Member member, Item item, Long quantity, boolean isEquipped) {
        this.member = member;
        this.item = item;
        this.quantity = quantity;
        this.isEquipped = isEquipped;
    }

    public boolean toggleEquipped() {
        this.isEquipped = !this.isEquipped;
        return this.isEquipped;
    }

    public void increaseQuantity() { this.quantity += 1;}
}
