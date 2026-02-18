# CQRS Order Management - Prova de Conceito

Prova de Conceito de arquitetura **Soft CQRS** utilizando **Spring Boot 3**, **Spring Data JPA**, **Flyway** e **PostgreSQL**.

## O que é Soft CQRS?

No CQRS (Command Query Responsibility Segregation), separamos a aplicação em dois lados:

| | Command (Escrita) | Query (Leitura) |
|---|---|---|
| **Responsabilidade** | Regras de negócio, validações, transições de estado | Leitura otimizada, sem lógica |
| **Modelo de dados** | Tabelas normalizadas (`orders`, `order_items`) | Materialized View ou records JPQL tipados |
| **Serviço** | `OrderCommandService` — específico, com regras complexas | `ReadService<T>` — interface única, genérica para N views |
| **Benefício** | Foco total no domínio | Foco total em performance e clareza de leitura |

A palavra **"Soft"** indica que ambos os lados compartilham o mesmo banco de dados (não há event sourcing nem bancos separados), mas a separação lógica já traz grandes benefícios de design.

---

## Arquitetura do Query Stack

O lado de leitura é construído sobre uma **interface única `ReadService<T>`** com duas implementações:

```
ReadService<T>                         ← Interface que o controller injeta
├── EntityReadService<T, ID>           ← Para @Entity (Materialized Views)
│   └── Usa JpaRepository + Specification (filtros dinâmicos + paginação)
└── JpqlReadService<T>                 ← Para records (JPQL com SELECT new)
    └── Usa method references via Builder (type-safe, sem @Entity)
```

### Quando usar cada um?

| Cenário | Implementação | Exemplo |
|---|---|---|
| Listagem com filtros dinâmicos | `EntityReadService` | Tela de pedidos com filtro por status, cliente, paginação |
| Relatório analítico | `JpqlReadService` | Receita por status, médias, totais agrupados |
| Detalhe com dados compostos | `JpqlReadService` | Pedido + itens (2 queries tipadas combinadas) |

### Como adicionar uma nova view de leitura

**Opção 1 — Record JPQL (sem @Entity):**
1. Criar o `record MeuView(...)` no pacote `query/dto`
2. Criar a `@Query("SELECT new MeuView(...)")` no repositório
3. Registrar o `@Bean` no `QueryServiceConfig`
4. Injetar `ReadService<MeuView>` no controller

**Opção 2 — @Entity com Materialized View:**
1. Criar a Materialized View no SQL (migration Flyway)
2. Criar a `@Entity @Immutable` no pacote `query/entity`
3. Criar o `JpaRepository` + `JpaSpecificationExecutor`
4. Registrar o `@Bean EntityReadService` no `QueryServiceConfig`
5. Injetar `EntityReadService` no controller (para usar `Specification`)

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

Os endpoints estão organizados em três seções:
- **Commands - Escrita**: operações que alteram estado
- **Queries - Leitura**: consultas na Materialized View (filtros dinâmicos + paginação)
- **Queries - JPQL Tipado**: consultas com records tipados via JPQL (sem @Entity, sem Materialized View)

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

### Passo 3 — Listar pedidos (Materialized View)

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

### Passo 4 — Relatório por status (JPQL Tipado)

**GET** `/api/orders/query/report/by-status`

Retorna `List<StatusReportView>` — um record tipado com total de pedidos, receita e média por status. Sem Materialized View, sem Map, 100% type-safe.

---

### Passo 5 — Detalhe do pedido com itens (JPQL Tipado)

**GET** `/api/orders/query/{orderId}`

Retorna `OrderWithItemsView` — composto por `OrderDetailView` + `List<OrderItemView>`. Duas queries JPQL tipadas combinadas no controller.

---

### Passo 6 — Confirmar um pedido (transição de estado)

**PATCH** `/api/orders/status`

```json
{
  "orderId": "COLE_O_ID_DO_PASSO_1",
  "newStatus": "CONFIRMED"
}
```

---

### Passo 7 — Enviar o pedido

**PATCH** `/api/orders/status`

```json
{
  "orderId": "COLE_O_ID_DO_PASSO_1",
  "newStatus": "SHIPPED"
}
```

---

### Passo 8 — Tentar cancelar pedido já enviado (erro esperado)

**PATCH** `/api/orders/status`

```json
{
  "orderId": "COLE_O_ID_DO_PASSO_1",
  "newStatus": "CANCELLED"
}
```

Resposta esperada (**409 Conflict**):
```json
{
  "error": "Transição inválida: SHIPPED -> CANCELLED"
}
```

> Isso demonstra a máquina de estados protegendo as regras de negócio no Command Stack.

---

### Passo 9 — Entregar o pedido

**PATCH** `/api/orders/status`

```json
{
  "orderId": "COLE_O_ID_DO_PASSO_1",
  "newStatus": "DELIVERED"
}
```

---

### Passo 10 — Remover item de um pedido

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

Consulte o detalhe do pedido para ver os IDs dos itens:

**GET** `/api/orders/query/{orderId}`

Depois remova um item:

**DELETE** `/api/orders/items`

```json
{
  "orderId": "COLE_O_ID_DO_PEDIDO",
  "itemId": "COLE_O_ID_DO_ITEM"
}
```

> O total será recalculado automaticamente. Se tentar remover o último item, retorna **409 Conflict**.

---

### Passo 11 — Verificar tudo na Materialized View

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
├── command/                              ← WRITE SIDE
│   ├── controller/
│   │   ├── api/OrderCommandApi.java         (interface Swagger)
│   │   └── OrderCommandController.java      (implementação)
│   ├── dto/
│   │   ├── CreateOrderCommand.java
│   │   ├── UpdateOrderStatusCommand.java
│   │   └── RemoveOrderItemCommand.java
│   ├── entity/
│   │   ├── Order.java                       (Aggregate Root com regras de negócio)
│   │   └── OrderItem.java
│   ├── enums/OrderStatus.java
│   ├── repository/OrderRepository.java
│   └── service/
│       ├── OrderCommandService.java         (regras de negócio)
│       └── MaterializedViewRefresher.java   (refresh pós-commit via evento)
│
├── query/                                ← READ SIDE
│   ├── controller/
│   │   ├── api/
│   │   │   ├── OrderQueryApi.java           (Swagger - Materialized View)
│   │   │   └── OrderNativeQueryApi.java     (Swagger - JPQL Tipado)
│   │   ├── OrderQueryController.java        (Materialized View + Specification)
│   │   └── OrderNativeQueryController.java  (JPQL + records tipados)
│   ├── dto/
│   │   ├── OrderDetailView.java             (record)
│   │   ├── OrderItemView.java               (record)
│   │   ├── OrderWithItemsView.java          (record composto)
│   │   └── StatusReportView.java            (record)
│   ├── entity/
│   │   └── OrderSummaryView.java            (@Entity @Immutable → Materialized View)
│   ├── repository/
│   │   ├── OrderSummaryViewRepository.java  (JPA + Specification)
│   │   └── OrderReadRepository.java         (JPQL com SELECT new → records)
│   └── service/
│       ├── ReadService.java                 (interface única de leitura)
│       ├── EntityReadService.java           (implementação para @Entity)
│       ├── JpqlReadService.java             (implementação para records)
│       └── QueryServiceConfig.java          (registro dos @Beans)
│
└── config/
    ├── OpenApiConfig.java
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__create_write_tables.sql          (orders, order_items)
    └── V2__create_materialized_view.sql     (order_summary_mview)
```

---

## Pontos-chave para a apresentação

1. **Command Stack complexo, Query Stack simples** — as regras de desconto e a máquina de estados existem apenas no lado de escrita. O lado de leitura é genérico e sem lógica de negócio.

2. **Interface `ReadService<T>` unificada** — o controller sempre injeta `ReadService<T>`, sem saber se por trás é uma `@Entity` ou um record JPQL. Duas implementações:
   - `EntityReadService` — para `@Entity` com Materialized View (suporta `Specification` + filtros dinâmicos)
   - `JpqlReadService` — para records tipados via JPQL (suporta `SELECT new Record(...)`, type-safe)

3. **Materialized View como read model** — o SQL da view faz o JOIN e os cálculos. A aplicação Java só pagina e filtra.

4. **Records como views tipadas** — para queries que não mapeiam para entidades (relatórios, detalhes compostos), usamos records Java como views de dados. JPQL com `SELECT new` instancia o record direto, com type-safety em compile-time.

5. **Checklist mínimo para nova view** — criar o record (ou @Entity), criar a query, registrar o bean. O dev repete zero lógica de serviço.

6. **Refresh síncrono (PoC)** — após cada command, a Materialized View é atualizada via `@TransactionalEventListener(AFTER_COMMIT)`. Em produção, isso seria assíncrono via eventos (Kafka, etc).
