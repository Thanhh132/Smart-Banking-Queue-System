package com.sbqs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    private String fullName;

    private String email;

    private String phone;

    private String status;
}