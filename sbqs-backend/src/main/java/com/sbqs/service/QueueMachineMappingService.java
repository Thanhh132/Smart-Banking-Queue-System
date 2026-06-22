package com.sbqs.service;

import com.sbqs.dto.MappingRequest;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.QueueMachineServiceMappingId;
import com.sbqs.entity.Services;
import com.sbqs.repository.QueueMachineRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueueMachineMappingService {
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final QueueMachineRepository queueMachineRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;

    public QueueMachineMappingService(
            QueueMachineServiceMappingRepository mappingRepository,
            QueueMachineRepository queueMachineRepository,
            ServiceRepository serviceRepository,
            CurrentUserService currentUserService) {

        this.mappingRepository = mappingRepository;
        this.queueMachineRepository = queueMachineRepository;
        this.serviceRepository = serviceRepository;
        this.currentUserService = currentUserService;
    }

    public List<QueueMachineServiceMapping> getAllMappings() {
        return mappingRepository.findByQueueMachineBranchBranchId(
                currentUserService.requireBranchId());
    }

    public QueueMachineServiceMapping createMapping(MappingRequest request) {
        QueueMachine queueMachine = queueMachineRepository.findById(request.getQueueMachineId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));
        Services service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));

        currentUserService.requireBranch(queueMachine.getBranch().getBranchId());
        currentUserService.requireBranch(service.getBranch().getBranchId());

        if (!queueMachine.getBranch().getBranchId().equals(service.getBranch().getBranchId())) {
            throw new RuntimeException("May boc so va dich vu phai thuoc cung chi nhanh");
        }

        QueueMachineServiceMappingId id = new QueueMachineServiceMappingId(
                queueMachine.getQueueMachineId(),
                service.getServiceId());
        if (mappingRepository.existsById(id)) {
            throw new RuntimeException("Mapping da ton tai");
        }

        QueueMachineServiceMapping mapping = new QueueMachineServiceMapping();
        mapping.setId(id);
        mapping.setQueueMachine(queueMachine);
        mapping.setService(service);
        return mappingRepository.save(mapping);
    }

    public void deleteMapping(MappingRequest request) {
        QueueMachine queueMachine = queueMachineRepository.findById(request.getQueueMachineId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));
        currentUserService.requireBranch(queueMachine.getBranch().getBranchId());

        mappingRepository.deleteById(new QueueMachineServiceMappingId(
                request.getQueueMachineId(),
                request.getServiceId()));
    }
}
