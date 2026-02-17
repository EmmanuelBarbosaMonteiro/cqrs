package com.poc.cqrs.command.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RemoveOrderItemCommand(
        @NotNull UUID orderId,
        @NotNull UUID itemId
) {}
