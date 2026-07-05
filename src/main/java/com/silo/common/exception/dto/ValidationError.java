package com.silo.common.exception.dto;

public record ValidationError(String field, String message) {
}
