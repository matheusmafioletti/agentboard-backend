package com.agentboard.auth.dto;

import java.util.UUID;

/** Minimal board summary returned on tenant registration. */
public record BoardInfo(UUID id, String name) {}
