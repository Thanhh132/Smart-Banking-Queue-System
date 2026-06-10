package com.sbqs.repository;

import com.sbqs.entity.History;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryRepository
        extends JpaRepository<History, Long> {
}