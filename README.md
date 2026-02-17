# CQRS Order Management - Prova de Conceito

Prova de Conceito de arquitetura **Soft CQRS** utilizando **Spring Boot 3**, **Spring Data JPA**, **Flyway** e **Materialized Views do PostgreSQL**.

## O que é Soft CQRS?

No CQRS (Command Query Responsibility Segregation), separamos a aplicação em dois lados:

| | Command (Escrita) | Query (Leitura) |
|---|---|---|
| **Responsabilidade** | Regras de negócio, validações, transições de estado | Leitura otimizada, sem lógica |
| **Modelo de dados** | Tabelas normalizadas (`orders`, `order_items`) | Materialized View pré-calculada (`order_summary_mview`) |
| **Serviço** | `OrderCommandService` — específico, com regras complexas | `GenericQueryService<T, ID>` — genérico, reaproveitável para N views |
| **Benefício** | Foco total no domínio | Foco total em performance de leitura |

A palavra **"Soft"** indica que ambos os lados compartilham o mesmo banco de dados (não há event sourcing nem bancos separados), mas a separação lógica já traz grandes benefícios de design.

---

## Pré-requisitos

- **Java 21+**
- **Maven 3.9+**
- **PostgreSQL 15+**

### Criando o banco

```sql
CREATE DATABASE cqrs_orders;
```

A configuração padrão espera PostgreSQL em `localhost:5432` com usuário `postgres` / senha `postgres`. Ajuste em `src/main/resources/application.yml` se necessário.

---

## Como rodar

```bash
mvn spring-boot:run
```

O Flyway cria automaticamente as tabelas e a Materialized View na primeira execução.

---

## Swagger UI

Após iniciar a aplicação, acesse:

```
http://localhost:8080/swagger-ui.html
```

Os endpoints estão organizados em duas seções:
- **Commands - Escrita**: operações que alteram estado
- **Queries - Leitura**: consultas na Materialized View

---

## Guia de uso passo a passo (para apresentação)

### Passo 1 — Criar um pedido COM desconto (total > R$500)

**POST** `/api/orders`

```json
{
  "customerName": "João Silva",
  "items": [
    { "product": "Notebook Dell Inspiron", "quantity": 1, "unitPrice": 3500.00 },
    { "product": "Mouse Logitech MX Master", "quantity": 2, "unitPrice": 150.00 },
    { "product": "Teclado Mecânico Redragon", "quantity": 1, "unitPrice": 280.00 }
  ]
}
```

> Subtotal: R$ 4.080,00 — como é acima de R$500, aplica **10% de desconto** automaticamente.
> Total final: **R$ 3.672,00**

Resposta esperada:
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000"
}
```

Guarde o `orderId` retornado para os próximos passos.

---

### Passo 2 — Criar um pedido SEM desconto (total <= R$500)

**POST** `/api/orders`

```json
{
  "customerName": "Maria Oliveira",
  "items": [
    { "product": "Cabo HDMI 2m", "quantity": 3, "unitPrice": 35.00 },
    { "product": "Mousepad Gamer", "quantity": 1, "unitPrice": 89.90 }
  ]
}
```

> Subtotal: R$ 194,90 — sem desconto.
> Total final: **R$ 194,90**

---

### Passo 3 — Listar pedidos (Query na Materialized View)

**GET** `/api/orders/view`

Parâmetros opcionais:
- `status` — filtrar por status (ex: `PENDING`)
- `customer` — busca parcial no nome (ex: `João`)
- `page` — página (começa em 0)
- `size` — itens por página (padrão 20)
- `sort` — ordenação (ex: `createdAt,desc`)

Exemplos:
```
GET /api/orders/view?page=0&size=10
GET /api/orders/view?status=PENDING
GET /api/orders/view?customer=Maria&sort=createdAt,desc
```

---

### Passo 4 — Confirmar um pedido (transição de estado)

**PATCH** `/api/orders/{orderId}/status?status=CONFIRMED`

Use o `orderId` do Passo 1.

---

### Passo 5 — Enviar o pedido

**PATCH** `/api/orders/{orderId}/status?status=SHIPPED`

---

### Passo 6 — Tentar cancelar pedido já enviado (erro esperado)

**PATCH** `/api/orders/{orderId}/status?status=CANCELLED`

Resposta esperada (**409 Conflict**):
```json
{
  "error": "Transição inválida: SHIPPED -> CANCELLED"
}
```

> Isso demonstra a máquina de estados protegendo as regras de negócio no Command Stack.

---

### Passo 7 — Entregar o pedido

**PATCH** `/api/orders/{orderId}/status?status=DELIVERED`

---

### Passo 8 — Remover item de um pedido

Primeiro, crie um pedido com múltiplos itens:

**POST** `/api/orders`

```json
{
  "customerName": "Carlos Souza",
  "items": [
    { "product": "Monitor LG 27\"", "quantity": 1, "unitPrice": 1800.00 },
    { "product": "Suporte de Monitor", "quantity": 1, "unitPrice": 120.00 },
    { "product": "Webcam HD", "quantity": 1, "unitPrice": 250.00 }
  ]
}
```

Depois, consulte o pedido na view para ver os IDs dos itens e use:

**DELETE** `/api/orders/{orderId}/items/{itemId}`

> O total será recalculado automaticamente. Se tentar remover o último item, retorna **409 Conflict**.

---

### Passo 9 — Verificar o resultado na Materialized View

**GET** `/api/orders/view`

Todos os dados já vêm prontos: nome do cliente, status, total de itens, subtotal, desconto e total com desconto — sem JOINs na aplicação, tudo pré-calculado no banco.

---

## Máquina de Estados dos Pedidos

```
PENDING ──→ CONFIRMED ──→ SHIPPED ──→ DELIVERED
   │              │
   └──→ CANCELLED ←──┘
```

- `DELIVERED` e `CANCELLED` são estados **terminais** (sem transição possível).

---

## Estrutura do Projeto

```
src/main/java/com/poc/cqrs/
├── command/                          ← WRITE SIDE
│   ├── controller/
│   │   ├── api/OrderCommandApi.java     (interface Swagger)
│   │   └── OrderCommandController.java  (implementação)
│   ├── dto/CreateOrderCommand.java
│   ├── entity/Order.java               (Aggregate Root)
│   ├── entity/OrderItem.java
│   ├── enums/OrderStatus.java
│   ├── repository/OrderRepository.java
│   └── service/
│       ├── OrderCommandService.java     (regras de negócio)
│       └── MaterializedViewRefresher.java
├── query/                            ← READ SIDE
│   ├── controller/
│   │   ├── api/OrderQueryApi.java       (interface Swagger)
│   │   └── OrderQueryController.java    (implementação)
│   ├── entity/OrderSummaryView.java     (@Immutable)
│   ├── repository/OrderSummaryViewRepository.java
│   └── service/
│       ├── GenericQueryService.java     (genérico para N views)
│       └── QueryServiceConfig.java
└── config/
    ├── OpenApiConfig.java
    └── GlobalExceptionHandler.java
```

---

## Pontos-chave para a apresentação

1. **Command Stack complexo, Query Stack simples** — as regras de desconto e a máquina de estados existem apenas no lado de escrita. O lado de leitura é genérico.

2. **Materialized View como read model** — o SQL da view faz o JOIN e os cálculos. A aplicação Java só pagina e filtra.

3. **GenericQueryService reutilizável** — para adicionar uma nova tela de listagem (ex: relatório de produtos), basta criar a Materialized View no SQL, mapear uma entidade `@Immutable` e registrar um `@Bean` no `QueryServiceConfig`. Zero lógica nova no Java.

4. **Refresh síncrono (PoC)** — após cada command, a view é atualizada. Em produção, isso seria assíncrono via eventos.
