package edu.cit.lobitana.ajapartelle.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull(message = "active is required") Boolean active) {}
