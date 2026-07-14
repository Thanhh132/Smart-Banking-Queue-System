package com.sbqs.service;

import com.sbqs.dto.BranchHoursRequest;
import com.sbqs.dto.BranchHoursResponse;
import com.sbqs.dto.BranchOpenStatusResponse;
import com.sbqs.entity.Branch;
import com.sbqs.entity.BranchOperatingHours;
import com.sbqs.repository.BranchOperatingHoursRepository;
import com.sbqs.repository.BranchRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BranchOperatingHoursService {
    private final BranchOperatingHoursRepository repository;
    private final BranchRepository branchRepository;
    private final CurrentUserService currentUserService;

    public BranchOperatingHoursService(BranchOperatingHoursRepository repository,
                                       BranchRepository branchRepository,
                                       CurrentUserService currentUserService) {
        this.repository = repository;
        this.branchRepository = branchRepository;
        this.currentUserService = currentUserService;
    }

    public List<BranchHoursResponse> getSchedule(Long branchId) {
        requireBranchExists(branchId);
        Map<Integer, BranchOperatingHours> saved = repository.findByBranchBranchIdOrderByDayOfWeek(branchId)
                .stream().collect(Collectors.toMap(BranchOperatingHours::getDayOfWeek, Function.identity()));
        List<BranchHoursResponse> result = new ArrayList<>();
        for (int day = 1; day <= 7; day++) result.add(toResponse(saved.get(day), day));
        return result;
    }

    @Transactional
    public List<BranchHoursResponse> updateCurrentBranch(List<BranchHoursRequest> requests) {
        Long branchId = currentUserService.requireBranchId();
        if (requests == null || requests.size() != 7
                || requests.stream().map(BranchHoursRequest::dayOfWeek).distinct().count() != 7) {
            throw new RuntimeException("Lịch làm việc phải có đủ 7 ngày và không được trùng ngày");
        }
        Branch branch = requireBranchExists(branchId);
        for (BranchHoursRequest request : requests) {
            validate(request);
            BranchOperatingHours hours = repository.findByBranchBranchIdAndDayOfWeek(branchId, request.dayOfWeek())
                    .orElseGet(BranchOperatingHours::new);
            hours.setBranch(branch);
            hours.setDayOfWeek(request.dayOfWeek());
            hours.setClosed(request.closed());
            hours.setMorningOpen(request.closed() ? null : request.morningOpen());
            hours.setMorningClose(request.closed() ? null : request.morningClose());
            hours.setAfternoonOpen(request.closed() ? null : request.afternoonOpen());
            hours.setAfternoonClose(request.closed() ? null : request.afternoonClose());
            repository.save(hours);
        }
        return getSchedule(branchId);
    }

    public boolean isOpenNow(Long branchId) { return isOpen(branchId, LocalDateTime.now()); }

    public boolean isOpen(Long branchId, LocalDateTime dateTime) {
        int day = dateTime.getDayOfWeek().getValue();
        BranchHoursResponse hours = repository.findByBranchBranchIdAndDayOfWeek(branchId, day)
                .map(value -> toResponse(value, day)).orElseGet(() -> defaultFor(day));
        if (hours.closed()) return false;
        LocalTime now = dateTime.toLocalTime();
        return within(now, hours.morningOpen(), hours.morningClose())
                || within(now, hours.afternoonOpen(), hours.afternoonClose());
    }

    public BranchOpenStatusResponse getStatus(Long branchId) {
        boolean open = isOpenNow(branchId);
        return new BranchOpenStatusResponse(branchId, open,
                open ? "Chi nhánh đang trong giờ phục vụ" : "Chi nhánh hiện ngoài giờ phục vụ", LocalDateTime.now());
    }

    public void requireOpen(Long branchId) {
        if (!isOpenNow(branchId)) {
            throw new RuntimeException("Chi nhánh hiện ngoài giờ phục vụ. Vui lòng quay lại trong khung giờ làm việc");
        }
    }

    private Branch requireBranchExists(Long branchId) {
        return branchRepository.findById(branchId).orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
    }

    private void validate(BranchHoursRequest value) {
        if (value.closed()) return;
        boolean morningValid = validRange(value.morningOpen(), value.morningClose());
        boolean afternoonValid = validRange(value.afternoonOpen(), value.afternoonClose());
        if (!morningValid && !afternoonValid) throw new RuntimeException("Ngày mở cửa phải có ít nhất một ca hợp lệ");
        if (morningValid && afternoonValid && value.morningClose().isAfter(value.afternoonOpen())) {
            throw new RuntimeException("Ca sáng và ca chiều không được chồng lấn");
        }
    }

    private boolean validRange(LocalTime from, LocalTime to) { return from != null && to != null && from.isBefore(to); }
    private boolean within(LocalTime value, LocalTime from, LocalTime to) {
        return validRange(from, to) && !value.isBefore(from) && value.isBefore(to);
    }
    private BranchHoursResponse toResponse(BranchOperatingHours value, int day) {
        return value == null ? defaultFor(day) : new BranchHoursResponse(day, value.isClosed(), value.getMorningOpen(),
                value.getMorningClose(), value.getAfternoonOpen(), value.getAfternoonClose());
    }
    private BranchHoursResponse defaultFor(int day) {
        boolean weekend = day == DayOfWeek.SATURDAY.getValue() || day == DayOfWeek.SUNDAY.getValue();
        return weekend
                ? new BranchHoursResponse(day, true, null, null, null, null)
                : new BranchHoursResponse(day, false, LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(13, 0), LocalTime.of(17, 0));
    }
}
