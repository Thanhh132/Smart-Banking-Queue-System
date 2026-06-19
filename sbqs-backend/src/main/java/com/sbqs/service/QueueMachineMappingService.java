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

        public QueueMachineMappingService(
                        QueueMachineServiceMappingRepository mappingRepository,
                        QueueMachineRepository queueMachineRepository,
                        ServiceRepository serviceRepository) {

                this.mappingRepository = mappingRepository;
                this.queueMachineRepository = queueMachineRepository;
                this.serviceRepository = serviceRepository;
        }

        public List<QueueMachineServiceMapping> getAllMappings() {
                return mappingRepository.findAll();
        }

        public QueueMachineServiceMapping createMapping(
                        MappingRequest request) {

                QueueMachine queueMachine = queueMachineRepository.findById(
                                request.getQueueMachineId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy máy bốc số"));

                Services service = serviceRepository.findById(
                                request.getServiceId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy dịch vụ"));

                QueueMachineServiceMapping mapping = new QueueMachineServiceMapping();

                mapping.setId(
                                new QueueMachineServiceMappingId(
                                                queueMachine.getQueueMachineId(),
                                                service.getServiceId()));

                mapping.setQueueMachine(queueMachine);
                mapping.setService(service);
                if (mappingRepository.existsById(mapping.getId())) {

                        throw new RuntimeException(
                                        "Mapping đã tồn tại");
                }
                return mappingRepository.save(mapping);
        }

        public void deleteMapping(
                        MappingRequest request) {

                QueueMachineServiceMappingId id = new QueueMachineServiceMappingId(
                                request.getQueueMachineId(),
                                request.getServiceId());

                mappingRepository.deleteById(id);
        }
}