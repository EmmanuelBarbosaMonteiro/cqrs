package com.poc.cqrs.query.repository;

import com.poc.cqrs.query.entity.OrderSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface OrderSummaryViewRepository
        extends JpaRepository<OrderSummaryView, UUID>, JpaSpecificationExecutor<OrderSummaryView> {
}
