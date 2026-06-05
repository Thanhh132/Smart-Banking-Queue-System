package com.sbqs.service;

import com.sbqs.entity.Counter;
import com.sbqs.entity.Branch;
import com.sbqs.repository.CounterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CounterService {

    private final CounterRepository counterRepository;

    public CounterService(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    public List<Counter> getAllCounters() {
        return counterRepository.findAll();
    }

    public List<Counter> getCountersByBranch(Branch branch) {
        return counterRepository.findByBranch(branch);
    }

    public Counter createCounter(Counter counter) {
        return counterRepository.save(counter);
    }
}