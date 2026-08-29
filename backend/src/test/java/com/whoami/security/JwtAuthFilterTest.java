package com.whoami.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.whoami.config.JwtProperties;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtAuthFilterTest {

    private static final String SECRET = "unit-test-jwt-secret-0123456789abcdef";

    private JwtService jwtService;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 7200));
        filter = new JwtAuthFilter(jwtService);
    }

    @Test
    void missingHeaderReturns401Envelope() throws Exception {
        MockHttpServletRequest request = request("POST", "/admin/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":401");
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        MockHttpServletRequest request = request("POST", "/admin/api/auth/me");
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void validTokenSetsAdminIdAndContinuesChain() throws Exception {
        String token = jwtService.issue(11L);
        MockHttpServletRequest request = request("GET", "/admin/api/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (req, res) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute("adminId")).isEqualTo(11L);
    }

    @Test
    void loginPathIsPublic() throws Exception {
        MockHttpServletRequest request = request("POST", "/admin/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (req, res) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void healthPathIsPublic() throws Exception {
        MockHttpServletRequest request = request("GET", "/admin/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (req, res) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isTrue();
    }

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }
}
