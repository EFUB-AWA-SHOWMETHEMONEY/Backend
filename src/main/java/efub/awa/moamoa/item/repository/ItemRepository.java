package efub.awa.moamoa.item.repository;

import efub.awa.moamoa.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("""
            select i from Item i
            where i.forSale = true
            order by i.id
            """)
    List<Item> findAllSaleItems();

    @Query("""
        select i from Item i
        where i.forSale = true
                and i.name like concat('%' , :keyword , '%')
        order by i.id
        """)
    List<Item> search(@Param("keyword") String keyword);
}
