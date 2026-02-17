package com.poc.cqrs.command.dto;

import com.poc.cqrs.command.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateOrderStatusCommand(
        @NotNull UUID orderId,
        @NotNull OrderStatus newStatus
) {}
