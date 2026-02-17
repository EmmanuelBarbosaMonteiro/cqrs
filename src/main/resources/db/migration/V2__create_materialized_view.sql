-- =====================================================
-- CQRS - READ SIDE: Materialized View para queries
-- Esta view já entrega os dados "prontos para tela",
-- eliminando JOINs e cálculos no lado da aplicação.
-- O lado de leitura nunca precisa conhecer regras de negócio.
-- =====================================================

CREATE
MATERIALIZED VIEW order_summary_mview AS
SELECT o.id                                          AS order_id,
       o.customer_name,
       o.status,
       o.discount,
       COUNT(oi.id)                                  AS total_items,
       COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS subtotal,
       o.total_amount                                AS total_with_discount,
       o.created_at,
       o.updated_at
FROM orders o
         LEFT JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.customer_name, o.status, o.discount,
         o.total_amount, o.created_at, o.updated_at WITH DATA;

-- Índice único necessário para REFRESH CONCURRENTLY
CREATE UNIQUE INDEX idx_order_summary_mview_id ON order_summary_mview (order_id);

-- Índices para filtros comuns na listagem
CREATE INDEX idx_order_summary_mview_status ON order_summary_mview (status);
CREATE INDEX idx_order_summary_mview_customer ON order_summary_mview (customer_name);
