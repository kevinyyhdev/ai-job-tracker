package com.kevin.jobtracker.application.dto;

import com.kevin.jobtracker.application.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateApplicationRequest {

    private String companyName;

    private String jobTitle;

    @URL(message = "Job link must be a valid URL")
    private String jobLink;

    private String location;

    private String employmentType;

    private ApplicationStatus status;

    private String source;

    private String notes;
}
