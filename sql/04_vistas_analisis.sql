-- ===========================================================================
--  VISTAS DE ANÁLISIS — Consolidación de Fuentes
--
--  Estas vistas son el puente hacia el trabajo de Ciencia de Datos: dejan
--  listos los cruces que de otro modo habría que rehacer en cada consulta.
-- ===========================================================================

-- 1. Última medición de agua por piscina, con el semáforo recalculado en el
--    servidor. Sirve de contraste contra el estado que envió el dispositivo:
--    si estado_servidor y estado_dispositivo difieren, o la app va desactualizada
--    o alguien insertó filas sin pasar por ella.
--
--    OJO — LÓGICA DUPLICADA: este CASE es la traducción a SQL de
--    domain/EvaluadorSemaforo.java. Los umbrales viven en dos idiomas y no hay
--    nada que los mantenga sincronizados: al mover uno, mover el otro, o la
--    comparación de arriba empieza a dar discrepancias falsas.
CREATE OR REPLACE VIEW public.v_semaforo_actual AS
SELECT DISTINCT ON (piscina)
       piscina,
       fecha_muestreo,
       temperatura_c,
       oxigeno_mg_l,
       ph,
       estado_alerta AS estado_dispositivo,
       CASE
           WHEN oxigeno_mg_l < 3.0
             OR temperatura_c < 20.0 OR temperatura_c > 32.0
             OR (ph IS NOT NULL AND (ph < 6.0 OR ph > 9.0))
             -- Riesgo combinado: agua caliente retiene menos oxígeno mientras
             -- el pez consume más, así que un OD "aceptable" deja de serlo.
             OR (temperatura_c > 30.0 AND oxigeno_mg_l < 5.0)
                THEN 'ROJO'
           WHEN oxigeno_mg_l < 5.0
             OR temperatura_c < 24.0 OR temperatura_c > 30.0
             OR (ph IS NOT NULL AND (ph < 6.5 OR ph > 8.5))
                THEN 'AMARILLO'
           ELSE 'VERDE'
       END AS estado_servidor,
       creado_en
FROM public.registro_agua
ORDER BY piscina, creado_en DESC;

-- 2. Crecimiento por piscina: evolución del peso y la talla promedio.
CREATE OR REPLACE VIEW public.v_crecimiento_mensual AS
SELECT piscina,
       DATE_TRUNC('month', fecha_muestreo)::DATE AS mes,
       COUNT(*)                    AS muestreos,
       ROUND(AVG(peso_g), 2)       AS peso_promedio_g,
       ROUND(AVG(talla_cm), 2)     AS talla_promedio_cm,
       ROUND(AVG(factor_condicion), 3) AS k_fulton_promedio
FROM public.registro_biometria
GROUP BY piscina, DATE_TRUNC('month', fecha_muestreo)
ORDER BY piscina, mes;

-- 3. CONSOLIDACIÓN: datos in situ + ensayos de laboratorio de la misma
--    piscina y mes, en una sola fila lista para análisis.
CREATE OR REPLACE VIEW public.v_consolidado_fuentes AS
WITH campo AS (
    SELECT piscina,
           DATE_TRUNC('month', fecha_muestreo)::DATE AS mes,
           ROUND(AVG(peso_g), 2)   AS peso_promedio_g,
           ROUND(AVG(talla_cm), 2) AS talla_promedio_cm
    FROM public.registro_biometria
    GROUP BY piscina, DATE_TRUNC('month', fecha_muestreo)
),
agua AS (
    SELECT piscina,
           DATE_TRUNC('month', fecha_muestreo)::DATE AS mes,
           ROUND(AVG(temperatura_c), 2) AS temperatura_promedio_c,
           ROUND(AVG(oxigeno_mg_l), 2)  AS oxigeno_promedio_mg_l,
           ROUND(AVG(ph), 2)            AS ph_promedio
    FROM public.registro_agua
    GROUP BY piscina, DATE_TRUNC('month', fecha_muestreo)
),
lab AS (
    SELECT piscina,
           DATE_TRUNC('month', fecha_muestreo)::DATE AS mes,
           parametro,
           ROUND(AVG(valor), 4) AS valor_promedio,
           MAX(unidad)          AS unidad
    FROM public.ensayo_laboratorio
    GROUP BY piscina, DATE_TRUNC('month', fecha_muestreo), parametro
)
SELECT COALESCE(c.piscina, a.piscina, l.piscina) AS piscina,
       COALESCE(c.mes, a.mes, l.mes)             AS mes,
       c.peso_promedio_g,
       c.talla_promedio_cm,
       a.temperatura_promedio_c,
       a.oxigeno_promedio_mg_l,
       a.ph_promedio,
       l.parametro     AS parametro_laboratorio,
       l.valor_promedio AS valor_laboratorio,
       l.unidad         AS unidad_laboratorio
FROM campo c
FULL OUTER JOIN agua a ON a.piscina = c.piscina AND a.mes = c.mes
FULL OUTER JOIN lab  l ON l.piscina = COALESCE(c.piscina, a.piscina)
                      AND l.mes     = COALESCE(c.mes, a.mes)
ORDER BY 1, 2;

-- 4. Control de la sincronización diferida: cuánto tardan los datos en llegar.
CREATE OR REPLACE VIEW public.v_desfase_sincronizacion AS
SELECT 'biometria' AS origen, piscina, creado_en, recibido_en,
       ROUND(EXTRACT(EPOCH FROM (recibido_en - creado_en)) / 3600.0, 2) AS horas_desfase
FROM public.registro_biometria
UNION ALL
SELECT 'agua', piscina, creado_en, recibido_en,
       ROUND(EXTRACT(EPOCH FROM (recibido_en - creado_en)) / 3600.0, 2)
FROM public.registro_agua
UNION ALL
SELECT 'laboratorio', piscina, creado_en, recibido_en,
       ROUND(EXTRACT(EPOCH FROM (recibido_en - creado_en)) / 3600.0, 2)
FROM public.ensayo_laboratorio
ORDER BY recibido_en DESC;
