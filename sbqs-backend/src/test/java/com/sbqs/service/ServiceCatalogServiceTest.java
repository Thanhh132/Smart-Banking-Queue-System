package com.sbqs.service;

import com.sbqs.dto.service.ServiceCatalogRequest;
import com.sbqs.entity.Branch;
import com.sbqs.entity.ServiceCatalog;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.DigitalDelegationRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock private ServiceCatalogRepository catalogRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private QueueMachineServiceMappingRepository mappingRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DigitalDelegationRepository delegationRepository;

    @InjectMocks private ServiceCatalogService service;

    @Test
    void createGeneratesNextShortServiceCodeWhenCodeIsBlank() {
        ServiceCatalog existingCatalog = catalog(1L);
        existingCatalog.setServiceCode("DV012");
        Services existingBranchService = branchService(10L, existingCatalog);
        existingBranchService.setServiceCode("DV009");

        when(catalogRepository.findAllByOrderByServiceNameAsc()).thenReturn(List.of(existingCatalog));
        when(serviceRepository.findAll()).thenReturn(List.of(existingBranchService));
        when(catalogRepository.save(org.mockito.ArgumentMatchers.any(ServiceCatalog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(branchRepository.findAll()).thenReturn(List.of());

        ServiceCatalog created = service.create(new ServiceCatalogRequest(
                "", "Dich vu moi", "BASIC", "Mo ta", 15, false));

        assertEquals("DV013", created.getServiceCode());
    }

    @Test
    void deleteRemovesCatalogButKeepsCompletedTicketHistoryReferences() {
        ServiceCatalog catalog = catalog(1L);
        Services branchService = branchService(10L, catalog);
        Ticket completedTicket = new Ticket();
        completedTicket.setStatus("COMPLETED");
        completedTicket.setService(branchService);

        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));
        when(serviceRepository.findByCatalog(catalog)).thenReturn(List.of(branchService));
        when(ticketRepository.findByService(branchService)).thenReturn(List.of(completedTicket));
        when(appointmentRepository.findByService(branchService)).thenReturn(List.of());
        when(delegationRepository.findByService(branchService)).thenReturn(List.of());
        when(mappingRepository.findByService(branchService)).thenReturn(List.of());

        service.delete(1L);

        assertEquals("DELETED", branchService.getStatus());
        assertNull(branchService.getCatalog());
        verify(serviceRepository).saveAll(List.of(branchService));
        verify(serviceRepository).flush();
        verify(catalogRepository).delete(catalog);
        verify(serviceRepository, never()).deleteAll(List.of(branchService));
    }

    @Test
    void deleteStillRejectsWaitingTickets() {
        ServiceCatalog catalog = catalog(1L);
        Services branchService = branchService(10L, catalog);
        Ticket waitingTicket = new Ticket();
        waitingTicket.setStatus("WAITING");

        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));
        when(serviceRepository.findByCatalog(catalog)).thenReturn(List.of(branchService));
        when(ticketRepository.findByService(branchService)).thenReturn(List.of(waitingTicket));

        assertThrows(RuntimeException.class, () -> service.delete(1L));

        verify(mappingRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
        verify(catalogRepository, never()).delete(catalog);
    }

    @Test
    void synchronizeRestoresArchivedBranchServiceWhenItsCatalogIsActiveAgain() {
        ServiceCatalog catalog = catalog(1L);
        catalog.setEstimatedTime(20);
        Branch branch = new Branch();
        branch.setBranchId(4L);
        Services archived = branchService(48L, catalog);
        archived.setCatalog(null);
        archived.setStatus("DELETED");
        archived.setEstimatedTime(10);

        when(catalogRepository.findAllByOrderByServiceNameAsc()).thenReturn(List.of(catalog));
        when(serviceRepository.findByBranch(branch)).thenReturn(List.of(archived));
        when(serviceRepository.save(archived)).thenReturn(archived);

        service.inheritCatalogForBranch(branch);

        assertEquals("ACTIVE", archived.getStatus());
        assertEquals(20, archived.getEstimatedTime());
        assertEquals(catalog, archived.getCatalog());
        verify(serviceRepository).save(archived);
    }

    private ServiceCatalog catalog(Long id) {
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setCatalogId(id);
        catalog.setServiceCode("TEST_SERVICE");
        catalog.setServiceName("Dich vu thu nghiem");
        return catalog;
    }

    private Services branchService(Long id, ServiceCatalog catalog) {
        Branch branch = new Branch();
        branch.setBranchId(2L);
        Services service = new Services();
        service.setServiceId(id);
        service.setServiceCode(catalog.getServiceCode());
        service.setServiceName(catalog.getServiceName());
        service.setCatalog(catalog);
        service.setBranch(branch);
        service.setStatus("ACTIVE");
        return service;
    }
}
