package com.sbqs.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class FormSchemaConverter implements AttributeConverter<List<FormFieldDefinition>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<FormFieldDefinition>> TYPE = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<FormFieldDefinition> value) {
        try {
            return MAPPER.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Khong the luu cau hinh bieu mau", exception);
        }
    }

    @Override
    public List<FormFieldDefinition> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        try {
            return new ArrayList<>(MAPPER.readValue(value, TYPE));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cau hinh bieu mau khong hop le", exception);
        }
    }
}
