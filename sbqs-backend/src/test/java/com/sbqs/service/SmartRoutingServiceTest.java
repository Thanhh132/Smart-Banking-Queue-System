package com.sbqs.service;

import com.sbqs.config.SmartRoutingProperties;
import com.sbqs.dto.BranchCounterLoad;
import com.sbqs.dto.BranchQueueLoad;
import com.sbqs.dto.SmartRoutingRecommendationResponse;
import com.sbqs.entity.Branch;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.TicketRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

class SmartRoutingServiceTest {

    @Test
    void recommendsSlightlyFartherBranchWhenItsQueueIsMuchShorter() {
        BranchRepository branchRepository = mock(BranchRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        CounterRepository counterRepository = mock(CounterRepository.class);
        Branch nearBusy = branch(1L, "Gần nhưng đông", 10.7800, 106.6900);
        Branch fartherQuiet = branch(2L, "Xa hơn nhưng thoáng", 10.8000, 106.7000);
        when(branchRepository.findByBankNameIgnoreCaseAndStatusIgnoreCase("SBQS Bank", "ACTIVE"))
                .thenReturn(List.of(nearBusy, fartherQuiet));
        when(ticketRepository.findWaitingLoadsByBranchIds(List.of(1L, 2L))).thenReturn(List.of(
                new BranchQueueLoad(1L, 12L, 180L),
                new BranchQueueLoad(2L, 2L, 30L)));
        when(counterRepository.findActiveCounterLoadsByBranchIds(List.of(1L, 2L))).thenReturn(List.of(
                new BranchCounterLoad(1L, 2L),
                new BranchCounterLoad(2L, 2L)));

        List<SmartRoutingRecommendationResponse> result = service(
                branchRepository, ticketRepository, counterRepository)
                .recommend("SBQS Bank", 10.7750, 106.6850);

        assertEquals(2L, result.getFirst().branchId());
        assertTrue(result.getFirst().recommended());
        assertEquals(15, result.getFirst().estimatedWaitMinutes());
        assertEquals(90, result.get(1).estimatedWaitMinutes());
    }

    @Test
    void addsPenaltyWhenBranchHasNoActiveCounter() {
        BranchRepository branchRepository = mock(BranchRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        CounterRepository counterRepository = mock(CounterRepository.class);
        Branch branch = branch(1L, "Chưa mở quầy", 10.7800, 106.6900);
        when(branchRepository.findByStatusIgnoreCase("ACTIVE")).thenReturn(List.of(branch));
        when(ticketRepository.findWaitingLoadsByBranchIds(List.of(1L)))
                .thenReturn(List.of(new BranchQueueLoad(1L, 0L, 0L)));
        when(counterRepository.findActiveCounterLoadsByBranchIds(List.of(1L))).thenReturn(List.of());

        SmartRoutingRecommendationResponse result = service(
                branchRepository, ticketRepository, counterRepository)
                .recommend("ALL", 10.7750, 106.6850)
                .getFirst();

        assertEquals(30, result.estimatedWaitMinutes());
        assertEquals(0, result.activeCounters());
        assertFalse(result.recommended());
    }

    @Test
    void rejectsInvalidCoordinates() {
        SmartRoutingService service = service(
                mock(BranchRepository.class), mock(TicketRepository.class), mock(CounterRepository.class));
        assertThrows(RuntimeException.class, () -> service.recommend("ALL", 91, 106.7));
    }

    @Test
    void onlyRanksBranchesThatProvideTheSelectedService() {
        BranchRepository branchRepository = mock(BranchRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        CounterRepository counterRepository = mock(CounterRepository.class);
        QueueMachineServiceMappingRepository mappingRepository = mock(QueueMachineServiceMappingRepository.class);
        Branch unsupported = branch(1L, "Unsupported", 10.7751, 106.6851);
        Branch supported = branch(2L, "Supported", 10.8000, 106.7000);
        when(branchRepository.findByStatusIgnoreCase("ACTIVE")).thenReturn(List.of(unsupported, supported));
        when(mappingRepository.findBranchIdsProvidingServiceCode(List.of(1L, 2L), "CASH_WITHDRAWAL"))
                .thenReturn(List.of(2L));
        when(ticketRepository.findWaitingLoadsByBranchIds(List.of(2L))).thenReturn(List.of());
        when(counterRepository.findActiveCounterLoadsByBranchIds(List.of(2L)))
                .thenReturn(List.of(new BranchCounterLoad(2L, 1L)));

        List<SmartRoutingRecommendationResponse> result = service(
                branchRepository, ticketRepository, counterRepository, mappingRepository)
                .recommend("ALL", 10.7750, 106.6850, "CASH_WITHDRAWAL");

        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().branchId());
    }

    private SmartRoutingService service(
            BranchRepository branchRepository,
            TicketRepository ticketRepository,
            CounterRepository counterRepository) {
        return service(branchRepository, ticketRepository, counterRepository,
                mock(QueueMachineServiceMappingRepository.class));
    }

    private SmartRoutingService service(
            BranchRepository branchRepository,
            TicketRepository ticketRepository,
            CounterRepository counterRepository,
            QueueMachineServiceMappingRepository mappingRepository) {
        SmartRoutingProperties properties = new SmartRoutingProperties();
        BranchOperatingHoursService operatingHoursService = mock(BranchOperatingHoursService.class);
        when(operatingHoursService.isOpenNow(anyLong())).thenReturn(true);
        return new SmartRoutingService(branchRepository, ticketRepository, counterRepository, properties,
                operatingHoursService, mappingRepository);
    }

    private Branch branch(Long id, String name, double latitude, double longitude) {
        Branch branch = new Branch();
        branch.setBranchId(id);
        branch.setBankName("SBQS Bank");
        branch.setBranchCode("CN" + id);
        branch.setBranchName(name);
        branch.setAddress("TP.HCM");
        branch.setStatus("ACTIVE");
        branch.setLatitude(latitude);
        branch.setLongitude(longitude);
        return branch;
    }
}
