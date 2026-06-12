package com.sbqs.service;

import com.sbqs.entity.QueueMachine;
import com.sbqs.repository.QueueMachineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueueMachineService {

    private final QueueMachineRepository queueMachineRepository;

    public QueueMachineService(
            QueueMachineRepository queueMachineRepository) {

        this.queueMachineRepository = queueMachineRepository;
    }

    public List<QueueMachine> getAllQueueMachines() {
        return queueMachineRepository.findAll();
    }

    public QueueMachine createQueueMachine(
            QueueMachine queueMachine) {

        return queueMachineRepository.save(queueMachine);
    }

    public QueueMachine updateQueueMachine(
            Long queueMachineId,
            QueueMachine updatedQueueMachine) {

        QueueMachine existingQueueMachine = queueMachineRepository.findById(queueMachineId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy máy bốc số"));

        existingQueueMachine.setMachineCode(
                updatedQueueMachine.getMachineCode());

        existingQueueMachine.setMachineName(
                updatedQueueMachine.getMachineName());

        existingQueueMachine.setLocationNote(
                updatedQueueMachine.getLocationNote());

        existingQueueMachine.setInstructionNote(
                updatedQueueMachine.getInstructionNote());

        existingQueueMachine.setStatus(
                updatedQueueMachine.getStatus());

        existingQueueMachine.setBranch(
                updatedQueueMachine.getBranch());

        return queueMachineRepository.save(existingQueueMachine);
    }

    public void deleteQueueMachine(Long queueMachineId) {

        QueueMachine existingQueueMachine = queueMachineRepository.findById(queueMachineId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy máy bốc số"));

        existingQueueMachine.setStatus("INACTIVE");

        queueMachineRepository.save(existingQueueMachine);
    }
}