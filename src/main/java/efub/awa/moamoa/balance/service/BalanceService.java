package efub.awa.moamoa.balance.service;

import efub.awa.moamoa.balance.entity.BalanceHistory;
import efub.awa.moamoa.balance.enums.BalanceChangeType;
import efub.awa.moamoa.balance.enums.ReferenceType;
import efub.awa.moamoa.balance.dto.TransferResult;
import efub.awa.moamoa.balance.enums.TransferType;
import efub.awa.moamoa.balance.repository.BalanceHistoryRepository;
import efub.awa.moamoa.global.exception.CustomException;
import efub.awa.moamoa.global.exception.ErrorCode;
import efub.awa.moamoa.member.entity.Member;
import efub.awa.moamoa.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final EntityManager entityManager;
    private final MemberRepository memberRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    @Transactional
    public Long withdraw(Long memberId, Long amount, BalanceChangeType type,
                         ReferenceType refType, Long refId) {
        Member member = getMember(memberId);
        Long balanceAfter = member.withdraw(amount);
        saveHistory(member, -amount, balanceAfter, type, refType, refId);

        return member.getCapital();
    }

    @Transactional
    public Long deposit(Long memberId, Long amount, BalanceChangeType type,
                        ReferenceType refType, Long refId) {
        Member member = getMember(memberId);
        Long balanceAfter = member.deposit(amount);
        saveHistory(member, amount, balanceAfter, type, refType, refId);

        return member.getCapital();
    }

    @Transactional
    public TransferResult transfer(Long fromId, Long toId, Long amount, TransferType transferType, Long refId) {
        if (fromId.equals(toId)) {
            throw new CustomException(ErrorCode.INVALID_TRANSFER_TARGET);
        }

        //데드락 방지: id 오름차순으로 락 획득
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Member first = getMember(firstId);
        Member second = getMember(secondId);

        Member from = fromId.equals(firstId) ? first : second;
        Long fromBalanceAfter = from.withdraw(amount);
        Member to = toId.equals(firstId) ? first : second;
        Long toBalanceAfter = to.deposit(amount);

        saveHistory(from, -amount, fromBalanceAfter, transferType.getSendType(), transferType.getReferenceType(), refId );
        saveHistory(to, amount, toBalanceAfter, transferType.getReceiveType(), transferType.getReferenceType(), refId );

        return new TransferResult(fromBalanceAfter, toBalanceAfter);
    }

    private void saveHistory(Member member, Long signedAmount, Long balanceAfter,
                             BalanceChangeType type, ReferenceType refType, Long refId) {

        BalanceHistory balanceHistory = BalanceHistory.builder()
                .member(member)
                .amount(signedAmount)
                .balanceAfter(balanceAfter)
                .type(type)
                .refType(refType)
                .refId(refId)
                .build();
        balanceHistoryRepository.save(balanceHistory);
    }

    private Member getMember(Long memberId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원입니다: memberId-{}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
        entityManager.refresh(member, LockModeType.PESSIMISTIC_WRITE);
        return member;
    }
}
