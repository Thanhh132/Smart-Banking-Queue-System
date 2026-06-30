package com.sbqs.repository;

import com.sbqs.entity.Branch;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
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

    @Query("""
            select count(t)
            from Ticket t
            where t.queueMachine = :queueMachine
              and t.status = 'WAITING'
              and t.ticketNumber < :ticketNumber
              and t.createdAt >= :startOfDay
              and t.createdAt < :startOfNextDay
            """)
    long countWaitingAhead(
            @Param("queueMachine") QueueMachine queueMachine,
            @Param("ticketNumber") Integer ticketNumber,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay);

    List<Ticket> findByBranch(Branch branch);

    List<Ticket> findByService(Services service);

    List<Ticket> findByQueueMachine(QueueMachine queueMachine);

    List<Ticket> findByStatus(String status);

    List<Ticket> findByCustomerEmailAndStatusIn(
            String customerEmail,
            List<String> statuses);

    Optional<Ticket> findFirstByCustomerEmailAndStatusInOrderByCreatedAtDesc(
            String customerEmail,
            List<String> statuses);

    Ticket findByTicketNumber(Integer ticketNumber);

   

}
