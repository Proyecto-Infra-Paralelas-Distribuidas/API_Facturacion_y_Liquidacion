CREATE TABLE IF NOT EXISTS liquidacion (
    id                    VARCHAR(64)   PRIMARY KEY,
    periodo               VARCHAR(7)    NOT NULL,
    planta_id             VARCHAR(64)   NOT NULL,
    cliente_id            VARCHAR(64),
    energia_entregada_kwh NUMERIC(14,2) NOT NULL,
    tarifa_por_kwh        NUMERIC(14,2) NOT NULL,
    valor_energia         NUMERIC(16,2) NOT NULL,
    costo_mantenimiento   NUMERIC(16,2) NOT NULL,
    total                 NUMERIC(16,2) NOT NULL,
    moneda                VARCHAR(3)    NOT NULL DEFAULT 'COP',
    generado_en           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (periodo, planta_id)
);

CREATE INDEX IF NOT EXISTS idx_liquidacion_periodo ON liquidacion (periodo, planta_id);
