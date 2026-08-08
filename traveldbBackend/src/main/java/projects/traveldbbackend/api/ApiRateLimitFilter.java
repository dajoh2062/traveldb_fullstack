package projects.traveldbbackend.api;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory protection for the single Render instance.
 *
 * <p>The global bucket bounds total API work even when callers rotate IP addresses, while the
 * per-client buckets stop one caller from consuming the whole allowance. Counters are deliberately
 * bounded so spoofed forwarding headers cannot grow the map indefinitely.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    static final int DEFAULT_READS_PER_MINUTE = 60;
    static final int DEFAULT_WRITES_PER_MINUTE = 10;
    static final int DEFAULT_GLOBAL_REQUESTS_PER_MINUTE = 120;
    static final int DEFAULT_MAX_TRACKED_CLIENTS = 10_000;
    static final long DEFAULT_WINDOW_MILLIS = 60_000L;

    private static final String RATE_LIMIT_BODY = """
            {"type":"urn:traveldb:error:rate-limit","title":"Too Many Requests","status":429,"detail":"Too many requests. Please try again shortly."}
            """.trim();

    private final int readsPerWindow;
    private final int writesPerWindow;
    private final int globalRequestsPerWindow;
    private final int maxTrackedClients;
    private final long windowMillis;
    private final WindowCounter globalCounter;
    private final Map<String, WindowCounter> clientCounters = new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong();

    public ApiRateLimitFilter() {
        this(
                DEFAULT_READS_PER_MINUTE,
                DEFAULT_WRITES_PER_MINUTE,
                DEFAULT_GLOBAL_REQUESTS_PER_MINUTE,
                DEFAULT_MAX_TRACKED_CLIENTS,
                DEFAULT_WINDOW_MILLIS
        );
    }

    ApiRateLimitFilter(
            int readsPerWindow,
            int writesPerWindow,
            int globalRequestsPerWindow,
            int maxTrackedClients,
            long windowMillis
    ) {
        this.readsPerWindow = requirePositive(readsPerWindow, "readsPerWindow");
        this.writesPerWindow = requirePositive(writesPerWindow, "writesPerWindow");
        this.globalRequestsPerWindow = requirePositive(globalRequestsPerWindow, "globalRequestsPerWindow");
        this.maxTrackedClients = requirePositive(maxTrackedClients, "maxTrackedClients");
        this.windowMillis = requirePositive(windowMillis, "windowMillis");
        this.globalCounter = new WindowCounter(windowMillis);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        cleanExpiredCountersOccasionally(now);

        boolean readRequest = request.getMethod().equals("GET") || request.getMethod().equals("HEAD");
        int clientLimit = readRequest ? readsPerWindow : writesPerWindow;
        String clientKey = (readRequest ? "read:" : "write:") + clientAddress(request);

        WindowCounter clientCounter = clientCounters.get(clientKey);
        if (clientCounter == null) {
            if (clientCounters.size() >= maxTrackedClients) {
                writeRateLimitResponse(response, clientLimit, 0, secondsUntil(now + windowMillis, now));
                return;
            }
            clientCounter = clientCounters.computeIfAbsent(clientKey, ignored -> new WindowCounter(windowMillis));
        }

        RateLimitDecision clientDecision = clientCounter.tryAcquire(clientLimit, now);
        if (!clientDecision.allowed()) {
            writeRateLimitResponse(response, clientLimit, 0, clientDecision.retryAfterSeconds());
            return;
        }

        RateLimitDecision globalDecision = globalCounter.tryAcquire(globalRequestsPerWindow, now);
        if (!globalDecision.allowed()) {
            writeRateLimitResponse(
                    response,
                    globalRequestsPerWindow,
                    0,
                    globalDecision.retryAfterSeconds()
            );
            return;
        }

        response.setHeader("X-RateLimit-Limit", Integer.toString(clientLimit));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(clientDecision.remaining()));
        filterChain.doFilter(request, response);
    }

    private void cleanExpiredCountersOccasionally(long now) {
        if ((requestSequence.incrementAndGet() & 255L) == 0L) {
            clientCounters.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        }
    }

    private static String clientAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            int comma = forwardedFor.indexOf(',');
            String firstAddress = (comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
            if (!firstAddress.isEmpty()) {
                return truncate(firstAddress, 64);
            }
        }

        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank()
                ? "unknown"
                : truncate(remoteAddress.trim(), 64);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static void writeRateLimitResponse(
            HttpServletResponse response,
            int limit,
            int remaining,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setHeader("X-RateLimit-Limit", Integer.toString(limit));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(remaining));
        response.getWriter().write(RATE_LIMIT_BODY);
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static long requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static long secondsUntil(long windowEndsAt, long now) {
        return Math.max(1L, (windowEndsAt - now + 999L) / 1_000L);
    }

    private record RateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {
    }

    private static final class WindowCounter {
        private final long windowMillis;
        private long windowEndsAt;
        private int requestCount;

        private WindowCounter(long windowMillis) {
            this.windowMillis = windowMillis;
        }

        synchronized RateLimitDecision tryAcquire(int limit, long now) {
            if (windowEndsAt <= now) {
                windowEndsAt = now + windowMillis;
                requestCount = 0;
            }

            long retryAfterSeconds = secondsUntil(windowEndsAt, now);
            if (requestCount >= limit) {
                return new RateLimitDecision(false, 0, retryAfterSeconds);
            }

            requestCount++;
            return new RateLimitDecision(true, limit - requestCount, retryAfterSeconds);
        }

        synchronized boolean isExpired(long now) {
            return windowEndsAt <= now;
        }
    }
}
