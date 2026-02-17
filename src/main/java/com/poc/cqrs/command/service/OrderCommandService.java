package com.poc.cqrs.command.service;

import com.poc.cqrs.command.dto.CreateOrderCommand;
import com.poc.cqrs.command.dto.RemoveOrderItemCommand;
import com.poc.cqrs.command.dto.UpdateOrderStatusCommand;
import com.poc.cqrs.command.entity.Order;
import com.poc.cqrs.command.entity.OrderItem;
import com.poc.cqrs.command.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final MaterializedViewRefresher viewRefresher;

    public OrderCommandService(
            OrderRepository orderRepository,
            MaterializedViewRefresher viewRefresher
    ) {
        this.orderRepository = orderRepository;
        this.viewRefresher = viewRefresher;
    }

    @Transactional
    public UUID createOrder(CreateOrderCommand cmd) {
        var items = cmd.items().stream()
                .map(i -> OrderItem.create(i.product(), i.quantity(), i.unitPrice()))
                .toList();

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Pedido deve conter ao menos um item.");
        }

        var order = Order.create(cmd.customerName(), items);
        orderRepository.save(order);

        viewRefresher.refreshOrderSummaryAfterCommit();

        return order.getId();
    }

    @Transactional
    public void updateOrderStatus(UpdateOrderStatusCommand cmd) {
        var order = orderRepository.findById(cmd.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + cmd.orderId()));

        order.transitionTo(cmd.newStatus());
        orderRepository.save(order);

        viewRefresher.refreshOrderSummaryAfterCommit();
    }

    @Transactional
    public void removeOrderItem(RemoveOrderItemCommand cmd) {
        var order = orderRepository.findById(cmd.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + cmd.orderId()));

        order.removeItem(cmd.itemId());

        if (!order.hasItems()) {
            throw new IllegalStateException(
                    "Pedido ficaria sem itens. Cancele o pedido ao invés de remover o último item.");
        }

        orderRepository.save(order);

        viewRefresher.refreshOrderSummaryAfterCommit();
    }
}
