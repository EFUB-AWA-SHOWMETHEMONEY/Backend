package efub.awa.moamoa.item.entity;


import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "is_equipped", nullable = false)
    private boolean isEquipped;

}
