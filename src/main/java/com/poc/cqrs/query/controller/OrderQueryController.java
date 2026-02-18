package com.poc.cqrs.query.controller;

import com.poc.cqrs.query.controller.api.OrderQueryApi;
import com.poc.cqrs.query.entity.OrderSummaryView;
import com.poc.cqrs.query.service.EntityReadService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

@RestController
public class OrderQueryController implements OrderQueryApi {

    private final EntityReadService<OrderSummaryView, UUID> readService;

    public OrderQueryController(EntityReadService<OrderSummaryView, UUID> readService) {
        this.readService = readService;
    }

    @Override
    public ResponseEntity<Page<OrderSummaryView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customer,
            Pageable pageable
    ) {
        Specification<OrderSummaryView> spec = buildSpecification(status, customer);
        Page<OrderSummaryView> page = readService.findAll(spec, pageable);
        return ResponseEntity.ok(page);
    }

    @Override
    public ResponseEntity<OrderSummaryView> getById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(readService.findById(orderId));
    }

    private Specification<OrderSummaryView> buildSpecification(String status, String customer) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }
            if (customer != null && !customer.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("customerName")),
                        "%" + customer.toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
