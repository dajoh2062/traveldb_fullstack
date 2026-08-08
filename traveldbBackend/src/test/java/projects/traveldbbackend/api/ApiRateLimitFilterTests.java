package projects.traveldbbackend.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRateLimitFilterTests {

    @Test
    void rejectsRequestsAboveThePerClientWriteLimit() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(10, 1, 10, 100, 60_000L);

        MockHttpServletResponse first = perform(filter, "POST", "/api/journey/check", "203.0.113.10");
        MockHttpServletResponse second = perform(filter, "POST", "/api/journey/check", "203.0.113.10");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(429);
        assertThat(second.getHeader("Retry-After")).isNotBlank();
        assertThat(second.getContentType()).startsWith("application/problem+json");
        assertThat(second.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void globalLimitStillAppliesWhenClientAddressesChange() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(10, 10, 1, 100, 60_000L);

        MockHttpServletResponse first = perform(filter, "GET", "/api/countries", "203.0.113.10");
        MockHttpServletResponse second = perform(filter, "GET", "/api/countries", "203.0.113.11");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(429);
    }

    @Test
    void healthChecksAreNotRateLimited() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(1, 1, 1, 1, 60_000L);

        MockHttpServletResponse first = perform(filter, "GET", "/api/health", "203.0.113.10");
        MockHttpServletResponse second = perform(filter, "GET", "/api/health", "203.0.113.10");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletResponse perform(
            ApiRateLimitFilter filter,
            String method,
            String path,
            String clientAddress
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("X-Forwarded-For", clientAddress + ", 198.51.100.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
