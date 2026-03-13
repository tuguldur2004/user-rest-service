package com.example.userrest.repository;

import com.example.userrest.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUsername(String username);

    boolean existsByAuthUserId(Integer authUserId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
