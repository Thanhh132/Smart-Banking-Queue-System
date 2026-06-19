package com.sbqs.controller;

import com.sbqs.dto.CreateStaffRequest;
import com.sbqs.entity.User;
import com.sbqs.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<List<User>> getUsers(
            @RequestParam(required = false) Long branchId) {

        if (branchId != null) {
            return ResponseEntity.ok(
                    userService.getUsersByBranch(branchId));
        }

        return ResponseEntity.ok(
                userService.getAllUsers());
    }

    @PostMapping("/staff")
    public ResponseEntity<User> createStaff(
            @RequestBody CreateStaffRequest request) {

        return ResponseEntity.ok(
                userService.createStaff(request));
    }

    @PostMapping("/admin-branch")
    public ResponseEntity<User> createAdminBranch(
            @RequestBody CreateStaffRequest request) {

        return ResponseEntity.ok(
                userService.createAdminBranch(request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(
                userService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId) {

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}
