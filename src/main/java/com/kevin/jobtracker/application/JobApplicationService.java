package com.kevin.jobtracker.application;

import com.kevin.jobtracker.application.dto.ApplicationResponse;
import com.kevin.jobtracker.application.dto.CreateApplicationRequest;
import com.kevin.jobtracker.common.api.PageResponse;
import com.kevin.jobtracker.common.exception.ResourceNotFoundException;
import com.kevin.jobtracker.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;

    public ApplicationResponse create(CreateApplicationRequest request, User currentUser) {
        JobApplication app = new JobApplication(currentUser, request.getCompanyName(), request.getJobTitle());
        app.setJobLink(request.getJobLink());
        app.setLocation(request.getLocation());
        app.setEmploymentType(request.getEmploymentType());
        app.setSource(request.getSource());
        app.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            app.setStatus(request.getStatus());
        }
        return toResponse(applicationRepository.save(app));
    }

    public PageResponse<ApplicationResponse> list(UUID userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
                applicationRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                        .map(this::toResponse));
    }

    public ApplicationResponse getById(UUID id, UUID userId) {
        return toResponse(getOwnedApplicationOrThrow(id, userId));
    }

    JobApplication getOwnedApplicationOrThrow(UUID id, UUID userId) {
        return applicationRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private ApplicationResponse toResponse(JobApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getCompanyName(),
                app.getJobTitle(),
                app.getJobLink(),
                app.getLocation(),
                app.getEmploymentType(),
                app.getStatus(),
                app.getSource(),
                app.getNotes(),
                app.getAppliedAt(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
