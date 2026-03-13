package com.example.userrest.service;

import com.example.userrest.dto.CreateUserRequest;
import com.example.userrest.dto.UpdateUserRequest;
import com.example.userrest.model.UserProfile;
import com.example.userrest.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Business logic layer for user profile CRUD.
 *
 * Authentication is intentionally NOT handled here; it is the
 * responsibility of {@link com.example.userrest.filter.AuthTokenFilter}
 * (which delegates to the SOAP Auth Service) before any request
 * reaches this service.
 */
@Service
@Transactional
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new profile.
     * The caller must have already registered an account in the SOAP Auth
     * Service; the username is used as the link between both services.
     *
     * @throws IllegalArgumentException if username or email is already taken
     */
    public UserProfile create(CreateUserRequest req) {
        if (req.getAuthUserId() == null || req.getAuthUserId() <= 0) {
            throw new IllegalArgumentException("authUserId is required");
        }
        if (repository.existsByAuthUserId(req.getAuthUserId())) {
            throw new IllegalArgumentException("Auth user id '" + req.getAuthUserId() + "' already has a profile");
        }
        if (repository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username '" + req.getUsername() + "' is already taken");
        }
        if (repository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email '" + req.getEmail() + "' is already registered");
        }

        UserProfile profile = new UserProfile();
        profile.setUsername(req.getUsername());
        profile.setAuthUserId(req.getAuthUserId());
        profile.setEmail(req.getEmail());
        profile.setName(req.getName());
        profile.setBio(req.getBio());
        profile.setPhone(req.getPhone());
        profile.setAvatar(req.getAvatar());
        profile.setLocation(req.getLocation());
        profile.setWebsite(req.getWebsite());

        return repository.save(profile);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<UserProfile> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Partially updates a profile — only non-null fields in the request are
     * applied.
     *
     * @throws RuntimeException if no profile with the given id exists
     */
    public UserProfile update(Long id, UpdateUserRequest req) {
        UserProfile profile = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with id=" + id + " not found"));

        if (req.getEmail() != null)
            profile.setEmail(req.getEmail());
        if (req.getName() != null)
            profile.setName(req.getName());
        if (req.getBio() != null)
            profile.setBio(req.getBio());
        if (req.getPhone() != null)
            profile.setPhone(req.getPhone());
        if (req.getAvatar() != null)
            profile.setAvatar(req.getAvatar());
        if (req.getLocation() != null)
            profile.setLocation(req.getLocation());
        if (req.getWebsite() != null)
            profile.setWebsite(req.getWebsite());

        return repository.save(profile);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Deletes a profile by id.
     *
     * @throws RuntimeException if the profile does not exist
     */
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("User with id=" + id + " not found");
        }
        repository.deleteById(id);
    }
}
