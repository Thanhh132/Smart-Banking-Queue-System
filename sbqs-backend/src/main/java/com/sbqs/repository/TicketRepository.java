package com.sbqs.repository;

import com.sbqs.entity.Branch;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.dto.BranchQueueLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    long countByStatus(String status);

    long countByBranchBranchIdAndStatus(
            Long branchId,
            String status);

    long countByQueueMachineQueueMachineIdAndStatus(
            Long queueMachineId,
            String status);

    long countByCustomerUserIdAndCreatedAtGreaterThanEqual(Long customerId, LocalDateTime createdAfter);

    Optional<Ticket> findFirstByCustomerUserIdAndStatusAndCancelledAtIsNotNullOrderByCancelledAtDesc(
            Long customerId,
            String status);

    @Query("""
            select new com.sbqs.dto.BranchQueueLoad(
                t.branch.branchId,
                count(t),
                coalesce(sum(coalesce(s.estimatedTime, 15)), 0))
            from Ticket t
            left join t.service s
            where t.branch.branchId in :branchIds
              and t.status = 'WAITING'
            group by t.branch.branchId
            """)
    List<BranchQueueLoad> findWaitingLoadsByBranchIds(@Param("branchIds") List<Long> branchIds);

    Ticket findTopByOrderByTicketIdDesc();

    Ticket findFirstByStatusOrderByTicketNumberAsc(String status);

    Ticket findTopByQueueMachineOrderByTicketNumberDesc(QueueMachine queueMachine);

    Ticket findTopByQueueMachineAndCreatedAtBetweenOrderByTicketNumberDesc(
            QueueMachine queueMachine,
            LocalDateTime start,
            LocalDateTime end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Ticket> findFirstByQueueMachineAndStatusOrderByTicketNumberAsc(
            QueueMachine queueMachine,
            String status);

    List<Ticket> findByQueueMachineAndStatusOrderByTicketNumberAsc(
            QueueMachine queueMachine,
            String status,
            Pageable pageable);

    long countByQueueMachineAndStatusAndTicketNumberLessThan(
            QueueMachine queueMachine,
            String status,
            Integer ticketNumber);

    @Query("""
            select count(t)
            from Ticket t
            where t.queueMachine = :queueMachine
              and t.status = 'WAITING'
              and t.ticketNumber < :ticketNumber
              and t.businessDate = :businessDate
            """)
    long countWaitingAhead(
            @Param("queueMachine") QueueMachine queueMachine,
            @Param("ticketNumber") Integer ticketNumber,
            @Param("businessDate") LocalDate businessDate);

    List<Ticket> findByBranch(Branch branch);

    List<Ticket> findByService(Services service);

    List<Ticket> findByQueueMachine(QueueMachine queueMachine);

    List<Ticket> findByStatus(String status);

    List<Ticket> findByCustomerUserIdAndStatusIn(Long customerId, List<String> statuses);

    Optional<Ticket> findByCustomerUserIdAndIdempotencyKey(Long customerId, String idempotencyKey);

    Optional<Ticket> findFirstByCustomerUserIdAndStatusInOrderByCreatedAtDesc(
            Long customerId,
            List<String> statuses);

    Ticket findByTicketNumber(Integer ticketNumber);

   

}
