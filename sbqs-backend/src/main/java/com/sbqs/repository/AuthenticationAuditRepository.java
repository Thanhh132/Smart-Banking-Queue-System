package com.sbqs.repository;

import com.sbqs.entity.AuthenticationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthenticationAuditRepository extends JpaRepository<AuthenticationAudit, Long> {
}
