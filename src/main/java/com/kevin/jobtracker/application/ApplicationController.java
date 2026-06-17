package com.kevin.jobtracker.application;

import com.kevin.jobtracker.application.dto.ApplicationResponse;
import com.kevin.jobtracker.application.dto.CreateApplicationRequest;
import com.kevin.jobtracker.common.api.ApiResponse;
import com.kevin.jobtracker.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
