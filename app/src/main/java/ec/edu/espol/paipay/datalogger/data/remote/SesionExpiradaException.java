package ec.edu.espol.paipay.datalogger.data.remote;

import java.io.IOException;

/**
 * La cookie de sesión de Neon Auth ya no sirve: caducó o fue revocada, y por
 * tanto no se puede obtener un JWT para hablar con la Data API.
 *
 * Es distinto de "no hay internet". Aquí reintentar no arregla nada: hace falta
 * que el productor vuelva a iniciar sesión desde la app. Por eso viaja como un
 * tipo propio y no como un IOException con un mensaje reconocible — el motor de
 * sincronización lo distingue con un catch, no comparando cadenas de texto.
 *
 * Extiende IOException porque se lanza desde un interceptor de OkHttp, que solo
 * puede fallar con ese tipo.
 */
public class SesionExpiradaException extends IOException {

    public SesionExpiradaException() {
        super("La sesión caducó: hay que iniciar sesión de nuevo.");
    }
}
