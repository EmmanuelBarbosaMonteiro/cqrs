package com.poc.cqrs.query.dto;

import com.poc.cqrs.command.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderListView(
        UUID orderId,
        String customerName,
        OrderStatus status,
        BigDecimal discount,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {}
