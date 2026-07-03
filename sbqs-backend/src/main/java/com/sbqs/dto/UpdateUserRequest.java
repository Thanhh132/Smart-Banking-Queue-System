package com.sbqs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank(message = "Ho ten khong duoc de trong")
    private String fullName;

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    private String email;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    private String phone;

    @Pattern(regexp = "(?i)ACTIVE|INACTIVE", message = "Trang thai chi duoc la ACTIVE hoac INACTIVE")
    private String status;

    private Long branchId;
}
