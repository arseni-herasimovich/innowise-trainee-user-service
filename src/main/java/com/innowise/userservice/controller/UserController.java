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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @securityService.canCreateUser(authentication.principal, #request)")
    public ResponseEntity<ApiResponse<UserResponse>> create(@RequestBody @Valid UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body((
                ApiResponse.success("User successfully created", userService.create(request))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User successfully found", userService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success("Page of users successfully formed", userService.getAllPaged(pageable))
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> get(
            @RequestParam(value = "email")
            @NotBlank @Email(message = "Please provide correct email address")
            String email) {
        return ResponseEntity.ok(
                ApiResponse.success("User successfully found", userService.getByEmail(email))
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable("id") UUID id,
                                                    @RequestBody @Valid UserUpdateRequest request) {
        userService.update(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("User successfully updated")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success("User successfully deleted")
        );
    }
}
