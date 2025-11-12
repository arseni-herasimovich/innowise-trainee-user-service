package com.innowise.userservice.client;

import com.innowise.userservice.dto.ApiResponse;
import com.innowise.userservice.dto.ValidateTokenRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @PostMapping("/api/v1/auth/validate")
    ApiResponse<Boolean> validate(ValidateTokenRequest request);
}
