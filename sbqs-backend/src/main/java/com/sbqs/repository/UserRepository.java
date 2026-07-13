package com.sbqs.repository;

import java.util.List;
import com.sbqs.entity.Branch;
import com.sbqs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

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
