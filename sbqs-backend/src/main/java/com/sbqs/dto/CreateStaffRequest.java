package com.sbqs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStaffRequest {

    private String fullName;

    private String email;

    private String password;

    private String phone;

    private Long branchId;
}