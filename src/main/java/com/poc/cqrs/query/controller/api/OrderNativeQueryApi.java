package com.poc.cqrs.query.controller.api;

import com.poc.cqrs.query.dto.OrderListView;
import com.poc.cqrs.query.dto.OrderWithItemsView;
import com.poc.cqrs.query.dto.StatusReportView;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Queries - JPQL Tipado")
@RequestMapping("/api/orders/query")
public interface OrderNativeQueryApi {

    @Operation(
            summary = "Listar pedidos paginado (JPQL + Record)",
            description = """
                    Listagem paginada usando **JPQL com projeção para record**.
                    
                    Demonstra que records também suportam `Page<T>` com paginação
                    e ordenação do Spring Data, igual a uma @Entity.
                    
                    Suporta `page`, `size` e `sort` (ex: `sort=createdAt,desc`).
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista paginada de pedidos")
            }
    )
    @GetMapping("/list")
    ResponseEntity<Page<OrderListView>> listOrders(@Parameter(hidden = true) Pageable pageable);

    @Operation(
            summary = "Detalhe do pedido com itens (JPQL + Record)",
            description = """
                    Busca um pedido com todos os seus itens usando **JPQL com projeção para record**.
                    
                    Demonstra que o Query Stack do CQRS **não depende de Materialized View**.
                    As queries usam `SELECT new Record(...)` no JpaRepository, sem @Entity de leitura.
                    
                    Retorno tipado: `OrderWithItemsView` composto por `OrderDetailView` + `List<OrderItemView>`.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pedido com itens"),
                    @ApiResponse(responseCode = "400", description = "Pedido não encontrado")
            }
    )
    @GetMapping("/{orderId}")
    ResponseEntity<OrderWithItemsView> getOrderDetail(
            @Parameter(description = "ID do pedido", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId);

    @Operation(
            summary = "Relatório por status (JPQL + Record)",
            description = """
                    Query analítica agrupada por status usando **JPQL com projeção para record**.
                    
                    Retorna `List<StatusReportView>` — tipado, validado em compile-time.
                    Sem Materialized View, sem JdbcTemplate, sem Map<String, Object>.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Relatório tipado por status")
            }
    )
    @GetMapping("/report/by-status")
    ResponseEntity<List<StatusReportView>> getReportByStatus();
}
