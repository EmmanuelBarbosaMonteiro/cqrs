package com.poc.cqrs.query.dto;

import com.poc.cqrs.command.enums.OrderStatus;

import java.math.BigDecimal;

public record StatusReportView(
        OrderStatus status,
        Long totalOrders,
        BigDecimal totalRevenue,
        Double avgOrderValue
) {}
