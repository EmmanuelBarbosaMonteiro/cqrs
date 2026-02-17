package com.poc.cqrs.command.controller.api;

import com.poc.cqrs.command.dto.CreateOrderCommand;
import com.poc.cqrs.command.dto.RemoveOrderItemCommand;
import com.poc.cqrs.command.dto.UpdateOrderStatusCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Commands - Escrita")
@RequestMapping("/api/orders")
public interface OrderCommandApi {

    @Operation(
            summary = "Criar novo pedido",
            description = """
                    Cria um pedido com itens. Regras aplicadas automaticamente:
                    - Pedidos acima de R$500 ganham **10% de desconto**
                    - Status inicial: PENDING
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            name = "Pedido com desconto",
                            value = """
                                    {
                                      "customerName": "João Silva",
                                      "items": [
                                        { "product": "Notebook Dell", "quantity": 1, "unitPrice": 3500.00 },
                                        { "product": "Mouse Logitech", "quantity": 2, "unitPrice": 150.00 }
                                      ]
                                    }
                                    """
                    ))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos")
            }
    )
    @PostMapping
    ResponseEntity<Map<String, UUID>> create(@Valid @RequestBody CreateOrderCommand cmd);

    @Operation(
            summary = "Atualizar status do pedido",
            description = """
                    Transições de estado válidas:
                    - PENDING → CONFIRMED | CANCELLED
                    - CONFIRMED → SHIPPED | CANCELLED
                    - SHIPPED → DELIVERED
                    - DELIVERED e CANCELLED são estados terminais
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            name = "Confirmar pedido",
                            value = """
                                    {
                                      "orderId": "550e8400-e29b-41d4-a716-446655440000",
                                      "newStatus": "CONFIRMED"
                                    }
                                    """
                    ))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Status atualizado"),
                    @ApiResponse(responseCode = "400", description = "Pedido não encontrado"),
                    @ApiResponse(responseCode = "409", description = "Transição de estado inválida")
            }
    )
    @PatchMapping("/status")
    ResponseEntity<Void> updateStatus(@Valid @RequestBody UpdateOrderStatusCommand cmd);

    @Operation(
            summary = "Remover item do pedido",
            description = """
                    Remove um item e recalcula o total do pedido.
                    Não permite remover o último item — cancele o pedido ao invés disso.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            name = "Remover item",
                            value = """
                                    {
                                      "orderId": "550e8400-e29b-41d4-a716-446655440000",
                                      "itemId": "660e8400-e29b-41d4-a716-446655440000"
                                    }
                                    """
                    ))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Item removido"),
                    @ApiResponse(responseCode = "400", description = "Pedido não encontrado"),
                    @ApiResponse(responseCode = "409", description = "Pedido ficaria sem itens")
            }
    )
    @DeleteMapping("/items")
    ResponseEntity<Void> removeItem(@Valid @RequestBody RemoveOrderItemCommand cmd);
}
