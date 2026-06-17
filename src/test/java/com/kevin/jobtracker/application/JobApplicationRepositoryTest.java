package com.kevin.jobtracker.application;

import com.kevin.jobtracker.user.User;
import com.kevin.jobtracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JobApplicationRepositoryTest {

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        userA = userRepository.save(new User("a@example.com", "hash", "User A"));
        userB = userRepository.save(new User("b@example.com", "hash", "User B"));
    }

    @Test
    void saveAndFindApplication() {
        JobApplication app = applicationRepository.save(
                new JobApplication(userA, "Google", "Software Engineer"));

        Optional<JobApplication> found = applicationRepository.findByIdAndUserIdAndDeletedAtIsNull(
                app.getId(), userA.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCompanyName()).isEqualTo("Google");
        assertThat(found.get().getStatus()).isEqualTo(ApplicationStatus.SAVED);
    }

    @Test
    void tenantIsolation_userBCannotSeeUserAApplication() {
        JobApplication app = applicationRepository.save(
                new JobApplication(userA, "Google", "Software Engineer"));

        Optional<JobApplication> found = applicationRepository.findByIdAndUserIdAndDeletedAtIsNull(
                app.getId(), userB.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void softDeletedApplicationIsNotFound() {
        JobApplication app = applicationRepository.save(
                new JobApplication(userA, "Amazon", "SDE"));
        app.setDeletedAt(java.time.OffsetDateTime.now());
        applicationRepository.save(app);

        Optional<JobApplication> found = applicationRepository.findByIdAndUserIdAndDeletedAtIsNull(
                app.getId(), userA.getId());

        assertThat(found).isEmpty();
    }
}
