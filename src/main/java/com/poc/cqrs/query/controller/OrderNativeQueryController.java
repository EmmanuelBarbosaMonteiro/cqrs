package com.poc.cqrs.query.controller;

import com.poc.cqrs.command.enums.OrderStatus;
import com.poc.cqrs.query.controller.api.OrderNativeQueryApi;
import com.poc.cqrs.query.dto.OrderSummaryJpqlView;
import com.poc.cqrs.query.repository.OrderReadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class OrderNativeQueryController implements OrderNativeQueryApi {

    private final OrderReadRepository readRepository;

    public OrderNativeQueryController(OrderReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public ResponseEntity<Page<OrderSummaryJpqlView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customer,
            Pageable pageable
    ) {
        OrderStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = OrderStatus.valueOf(status.toUpperCase());
        }

        Page<OrderSummaryJpqlView> page = readRepository.findAllSummaryFiltered(
                statusEnum, customer, pageable);
        return ResponseEntity.ok(page);
    }

    @Override
    public ResponseEntity<OrderSummaryJpqlView> getById(@PathVariable UUID orderId) {
        var summary = readRepository.findSummaryById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + orderId));
        return ResponseEntity.ok(summary);
    }
}
