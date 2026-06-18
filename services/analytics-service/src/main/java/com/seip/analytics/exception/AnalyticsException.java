package com.seip.analytics.exception;

public class AnalyticsException extends RuntimeException {
    public AnalyticsException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnalyticsException(String message) {
        super(message);
    }
}
