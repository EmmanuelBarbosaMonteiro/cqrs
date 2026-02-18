package com.poc.cqrs.query.controller;

import com.poc.cqrs.query.controller.api.OrderNativeQueryApi;
import com.poc.cqrs.query.dto.OrderDetailView;
import com.poc.cqrs.query.dto.OrderItemView;
import com.poc.cqrs.query.dto.OrderListView;
import com.poc.cqrs.query.dto.OrderWithItemsView;
import com.poc.cqrs.query.dto.StatusReportView;
import com.poc.cqrs.query.service.ReadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class OrderNativeQueryController implements OrderNativeQueryApi {

    private final ReadService<OrderListView> orderListRead;
    private final ReadService<OrderDetailView> orderDetailRead;
    private final ReadService<OrderItemView> orderItemRead;
    private final ReadService<StatusReportView> statusReportRead;

    public OrderNativeQueryController(
            ReadService<OrderListView> orderListRead,
            ReadService<OrderDetailView> orderDetailRead,
            ReadService<OrderItemView> orderItemRead,
            ReadService<StatusReportView> statusReportRead
    ) {
        this.orderListRead = orderListRead;
        this.orderDetailRead = orderDetailRead;
        this.orderItemRead = orderItemRead;
        this.statusReportRead = statusReportRead;
    }

    @Override
    public ResponseEntity<Page<OrderListView>> listOrders(Pageable pageable) {
        return ResponseEntity.ok(orderListRead.findAll(pageable));
    }

    @Override
    public ResponseEntity<OrderWithItemsView> getOrderDetail(@PathVariable UUID orderId) {
        var order = orderDetailRead.findById(orderId);
        var items = orderItemRead.findListById(orderId);
        return ResponseEntity.ok(new OrderWithItemsView(order, items));
    }

    @Override
    public ResponseEntity<List<StatusReportView>> getReportByStatus() {
        return ResponseEntity.ok(statusReportRead.findAll());
    }
}
