package com.sbqs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteSocialProfileRequest(
        @NotBlank(message = "Ho ten khong duoc de trong")
        @Size(max = 150, message = "Ho ten khong duoc vuot qua 150 ky tu")
        String fullName,

        @NotBlank(message = "So dien thoai khong duoc de trong")
        @Pattern(regexp = "^(?:\\+84|0)[0-9]{9,10}$", message = "So dien thoai khong hop le")
        String phone,

        @NotBlank(message = "Dia chi thuong tru khong duoc de trong")
        @Size(max = 500, message = "Dia chi thuong tru khong duoc vuot qua 500 ky tu")
        String permanentAddress,

        @NotBlank(message = "Dia chi hien tai khong duoc de trong")
        @Size(max = 500, message = "Dia chi hien tai khong duoc vuot qua 500 ky tu")
        String contactAddress) {
}
