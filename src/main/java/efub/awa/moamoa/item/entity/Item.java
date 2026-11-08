package efub.awa.moamoa.item.entity;

import efub.awa.moamoa.global.common.entity.BaseEntity;
import efub.awa.moamoa.item.enums.ItemGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Table(name = "item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false)
    private ItemGrade grade;

    @Column(name = "for_sale", nullable = false)
    @ColumnDefault("true")
    private boolean forSale;

    @Column(name = "image_url", nullable = true)
    private String imageUrl;

    @Builder
    private Item(String name, Long price, String description, ItemGrade grade, boolean forSale, String imageUrl) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.grade = grade;
        this.forSale = forSale;
        this.imageUrl = imageUrl;
    }
}
