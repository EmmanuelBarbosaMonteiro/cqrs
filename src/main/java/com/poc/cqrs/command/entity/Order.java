package com.poc.cqrs.command.entity;

import com.poc.cqrs.command.enums.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    private static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("10.00");

    @Id
    private UUID id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal discount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Order() {}

    private Order(String customerName, List<OrderItem> items) {
        this.id = UUID.randomUUID();
        this.customerName = customerName;
        this.status = OrderStatus.PENDING;
        this.discount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        items.forEach(this::addItem);
        recalculate();
    }

    public static Order create(String customerName, List<OrderItem> items) {
        return new Order(customerName, items);
    }

    // --- Regras de Negócio encapsuladas no Aggregate ---

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public void removeItem(UUID itemId) {
        this.items.removeIf(i -> i.getId().equals(itemId));
        recalculate();
    }

    /**
     * Recalcula subtotal, aplica desconto se elegível e atualiza total.
     * Regra: pedidos acima de R$500 ganham 10% de desconto.
     */
    public void recalculate() {
        BigDecimal subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (subtotal.compareTo(DISCOUNT_THRESHOLD) > 0) {
            this.discount = DISCOUNT_PERCENTAGE;
            BigDecimal discountValue = subtotal
                    .multiply(DISCOUNT_PERCENTAGE)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            this.totalAmount = subtotal.subtract(discountValue);
        } else {
            this.discount = BigDecimal.ZERO;
            this.totalAmount = subtotal;
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Valida transições de estado permitidas.
     * Ex: SHIPPED não pode voltar a PENDING, nem ser CANCELLED.
     */
    public void transitionTo(OrderStatus newStatus) {
        if (this.status == newStatus) {
            throw new IllegalStateException("Pedido já está no status " + newStatus);
        }

        switch (this.status) {
            case PENDING -> {
                if (newStatus != OrderStatus.CONFIRMED && newStatus != OrderStatus.CANCELLED)
                    throw invalidTransition(newStatus);
            }
            case CONFIRMED -> {
                if (newStatus != OrderStatus.SHIPPED && newStatus != OrderStatus.CANCELLED)
                    throw invalidTransition(newStatus);
            }
            case SHIPPED -> {
                if (newStatus != OrderStatus.DELIVERED)
                    throw invalidTransition(newStatus);
            }
            case DELIVERED, CANCELLED -> throw invalidTransition(newStatus);
        }

        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    private IllegalStateException invalidTransition(OrderStatus target) {
        return new IllegalStateException(
                "Transição inválida: " + this.status + " -> " + target);
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    // --- Getters ---
    public UUID getId() { return id; }
    public String getCustomerName() { return customerName; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
