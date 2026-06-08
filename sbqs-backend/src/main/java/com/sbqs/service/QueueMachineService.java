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
}