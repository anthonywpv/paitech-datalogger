-- ===========================================================================
--  PERMISOS Y SEGURIDAD A NIVEL DE FILA (RLS)
--
--  La Data API queda expuesta en internet, así que la seguridad NO puede vivir
--  en la app: vive en la base de datos. Sin estas políticas, o la app recibe
--  "permission denied", o cualquiera con un token vería todo.
--
--  Modelo elegido para Paipayales:
--    · Todos los usuarios autenticados PUEDEN LEER todos los registros.
--      Es un proyecto comunitario: los datos de la comuna son de la comuna, y
--      los estudiantes y técnicos necesitan verlos completos para analizarlos.
--    · Cada usuario solo puede ESCRIBIR filas a su propio nombre.
--      Así nadie puede alterar ni suplantar el registro de otro productor.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. PRIVILEGIOS DE TABLA
--    (si al habilitar la Data API marcaste "Grant public schema access",
--     Neon ya ejecutó esto por ti; repetirlo no hace daño)
-- ---------------------------------------------------------------------------
GRANT USAGE ON SCHEMA public TO authenticated;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES
    IN SCHEMA public TO authenticated;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO authenticated;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authenticated;

-- ---------------------------------------------------------------------------
-- 2. ACTIVAR RLS
-- ---------------------------------------------------------------------------
ALTER TABLE public.perfil_productor    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.piscina             ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.registro_biometria  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.registro_agua       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ensayo_laboratorio  ENABLE ROW LEVEL SECURITY;

-- ---------------------------------------------------------------------------
-- 3. POLÍTICAS
-- ---------------------------------------------------------------------------

-- Catálogo de piscinas: lectura para todos los autenticados, escritura solo
-- desde la consola (el rol authenticated no la modifica).
DROP POLICY IF EXISTS piscina_lectura ON public.piscina;
CREATE POLICY piscina_lectura ON public.piscina
    FOR SELECT TO authenticated
    USING (TRUE);

-- Perfil: cada quien administra el suyo, pero todos pueden ver los nombres.
DROP POLICY IF EXISTS perfil_lectura ON public.perfil_productor;
CREATE POLICY perfil_lectura ON public.perfil_productor
    FOR SELECT TO authenticated
    USING (TRUE);

DROP POLICY IF EXISTS perfil_propio ON public.perfil_productor;
CREATE POLICY perfil_propio ON public.perfil_productor
    FOR ALL TO authenticated
    USING ((SELECT auth.user_id()) = user_id)
    WITH CHECK ((SELECT auth.user_id()) = user_id);

-- Plantilla aplicada a las tres tablas de registro:
--   lectura comunitaria + escritura solo de lo propio.

DROP POLICY IF EXISTS biometria_lectura ON public.registro_biometria;
CREATE POLICY biometria_lectura ON public.registro_biometria
    FOR SELECT TO authenticated
    USING (TRUE);

DROP POLICY IF EXISTS biometria_escritura ON public.registro_biometria;
CREATE POLICY biometria_escritura ON public.registro_biometria
    FOR ALL TO authenticated
    USING ((SELECT auth.user_id()) = user_id)
    WITH CHECK ((SELECT auth.user_id()) = user_id);

DROP POLICY IF EXISTS agua_lectura ON public.registro_agua;
CREATE POLICY agua_lectura ON public.registro_agua
    FOR SELECT TO authenticated
    USING (TRUE);

DROP POLICY IF EXISTS agua_escritura ON public.registro_agua;
CREATE POLICY agua_escritura ON public.registro_agua
    FOR ALL TO authenticated
    USING ((SELECT auth.user_id()) = user_id)
    WITH CHECK ((SELECT auth.user_id()) = user_id);

DROP POLICY IF EXISTS lab_lectura ON public.ensayo_laboratorio;
CREATE POLICY lab_lectura ON public.ensayo_laboratorio
    FOR SELECT TO authenticated
    USING (TRUE);

DROP POLICY IF EXISTS lab_escritura ON public.ensayo_laboratorio;
CREATE POLICY lab_escritura ON public.ensayo_laboratorio
    FOR ALL TO authenticated
    USING ((SELECT auth.user_id()) = user_id)
    WITH CHECK ((SELECT auth.user_id()) = user_id);

-- ---------------------------------------------------------------------------
-- 4. RECORDATORIO
--    Tras crear o modificar tablas hay que pulsar "Refresh schema cache" en la
--    página Data API de la consola de Neon. Si no, la API sigue respondiendo
--    con el esquema viejo y la app recibirá errores 404 o 400.
-- ---------------------------------------------------------------------------
