package com.sbqs.dto.bulkimport;

public record StaffImportRow(
        int rowNumber,
        String fullName,
        String email,
        String phone,
        String password) {
}
