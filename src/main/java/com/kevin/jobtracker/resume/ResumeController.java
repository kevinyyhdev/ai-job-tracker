package com.kevin.jobtracker.resume;

import com.kevin.jobtracker.common.api.ApiResponse;
import com.kevin.jobtracker.resume.dto.ResumeResponse;
import com.kevin.jobtracker.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResumeResponse> upload(
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(resumeService.upload(file, currentUser));
    }

    @GetMapping
    public ApiResponse<List<ResumeResponse>> list(
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(resumeService.list(currentUser.getId()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return resumeService.download(id, currentUser.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        resumeService.delete(id, currentUser.getId());
    }
}
