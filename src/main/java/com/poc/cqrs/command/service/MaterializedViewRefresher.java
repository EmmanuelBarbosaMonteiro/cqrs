package com.poc.cqrs.command.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MaterializedViewRefresher {

    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    public MaterializedViewRefresher(
            ApplicationEventPublisher eventPublisher,
            JdbcTemplate jdbcTemplate
    ) {
        this.eventPublisher = eventPublisher;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void refreshOrderSummaryAfterCommit() {
        eventPublisher.publishEvent(new OrderDataChangedEvent());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderDataChanged(OrderDataChangedEvent event) {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY order_summary_mview");
    }
    
    public static class OrderDataChangedEvent {
    }
}
