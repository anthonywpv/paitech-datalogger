-- ===========================================================================
--  AJUSTES v1.2 — retirar el factor de condición y dejar solo las dos
--                 unidades que existen de verdad en el recinto
--
--  Ejecutar UNA VEZ sobre una base ya montada con 01..04.
--  No borra registros. Es idempotente: repetirlo no rompe nada.
--
--  DESPUÉS: pulsar "Refresh schema cache" en la página Data API.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. FUERA EL FACTOR DE CONDICIÓN
--    Se calculaba en el teléfono y viajaba con cada pez, pero no se usa. Y es
--    derivable en cualquier momento a partir de peso y talla, así que
--    guardarlo solo añadía una columna que mantener sincronizada.
--
--    Las vistas que lo exponían se recrean sin él más abajo.
-- ---------------------------------------------------------------------------
DROP VIEW IF EXISTS public.v_crecimiento_mensual  CASCADE;
DROP VIEW IF EXISTS public.v_muestreo_biometria   CASCADE;
DROP VIEW IF EXISTS public.v_consolidado_fuentes  CASCADE;

ALTER TABLE public.registro_biometria DROP COLUMN IF EXISTS factor_condicion;

-- ---------------------------------------------------------------------------
-- 2. CATÁLOGO REAL DE UNIDADES
--    En Paipayales hay una piscina de Vieja Azul y un lecho de lombricultura.
--    El resto eran de ejemplo. Ofrecer piscinas que no existen solo invita a
--    registrar en la equivocada.
--
--    Solo se borran las que NO tengan registros asociados: si alguna llegó a
--    usarse, se conserva para no dejar datos huérfanos.
-- ---------------------------------------------------------------------------
INSERT INTO public.piscina (codigo, nombre, area_m2, activa) VALUES
    ('P-01',   'Piscina 1 Paipayales',     120.00, TRUE),
    ('LOM-01', 'Lecho de Lombricultura 1',  12.00, TRUE)
ON CONFLICT (codigo) DO UPDATE
    SET nombre = EXCLUDED.nombre,
        activa = TRUE;

DELETE FROM public.piscina p
 WHERE p.codigo NOT IN ('P-01', 'LOM-01')
   AND NOT EXISTS (SELECT 1 FROM public.registro_biometria b WHERE b.piscina = p.codigo)
   AND NOT EXISTS (SELECT 1 FROM public.registro_agua      a WHERE a.piscina = p.codigo)
   AND NOT EXISTS (SELECT 1 FROM public.ensayo_laboratorio l WHERE l.piscina = p.codigo);

-- Las que sí tienen datos se desactivan en vez de borrarse: dejan de ofrecerse
-- pero sus registros históricos siguen teniendo a qué apuntar.
UPDATE public.piscina
   SET activa = FALSE
 WHERE codigo NOT IN ('P-01', 'LOM-01');

-- ---------------------------------------------------------------------------
-- 3. VISTAS RECREADAS SIN factor_condicion
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW public.v_crecimiento_mensual AS
SELECT piscina,
       DATE_TRUNC('month', fecha_muestreo)::DATE AS mes,
       COUNT(DISTINCT codigo_muestreo) AS muestreos,
       COUNT(*)                        AS peces_medidos,
       ROUND(AVG(peso_g), 2)           AS peso_promedio_g,
       ROUND(STDDEV_SAMP(peso_g), 2)   AS peso_desviacion_g,
       ROUND(AVG(talla_cm), 2)         AS talla_promedio_cm,
       ROUND(STDDEV_SAMP(talla_cm), 2) AS talla_desviacion_cm
FROM public.registro_biometria
GROUP BY piscina, DATE_TRUNC('month', fecha_muestreo)
ORDER BY piscina, mes;

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
       MIN(registrado_por)             AS registrado_por
FROM public.registro_biometria
GROUP BY codigo_muestreo, fecha_muestreo, piscina
ORDER BY fecha_muestreo DESC, piscina;

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
       l.parametro      AS parametro_laboratorio,
       l.valor_promedio AS valor_laboratorio,
       l.unidad         AS unidad_laboratorio
FROM campo c
FULL OUTER JOIN agua a ON a.piscina = c.piscina AND a.mes = c.mes
FULL OUTER JOIN lab  l ON l.piscina = COALESCE(c.piscina, a.piscina)
                      AND l.mes     = COALESCE(c.mes, a.mes)
ORDER BY 1, 2;

-- ---------------------------------------------------------------------------
-- 4. COMPROBACIÓN
-- ---------------------------------------------------------------------------
-- SELECT codigo, nombre, activa FROM public.piscina ORDER BY codigo;
-- SELECT column_name FROM information_schema.columns
--  WHERE table_name = 'registro_biometria' AND column_name = 'factor_condicion';
--    (la segunda debe devolver CERO filas)
