package edu.cit.lobitana.ajapartelle.auth.security;

import edu.cit.lobitana.ajapartelle.user.entity.Role;


/** The authenticated principal placed in the SecurityContext for each request. */
public record AuthUser(Long id, String email, Role role, String tokenId, java.time.Instant tokenExpiresAt) {}
