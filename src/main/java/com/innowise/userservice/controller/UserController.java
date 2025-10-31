package com.innowise.userservice.controller;

import com.innowise.userservice.dto.ApiResponse;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@RequestBody @Valid UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body((
                ApiResponse.success("User successfully created", userService.create(request))
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User successfully found", userService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success("Page of users successfully formed", userService.getAllPaged(pageable))
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<UserResponse>> get(
            @RequestParam(value = "email")
            @NotBlank @Email(message = "Please provide correct email address")
            String email) {
        return ResponseEntity.ok(
                ApiResponse.success("User successfully found", userService.getByEmail(email))
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable("id") UUID id,
                                                    @RequestBody @Valid UserUpdateRequest request) {
        userService.update(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("User successfully updated")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success("User successfully deleted")
        );
    }
}
