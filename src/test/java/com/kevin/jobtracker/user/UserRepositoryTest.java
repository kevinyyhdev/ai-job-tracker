package com.kevin.jobtracker.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByEmail() {
        userRepository.save(new User("alice@example.com", "hashed_password", "Alice"));

        var found = userRepository.findByEmail("alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Alice");
        assertThat(found.get().getRole()).isEqualTo("USER");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void existsByEmail_returnsTrueForExistingEmail() {
        userRepository.save(new User("bob@example.com", "hashed_password", "Bob"));

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalseForUnknownEmail() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void duplicateEmailThrowsException() {
        userRepository.save(new User("carol@example.com", "hashed_password", "Carol"));

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(new User("carol@example.com", "other_hash", "Carol 2"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
