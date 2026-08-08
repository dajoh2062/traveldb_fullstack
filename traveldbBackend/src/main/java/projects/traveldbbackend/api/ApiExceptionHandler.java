package projects.traveldbbackend.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    @ExceptionHandler(InvalidJourneyRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidJourneyRequest(
            InvalidJourneyRequestException error,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-journey-request",
                "Invalid journey request",
                "INVALID_JOURNEY_REQUEST",
                error.getMessage(),
                request,
                error.getViolations()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "malformed-json",
                "Malformed request body",
                "MALFORMED_JSON",
                "Request body must contain valid JSON matching the journey request schema.",
                request,
                List.of()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpServletRequest request) {
        return problem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "unsupported-media-type",
                "Unsupported media type",
                "UNSUPPORTED_MEDIA_TYPE",
                "Send request bodies as application/json.",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException error,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-parameter",
                "Invalid request parameter",
                "INVALID_PARAMETER",
                "A required request parameter is missing.",
                request,
                List.of(new InvalidJourneyRequestException.FieldViolation(
                        error.getParameterName(),
                        "Parameter is required."
                ))
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidParameterType(
            MethodArgumentTypeMismatchException error,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-parameter",
                "Invalid request parameter",
                "INVALID_PARAMETER",
                "A request parameter has an invalid value.",
                request,
                List.of(new InvalidJourneyRequestException.FieldViolation(
                        error.getName(),
                        "Value has the wrong type."
                ))
        );
    }

    @ExceptionHandler(InvalidRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidParameter(
            InvalidRequestParameterException error,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-parameter",
                "Invalid request parameter",
                "INVALID_PARAMETER",
                "A request parameter has an invalid value.",
                request,
                List.of(new InvalidJourneyRequestException.FieldViolation(
                        error.getParameter(),
                        error.getMessage()
                ))
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMethod(HttpServletRequest request) {
        return problem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "method-not-allowed",
                "Method not allowed",
                "METHOD_NOT_ALLOWED",
                "This endpoint does not support the requested HTTP method.",
                request,
                List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "not-found",
                "Resource not found",
                "NOT_FOUND",
                "The requested resource does not exist.",
                request,
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(Exception error, HttpServletRequest request) {
        log.error("Unhandled API error for {} {}", request.getMethod(), request.getRequestURI(), error);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Unexpected server error",
                "INTERNAL_ERROR",
                "We could not process this request. Please try again.",
                request,
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> problem(
            HttpStatus status,
            String type,
            String title,
            String code,
            String detail,
            HttpServletRequest request,
            List<InvalidJourneyRequestException.FieldViolation> errors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                "urn:traveldb:error:" + type,
                title,
                status.value(),
                code,
                detail,
                request.getRequestURI(),
                Instant.now(),
                errors
        );
        return ResponseEntity.status(status).contentType(PROBLEM_JSON).body(body);
    }
}
