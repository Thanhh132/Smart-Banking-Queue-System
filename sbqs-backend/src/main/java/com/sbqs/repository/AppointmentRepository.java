package com.sbqs.repository;

import com.sbqs.entity.Appointment;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByBranch(Branch branch);

    List<Appointment> findByService(Services service);

    List<Appointment> findByAppointmentDate(LocalDate appointmentDate);

    List<Appointment> findByStatus(String status);
}
