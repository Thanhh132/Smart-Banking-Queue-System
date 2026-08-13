package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.entity.User;
import com.sbqs.event.DomainEventPublisher;
import com.sbqs.mapper.ServiceDtoMapper;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceCatalogRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicesServiceTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private ServiceCatalogRepository catalogRepository;
    @Mock private QueueMachineServiceMappingRepository mappingRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private ServiceDtoMapper serviceDtoMapper;
    @Mock private CurrentUserService currentUserService;
    @Mock private ServiceCatalogService serviceCatalogService;
    @Mock private DomainEventPublisher eventPublisher;

    @InjectMocks private ServicesService service;

    @Test
    void getAllServicesRepairsMissingCatalogInheritanceBeforeReturningBranchServices() {
        Branch branch = new Branch();
        branch.setBranchId(4L);
        User branchAdmin = new User();
        branchAdmin.setBranch(branch);

        Services first = branchService(48L, "DV001", branch);
        Services second = branchService(50L, "DV002", branch);
        Services third = branchService(52L, "DV003", branch);
        when(currentUserService.requireUser()).thenReturn(branchAdmin);
        when(serviceRepository.findByBranchAndStatusNotIgnoreCase(branch, "DELETED"))
                .thenReturn(List.of(first, second, third));

        List<Services> result = service.getAllServices();

        verify(serviceCatalogService).inheritCatalogForBranch(branch);
        assertEquals(List.of("DV001", "DV002", "DV003"),
                result.stream().map(Services::getServiceCode).toList());
    }

    private Services branchService(Long id, String code, Branch branch) {
        Services service = new Services();
        service.setServiceId(id);
        service.setServiceCode(code);
        service.setBranch(branch);
        return service;
    }
}
