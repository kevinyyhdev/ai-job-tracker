package com.kevin.jobtracker.application;

import com.kevin.jobtracker.application.dto.ApplicationResponse;
import com.kevin.jobtracker.application.dto.CreateApplicationRequest;
import com.kevin.jobtracker.application.dto.StatusTransitionRequest;
import com.kevin.jobtracker.application.dto.UpdateApplicationRequest;
import com.kevin.jobtracker.common.api.ApiResponse;
import com.kevin.jobtracker.common.api.PageResponse;
import com.kevin.jobtracker.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final JobApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationResponse> create(
            @Valid @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(applicationService.create(request, currentUser));
    }

    @GetMapping
    public ApiResponse<PageResponse<ApplicationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(applicationService.list(currentUser.getId(), page, size, status, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(applicationService.getById(id, currentUser.getId()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ApplicationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(applicationService.update(id, request, currentUser.getId()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ApplicationResponse> transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusTransitionRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(applicationService.transitionStatus(id, request.getStatus(), currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        applicationService.delete(id, currentUser.getId());
    }
}
