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
       ph,
       amonio_mg_l,
       nitrito_mg_l,
       nitrato_mg_l,
       poblacion_estimada,
       -- Amoníaco libre NH3: la fracción del amonio que de verdad envenena.
       -- NH3 = total / (1 + 10^(pKa - pH)), pKa 9.25 a 25 °C.
       ROUND((amonio_mg_l / (1 + POWER(10, 9.25 - ph)))::NUMERIC, 4) AS amoniaco_libre_mg_l,
       estado_alerta AS estado_dispositivo,
       CASE
           WHEN nitrito_mg_l > 1.0
             OR amonio_mg_l  > 1.0
             OR nitrato_mg_l > 100.0
             OR ph < 6.0 OR ph > 9.0
             -- Amoníaco libre por encima del umbral agudo.
             OR (amonio_mg_l / (1 + POWER(10, 9.25 - ph))) > 0.05
             -- Amonio y nitrito altos a la vez: el filtro biológico no da abasto.
             OR (amonio_mg_l >= 0.5 AND nitrito_mg_l >= 0.5)
                THEN 'ROJO'
           WHEN nitrito_mg_l >= 0.5
             OR amonio_mg_l  >= 0.5
             OR nitrato_mg_l >= 50.0
             OR ph < 6.5 OR ph > 8.5
             OR (amonio_mg_l / (1 + POWER(10, 9.25 - ph))) > 0.02
                THEN 'AMARILLO'
           ELSE 'VERDE'
       END AS estado_servidor,
       creado_en
FROM public.registro_agua
ORDER BY piscina, creado_en DESC;

-- 1b. Evolución de la población por piscina, con la caída respecto al muestreo
--     anterior. Una pérdida fuerte de peces es la señal más clara de que algo
--     va mal, aunque el agua salga verde el día de la visita: el productor pudo
--     llegar después del episodio.
CREATE OR REPLACE VIEW public.v_mortalidad_piscina AS
WITH lecturas AS (
    SELECT piscina,
           fecha_muestreo,
           poblacion_estimada,
           LAG(poblacion_estimada) OVER (
               PARTITION BY piscina ORDER BY fecha_muestreo
           ) AS poblacion_anterior
      FROM public.registro_agua
     WHERE poblacion_estimada IS NOT NULL
)
SELECT piscina,
       fecha_muestreo,
       poblacion_anterior,
       poblacion_estimada,
       poblacion_anterior - poblacion_estimada AS peces_perdidos,
       ROUND(100.0 * (poblacion_anterior - poblacion_estimada)
             / NULLIF(poblacion_anterior, 0), 1) AS caida_porcentual,
       CASE
           WHEN poblacion_anterior IS NULL THEN 'SIN_DATO'
           WHEN poblacion_estimada >= poblacion_anterior THEN 'VERDE'
           WHEN (poblacion_anterior - poblacion_estimada)::NUMERIC
                / NULLIF(poblacion_anterior, 0) >= 0.25 THEN 'ROJO'
           WHEN (poblacion_anterior - poblacion_estimada)::NUMERIC
                / NULLIF(poblacion_anterior, 0) >= 0.10 THEN 'AMARILLO'
           ELSE 'VERDE'
       END AS estado_mortalidad
  FROM lecturas
 ORDER BY piscina, fecha_muestreo DESC;

-- 2. Crecimiento por piscina: evolución del peso y la talla.
--
--    Desde que cada fila es UN PEZ, estas columnas dejaron de ser un promedio
--    de promedios y pasaron a calcularse sobre las medidas individuales. Eso
--    hace posible la desviación estándar, que es el dato que de verdad importa:
--    dos piscinas con el mismo peso medio pero distinta dispersión están en
--    situaciones muy diferentes — mucha dispersión suele indicar competencia
--    por el alimento o siembra desigual.
CREATE OR REPLACE VIEW public.v_crecimiento_mensual AS
SELECT piscina,
       DATE_TRUNC('month', fecha_muestreo)::DATE AS mes,
       COUNT(DISTINCT codigo_muestreo) AS muestreos,
       COUNT(*)                        AS peces_medidos,
       ROUND(AVG(peso_g), 2)           AS peso_promedio_g,
       ROUND(STDDEV_SAMP(peso_g), 2)   AS peso_desviacion_g,
       ROUND(AVG(talla_cm), 2)         AS talla_promedio_cm,
       ROUND(STDDEV_SAMP(talla_cm), 2) AS talla_desviacion_cm,
       ROUND(AVG(factor_condicion), 3) AS k_fulton_promedio
FROM public.registro_biometria
GROUP BY piscina, DATE_TRUNC('month', fecha_muestreo)
ORDER BY piscina, mes;

-- 2b. Un renglón por muestreo: lo que el productor midió en una jornada.
CREATE OR REPLACE VIEW public.v_muestreo_biometria AS
SELECT codigo_muestreo,
       fecha_muestreo,
       piscina,
       COUNT(*)                        AS peces_medidos,
       ROUND(AVG(peso_g), 2)           AS peso_promedio_g,
       MIN(peso_g)                     AS peso_minimo_g,
       MAX(peso_g)                     AS peso_maximo_g,
       ROUND(STDDEV_SAMP(peso_g), 2)   AS peso_desviacion_g,
       ROUND(AVG(talla_cm), 2)         AS talla_promedio_cm,
       ROUND(AVG(factor_condicion), 3) AS k_fulton_promedio,
       MIN(registrado_por)             AS registrado_por
FROM public.registro_biometria
GROUP BY codigo_muestreo, fecha_muestreo, piscina
ORDER BY fecha_muestreo DESC, piscina;

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
           ROUND(AVG(ph), 2)           AS ph_promedio,
           ROUND(AVG(amonio_mg_l), 3)  AS amonio_promedio_mg_l,
           ROUND(AVG(nitrito_mg_l), 3) AS nitrito_promedio_mg_l,
           ROUND(AVG(nitrato_mg_l), 2) AS nitrato_promedio_mg_l,
           MAX(poblacion_estimada)     AS poblacion_estimada
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
       a.ph_promedio,
       a.amonio_promedio_mg_l,
       a.nitrito_promedio_mg_l,
       a.nitrato_promedio_mg_l,
       a.poblacion_estimada,
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
