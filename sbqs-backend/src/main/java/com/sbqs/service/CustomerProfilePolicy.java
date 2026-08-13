package com.sbqs.service;

import com.sbqs.entity.User;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Quy định các thông tin hồ sơ tối thiểu dùng chung cho mọi giao dịch không giấy. */
public final class CustomerProfilePolicy {
    public static final String FULL_NAME = "FULL_NAME";
    public static final String MOBILE_PHONE = "MOBILE_PHONE";
    public static final String PERMANENT_ADDRESS = "PERMANENT_ADDRESS";
    public static final String CONTACT_ADDRESS = "CONTACT_ADDRESS";

    public static final List<String> DEFAULT_REQUIRED_FIELDS = List.of(
            FULL_NAME,
            MOBILE_PHONE,
            PERMANENT_ADDRESS,
            CONTACT_ADDRESS);

    private CustomerProfilePolicy() {
    }

    public static List<String> includeDefaults(List<String> configuredFields) {
        LinkedHashSet<String> fields = new LinkedHashSet<>(DEFAULT_REQUIRED_FIELDS);
        if (configuredFields != null) {
            configuredFields.stream()
                    .filter(field -> field != null && !field.isBlank())
                    .map(String::trim)
                    .forEach(fields::add);
        }
        return new ArrayList<>(fields);
    }

    public static boolean isAccountManaged(String field) {
        return FULL_NAME.equals(field) || MOBILE_PHONE.equals(field);
    }

    public static boolean isComplete(User user) {
        return user != null
                && !isBlank(user.getFullName())
                && !isBlank(user.getPhone())
                && user.getCustomerProfile() != null
                && !isBlank(user.getCustomerProfile().getPermanentAddress())
                && !isBlank(user.getCustomerProfile().getContactAddress());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
