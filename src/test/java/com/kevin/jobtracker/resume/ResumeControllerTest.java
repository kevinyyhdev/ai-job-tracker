package com.kevin.jobtracker.resume;

import com.kevin.jobtracker.auth.JwtService;
import com.kevin.jobtracker.common.config.SecurityConfig;
import com.kevin.jobtracker.resume.dto.ResumeResponse;
import com.kevin.jobtracker.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeService resumeService;

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
    void validPdfUploadReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "pdf content".getBytes());

        ResumeResponse response = new ResumeResponse(
                UUID.randomUUID(), "resume.pdf", "application/pdf", 11L,
                "Software engineer with 5 years experience", OffsetDateTime.now(), OffsetDateTime.now());

        when(resumeService.upload(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFilename").value("resume.pdf"))
                .andExpect(jsonPath("$.data.contentType").value("application/pdf"));
    }

    @Test
    void validDocxUploadReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx content".getBytes());

        ResumeResponse response = new ResumeResponse(
                UUID.randomUUID(), "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                12L, "Software engineer with 5 years experience", OffsetDateTime.now(), OffsetDateTime.now());

        when(resumeService.upload(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFilename").value("resume.docx"));
    }

    @Test
    void unauthenticatedUploadReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/resumes").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReturns200WithResumes() throws Exception {
        ResumeResponse response = new ResumeResponse(
                UUID.randomUUID(), "cv.pdf", "application/pdf", 500L,
                "Data analyst with Python skills", OffsetDateTime.now(), OffsetDateTime.now());

        when(resumeService.list(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/resumes")
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].originalFilename").value("cv.pdf"));
    }

    @Test
    void downloadReturns200WithBytes() throws Exception {
        UUID resumeId = UUID.randomUUID();
        byte[] content = "pdf bytes".getBytes();

        when(resumeService.download(any(), any())).thenReturn(
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume.pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(content));

        mockMvc.perform(get("/api/resumes/" + resumeId + "/download")
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"resume.pdf\""))
                .andExpect(content().bytes(content));
    }

    @Test
    void downloadNotFoundReturns404() throws Exception {
        when(resumeService.download(any(), any()))
                .thenThrow(new com.kevin.jobtracker.common.exception.ResourceNotFoundException("Resume not found"));

        mockMvc.perform(get("/api/resumes/" + UUID.randomUUID() + "/download")
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/resumes/" + UUID.randomUUID())
                        .with(authentication(authAs(mockUser))))
                .andExpect(status().isNoContent());
    }
}
