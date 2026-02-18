# CQRS Order Management - Prova de Conceito

Prova de Conceito de arquitetura **Soft CQRS** utilizando **Spring Boot 3**, **Spring Data JPA**, **Flyway** e **PostgreSQL**.

## O que é Soft CQRS?

No CQRS (Command Query Responsibility Segregation), separamos a aplicação em dois lados:

| | Command (Escrita) | Query (Leitura) |
|---|---|---|
| **Responsabilidade** | Regras de negócio, validações, transições de estado | Leitura otimizada, sem lógica |
| **Modelo de dados** | Tabelas normalizadas (`orders`, `order_items`) | Materialized View **ou** JPQL com JOIN |
| **Serviço** | `OrderCommandService` — específico, com regras complexas | `EntityReadService<T>` — genérico para views |
| **Benefício** | Foco total no domínio | Foco total em performance e clareza de leitura |

A palavra **"Soft"** indica que ambos os lados compartilham o mesmo banco de dados (não há event sourcing nem bancos separados), mas a separação lógica já traz grandes benefícios de design.

---

## Duas estratégias de leitura lado a lado

O objetivo principal desta PoC é demonstrar que o CQRS funciona com **diferentes estratégias de leitura**, ambas produzindo o **mesmo resultado**:

| | Materialized View (`/api/orders/view`) | JPQL com JOIN (`/api/orders/jpql`) |
|---|---|---|
| **Modelo** | `@Entity @Immutable` (`OrderSummaryView`) | `record` (`OrderSummaryJpqlView`) |
| **Consulta** | Dados pré-calculados no banco (Materialized View) | JOIN calculado em tempo de execução (JPQL) |
| **Filtros** | `Specification<T>` — filtros dinâmicos | `@Query` com parâmetros condicionais |
| **Quando usar** | Alto volume de leitura, dados podem ter leve atraso | Dados sempre atualizados, sem manutenção de view |
| **Endpoints** | `GET /api/orders/view` e `GET /api/orders/view/{id}` | `GET /api/orders/jpql` e `GET /api/orders/jpql/{id}` |

Ambos os endpoints retornam a **mesma estrutura de dados**: nome do cliente, status, desconto, total de itens, subtotal e total com desconto.

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
- **Queries - JPQL Tipado**: mesmas consultas, mas com JOIN calculado em tempo real via JPQL

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

### Passo 4 — Listar pedidos (JPQL com JOIN)

**GET** `/api/orders/jpql`

Mesmos parâmetros e mesma resposta do Passo 3, mas o JOIN é calculado em tempo real. Compare os resultados — devem ser idênticos.

```
GET /api/orders/jpql?page=0&size=10
GET /api/orders/jpql?status=PENDING
GET /api/orders/jpql?customer=Maria&sort=createdAt,desc
```

> Este é o ponto principal da demonstração: duas estratégias diferentes, mesmo resultado.

---

### Passo 5 — Buscar pedido por ID (ambas estratégias)

Compare os dois endpoints:

**Materialized View:**
```
GET /api/orders/view/{orderId}
```

**JPQL:**
```
GET /api/orders/jpql/{orderId}
```

Ambos retornam o mesmo resumo do pedido.

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

### Passo 11 — Verificar o resultado final nas duas estratégias

Compare os dois endpoints após todas as alterações:

```
GET /api/orders/view
GET /api/orders/jpql
```

Os dados devem ser idênticos — ambos refletem o estado atual dos pedidos, mas com estratégias de consulta diferentes.

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
│   │   │   └── OrderNativeQueryApi.java     (Swagger - JPQL com JOIN)
│   │   ├── OrderQueryController.java        (Materialized View + Specification)
│   │   └── OrderNativeQueryController.java  (JPQL + record tipado)
│   ├── dto/
│   │   └── OrderSummaryJpqlView.java        (record espelho da view materializada)
│   ├── entity/
│   │   └── OrderSummaryView.java            (@Entity @Immutable → Materialized View)
│   ├── repository/
│   │   ├── OrderSummaryViewRepository.java  (JPA + Specification)
│   │   └── OrderReadRepository.java         (JPQL com SELECT new → record)
│   └── service/
│       ├── ReadService.java                 (interface de leitura)
│       ├── EntityReadService.java           (implementação para @Entity)
│       ├── JpqlReadService.java             (implementação para records - extensível)
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

2. **Duas estratégias, mesmo resultado** — os endpoints `/api/orders/view` e `/api/orders/jpql` retornam dados idênticos usando abordagens diferentes:
   - **Materialized View** — dados pré-calculados no banco, leitura ultra-rápida
   - **JPQL com JOIN** — dados calculados em tempo real, sempre atualizados

3. **Materialized View como read model** — o SQL da view faz o JOIN e os cálculos. A aplicação Java só pagina e filtra via `Specification`.

4. **Records como views tipadas** — `OrderSummaryJpqlView` é um `record` Java que espelha a Materialized View. JPQL com `SELECT new` instancia o record direto, com type-safety em compile-time.

5. **Refresh síncrono (PoC)** — após cada command, a Materialized View é atualizada via `@TransactionalEventListener(AFTER_COMMIT)`. Em produção, isso seria assíncrono via eventos (Kafka, etc).

6. **Extensível** — o projeto inclui `ReadService<T>` com `EntityReadService` e `JpqlReadService` como padrão reutilizável para adicionar novas views de leitura sem repetir lógica de serviço.
