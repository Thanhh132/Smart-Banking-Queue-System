package com.sbqs.repository;

import com.sbqs.entity.TransactionDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionDraftRepository extends JpaRepository<TransactionDraft, Long> {
    Optional<TransactionDraft> findByTicketTicketId(Long ticketId);
}
