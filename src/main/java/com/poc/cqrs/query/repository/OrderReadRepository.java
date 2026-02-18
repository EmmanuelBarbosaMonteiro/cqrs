package com.poc.cqrs.query.repository;

import com.poc.cqrs.command.entity.Order;
import com.poc.cqrs.query.dto.OrderDetailView;
import com.poc.cqrs.query.dto.OrderItemView;
import com.poc.cqrs.query.dto.OrderListView;
import com.poc.cqrs.query.dto.StatusReportView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderReadRepository extends JpaRepository<Order, UUID> {

    @Query("""
            SELECT new com.poc.cqrs.query.dto.OrderListView(
                o.id,
                o.customerName,
                o.status,
                o.discount,
                o.totalAmount,
                o.createdAt
            )
            FROM Order o
            """)
    Page<OrderListView> findAllOrders(Pageable pageable);

    @Query("""
            SELECT new com.poc.cqrs.query.dto.StatusReportView(
                o.status,
                COUNT(o),
                COALESCE(SUM(o.totalAmount), 0),
                COALESCE(AVG(o.totalAmount), 0)
            )
            FROM Order o
            GROUP BY o.status
            ORDER BY SUM(o.totalAmount) DESC
            """)
    List<StatusReportView> findStatusReport();

    @Query("""
            SELECT new com.poc.cqrs.query.dto.OrderDetailView(
                o.id,
                o.customerName,
                o.status,
                o.discount,
                o.totalAmount,
                o.createdAt
            )
            FROM Order o
            WHERE o.id = :orderId
            """)
    Optional<OrderDetailView> findOrderDetailById(@Param("orderId") UUID orderId);

    @Query("""
            SELECT new com.poc.cqrs.query.dto.OrderItemView(
                i.id,
                i.product,
                i.quantity,
                i.unitPrice,
                i.unitPrice * i.quantity
            )
            FROM OrderItem i
            WHERE i.order.id = :orderId
            ORDER BY i.createdAt
            """)
    List<OrderItemView> findItemsByOrderId(@Param("orderId") UUID orderId);
}
