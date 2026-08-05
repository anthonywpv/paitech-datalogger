-- ===========================================================================
--  MIGRACIÓN — de "promedio + número de peces" a UNA FILA = UN PEZ
--
--  ⚠️  SI ACABAS DE REINICIAR CON 00_reiniciar_desde_cero.sql, NO EJECUTES
--      ESTE ARCHIVO. Sobre una base recién creada por 01 no tiene nada que
--      migrar y sus ALTER TABLE fallarán. Este script existe únicamente para
--      actualizar una base con datos que no se quieren perder.
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
-- 4. CALIDAD DE AGUA — del par temperatura/oxígeno al ciclo del nitrógeno
--
--    Los parámetros que se toman en campo pasan a ser pH, amonio, nitrito,
--    nitrato y población estimada.
--
--    Las lecturas viejas NO se pueden convertir: no hay forma de deducir el
--    amonio a partir de la temperatura. Se conservan fecha, piscina y
--    observación, se preserva el pH cuando estaba anotado, y los valores de
--    temperatura y oxígeno se vuelcan a la observación para no perderlos.
-- ---------------------------------------------------------------------------
ALTER TABLE public.registro_agua ADD COLUMN IF NOT EXISTS amonio_mg_l  NUMERIC(8,3);
ALTER TABLE public.registro_agua ADD COLUMN IF NOT EXISTS nitrito_mg_l NUMERIC(8,3);
ALTER TABLE public.registro_agua ADD COLUMN IF NOT EXISTS nitrato_mg_l NUMERIC(8,2);
ALTER TABLE public.registro_agua ADD COLUMN IF NOT EXISTS poblacion_estimada INTEGER;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public'
                 AND table_name   = 'registro_agua'
                 AND column_name  = 'temperatura_c') THEN

        -- Volcar lo viejo a la observación antes de borrar las columnas.
        UPDATE public.registro_agua
           SET observacion = COALESCE(observacion || ' | ', '')
                             || 'Medición anterior al cambio de parámetros: se registró '
                             || 'temperatura ' || temperatura_c || ' C y oxígeno '
                             || oxigeno_mg_l || ' mg/L. Amonio, nitrito y nitrato sin medir.';

        ALTER TABLE public.registro_agua DROP COLUMN temperatura_c;
        ALTER TABLE public.registro_agua DROP COLUMN oxigeno_mg_l;
    END IF;
END $$;

-- Los parámetros nuevos quedan en cero en las filas viejas (sin medir), y el
-- pH pasa a ser obligatorio: donde faltaba se asume neutro.
UPDATE public.registro_agua SET amonio_mg_l  = 0 WHERE amonio_mg_l  IS NULL;
UPDATE public.registro_agua SET nitrito_mg_l = 0 WHERE nitrito_mg_l IS NULL;
UPDATE public.registro_agua SET nitrato_mg_l = 0 WHERE nitrato_mg_l IS NULL;
UPDATE public.registro_agua SET ph = 7.0 WHERE ph IS NULL;

ALTER TABLE public.registro_agua ALTER COLUMN ph           SET NOT NULL;
ALTER TABLE public.registro_agua ALTER COLUMN amonio_mg_l  SET NOT NULL;
ALTER TABLE public.registro_agua ALTER COLUMN nitrito_mg_l SET NOT NULL;
ALTER TABLE public.registro_agua ALTER COLUMN nitrato_mg_l SET NOT NULL;

-- ---------------------------------------------------------------------------
-- 5. COMPROBACIÓN
--    Debe devolver una fila por muestreo con el número de peces medidos.
-- ---------------------------------------------------------------------------
-- SELECT codigo_muestreo, piscina, COUNT(*) AS peces,
--        ROUND(AVG(peso_g), 2) AS peso_promedio_g,
--        ROUND(STDDEV_SAMP(peso_g), 2) AS desviacion_peso_g
--   FROM public.registro_biometria
--  GROUP BY codigo_muestreo, piscina
--  ORDER BY codigo_muestreo DESC, piscina;
