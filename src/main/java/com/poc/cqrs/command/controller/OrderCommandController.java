package com.poc.cqrs.command.controller;

import com.poc.cqrs.command.controller.api.OrderCommandApi;
import com.poc.cqrs.command.dto.CreateOrderCommand;
import com.poc.cqrs.command.dto.RemoveOrderItemCommand;
import com.poc.cqrs.command.dto.UpdateOrderStatusCommand;
import com.poc.cqrs.command.service.OrderCommandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class OrderCommandController implements OrderCommandApi {

    private final OrderCommandService commandService;

    public OrderCommandController(OrderCommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public ResponseEntity<Map<String, UUID>> create(
            @Valid @RequestBody CreateOrderCommand cmd
    ) {
        UUID orderId = commandService.createOrder(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("orderId", orderId));
    }

    @Override
    public ResponseEntity<Void> updateStatus(
            @Valid @RequestBody UpdateOrderStatusCommand cmd
    ) {
        commandService.updateOrderStatus(cmd);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeItem(
            @Valid @RequestBody RemoveOrderItemCommand cmd
    ) {
        commandService.removeOrderItem(cmd);
        return ResponseEntity.noContent().build();
    }
}
