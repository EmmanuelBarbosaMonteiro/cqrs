package com.poc.cqrs.command.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderCommand(
        @NotBlank String customerName,
        @NotEmpty @Valid List<OrderItemCommand> items
) {
    public record OrderItemCommand(
            @NotBlank String product,
            int quantity,
            java.math.BigDecimal unitPrice
    ) {}
}
