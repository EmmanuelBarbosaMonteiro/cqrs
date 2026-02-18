package com.poc.cqrs.query.dto;

import java.util.List;

public record OrderWithItemsView(
        OrderDetailView order,
        List<OrderItemView> items
) {}
