package com.kevin.jobtracker.application;

import com.kevin.jobtracker.application.dto.ApplicationResponse;
import com.kevin.jobtracker.application.dto.CreateApplicationRequest;
import com.kevin.jobtracker.application.dto.UpdateApplicationRequest;
import com.kevin.jobtracker.common.api.PageResponse;
import com.kevin.jobtracker.common.exception.BusinessRuleException;
import com.kevin.jobtracker.common.exception.ResourceNotFoundException;
import com.kevin.jobtracker.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ApplicationStatus.SAVED,         Set.of(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN, ApplicationStatus.EXPIRED),
            ApplicationStatus.APPLIED,       Set.of(ApplicationStatus.INTERVIEWING, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN, ApplicationStatus.EXPIRED),
            ApplicationStatus.INTERVIEWING,  Set.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.OFFER,         Set.of(ApplicationStatus.WITHDRAWN),
            ApplicationStatus.REJECTED,      Set.of(),
            ApplicationStatus.WITHDRAWN,     Set.of(),
            ApplicationStatus.EXPIRED,       Set.of()
    );

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

    public PageResponse<ApplicationResponse> list(UUID userId, int page, int size,
                                                   ApplicationStatus status, String keyword) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<JobApplication> spec = ApplicationSpecification.belongsToUser(userId)
                .and(ApplicationSpecification.notDeleted());

        if (status != null) {
            spec = spec.and(ApplicationSpecification.hasStatus(status));
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ApplicationSpecification.hasKeyword(keyword));
        }

        return PageResponse.from(applicationRepository.findAll(spec, pageable).map(this::toResponse));
    }

    public ApplicationResponse getById(UUID id, UUID userId) {
        return toResponse(getOwnedApplicationOrThrow(id, userId));
    }

    public ApplicationResponse update(UUID id, UpdateApplicationRequest request, UUID userId) {
        JobApplication app = getOwnedApplicationOrThrow(id, userId);
        if (request.getCompanyName() != null) app.setCompanyName(request.getCompanyName());
        if (request.getJobTitle() != null) app.setJobTitle(request.getJobTitle());
        if (request.getJobLink() != null) app.setJobLink(request.getJobLink());
        if (request.getLocation() != null) app.setLocation(request.getLocation());
        if (request.getEmploymentType() != null) app.setEmploymentType(request.getEmploymentType());
        if (request.getStatus() != null) app.setStatus(request.getStatus());
        if (request.getSource() != null) app.setSource(request.getSource());
        if (request.getNotes() != null) app.setNotes(request.getNotes());
        return toResponse(applicationRepository.save(app));
    }

    public ApplicationResponse transitionStatus(UUID id, ApplicationStatus newStatus, UUID userId) {
        JobApplication app = getOwnedApplicationOrThrow(id, userId);
        Set<ApplicationStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(app.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new BusinessRuleException(
                    "Cannot transition from " + app.getStatus() + " to " + newStatus);
        }
        app.setStatus(newStatus);
        return toResponse(applicationRepository.save(app));
    }

    public void delete(UUID id, UUID userId) {
        JobApplication app = getOwnedApplicationOrThrow(id, userId);
        app.setDeletedAt(OffsetDateTime.now());
        applicationRepository.save(app);
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
