package edu.cit.lobitana.ajapartelle.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull(message = "active is required") Boolean active) {}
