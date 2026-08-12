package com.sbqs.service;

import com.sbqs.entity.CustomerProfile;
import com.sbqs.entity.User;
import com.sbqs.repository.CustomerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerProfileService {
    private final CustomerProfileRepository repository;

    public CustomerProfileService(CustomerProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CustomerProfile find(User user) {
        if (user == null || user.getUserId() == null || !"CUSTOMER".equals(user.getRole())) return null;
        return repository.findByUserUserId(user.getUserId()).orElse(null);
    }

    @Transactional
    public CustomerProfile requireForUpdate(User user) {
        if (user == null || !"CUSTOMER".equals(user.getRole())) {
            throw new RuntimeException("Chi khach hang moi co ho so khach hang");
        }
        return repository.findByUserUserId(user.getUserId()).orElseGet(() -> {
            CustomerProfile profile = new CustomerProfile();
            profile.setUser(user);
            user.setCustomerProfile(profile);
            copyLegacyValues(user, profile);
            return repository.save(profile);
        });
    }

    public CustomerProfile save(CustomerProfile profile) {
        return repository.save(profile);
    }

    public Map<String, String> values(User user) {
        CustomerProfile profile = find(user);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("FULL_NAME", user.getFullName());
        values.put("MOBILE_PHONE", user.getPhone());
        values.put("EMAIL_ADDRESS", user.getEmail());
        for (String key : profileKeys()) values.put(key, value(profile, user, key));
        return values;
    }

    public Map<String, Object> snapshot(User user, List<String> requiredFields) {
        Map<String, String> allValues = values(user);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (requiredFields != null) {
            requiredFields.stream().distinct().forEach(key -> {
                if (allValues.containsKey(key)) snapshot.put(key, allValues.get(key));
            });
        }
        return snapshot;
    }

    public String value(User user, String key) {
        return values(user).getOrDefault(key, "");
    }

    public void apply(CustomerProfile profile, String key, String value) {
        switch (key) {
            case "DATE_OF_BIRTH" -> profile.setDateOfBirth(value);
            case "GENDER" -> profile.setGender(value);
            case "NATIONALITY" -> profile.setNationality(isBlank(value) ? "Việt Nam" : value);
            case "IDENTITY_NUMBER" -> profile.setIdentityNumber(value);
            case "IDENTITY_ISSUE_DATE" -> profile.setIdentityIssueDate(value);
            case "IDENTITY_ISSUE_PLACE" -> profile.setIdentityIssuePlace(value);
            case "PASSPORT_NUMBER" -> profile.setPassportNumber(value);
            case "VISA_NUMBER" -> profile.setVisaNumber(value);
            case "PERMANENT_ADDRESS" -> profile.setPermanentAddress(value);
            case "CONTACT_ADDRESS" -> profile.setContactAddress(value);
            case "OCCUPATION" -> profile.setOccupation(value);
            case "EMPLOYMENT_STATUS" -> profile.setEmploymentStatus(value);
            case "EMPLOYER_NAME" -> profile.setEmployerName(value);
            case "WORK_PHONE" -> profile.setWorkPhone(value);
            case "JOB_TITLE" -> profile.setJobTitle(value);
            case "MONTHLY_INCOME" -> profile.setMonthlyIncome(value);
            case "SALARY_PAYMENT_METHOD" -> profile.setSalaryPaymentMethod(value);
            default -> throw new RuntimeException("Truong ho so khong hop le: " + key);
        }
    }

    private String value(CustomerProfile profile, User legacyUser, String key) {
        String current = profile == null ? null : switch (key) {
            case "DATE_OF_BIRTH" -> profile.getDateOfBirth();
            case "GENDER" -> profile.getGender();
            case "NATIONALITY" -> profile.getNationality();
            case "IDENTITY_NUMBER" -> profile.getIdentityNumber();
            case "IDENTITY_ISSUE_DATE" -> profile.getIdentityIssueDate();
            case "IDENTITY_ISSUE_PLACE" -> profile.getIdentityIssuePlace();
            case "PASSPORT_NUMBER" -> profile.getPassportNumber();
            case "VISA_NUMBER" -> profile.getVisaNumber();
            case "PERMANENT_ADDRESS" -> profile.getPermanentAddress();
            case "CONTACT_ADDRESS" -> profile.getContactAddress();
            case "OCCUPATION" -> profile.getOccupation();
            case "EMPLOYMENT_STATUS" -> profile.getEmploymentStatus();
            case "EMPLOYER_NAME" -> profile.getEmployerName();
            case "WORK_PHONE" -> profile.getWorkPhone();
            case "JOB_TITLE" -> profile.getJobTitle();
            case "MONTHLY_INCOME" -> profile.getMonthlyIncome();
            case "SALARY_PAYMENT_METHOD" -> profile.getSalaryPaymentMethod();
            default -> "";
        };
        if (profile != null) return current;

        // Compatibility fallback for a legacy database that has not completed backfill yet.
        return switch (key) {
            case "DATE_OF_BIRTH" -> legacyUser.getDateOfBirth();
            case "GENDER" -> legacyUser.getGender();
            case "NATIONALITY" -> legacyUser.getNationality();
            case "IDENTITY_NUMBER" -> legacyUser.getIdentityNumber();
            case "IDENTITY_ISSUE_DATE" -> legacyUser.getIdentityIssueDate();
            case "IDENTITY_ISSUE_PLACE" -> legacyUser.getIdentityIssuePlace();
            case "PASSPORT_NUMBER" -> legacyUser.getPassportNumber();
            case "VISA_NUMBER" -> legacyUser.getVisaNumber();
            case "PERMANENT_ADDRESS" -> legacyUser.getPermanentAddress();
            case "CONTACT_ADDRESS" -> legacyUser.getContactAddress();
            case "OCCUPATION" -> legacyUser.getOccupation();
            case "EMPLOYMENT_STATUS" -> legacyUser.getEmploymentStatus();
            case "EMPLOYER_NAME" -> legacyUser.getEmployerName();
            case "WORK_PHONE" -> legacyUser.getWorkPhone();
            case "JOB_TITLE" -> legacyUser.getJobTitle();
            case "MONTHLY_INCOME" -> legacyUser.getMonthlyIncome();
            case "SALARY_PAYMENT_METHOD" -> legacyUser.getSalaryPaymentMethod();
            default -> "";
        };
    }

    private void copyLegacyValues(User user, CustomerProfile profile) {
        for (String key : profileKeys()) apply(profile, key, normalize(value(null, user, key)));
    }

    private List<String> profileKeys() {
        return List.of("DATE_OF_BIRTH", "GENDER", "NATIONALITY", "IDENTITY_NUMBER",
                "IDENTITY_ISSUE_DATE", "IDENTITY_ISSUE_PLACE", "PASSPORT_NUMBER", "VISA_NUMBER",
                "PERMANENT_ADDRESS", "CONTACT_ADDRESS", "OCCUPATION", "EMPLOYMENT_STATUS",
                "EMPLOYER_NAME", "WORK_PHONE", "JOB_TITLE", "MONTHLY_INCOME", "SALARY_PAYMENT_METHOD");
    }

    private String normalize(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
