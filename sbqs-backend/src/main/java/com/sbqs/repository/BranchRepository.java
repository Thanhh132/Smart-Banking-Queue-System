package com.sbqs.repository;

import com.sbqs.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    // Lấy tất cả chi nhánh theo tên ngân hàng
    List<Branch> findByBankName(String bankName);

    // Tìm chi nhánh gần nhất theo Haversine formula
    @Query("SELECT b FROM Branch b WHERE b.bankName = :bankName ORDER BY " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(b.latitude)) * " +
            "cos(radians(b.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(b.latitude)))) ASC")
    List<Branch> findNearestBranches(double lat, double lng, String bankName);
}