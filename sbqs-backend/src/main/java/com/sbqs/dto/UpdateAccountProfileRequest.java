package com.sbqs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountProfileRequest(
        @NotBlank(message = "Ho ten khong duoc de trong")
        @Size(max = 150, message = "Ho ten khong duoc vuot qua 150 ky tu")
        String fullName,

        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong hop le")
        String email,

        @NotBlank(message = "So dien thoai khong duoc de trong")
        @Size(max = 30, message = "So dien thoai khong duoc vuot qua 30 ky tu")
        String phone) {
}
