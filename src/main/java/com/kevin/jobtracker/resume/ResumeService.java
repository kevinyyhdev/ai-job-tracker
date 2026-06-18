package com.kevin.jobtracker.resume;

import com.kevin.jobtracker.common.exception.BusinessRuleException;
import com.kevin.jobtracker.common.exception.ResourceNotFoundException;
import com.kevin.jobtracker.common.storage.StorageService;
import com.kevin.jobtracker.resume.dto.ResumeResponse;
import com.kevin.jobtracker.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ResumeRepository resumeRepository;
    private final StorageService storageService;

    // Validates the file, writes bytes to storage, saves metadata row in DB, returns metadata response.
    public ResumeResponse upload(MultipartFile file, User currentUser) {
        validateFile(file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to read uploaded file");
        }
        String storageKey = storageService.store(bytes, file.getOriginalFilename());
        Resume resume = new Resume(currentUser, file.getOriginalFilename(),
                file.getContentType(), file.getSize(), storageKey);
        return toResponse(resumeRepository.save(resume));
    }

    // Returns metadata for all resumes owned by the current user, newest first.
    public List<ResumeResponse> list(UUID userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    // Verifies ownership, loads file bytes from storage, streams them back with correct headers.
    public ResponseEntity<byte[]> download(UUID id, UUID userId) {
        Resume resume = getOwnedResumeOrThrow(id, userId);
        byte[] bytes = storageService.load(resume.getStorageKey());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resume.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(resume.getContentType()))
                .body(bytes);
    }

    // Deletes file bytes from storage first, then deletes the DB row. Order matters: if the DB row
    // were deleted first and storage deletion failed, the bytes would be orphaned with no way to clean them up.
    public void delete(UUID id, UUID userId) {
        Resume resume = getOwnedResumeOrThrow(id, userId);
        storageService.delete(resume.getStorageKey());
        resumeRepository.delete(resume);
    }

    // Rejects empty files, wrong MIME types, and files over 5 MB with a 422 BusinessRuleException.
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("File must not be empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException("Only PDF and DOCX files are accepted");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("File size must not exceed 5 MB");
        }
    }

    // Tenant-safe lookup: returns 404 if the resume doesn't exist or belongs to a different user.
    private Resume getOwnedResumeOrThrow(UUID id, UUID userId) {
        return resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
    }

    // Maps Resume entity to ResumeResponse DTO, excluding internal fields (storageKey, extractedText).
    private ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getContentType(),
                resume.getSizeBytes(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
