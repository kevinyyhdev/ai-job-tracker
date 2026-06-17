package com.kevin.jobtracker.application.dto;

import com.kevin.jobtracker.application.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationResponse {

    private UUID id;
    private String companyName;
    private String jobTitle;
    private String jobLink;
    private String location;
    private String employmentType;
    private ApplicationStatus status;
    private String source;
    private String notes;
    private OffsetDateTime appliedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
