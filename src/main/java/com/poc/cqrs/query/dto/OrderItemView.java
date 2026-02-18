package com.poc.cqrs.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemView(
        UUID itemId,
        String product,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
