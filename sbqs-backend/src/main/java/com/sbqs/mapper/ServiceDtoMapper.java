package com.sbqs.mapper;

import com.sbqs.dto.service.ServiceRequest;
import com.sbqs.dto.service.ServiceResponse;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ServiceDtoMapper {

    public Services toEntity(ServiceRequest request, Branch branch) {
        Services service = new Services();
        service.setBranch(branch);
        apply(request, service);
        return service;
    }

    public void apply(ServiceRequest request, Services service) {
        service.setServiceCode(request.serviceCode());
        service.setServiceName(request.serviceName());
        service.setServiceType(request.serviceType());
        service.setDescription(request.description());
        service.setEstimatedTime(request.estimatedTime());
        service.setStatus(request.status());
        service.setRequiredCustomerFields(copy(request.requiredCustomerFields()));
        service.setFormSchema(copy(request.formSchema()));
    }

    public ServiceResponse toResponse(Services service) {
        return new ServiceResponse(
                service.getServiceId(),
                service.getServiceCode(),
                service.getServiceName(),
                service.getServiceType(),
                service.getDescription(),
                service.getEstimatedTime(),
                service.getStatus(),
                copy(service.getRequiredCustomerFields()),
                copy(service.getFormSchema()),
                new ServiceResponse.BranchReference(service.getBranch().getBranchId()));
    }

    private <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
