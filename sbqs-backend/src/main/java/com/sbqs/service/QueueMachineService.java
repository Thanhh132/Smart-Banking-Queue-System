package com.sbqs.service;

import com.sbqs.entity.QueueMachine;
import com.sbqs.repository.QueueMachineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueueMachineService {

    private final QueueMachineRepository queueMachineRepository;
    private final CurrentUserService currentUserService;

    public QueueMachineService(
            QueueMachineRepository queueMachineRepository,
            CurrentUserService currentUserService) {
        this.queueMachineRepository = queueMachineRepository;
        this.currentUserService = currentUserService;
    }

    public List<QueueMachine> getAllQueueMachines() {
        return queueMachineRepository.findByBranch(currentUserService.requireUser().getBranch());
    }

    public QueueMachine createQueueMachine(QueueMachine queueMachine) {
        currentUserService.requireBranch(queueMachine.getBranch().getBranchId());
        queueMachine.setBranch(currentUserService.requireUser().getBranch());
        if (queueMachineRepository.existsByBranchAndMachineCode(
                queueMachine.getBranch(),
                queueMachine.getMachineCode())) {
            throw new RuntimeException("Ma may boc so da ton tai trong chi nhanh nay");
        }

        return queueMachineRepository.save(queueMachine);
    }

    public QueueMachine updateQueueMachine(
            Long queueMachineId,
            QueueMachine updatedQueueMachine) {

        QueueMachine existingQueueMachine = queueMachineRepository.findById(queueMachineId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));

        currentUserService.requireBranch(existingQueueMachine.getBranch().getBranchId());
        currentUserService.requireBranch(updatedQueueMachine.getBranch().getBranchId());

        if (queueMachineRepository.existsByBranchAndMachineCodeAndQueueMachineIdNot(
                updatedQueueMachine.getBranch(),
                updatedQueueMachine.getMachineCode(),
                queueMachineId)) {
            throw new RuntimeException("Ma may boc so da ton tai trong chi nhanh nay");
        }

        existingQueueMachine.setMachineCode(updatedQueueMachine.getMachineCode());
        existingQueueMachine.setMachineName(updatedQueueMachine.getMachineName());
        existingQueueMachine.setLocationNote(updatedQueueMachine.getLocationNote());
        existingQueueMachine.setInstructionNote(updatedQueueMachine.getInstructionNote());
        existingQueueMachine.setStatus(updatedQueueMachine.getStatus());
        existingQueueMachine.setBranch(updatedQueueMachine.getBranch());

        return queueMachineRepository.save(existingQueueMachine);
    }

    public void deleteQueueMachine(Long queueMachineId) {
        QueueMachine existingQueueMachine = queueMachineRepository.findById(queueMachineId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));

        currentUserService.requireBranch(existingQueueMachine.getBranch().getBranchId());

        queueMachineRepository.delete(existingQueueMachine);
    }
}
