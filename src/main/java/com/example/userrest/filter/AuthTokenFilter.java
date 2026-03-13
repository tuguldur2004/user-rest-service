package com.example.userrest.filter;

import com.example.userrest.dto.TokenValidationResult;
import com.example.userrest.service.SoapAuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Authentication middleware (Servlet Filter).
 *
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │ Incoming request │
 * │ │ │
 * │ ▼ │
 * │ Is path public? ──YES──▶ pass through │
 * │ │ NO │
 * │ ▼ │
 * │ Authorization: Bearer <token> present? ──NO──▶ 401 │
 * │ │ YES │
 * │ ▼ │
 * │ POST to SOAP /soap/auth ValidateToken(<token>) │
 * │ │ │
 * │ valid == true? ──NO──▶ 401 │
 * │ │ YES │
 * │ ▼ │
 * │ Set request attrs: authenticatedUserId, authenticatedUsername │
 * │ │ │
 * │ ▼ │
 * │ Pass to next filter / controller │
 * └────────────────────────────────────────────────────────────────────────┘
 *
 * Request attributes set on success:
 * - "authenticatedUserId" (Integer) — userId from the SOAP response
 * - "authenticatedUsername" (String) — username from the SOAP response
 *
 * These attributes are read by the controller to enforce ownership rules.
 */
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);

    /**
     * Paths that bypass authentication entirely.
     * Exact-match check is performed (no wildcards here).
     */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/health",
            "/h2-console");

    private final SoapAuthClient soapAuthClient;

    public AuthTokenFilter(SoapAuthClient soapAuthClient) {
        this.soapAuthClient = soapAuthClient;
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ── 1. Allow public paths (health, H2 console …) ──────────────────
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // ── 2. Require Authorization header for all /users actions ────────
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for {} {}", method, path);
            sendUnauthorized(response, "Authorization header is missing or invalid. " +
                    "Use: Authorization: Bearer <token>");
            return;
        }

        String token = authHeader.substring(7).trim();

        // ── 3. Delegate token validation to the SOAP Auth Service ──────────
        log.debug("Validating token via SOAP for {} {}", method, path);
        TokenValidationResult validation = soapAuthClient.validateToken(token);

        if (!validation.isValid()) {
            log.warn("Token validation failed for {} {}", method, path);
            sendUnauthorized(response, "Token is invalid or has expired");
            return;
        }

        // ── 4. Attach identity to the request for downstream use ───────────
        request.setAttribute("authenticatedUserId", validation.getUserId());
        request.setAttribute("authenticatedUsername", validation.getUsername());

        log.debug("Auth OK — user='{}' id={} — {} {}",
                validation.getUsername(), validation.getUserId(), method, path);

        chain.doFilter(request, response);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /** Writes a 401 JSON response in the standard {@code ApiResponse} envelope. */
    private void sendUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}
