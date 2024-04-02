package com.learnwiremock.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class MovieErrorResponse extends RuntimeException {
    public MovieErrorResponse(String statusText, WebClientResponseException e) {
        super(statusText, e);
    }

    public MovieErrorResponse(Exception e) {
        super(e);
    }
}
