package com.kevin.jobtracker.application;

import com.kevin.jobtracker.application.dto.ApplicationResponse;
import com.kevin.jobtracker.auth.JwtService;
import com.kevin.jobtracker.common.config.SecurityConfig;
import com.kevin.jobtracker.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import com.kevin.jobtracker.common.api.PageResponse;
import com.kevin.jobtracker.common.exception.ResourceNotFoundException;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import com.kevin.jobtracker.common.exception.BusinessRuleException;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationService applicationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private com.kevin.jobtracker.user.UserRepository userRepository;


    private final User mockUser = new User("alice@example.com", "hash", "Alice");

    private UsernamePasswordAuthenticationToken authAs(User user) {
        return new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void validCreateReturns201() throws Exception {
        ApplicationResponse response = new ApplicationResponse(
                UUID.randomUUID(), "Google", "Software Engineer",
                null, null, null, ApplicationStatus.SAVED,
                null, null, null, null, null);

        when(applicationService.create(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/applications")
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Google\",\"jobTitle\":\"Software Engineer\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyName").value("Google"))
                .andExpect(jsonPath("$.data.status").value("SAVED"));
    }

    @Test
    void missingCompanyNameReturns422() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobTitle\":\"Software Engineer\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.companyName").exists());
    }

    @Test
    void missingJobTitleReturns422() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Google\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.jobTitle").exists());
    }

    @Test
    void invalidJobLinkReturns422() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Google\",\"jobTitle\":\"SDE\",\"jobLink\":\"not-a-url\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.jobLink").exists());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Google\",\"jobTitle\":\"SDE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReturns200WithPage() throws Exception {
        ApplicationResponse response = new ApplicationResponse(
                UUID.randomUUID(), "Google", "Software Engineer",
                null, null, null, ApplicationStatus.SAVED,
                null, null, null, null, null);

        PageResponse<ApplicationResponse> page = new PageResponse<>(
                Collections.singletonList(response), 0, 20, 1, 1, true);

        when(applicationService.list(any(), anyInt(), anyInt(), isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/applications")
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].companyName").value("Google"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getByIdReturns200() throws Exception {
        UUID appId = UUID.randomUUID();
        ApplicationResponse response = new ApplicationResponse(
                appId, "Amazon", "SDE",
                null, null, null, ApplicationStatus.SAVED,
                null, null, null, null, null);

        when(applicationService.getById(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/applications/" + appId)
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Amazon"));
    }

    @Test
    void getByIdNotFoundReturns404() throws Exception {
        when(applicationService.getById(any(), any()))
                .thenThrow(new ResourceNotFoundException("Application not found"));

        mockMvc.perform(get("/api/applications/" + UUID.randomUUID())
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void updateReturns200WithUpdatedFields() throws Exception {
        UUID appId = UUID.randomUUID();
        ApplicationResponse updated = new ApplicationResponse(
                appId, "Google", "Senior SDE",
                null, null, null, ApplicationStatus.APPLIED,
                null, null, null, null, null);

        when(applicationService.update(any(), any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/applications/" + appId)
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobTitle\":\"Senior SDE\",\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobTitle").value("Senior SDE"))
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    void updateNotFoundReturns404() throws Exception {
        when(applicationService.update(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Application not found"));

        mockMvc.perform(patch("/api/applications/" + UUID.randomUUID())
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobTitle\":\"Senior SDE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/applications/" + UUID.randomUUID())
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNotFoundReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Application not found"))
                .when(applicationService).delete(any(), any());

        mockMvc.perform(delete("/api/applications/" + UUID.randomUUID())
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isNotFound());
    }

    @Test
    void listWithStatusFilterReturns200() throws Exception {
        ApplicationResponse response = new ApplicationResponse(
                UUID.randomUUID(), "Google", "Software Engineer",
                null, null, null, ApplicationStatus.APPLIED,
                null, null, null, null, null);

        PageResponse<ApplicationResponse> page = new PageResponse<>(
                Collections.singletonList(response), 0, 20, 1, 1, true);

        when(applicationService.list(any(), anyInt(), anyInt(), any(ApplicationStatus.class), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/applications?status=APPLIED")
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("APPLIED"));
    }

    @Test
    void listWithKeywordFilterReturns200() throws Exception {
        ApplicationResponse response = new ApplicationResponse(
                UUID.randomUUID(), "Google", "Senior Software Engineer",
                null, null, null, ApplicationStatus.SAVED,
                null, null, null, null, null);

        PageResponse<ApplicationResponse> page = new PageResponse<>(
                Collections.singletonList(response), 0, 20, 1, 1, true);

        when(applicationService.list(any(), anyInt(), anyInt(), isNull(), any(String.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/applications?keyword=senior")
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].companyName").value("Google"));
    }

    @Test
    void legalStatusTransitionReturns200() throws Exception {
        UUID appId = UUID.randomUUID();
        ApplicationResponse updated = new ApplicationResponse(
                appId, "Google", "SDE",
                null, null, null, ApplicationStatus.APPLIED,
                null, null, null, null, null);

        when(applicationService.transitionStatus(any(), any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    void illegalStatusTransitionReturns422() throws Exception {
        when(applicationService.transitionStatus(any(), any(), any()))
                .thenThrow(new BusinessRuleException("Cannot transition from REJECTED to APPLIED"));

        mockMvc.perform(patch("/api/applications/" + UUID.randomUUID() + "/status")
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void statusTransitionMissingStatusReturns422() throws Exception {
        mockMvc.perform(patch("/api/applications/" + UUID.randomUUID() + "/status")
                        .with(authentication(authAs(mockUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
