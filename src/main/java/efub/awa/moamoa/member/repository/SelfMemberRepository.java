package efub.awa.moamoa.member.repository;

import efub.awa.moamoa.member.entity.SelfMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SelfMemberRepository extends JpaRepository<SelfMember, Long> {

    boolean existsByLoginId(String loginId);

    @EntityGraph(attributePaths = "member")
    Optional<SelfMember> findByLoginId(String loginId);
}
