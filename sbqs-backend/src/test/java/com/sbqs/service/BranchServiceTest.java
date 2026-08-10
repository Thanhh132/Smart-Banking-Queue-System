package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.entity.DigitalDelegation;
import com.sbqs.entity.Services;
import com.sbqs.entity.User;
import com.sbqs.event.DomainEventPublisher;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.DigitalDelegationRepository;
import com.sbqs.repository.QueueMachineRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import com.sbqs.repository.UserRepository;
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
class BranchServiceTest {

    @Mock private BranchRepository branchRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private CounterRepository counterRepository;
    @Mock private DigitalDelegationRepository delegationRepository;
    @Mock private QueueMachineRepository queueMachineRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private QueueMachineServiceMappingRepository mappingRepository;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private ServiceCatalogService serviceCatalogService;

    @InjectMocks private BranchService branchService;

    @Test
    void deleteBranchDetachesSoftDeletedUsersBeforePhysicalDeletion() {
        Branch branch = branch(1L);
        User deletedAdmin = new User();
        deletedAdmin.setStatus("DELETED");
        deletedAdmin.setBranch(branch);
        Services service = new Services();
        service.setServiceName("Dich vu cu");
        DigitalDelegation delegation = new DigitalDelegation();
        delegation.setStatus("USED");
        delegation.setBranch(branch);
        delegation.setService(service);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findByBranch(branch)).thenReturn(List.of(deletedAdmin));
        when(delegationRepository.findByBranch(branch)).thenReturn(List.of(delegation));
        when(appointmentRepository.findByBranch(branch)).thenReturn(List.of());
        when(mappingRepository.findAllRelatedToBranch(1L)).thenReturn(List.of());
        when(counterRepository.findByBranch(branch)).thenReturn(List.of());
        when(ticketRepository.findByBranch(branch)).thenReturn(List.of());
        when(serviceRepository.findByBranch(branch)).thenReturn(List.of());
        when(queueMachineRepository.findByBranch(branch)).thenReturn(List.of());

        branchService.deleteBranch(1L);

        assertNull(deletedAdmin.getBranch());
        assertNull(delegation.getBranch());
        assertNull(delegation.getService());
        assertEquals("Chi nhanh thu nghiem", delegation.getBranchNameSnapshot());
        assertEquals("Dich vu cu", delegation.getServiceNameSnapshot());
        verify(userRepository).saveAll(List.of(deletedAdmin));
        verify(delegationRepository).saveAll(List.of(delegation));
        verify(userRepository).flush();
        verify(branchRepository).delete(branch);
    }

    @Test
    void deleteBranchFinalizesInactiveUsersBeforePhysicalDeletion() {
        Branch branch = branch(1L);
        User inactiveAdmin = new User();
        inactiveAdmin.setStatus("INACTIVE");
        inactiveAdmin.setBranch(branch);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findByBranch(branch)).thenReturn(List.of(inactiveAdmin));
        when(appointmentRepository.findByBranch(branch)).thenReturn(List.of());
        when(mappingRepository.findAllRelatedToBranch(1L)).thenReturn(List.of());
        when(counterRepository.findByBranch(branch)).thenReturn(List.of());
        when(ticketRepository.findByBranch(branch)).thenReturn(List.of());
        when(serviceRepository.findByBranch(branch)).thenReturn(List.of());
        when(queueMachineRepository.findByBranch(branch)).thenReturn(List.of());

        branchService.deleteBranch(1L);

        assertEquals("DELETED", inactiveAdmin.getStatus());
        assertNull(inactiveAdmin.getBranch());
        verify(branchRepository).delete(branch);
    }

    @Test
    void deleteBranchStillRejectsActiveUsers() {
        Branch branch = branch(1L);
        User activeAdmin = new User();
        activeAdmin.setStatus("ACTIVE");
        activeAdmin.setBranch(branch);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findByBranch(branch)).thenReturn(List.of(activeAdmin));

        assertThrows(RuntimeException.class, () -> branchService.deleteBranch(1L));

        verify(userRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(branchRepository, never()).delete(branch);
    }

    private Branch branch(Long id) {
        Branch branch = new Branch();
        branch.setBranchId(id);
        branch.setBranchCode("SBQS-001");
        branch.setBranchName("Chi nhanh thu nghiem");
        return branch;
    }
}
