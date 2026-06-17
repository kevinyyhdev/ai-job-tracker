package com.kevin.jobtracker.application;

import com.kevin.jobtracker.application.dto.ApplicationResponse;
import com.kevin.jobtracker.auth.JwtService;
import com.kevin.jobtracker.common.config.SecurityConfig;
import com.kevin.jobtracker.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobApplicationService applicationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
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
}
