package com.sbqs.repository;

import com.sbqs.entity.Branch;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Query(value = "select pg_advisory_xact_lock(:queueMachineId)", nativeQuery = true)
    Object lockQueueMachineTicketSequence(@Param("queueMachineId") Long queueMachineId);

    @Query("""
            select coalesce(max(t.ticketNumber), 0)
            from Ticket t
            where t.queueMachine.queueMachineId = :queueMachineId
            and t.createdAt >= :start
            and t.createdAt < :end
            """)
    Integer findMaxTicketNumberByQueueMachineAndCreatedAtBetween(
            @Param("queueMachineId") Long queueMachineId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    Ticket findFirstByQueueMachineAndStatusOrderByTicketNumberAsc(
            QueueMachine queueMachine,
            String status);

    List<Ticket> findByBranch(Branch branch);

    List<Ticket> findByService(Services service);

    List<Ticket> findByStatus(String status);

    List<Ticket> findByCustomerEmailAndStatusIn(
            String customerEmail,
            List<String> statuses);

    Optional<Ticket> findFirstByCustomerEmailAndStatusInOrderByCreatedAtDesc(
            String customerEmail,
            List<String> statuses);

    Ticket findByTicketNumber(Integer ticketNumber);

   

}
