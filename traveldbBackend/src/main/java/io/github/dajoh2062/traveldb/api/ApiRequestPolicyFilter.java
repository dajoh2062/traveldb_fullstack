package io.github.dajoh2062.traveldb.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Adds API security headers and rejects unexpectedly large request bodies before JSON parsing. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ApiRequestPolicyFilter extends OncePerRequestFilter {

    static final long MAX_REQUEST_BODY_BYTES = 64 * 1024L;

    private static final Set<String> METHODS_WITHOUT_BODIES = Set.of("GET", "HEAD", "OPTIONS");

    private final ObjectMapper objectMapper;

    public ApiRequestPolicyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        addSecurityHeaders(response);

        long contentLength = request.getContentLengthLong();
        if (!METHODS_WITHOUT_BODIES.contains(request.getMethod())
                && contentLength > MAX_REQUEST_BODY_BYTES) {
            writePayloadTooLarge(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        response.setHeader("Permissions-Policy", "camera=(), geolocation=(), microphone=()");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
    }

    private void writePayloadTooLarge(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                "urn:traveldb:error:payload-too-large",
                "Payload too large",
                HttpStatus.CONTENT_TOO_LARGE.value(),
                "PAYLOAD_TOO_LARGE",
                "Request bodies cannot exceed 64 KB.",
                request.getRequestURI(),
                Instant.now(),
                List.of()
        );

        response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
