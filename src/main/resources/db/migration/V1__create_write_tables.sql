-- =====================================================
-- CQRS - WRITE SIDE: Tabelas normalizadas para commands
-- As tabelas de escrita são modeladas para integridade
-- e regras de negócio, não para consultas rápidas.
-- =====================================================

CREATE TABLE orders
(
    id            UUID PRIMARY KEY,
    customer_name VARCHAR(255)   NOT NULL,
    status        VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    discount      NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    total_amount  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items
(
    id         UUID PRIMARY KEY,
    order_id   UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product    VARCHAR(255)   NOT NULL,
    quantity   INT            NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    created_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
