package com.silo.common.storage;

import com.silo.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class DocumentUploadException extends ApiException {

    public DocumentUploadException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
