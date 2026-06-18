package com.kevin.jobtracker.application.dto;

import com.kevin.jobtracker.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusTransitionRequest {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;
}
