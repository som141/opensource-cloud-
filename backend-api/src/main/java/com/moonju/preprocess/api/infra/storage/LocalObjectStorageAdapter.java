package com.moonju.preprocess.api.infra.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalObjectStorageAdapter implements ObjectStoragePort {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public boolean exists(String objectKey) {
        return objects.containsKey(objectKey);
    }

    @Override
    public byte[] downloadBytes(String objectKey) {
        byte[] bytes = objects.get(objectKey);
        if (bytes == null) {
            throw new ObjectStorageAccessException(
                "Local object not found: " + objectKey,
                new IllegalArgumentException(objectKey)
            );
        }
        return bytes;
    }

    @Override
    public void uploadBytes(String objectKey, byte[] bytes, String contentType) {
        objects.put(objectKey, bytes);
    }
}
