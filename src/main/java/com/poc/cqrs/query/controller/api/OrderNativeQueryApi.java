package com.poc.cqrs.query.controller.api;

import com.poc.cqrs.query.dto.OrderSummaryJpqlView;
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

@Tag(name = "Queries - JPQL Tipado")
@RequestMapping("/api/orders/jpql")
public interface OrderNativeQueryApi {

    @Operation(
            summary = "Listar pedidos (JPQL + Record)",
            description = """
                    Mesmo resultado do endpoint `/api/orders/view`, mas usando
                    **JPQL com JOIN calculado em tempo de execução** ao invés de Materialized View.

                    Retorna `Page<OrderSummaryJpqlView>` — record tipado com os mesmos campos:
                    total de itens, subtotal, desconto e total com desconto.

                    Suporta **paginação** (page, size) e **ordenação** (sort=createdAt,desc).
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista paginada de pedidos")
            }
    )
    @GetMapping
    ResponseEntity<Page<OrderSummaryJpqlView>> list(
            @Parameter(description = "Filtrar por status (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)",
                    example = "PENDING")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filtrar por nome do cliente (busca parcial, case-insensitive)",
                    example = "João")
            @RequestParam(required = false) String customer,

            @Parameter(hidden = true) Pageable pageable);

    @Operation(
            summary = "Buscar pedido por ID (JPQL + Record)",
            description = """
                    Mesmo resultado do endpoint `/api/orders/view/{orderId}`, mas via JPQL.
                    Retorna os dados resumidos com JOIN calculado em tempo real.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
                    @ApiResponse(responseCode = "400", description = "Pedido não encontrado")
            }
    )
    @GetMapping("/{orderId}")
    ResponseEntity<OrderSummaryJpqlView> getById(
            @Parameter(description = "ID do pedido", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId);
}
