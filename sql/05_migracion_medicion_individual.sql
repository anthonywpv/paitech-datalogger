-- ===========================================================================
--  MIGRACIÓN — de "promedio + número de peces" a UNA FILA = UN PEZ
--
--  Ejecutar UNA SOLA VEZ en el SQL Editor de Neon, sobre una base que ya se
--  creó con la versión anterior de 01_esquema_neon.sql.
--
--  Si la base es nueva no hace falta: 01_esquema_neon.sql ya la crea así.
--  Aun así el script es idempotente (IF EXISTS / IF NOT EXISTS), de modo que
--  volver a ejecutarlo no rompe nada.
--
--  DESPUÉS DE EJECUTARLO hay que pulsar "Refresh schema cache" en la página
--  Data API, o la app seguirá recibiendo 404 aunque las columnas ya existan.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. HORA DE SUBIDA POR REGISTRO
--    Se guardaba solo en el teléfono. Ahora viaja con el dato, junto al correo
--    de quien lo subió (columna registrado_por, que ya existía).
--
--    Es distinta de recibido_en: sincronizado_en es el reloj del teléfono en el
--    momento de subir, recibido_en es el del servidor al recibir. La diferencia
--    entre ambas delata teléfonos con la hora mal puesta.
-- ---------------------------------------------------------------------------
ALTER TABLE public.registro_biometria  ADD COLUMN IF NOT EXISTS sincronizado_en TIMESTAMPTZ;
ALTER TABLE public.registro_agua       ADD COLUMN IF NOT EXISTS sincronizado_en TIMESTAMPTZ;
ALTER TABLE public.ensayo_laboratorio  ADD COLUMN IF NOT EXISTS sincronizado_en TIMESTAMPTZ;

COMMENT ON COLUMN public.registro_biometria.sincronizado_en
    IS 'Reloj del teléfono al momento de subir. Comparar con recibido_en para detectar equipos con la hora mal configurada.';

-- ---------------------------------------------------------------------------
-- 2. CÓDIGO DE MUESTREO
--    Agrupa los peces medidos el mismo día: "M-04082026" (DDMMYYYY).
--    Es GENERATED ALWAYS, así que lo calcula Postgres y la app NO debe enviarlo
--    (si lo enviara, la Data API rechazaría el INSERT).
-- ---------------------------------------------------------------------------
ALTER TABLE public.registro_biometria
    ADD COLUMN IF NOT EXISTS codigo_muestreo TEXT
    GENERATED ALWAYS AS ('M-' || to_char(fecha_muestreo, 'DDMMYYYY')) STORED;

CREATE INDEX IF NOT EXISTS ix_biometria_muestreo
    ON public.registro_biometria (codigo_muestreo);

-- ---------------------------------------------------------------------------
-- 3. RETIRO DE cantidad_muestreada
--
--    Las filas que promediaban varios peces NO se pueden desagregar: no existe
--    la medida individual de cada uno. Se conservan como están, pero se les
--    anota en la observación de cuántos peces era el promedio, para que nadie
--    las lea después como si fueran la medición de un solo pez.
--
--    Este UPDATE debe ir ANTES del DROP, obviamente.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public'
                 AND table_name   = 'registro_biometria'
                 AND column_name  = 'cantidad_muestreada') THEN

        UPDATE public.registro_biometria
           SET observacion = COALESCE(observacion || ' | ', '')
                             || 'Promedio de ' || cantidad_muestreada
                             || ' peces (registro anterior al cambio a medición individual).'
         WHERE cantidad_muestreada > 1;

        ALTER TABLE public.registro_biometria DROP COLUMN cantidad_muestreada;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 4. COMPROBACIÓN
--    Debe devolver una fila por muestreo con el número de peces medidos.
-- ---------------------------------------------------------------------------
-- SELECT codigo_muestreo, piscina, COUNT(*) AS peces,
--        ROUND(AVG(peso_g), 2) AS peso_promedio_g,
--        ROUND(STDDEV_SAMP(peso_g), 2) AS desviacion_peso_g
--   FROM public.registro_biometria
--  GROUP BY codigo_muestreo, piscina
--  ORDER BY codigo_muestreo DESC, piscina;
