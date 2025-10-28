package com.innowise.userservice.controller;

import com.innowise.userservice.dto.ApiResponse;
import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    @PostMapping
    public ResponseEntity<ApiResponse<CardResponse>> create(@RequestBody @Valid CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Card created successfully", cardService.create(request))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardResponse>> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Card successfully found", cardService.getById(id))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CardResponse>>> getAllPaged(Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success("Page of cards successfully formed", cardService.getAllPaged(pageable))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        cardService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success("Card successfully deleted")
        );
    }
}
