package com.poc.cqrs.query.controller.api;

import com.poc.cqrs.query.entity.OrderSummaryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Queries - Leitura")
@RequestMapping("/api/orders/view")
public interface OrderQueryApi {

    @Operation(
            summary = "Listar pedidos (Materialized View)",
            description = """
                    Retorna dados da **Materialized View** `order_summary_mview`.
                    Os dados já vêm pré-calculados do banco (total de itens, subtotal, desconto).
                    
                    Suporta **paginação** (page, size) e **ordenação** (sort=createdAt,desc).
                    
                    **Este endpoint não executa nenhuma regra de negócio** — é pura leitura otimizada.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista paginada de pedidos")
            }
    )
    @GetMapping
    ResponseEntity<Page<OrderSummaryView>> list(
            @Parameter(description = "Filtrar por status (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)",
                    example = "PENDING")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filtrar por nome do cliente (busca parcial, case-insensitive)",
                    example = "João")
            @RequestParam(required = false) String customer,

            @Parameter(hidden = true) Pageable pageable);

    @Operation(
            summary = "Buscar pedido por ID (Materialized View)",
            description = "Retorna os dados resumidos de um pedido específico da Materialized View.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
                    @ApiResponse(responseCode = "400", description = "Pedido não encontrado")
            }
    )
    @GetMapping("/{orderId}")
    ResponseEntity<OrderSummaryView> getById(
            @Parameter(description = "ID do pedido", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId);
}
