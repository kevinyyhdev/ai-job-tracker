package com.kevin.jobtracker.application;

import com.kevin.jobtracker.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;

    @InjectMocks
    private JobApplicationService applicationService;

    @Test
    void getOwnedApplication_wrongUser_throws404() {
        UUID appId = UUID.randomUUID();
        UUID wrongUserId = UUID.randomUUID();

        when(applicationRepository.findByIdAndUserIdAndDeletedAtIsNull(appId, wrongUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getOwnedApplicationOrThrow(appId, wrongUserId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
