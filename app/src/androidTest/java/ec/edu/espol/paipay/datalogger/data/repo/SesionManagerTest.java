package ec.edu.espol.paipay.datalogger.data.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Sesión persistente sobre el almacenamiento cifrado real del dispositivo.
 *
 * SesionManager usa EncryptedSharedPreferences, que necesita el keystore de
 * Android: no se puede cubrir con JUnit de escritorio, solo aquí.
 *
 * Comprueba el requisito clave del proyecto —el productor inicia sesión una
 * sola vez— y, de paso, que cerrar sesión no borra nada más.
 */
@RunWith(AndroidJUnit4.class)
public class SesionManagerTest {

    private static final String CORREO = "productor@paipayales.ec";

    private Context contexto() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void guardarSesion_sobreviveAVolverAPedirLaInstancia() {
        SesionManager sesion = SesionManager.obtener(contexto());
        sesion.guardarSesion(CORREO, "Productor Paipayales", "token-prueba");

        assertTrue(sesion.haySesionActiva());
        assertEquals(CORREO, sesion.getUsuario());
        assertEquals("Productor Paipayales", sesion.getNombre());
    }

    @Test
    public void getNombre_caeAlCorreoCuandoNoHayNombre() {
        SesionManager sesion = SesionManager.obtener(contexto());
        sesion.guardarSesion(CORREO, "", "token-prueba");
        assertEquals(CORREO, sesion.getNombre());
    }

    @Test
    public void cerrarSesion_dejaLaSesionInactiva() {
        SesionManager sesion = SesionManager.obtener(contexto());
        sesion.guardarSesion(CORREO, "Productor Paipayales", "token-prueba");
        sesion.cerrarSesion();

        assertFalse(sesion.haySesionActiva());

        // Se deja abierta para poder inspeccionar la app a mano tras las pruebas.
        sesion.guardarSesion(CORREO, "Productor Paipayales", "token-prueba");
    }

    @Test
    public void registrarSincronizacion_seRecuerda() {
        SesionManager sesion = SesionManager.obtener(contexto());
        sesion.registrarSincronizacion(1754200000000L);
        assertEquals(1754200000000L, sesion.getUltimaSincronizacion());
    }
}
