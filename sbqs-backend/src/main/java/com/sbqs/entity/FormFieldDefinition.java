package com.sbqs.entity;

import java.util.List;

public record FormFieldDefinition(
        String key,
        String label,
        String type,
        boolean required,
        String placeholder,
        String section,
        List<String> options) {
}
