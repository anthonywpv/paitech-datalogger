-- ===========================================================================
--  DATOS INICIALES
-- ===========================================================================

-- Las DOS unidades que existen en el recinto: una piscina de Vieja Azul y un
-- lecho de lombricultura. No se listan piscinas hipotéticas: ofrecer una que
-- no existe solo invita a registrar datos en la equivocada.
-- Estos mismos códigos vienen precargados en la app (PaipayDatabase.SEMILLA),
-- para que el productor pueda registrar desde el primer día sin sincronizar.
INSERT INTO public.piscina (codigo, nombre, area_m2, activa) VALUES
    ('P-01',   'Piscina 1 Paipayales',     120.00, TRUE),
    ('LOM-01', 'Lecho de Lombricultura 1',  12.00, TRUE)
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
