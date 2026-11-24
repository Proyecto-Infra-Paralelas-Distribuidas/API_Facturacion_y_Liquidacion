-- Datos de EJEMPLO del Avance 1 (los mismos del contrato). No son datos reales:
-- en el Avance 2 las liquidaciones se calculan con Reportes y Mantenimiento.
INSERT INTO liquidacion (id, periodo, planta_id, cliente_id, energia_entregada_kwh, tarifa_por_kwh, valor_energia, costo_mantenimiento, total, moneda, generado_en)
VALUES ('LIQ-2026-11-PL-001', '2026-11', 'PL-001', NULL, 12450.50, 450, 5602725, 360000, 5242725, 'COP', '2026-12-01T08:00:00-05:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO liquidacion (id, periodo, planta_id, cliente_id, energia_entregada_kwh, tarifa_por_kwh, valor_energia, costo_mantenimiento, total, moneda, generado_en)
VALUES ('LIQ-2026-11-PL-002', '2026-11', 'PL-002', NULL, 8300.00, 450, 3735000, 650000, 3085000, 'COP', '2026-12-01T08:00:00-05:00')
ON CONFLICT (id) DO NOTHING;
