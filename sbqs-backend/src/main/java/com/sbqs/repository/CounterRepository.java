package com.sbqs.repository;

import com.sbqs.entity.Counter;
import com.sbqs.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CounterRepository extends JpaRepository<Counter, Long> {

    List<Counter> findByBranch(Branch branch);
}