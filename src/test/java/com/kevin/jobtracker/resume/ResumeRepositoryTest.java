package com.kevin.jobtracker.resume;

import com.kevin.jobtracker.application.JobApplicationRepository;
import com.kevin.jobtracker.user.User;
import com.kevin.jobtracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ResumeRepositoryTest {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        resumeRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        userA = userRepository.save(new User("a@example.com", "hash", "User A"));
        userB = userRepository.save(new User("b@example.com", "hash", "User B"));
    }

    @Test
    void saveAndFindResume() {
        Resume resume = resumeRepository.save(
                new Resume(userA, "resume.pdf", "application/pdf", 1024L, "key-resume.pdf"));

        Optional<Resume> found = resumeRepository.findByIdAndUserId(resume.getId(), userA.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOriginalFilename()).isEqualTo("resume.pdf");
        assertThat(found.get().getSizeBytes()).isEqualTo(1024L);
    }

    @Test
    void tenantIsolation_userBCannotSeeUserAResume() {
        Resume resume = resumeRepository.save(
                new Resume(userA, "resume.pdf", "application/pdf", 1024L, "key-resume.pdf"));

        Optional<Resume> found = resumeRepository.findByIdAndUserId(resume.getId(), userB.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void listReturnsOnlyCurrentUserResumes() {
        resumeRepository.save(new Resume(userA, "cv.pdf", "application/pdf", 500L, "key-cv-a.pdf"));
        resumeRepository.save(new Resume(userB, "cv.pdf", "application/pdf", 600L, "key-cv-b.pdf"));

        List<Resume> results = resumeRepository.findByUserIdOrderByCreatedAtDesc(userA.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStorageKey()).isEqualTo("key-cv-a.pdf");
    }
}
