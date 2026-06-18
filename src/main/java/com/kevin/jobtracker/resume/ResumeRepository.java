package com.kevin.jobtracker.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findByIdAndUserId(UUID id, UUID userId);

    List<Resume> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
