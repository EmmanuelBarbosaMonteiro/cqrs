package com.poc.cqrs.query.service;

import com.poc.cqrs.query.dto.OrderDetailView;
import com.poc.cqrs.query.dto.OrderItemView;
import com.poc.cqrs.query.dto.StatusReportView;
import com.poc.cqrs.query.entity.OrderSummaryView;
import com.poc.cqrs.query.repository.OrderReadRepository;
import com.poc.cqrs.query.repository.OrderSummaryViewRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class QueryServiceConfig {

    // @Entity mapeada à Materialized View (suporta Specification + paginação)
    @Bean
    public EntityReadService<OrderSummaryView, UUID> orderSummaryReadService(
            OrderSummaryViewRepository repository
    ) {
        return new EntityReadService<>(repository);
    }

    // Record JPQL — relatório por status
    @Bean
    public ReadService<StatusReportView> statusReportReadService(
            OrderReadRepository repository
    ) {
        return JpqlReadService.<StatusReportView>builder()
                .findAll(repository::findStatusReport)
                .build();
    }

    // Record JPQL — detalhe do pedido
    @Bean
    public ReadService<OrderDetailView> orderDetailReadService(
            OrderReadRepository repository
    ) {
        return JpqlReadService.<OrderDetailView>builder()
                .findById(id -> repository.findOrderDetailById((UUID) id))
                .build();
    }

    // Record JPQL — itens do pedido
    @Bean
    public ReadService<OrderItemView> orderItemReadService(
            OrderReadRepository repository
    ) {
        return JpqlReadService.<OrderItemView>builder()
                .findListById(id -> repository.findItemsByOrderId((UUID) id))
                .build();
    }
}
