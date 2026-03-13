package com.example.userrest.controller;

import com.example.userrest.dto.ApiResponse;
import com.example.userrest.dto.CreateUserRequest;
import com.example.userrest.dto.UpdateUserRequest;
import com.example.userrest.model.UserProfile;
import com.example.userrest.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile CRUD.
 *
 * Authentication is enforced upstream by
 * {@link com.example.userrest.filter.AuthTokenFilter}.
 * By the time a request reaches this controller, the following attributes are
 * guaranteed to be present (for protected endpoints):
 *
 * request.getAttribute("authenticatedUserId") → Integer
 * request.getAttribute("authenticatedUsername") → String
 *
 * Endpoint summary:
 * POST /users — create profile (public)
 * GET /users/:id — read profile by numeric id (auth required)
 * GET /users/name/:username — read profile by username (auth required)
 * PUT /users/:id — update profile (auth required, owner only)
 * DELETE /users/:id — delete profile (auth required, owner only)
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE — POST /users
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new user profile.
     *
     * This endpoint is public: users first register via the SOAP Auth Service
     * to obtain credentials, then call this endpoint to build their profile.
     *
     * @param req profile data (username + email required)
     * @return 201 Created with the new profile, or 409 Conflict on duplicate
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserProfile>> createUser(
            @Valid @RequestBody CreateUserRequest req,
            HttpServletRequest request) {

        Object authUserId = request.getAttribute("authenticatedUserId");
        Object authUsername = request.getAttribute("authenticatedUsername");

        if (!(authUserId instanceof Integer) || authUsername == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized request"));
        }

        // Always bind profile identity to the validated token identity
        // (prevents null auth_user_id inserts and username spoofing).
        req.setAuthUserId((Integer) authUserId);
        req.setUsername(authUsername.toString());

        try {
            UserProfile created = userProfileService.create(req);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(created, "User profile created successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ — GET /users/:id
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves a profile by its numeric database id.
     * Requires a valid Bearer token.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfile>> getUserById(@PathVariable Long id) {
        return userProfileService.findById(id)
                .map(u -> ResponseEntity.ok(ApiResponse.ok(u, "User found")))
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User with id=" + id + " not found")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ — GET /users/name/:username
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves a profile by username (the link to the Auth Service identity).
     * Requires a valid Bearer token.
     */
    @GetMapping("/name/{username}")
    public ResponseEntity<ApiResponse<UserProfile>> getUserByUsername(
            @PathVariable String username) {

        return userProfileService.findByUsername(username)
                .map(u -> ResponseEntity.ok(ApiResponse.ok(u, "User found")))
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User '" + username + "' not found")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UPDATE — PUT /users/:id
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Updates an existing profile.
     * Only the authenticated user (whose token was validated by the SOAP service)
     * may modify their own profile.
     *
     * @param id      profile id to update
     * @param req     fields to update (all optional — only non-null values applied)
     * @param request used to read the authenticated userId set by the filter
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfile>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req,
            HttpServletRequest request) {

        if (!isOwner(id, request)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only update your own profile"));
        }

        try {
            UserProfile updated = userProfileService.update(id, req);
            return ResponseEntity.ok(ApiResponse.ok(updated, "Profile updated successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE — DELETE /users/:id
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Deletes a profile.
     * Only the owner may delete their own profile.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        if (!isOwner(id, request)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only delete your own profile"));
        }

        try {
            userProfileService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "User profile deleted"));
        } catch (RuntimeException ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the authenticated username (set by the filter from SOAP
     * ValidateToken response) matches the username of the profile being
     * operated on.
     *
     * Using username avoids coupling DB identity values across services.
     * If no auth username attribute is present (filter was bypassed somehow),
     * access
     * is denied for safety.
     */
    private boolean isOwner(Long profileId, HttpServletRequest request) {
        Object authUsername = request.getAttribute("authenticatedUsername");
        if (authUsername == null)
            return false;

        return userProfileService.findById(profileId)
                .map(profile -> profile.getUsername() != null
                        && profile.getUsername().equals(authUsername.toString()))
                .orElse(false);
    }
}
