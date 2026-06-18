package com.kevin.jobtracker.common.storage;

import com.kevin.jobtracker.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path storageRoot;

    public LocalStorageService(@Value("${storage.local.root:uploads}") String root) {
        this.storageRoot = Paths.get(root);
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(byte[] bytes, String originalFilename) {
        String storageKey = UUID.randomUUID() + "_" + originalFilename;
        try {
            Files.write(storageRoot.resolve(storageKey), bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
        return storageKey;
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(storageRoot.resolve(storageKey));
        } catch (IOException e) {
            throw new ResourceNotFoundException("File not found: " + storageKey);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(storageRoot.resolve(storageKey));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}
