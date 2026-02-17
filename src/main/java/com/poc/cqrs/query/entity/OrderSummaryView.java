package com.poc.cqrs.query.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_summary_mview")
@Immutable
@Getter // Getters apenas — sem setters, entidade imutável
public class OrderSummaryView {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "customer_name")
    private String customerName;

    private String status;

    private BigDecimal discount;

    @Column(name = "total_items")
    private Long totalItems;

    private BigDecimal subtotal;

    @Column(name = "total_with_discount")
    private BigDecimal totalWithDiscount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
