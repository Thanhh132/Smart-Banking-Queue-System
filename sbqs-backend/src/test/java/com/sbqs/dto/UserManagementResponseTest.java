package com.sbqs.dto;

import com.sbqs.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementResponseTest {
    @Test
    void exposesOnlyAccountManagementFields() {
        User user = new User();
        user.setUserId(1L);
        user.setFullName("Staff");
        user.setEmail("staff@example.com");
        user.setPhone("0900000000");
        user.setRole("STAFF");
        user.setStatus("ACTIVE");
        user.setPasswordHash("secret");

        UserManagementResponse response = UserManagementResponse.from(user);

        assertEquals("Staff", response.fullName());
        assertEquals("STAFF", response.role());
        assertEquals(8, UserManagementResponse.class.getRecordComponents().length);
    }
}
