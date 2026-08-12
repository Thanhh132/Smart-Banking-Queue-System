package com.sbqs.controller;

import com.sbqs.dto.CreateStaffRequest;
import com.sbqs.entity.User;
import com.sbqs.dto.UserManagementResponse;
import com.sbqs.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.sbqs.dto.UpdateUserRequest;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService) {

        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserManagementResponse>> getUsers(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String role) {

        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(userService.getUsersByRole(role).stream()
                    .map(UserManagementResponse::from).toList());
        }

        if (branchId != null) {
            return ResponseEntity.ok(userService.getUsersByBranch(branchId).stream()
                    .map(UserManagementResponse::from).toList());
        }

        return ResponseEntity.ok(userService.getAllUsers().stream()
                .map(UserManagementResponse::from).toList());
    }

    @PostMapping("/staff")
    public ResponseEntity<UserManagementResponse> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {

        return ResponseEntity.ok(UserManagementResponse.from(userService.createStaff(request)));
    }

    @PostMapping("/admin-branch")
    public ResponseEntity<UserManagementResponse> createAdminBranch(
            @Valid @RequestBody CreateStaffRequest request) {

        return ResponseEntity.ok(UserManagementResponse.from(userService.createAdminBranch(request)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserManagementResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(UserManagementResponse.from(userService.updateUser(userId, request)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId) {

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}
