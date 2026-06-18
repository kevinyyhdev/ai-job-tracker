package com.kevin.jobtracker.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID>,
        JpaSpecificationExecutor<JobApplication> {

    Optional<JobApplication> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
