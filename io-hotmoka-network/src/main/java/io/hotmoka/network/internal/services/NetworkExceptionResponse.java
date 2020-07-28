package io.hotmoka.network.internal.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Wrapper class of {@link org.springframework.web.server.ResponseStatusException}
 * to throw exceptions and hence return to client a message with an HTTP status
 */
public class NetworkExceptionResponse extends ResponseStatusException {
	private static final long serialVersionUID = 1L;

	NetworkExceptionResponse(HttpStatus status, String reason) {
        super(status, reason);
    }

    @Override
    public String getMessage() {
        return getReason();
    }
}