package com.sbqs.repository;

import java.util.List;
import com.sbqs.entity.Branch;
import com.sbqs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findByIdForTicketIssuing(@Param("userId") Long userId);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByBranch(Branch branch);

    List<User> findByBranchAndStatusNotIgnoreCase(Branch branch, String status);

    List<User> findByStatusNotIgnoreCase(String status);

    List<User> findByRole(String role);

    List<User> findByRoleAndStatusNotIgnoreCase(String role, String status);

    List<User> findByBranchAndRole(Branch branch, String role);

    List<User> findByBranchAndRoleAndStatusNotIgnoreCase(Branch branch, String role, String status);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailAndUserIdNot(String email, Long userId);

    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Long userId);

    boolean existsByPhoneAndUserIdNot(String phone, Long userId);
}
