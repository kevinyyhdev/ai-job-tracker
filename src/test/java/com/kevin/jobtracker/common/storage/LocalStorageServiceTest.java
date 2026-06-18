package com.kevin.jobtracker.common.storage;

import com.kevin.jobtracker.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    void storeAndLoadBytes() {
        byte[] content = "hello resume".getBytes();
        String key = storageService.store(content, "resume.pdf");

        byte[] loaded = storageService.load(key);

        assertThat(loaded).isEqualTo(content);
    }

    @Test
    void storageKeyIsUnique() {
        byte[] content = "data".getBytes();
        String key1 = storageService.store(content, "resume.pdf");
        String key2 = storageService.store(content, "resume.pdf");

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void deleteRemovesFile() {
        String key = storageService.store("data".getBytes(), "resume.pdf");
        storageService.delete(key);

        assertThatThrownBy(() -> storageService.load(key))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loadNonExistentKeyThrowsNotFound() {
        assertThatThrownBy(() -> storageService.load("nonexistent.pdf"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
