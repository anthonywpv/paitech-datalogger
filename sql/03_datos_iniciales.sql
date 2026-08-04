-- ===========================================================================
--  DATOS INICIALES
-- ===========================================================================

-- Piscinas y lechos del recinto Paipayales.
-- Estos mismos códigos vienen precargados en la app (PaipayDatabase.SEMILLA),
-- para que el productor pueda registrar desde el primer día sin sincronizar.
INSERT INTO public.piscina (codigo, nombre, area_m2, activa) VALUES
    ('P-01',   'Piscina 1 - Engorde',                120.00, TRUE),
    ('P-02',   'Piscina 2 - Engorde',                120.00, TRUE),
    ('P-03',   'Piscina 3 - Alevinaje',               60.00, TRUE),
    ('P-04',   'Piscina 4 - Reproductores',           80.00, TRUE),
    ('UE-01',  'U.E. Galo Plaza - Demostrativa',      40.00, TRUE),
    ('LOM-01', 'Lecho lombricultura 1',               12.00, TRUE),
    ('LOM-02', 'Lecho lombricultura 2',               12.00, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- ---------------------------------------------------------------------------
--  USUARIOS
--
--  NO se crean por SQL. Las cuentas las administra Neon Auth (Managed Better
--  Auth), que es quien guarda las contraseñas con hashing seguro.
--
--  Para crear el primer usuario de prueba, desde la terminal:
--
--    curl -X POST 'https://TU-ENDPOINT.neonauth.REGION.aws.neon.tech/neondb/auth/sign-up/email' \
--      -H 'Content-Type: application/json' \
--      -H 'Origin: https://paipay.espol.edu.ec' \
--      -d '{"email":"productor@paipayales.ec",
--           "password":"${PAIPAY_PASSWORD}",
--           "name":"Productor Paipayales",
--           "callbackURL":"https://paipay.espol.edu.ec"}'
--
--  (El Origin debe estar declarado como confiable en la consola de Neon.)
--
--  Después, ya con sesión iniciada desde la app, cada usuario crea su perfil:
--
--    INSERT INTO public.perfil_productor (nombre, comuna, rol)
--    VALUES ('Productor Paipayales', 'Paipayales', 'PRODUCTOR');
--
--  (user_id se llena solo con auth.user_id().)
-- ---------------------------------------------------------------------------
