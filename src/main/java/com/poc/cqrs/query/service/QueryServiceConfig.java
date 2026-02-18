package com.poc.cqrs.query.service;

import com.poc.cqrs.query.entity.OrderSummaryView;
import com.poc.cqrs.query.repository.OrderSummaryViewRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class QueryServiceConfig {

    @Bean
    public EntityReadService<OrderSummaryView, UUID> orderSummaryReadService(
            OrderSummaryViewRepository repository
    ) {
        return new EntityReadService<>(repository);
    }
}
