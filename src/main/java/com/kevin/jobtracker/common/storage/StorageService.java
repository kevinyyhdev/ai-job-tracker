package com.kevin.jobtracker.common.storage;

public interface StorageService {

    String store(byte[] bytes, String originalFilename);

    byte[] load(String storageKey);

    void delete(String storageKey);
}
