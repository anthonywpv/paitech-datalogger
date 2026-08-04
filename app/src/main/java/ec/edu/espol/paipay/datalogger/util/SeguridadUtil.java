package ec.edu.espol.paipay.datalogger.util;

import java.util.UUID;

/**
 * Utilidades de identificación.
 *
 * Nota: la app NO maneja contraseñas por su cuenta. La autenticación la
 * resuelve Neon Auth (Managed Better Auth) del lado del servidor, que ya
 * aplica hashing seguro. Aquí solo se generan identificadores de registro.
 */
public final class SeguridadUtil {

    private SeguridadUtil() { }

    /**
     * Identificador único de registro, generado sin necesidad de internet.
     *
     * Es la clave que vuelve idempotente la sincronización: la base principal
     * hace UPSERT sobre este uuid, de modo que reintentar una subida no
     * duplica filas.
     */
    public static String nuevoUuid() {
        return UUID.randomUUID().toString();
    }
}
