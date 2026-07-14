package com.sbqs.service;

import com.sbqs.config.SmartRoutingProperties;
import com.sbqs.dto.BranchCounterLoad;
import com.sbqs.dto.BranchQueueLoad;
import com.sbqs.dto.SmartRoutingRecommendationResponse;
import com.sbqs.entity.Branch;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SmartRoutingService {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final BranchRepository branchRepository;
    private final TicketRepository ticketRepository;
    private final CounterRepository counterRepository;
    private final SmartRoutingProperties properties;
    private final BranchOperatingHoursService operatingHoursService;
    private final com.sbqs.repository.QueueMachineServiceMappingRepository mappingRepository;

    public SmartRoutingService(
            BranchRepository branchRepository,
            TicketRepository ticketRepository,
            CounterRepository counterRepository,
            SmartRoutingProperties properties,
            BranchOperatingHoursService operatingHoursService,
            com.sbqs.repository.QueueMachineServiceMappingRepository mappingRepository) {
        this.branchRepository = branchRepository;
        this.ticketRepository = ticketRepository;
        this.counterRepository = counterRepository;
        this.properties = properties;
        this.operatingHoursService = operatingHoursService;
        this.mappingRepository = mappingRepository;
    }

    /**
     * Xếp hạng chi nhánh đang hoạt động bằng khoảng cách GPS và thời gian xử lý hàng đợi ước tính.
     * Dữ liệu tải được truy vấn trực tiếp để kết quả không phụ thuộc cache của màn hình monitor.
     */
    @Transactional(readOnly = true)
    public List<SmartRoutingRecommendationResponse> recommend(
            String bankName,
            double customerLatitude,
            double customerLongitude) {
        return recommend(bankName, customerLatitude, customerLongitude, null);
    }

    public List<SmartRoutingRecommendationResponse> recommend(
            String bankName, double customerLatitude, double customerLongitude, String serviceCode) {
        validateCoordinates(customerLatitude, customerLongitude);

        List<Branch> branches = isAllBanks(bankName)
                ? branchRepository.findByStatusIgnoreCase("ACTIVE")
                : branchRepository.findByBankNameIgnoreCaseAndStatusIgnoreCase(bankName.trim(), "ACTIVE");
        branches = branches.stream()
                .filter(branch -> branch.getLatitude() != null && branch.getLongitude() != null)
                .filter(branch -> operatingHoursService.isOpenNow(branch.getBranchId()))
                .toList();
        if (branches.isEmpty()) {
            return List.of();
        }

        if (serviceCode != null && !serviceCode.isBlank()) {
            List<Long> eligibleIds = mappingRepository.findBranchIdsProvidingServiceCode(
                    branches.stream().map(Branch::getBranchId).toList(), serviceCode.trim());
            branches = branches.stream().filter(branch -> eligibleIds.contains(branch.getBranchId())).toList();
            if (branches.isEmpty()) return List.of();
        }

        List<Long> branchIds = branches.stream().map(Branch::getBranchId).toList();
        Map<Long, BranchQueueLoad> queueLoads = ticketRepository.findWaitingLoadsByBranchIds(branchIds)
                .stream()
                .collect(Collectors.toMap(BranchQueueLoad::branchId, Function.identity()));
        Map<Long, Long> activeCounters = counterRepository.findActiveCounterLoadsByBranchIds(branchIds)
                .stream()
                .collect(Collectors.toMap(BranchCounterLoad::branchId, BranchCounterLoad::activeCounters));

        List<Candidate> candidates = branches.stream()
                .map(branch -> candidate(
                        branch,
                        queueLoads.get(branch.getBranchId()),
                        activeCounters.getOrDefault(branch.getBranchId(), 0L),
                        customerLatitude,
                        customerLongitude))
                .toList();

        double maxDistance = candidates.stream().mapToDouble(Candidate::distanceKm).max().orElse(0);
        long maxWait = candidates.stream().mapToLong(Candidate::estimatedWaitMinutes).max().orElse(0);
        double[] weights = normalizedWeights();

        List<ScoredCandidate> ranked = candidates.stream()
                .map(candidate -> score(candidate, maxDistance, maxWait, weights[0], weights[1]))
                .sorted(Comparator.comparing(
                                (ScoredCandidate value) -> value.candidate().activeCounters() == 0)
                        .thenComparingDouble(ScoredCandidate::routingScore)
                        .thenComparingDouble(value -> value.candidate().distanceKm())
                        .thenComparing(value -> value.candidate().branch().getBranchId()))
                .toList();

        LocalDateTime calculatedAt = LocalDateTime.now();
        return java.util.stream.IntStream.range(0, ranked.size())
                .mapToObj(index -> response(ranked.get(index), index + 1, calculatedAt))
                .toList();
    }

    private Candidate candidate(
            Branch branch,
            BranchQueueLoad queueLoad,
            long activeCounters,
            double customerLatitude,
            double customerLongitude) {
        long waitingTickets = queueLoad == null ? 0 : queueLoad.waitingTickets();
        long workMinutes = queueLoad == null ? 0 : queueLoad.estimatedWorkMinutes();
        long estimatedWaitMinutes;
        if (activeCounters > 0) {
            estimatedWaitMinutes = (long) Math.ceil((double) workMinutes / activeCounters);
        } else {
            estimatedWaitMinutes = workMinutes + Math.max(0, properties.getNoActiveCounterPenaltyMinutes());
        }

        return new Candidate(
                branch,
                distanceKm(customerLatitude, customerLongitude, branch.getLatitude(), branch.getLongitude()),
                waitingTickets,
                activeCounters,
                estimatedWaitMinutes);
    }

    private ScoredCandidate score(
            Candidate candidate,
            double maxDistance,
            long maxWait,
            double distanceWeight,
            double waitWeight) {
        double distanceScore = maxDistance == 0 ? 0 : candidate.distanceKm() / maxDistance;
        double waitScore = maxWait == 0 ? 0 : (double) candidate.estimatedWaitMinutes() / maxWait;
        double routingScore = distanceWeight * distanceScore + waitWeight * waitScore;
        return new ScoredCandidate(candidate, distanceScore, waitScore, routingScore);
    }

    private SmartRoutingRecommendationResponse response(
            ScoredCandidate value,
            int rank,
            LocalDateTime calculatedAt) {
        Candidate candidate = value.candidate();
        Branch branch = candidate.branch();
        boolean recommended = rank == 1 && candidate.activeCounters() > 0;
        String explanation = candidate.activeCounters() == 0
                ? "Chưa có quầy hoạt động nên điểm chờ được cộng thêm mức dự phòng."
                : recommended
                        ? "Cân bằng tốt nhất giữa quãng đường và thời gian chờ hiện tại."
                        : "Phương án thay thế dựa trên khoảng cách và tải hàng đợi hiện tại.";

        return new SmartRoutingRecommendationResponse(
                rank,
                recommended,
                branch.getBranchId(),
                branch.getBankName(),
                branch.getBranchCode(),
                branch.getBranchName(),
                branch.getAddress(),
                branch.getProvince(),
                branch.getDistrict(),
                branch.getWard(),
                branch.getPhone(),
                branch.getLatitude(),
                branch.getLongitude(),
                round(candidate.distanceKm(), 2),
                candidate.waitingTickets(),
                candidate.activeCounters(),
                candidate.estimatedWaitMinutes(),
                round(value.distanceScore(), 4),
                round(value.waitScore(), 4),
                round(value.routingScore(), 4),
                explanation,
                calculatedAt);
    }

    private double[] normalizedWeights() {
        double distanceWeight = Math.max(0, properties.getDistanceWeight());
        double waitWeight = Math.max(0, properties.getWaitWeight());
        double total = distanceWeight + waitWeight;
        if (total == 0) {
            return new double[] {0.4, 0.6};
        }
        return new double[] {distanceWeight / total, waitWeight / total};
    }

    private boolean isAllBanks(String bankName) {
        return bankName == null || bankName.isBlank() || "ALL".equalsIgnoreCase(bankName);
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new RuntimeException("Tọa độ khách hàng không hợp lệ");
        }
    }

    private double distanceKm(double fromLat, double fromLng, double toLat, double toLng) {
        double latitudeDistance = Math.toRadians(toLat - fromLat);
        double longitudeDistance = Math.toRadians(toLng - fromLng);
        double value = Math.pow(Math.sin(latitudeDistance / 2), 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat))
                * Math.pow(Math.sin(longitudeDistance / 2), 2);
        double clamped = Math.max(0, Math.min(1, value));
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(clamped), Math.sqrt(1 - clamped));
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private record Candidate(
            Branch branch,
            double distanceKm,
            long waitingTickets,
            long activeCounters,
            long estimatedWaitMinutes) {
    }

    private record ScoredCandidate(
            Candidate candidate,
            double distanceScore,
            double waitScore,
            double routingScore) {
    }
}
