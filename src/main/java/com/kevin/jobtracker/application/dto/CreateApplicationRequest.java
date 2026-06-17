package com.kevin.jobtracker.application.dto;

import com.kevin.jobtracker.application.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateApplicationRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @URL(message = "Job link must be a valid URL")
    private String jobLink;

    private String location;

    private String employmentType;

    private ApplicationStatus status;

    private String source;

    private String notes;
}
