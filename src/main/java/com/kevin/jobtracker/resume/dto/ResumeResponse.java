package com.kevin.jobtracker.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeResponse {

    private UUID id;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String extractedText;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
