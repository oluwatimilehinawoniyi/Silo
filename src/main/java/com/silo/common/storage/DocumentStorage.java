package com.silo.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorage {

    /**
     * @return a publicly-reachable URL for the uploaded file
     */
    String upload(MultipartFile file);
}
