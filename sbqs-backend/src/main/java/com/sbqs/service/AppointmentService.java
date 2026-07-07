package com.sbqs.service;

import com.sbqs.entity.Appointment;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.ServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;
    private final BranchRepository branchRepository;
    private final ServiceRepository serviceRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            CurrentUserService currentUserService,
            BranchRepository branchRepository,
            ServiceRepository serviceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.currentUserService = currentUserService;
        this.branchRepository = branchRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findByBranch(currentUserService.requireUser().getBranch());
    }

    /** Tạo lịch hẹn sau khi xác nhận dịch vụ thực sự thuộc chi nhánh khách hàng đã chọn. */
    public Appointment createAppointment(Appointment appointment) {
        if (appointment.getBranch() == null || appointment.getService() == null) {
            throw new RuntimeException("Chua chon chi nhanh hoac dich vu");
        }

        Branch branch = branchRepository.findById(appointment.getBranch().getBranchId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));
        Services service = serviceRepository.findById(appointment.getService().getServiceId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));

        if (service.getBranch() == null
                || !branch.getBranchId().equals(service.getBranch().getBranchId())) {
            throw new RuntimeException("Dich vu khong thuoc chi nhanh da chon");
        }

        appointment.setBranch(branch);
        appointment.setService(service);
        return appointmentRepository.save(appointment);
    }
}
