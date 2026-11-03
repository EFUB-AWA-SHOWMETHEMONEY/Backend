package efub.awa.moamoa.member.repository;

import efub.awa.moamoa.member.entity.KakaoMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KakaoMemberRepository extends JpaRepository<KakaoMember, String> {

    boolean existsByOauthId(String oauthId);

    @EntityGraph(attributePaths = "member")
    Optional<KakaoMember> findByOauthId(String oauthId);
}
