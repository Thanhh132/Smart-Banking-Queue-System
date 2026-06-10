package com.sbqs.service;

import com.sbqs.entity.History;
import com.sbqs.repository.HistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoryService {

    private final HistoryRepository historyRepository;

    public HistoryService(
            HistoryRepository historyRepository) {

        this.historyRepository = historyRepository;
    }

    public List<History> getAllHistory() {
        return historyRepository.findAll();
    }
}