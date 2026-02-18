package com.poc.cqrs.query.repository;

import com.poc.cqrs.command.entity.Order;
import com.poc.cqrs.command.enums.OrderStatus;
import com.poc.cqrs.query.dto.OrderSummaryJpqlView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderReadRepository extends JpaRepository<Order, UUID> {

    @Query("""
            SELECT new com.poc.cqrs.query.dto.OrderSummaryJpqlView(
                o.id,
                o.customerName,
                o.status,
                o.discount,
                COUNT(i),
                COALESCE(SUM(i.unitPrice * i.quantity), 0),
                o.totalAmount,
                o.createdAt,
                o.updatedAt
            )
            FROM Order o
            LEFT JOIN o.items i
            WHERE (:status IS NULL OR o.status = :status)
              AND (:customer IS NULL OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', CAST(:customer AS string), '%')))
            GROUP BY o.id, o.customerName, o.status, o.discount,
                     o.totalAmount, o.createdAt, o.updatedAt
            """)
    Page<OrderSummaryJpqlView> findAllSummaryFiltered(
            @Param("status") OrderStatus status,
            @Param("customer") String customer,
            Pageable pageable);

    @Query("""
            SELECT new com.poc.cqrs.query.dto.OrderSummaryJpqlView(
                o.id,
                o.customerName,
                o.status,
                o.discount,
                COUNT(i),
                COALESCE(SUM(i.unitPrice * i.quantity), 0),
                o.totalAmount,
                o.createdAt,
                o.updatedAt
            )
            FROM Order o
            LEFT JOIN o.items i
            WHERE o.id = :orderId
            GROUP BY o.id, o.customerName, o.status, o.discount,
                     o.totalAmount, o.createdAt, o.updatedAt
            """)
    Optional<OrderSummaryJpqlView> findSummaryById(@Param("orderId") UUID orderId);
}
