package edu.cit.lobitana.ajapartelle.security;

import edu.cit.lobitana.ajapartelle.entity.Role;

/** The authenticated principal placed in the SecurityContext for each request. */
public record AuthUser(Long id, String email, Role role, String tokenId, java.time.Instant tokenExpiresAt) {}
