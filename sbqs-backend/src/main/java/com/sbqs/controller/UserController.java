package com.sbqs.controller;

import com.sbqs.dto.CreateStaffRequest;
import com.sbqs.entity.User;
import com.sbqs.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false)
            Long branchId) {

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
}