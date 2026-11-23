package efub.awa.moamoa.auction.repository;

import efub.awa.moamoa.auction.entity.PublicBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

//[변경 내용] 빈 인터페이스였던 것을 JpaRepository 상속으로 바꾸고, 경매의 마지막 입찰 시각 조회를 추가했다.
//[변경 이유] 경매 상태 전환에서 PUBLIC 경매의 "마지막 입찰 후 30초 무입찰 종료"를 판단하려면 마지막 입찰 시각이 필요하다.
public interface PublicBidRepository extends JpaRepository<PublicBid, Long> {

    //경매의 마지막 입찰 시각 (입찰이 없으면 빈 값)
    @Query("select max(b.createdAt) from PublicBid b where b.auction.id = :auctionId")
    Optional<LocalDateTime> findLastBidTime(@Param("auctionId") Long auctionId);

    //경매의 입찰 횟수 (최고 입찰 조회)
    long countByAuctionId(Long auctionId);
}
