-- ===========================================================================
--  REINICIO LIMPIO — borra el esquema de Paipay para volver a crearlo
--
--  ⚠️  ESTO BORRA TODOS LOS DATOS de las tablas de Paipay en Neon.
--      No hay deshacer. Ejecutar solo en desarrollo, cuando el esquema quedó
--      a medias y se prefiere partir de cero.
--
--  POR QUÉ HACE FALTA
--  01_esquema_neon.sql usa CREATE TABLE IF NOT EXISTS, que NO modifica una
--  tabla que ya existe. Si el esquema cambió (columnas nuevas), volver a
--  ejecutar 01 no añade nada y luego 04 falla al crear vistas que usan
--  columnas inexistentes. Borrar y recrear resuelve eso de raíz.
--
--  LO QUE NO SE BORRA
--  Las cuentas de usuario. Viven en Neon Auth, no en estas tablas: podrás
--  seguir entrando con el mismo correo y contraseña.
--
--  ORDEN DE EJECUCIÓN DESPUÉS DE ESTE ARCHIVO
--      01_esquema_neon.sql
--      02_permisos_y_rls.sql
--      03_datos_iniciales.sql
--      04_vistas_analisis.sql
--  NO ejecutar 05: es solo para migrar una base vieja sin borrarla. Sobre una
--  base recién creada por 01 no tiene nada que hacer y dará error.
--
--  Y AL TERMINAR: pulsar "Refresh schema cache" en la página Data API.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. COMPROBACIÓN PREVIA (opcional pero recomendable)
--    Descomenta y ejecuta SOLO esto primero para ver cuántos datos vas a
--    perder. Si devuelve ceros, borrar es inofensivo.
-- ---------------------------------------------------------------------------
-- SELECT 'biometria' AS tabla, COUNT(*) FROM public.registro_biometria
-- UNION ALL SELECT 'agua',        COUNT(*) FROM public.registro_agua
-- UNION ALL SELECT 'laboratorio', COUNT(*) FROM public.ensayo_laboratorio;

-- ---------------------------------------------------------------------------
-- 2. VISTAS
--    Van primero porque dependen de las tablas.
-- ---------------------------------------------------------------------------
DROP VIEW IF EXISTS public.v_semaforo_actual        CASCADE;
DROP VIEW IF EXISTS public.v_mortalidad_piscina     CASCADE;
DROP VIEW IF EXISTS public.v_crecimiento_mensual    CASCADE;
DROP VIEW IF EXISTS public.v_muestreo_biometria     CASCADE;
DROP VIEW IF EXISTS public.v_consolidado_fuentes    CASCADE;
DROP VIEW IF EXISTS public.v_desfase_sincronizacion CASCADE;

-- ---------------------------------------------------------------------------
-- 3. TABLAS
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS public.registro_biometria CASCADE;
DROP TABLE IF EXISTS public.registro_agua      CASCADE;
DROP TABLE IF EXISTS public.ensayo_laboratorio CASCADE;
DROP TABLE IF EXISTS public.piscina            CASCADE;
DROP TABLE IF EXISTS public.perfil_productor   CASCADE;

-- ---------------------------------------------------------------------------
-- 4. COMPROBACIÓN
--    Debe devolver CERO filas. Si aparece alguna, quedó algo sin borrar.
-- ---------------------------------------------------------------------------
SELECT table_name
  FROM information_schema.tables
 WHERE table_schema = 'public'
   AND table_name IN ('registro_biometria','registro_agua','ensayo_laboratorio',
                      'piscina','perfil_productor');
