package com.sbqs.repository;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    long countByStatus(String status);

    Ticket findTopByOrderByTicketIdDesc();

    Ticket findFirstByStatusOrderByTicketNumberAsc(String status);

    List<Ticket> findByBranch(Branch branch);

    List<Ticket> findByService(Services service);

    List<Ticket> findByStatus(String status);

    Ticket findByTicketNumber(Integer ticketNumber);

}