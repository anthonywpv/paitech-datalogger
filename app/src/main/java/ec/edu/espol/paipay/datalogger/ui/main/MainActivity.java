package ec.edu.espol.paipay.datalogger.ui.main;

import android.app.KeyguardManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.repo.AlmacenamientoRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.AutenticacionRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.databinding.ActivityMainBinding;
import ec.edu.espol.paipay.datalogger.sync.SincronizacionWorker;
import ec.edu.espol.paipay.datalogger.ui.historial.HistorialFragment;
import ec.edu.espol.paipay.datalogger.ui.ciclo.CicloFragment;
import ec.edu.espol.paipay.datalogger.ui.login.LoginActivity;
import ec.edu.espol.paipay.datalogger.ui.registro.RegistroFragment;
import ec.edu.espol.paipay.datalogger.ui.semaforo.SemaforoFragment;
import ec.edu.espol.paipay.datalogger.ui.sync.SincronizacionFragment;

/** Contenedor principal: las cuatro secciones más el menú lateral. */
public class MainActivity extends AppCompatActivity {

    private static final String DRIVE_APK_URL =
            "https://drive.google.com/drive/folders/1Yvy9CwyEFOKCLgKGB7nig4FBN5NUm-vT?usp=sharing";

    private ActivityMainBinding vista;
    private SesionManager sesion;
    private AlmacenamientoRepositorio almacenamiento;
    private boolean avisoEspacioMostrado;
    private boolean avisoDispositivoMostrado;
    private int recursoManualPendiente;

    private final ActivityResultLauncher<Intent> selectorDestinoManual =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() != Activity.RESULT_OK
                                || resultado.getData() == null
                                || resultado.getData().getData() == null
                                || recursoManualPendiente == 0) {
                            recursoManualPendiente = 0;
                            return;
                        }
                        guardarManual(resultado.getData().getData(), recursoManualPendiente);
                        recursoManualPendiente = 0;
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Paipay);
        vista = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());

        // El primer ingreso requiere conexión; después el token cifrado permite
        // abrir la app y atribuir registros correctamente incluso sin señal.
        sesion = SesionManager.obtener(this);
        if (!sesion.haySesionActiva()) {
            startActivity(sesion.hayCredencialesGuardadas()
                    ? LoginActivity.intentSesionExpirada(this)
                    : LoginActivity.intent(this));
            finish();
            return;
        }
        almacenamiento = new AlmacenamientoRepositorio(this);

        vista.barraHerramientas.setNavigationOnClickListener(
                v -> vista.cajon.openDrawer(GravityCompat.START));

        configurarMenuLateral();

        vista.navegacionInferior.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_registro) return mostrar(new RegistroFragment());
            if (id == R.id.nav_ciclos) return mostrar(new CicloFragment());
            if (id == R.id.nav_semaforo) return mostrar(new SemaforoFragment());
            if (id == R.id.nav_historial) return mostrar(new HistorialFragment());
            if (id == R.id.nav_sincronizar) return mostrar(new SincronizacionFragment());
            return false;
        });

        if (savedInstanceState == null) {
            // El semáforo es la pantalla de inicio: al abrir la app, lo primero
            // que el productor tiene que ver es si algo está en rojo.
            vista.navegacionInferior.setSelectedItemId(R.id.nav_semaforo);
        }

        // Con el menú abierto, "atrás" debe cerrarlo antes que salir de la app.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (vista.cajon.isDrawerOpen(GravityCompat.START)) {
                    vista.cajon.closeDrawer(GravityCompat.START);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        // Si quedaron datos pendientes de una sesión anterior, se programa un
        // intento automático para cuando el teléfono recupere conexión.
        SincronizacionWorker.programar(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pintarMenuLateral();
        advertirEspacioBajo();
        advertirDispositivoSinBloqueo();
    }

    // ------------------------------------------------------------------
    //  MENÚ LATERAL
    // ------------------------------------------------------------------

    private void configurarMenuLateral() {
        vista.menuLateral.opcionAlmacenamiento.setOnClickListener(v -> {
            cerrarMenu();
            mostrarAlmacenamiento();
        });

        vista.menuLateral.opcionManual.setOnClickListener(v -> {
            cerrarMenu();
            mostrarManuales();
        });

        vista.menuLateral.opcionCerrarSesion.setOnClickListener(v -> {
            if (!puedeCerrarSesion()) return;
            cerrarMenu();
            confirmarCierreSesion();
        });
    }

    /** Cerrar sesión solo está disponible cuando existe una identidad activa. */
    private boolean puedeCerrarSesion() {
        return sesion.haySesionActiva();
    }

    /** Refresca lo que el menú muestra: sesión, espacio ocupado y disponibilidad. */
    private void pintarMenuLateral() {
        boolean hay = sesion.haySesionActiva();
        vista.menuLateral.textoSesionMenu.setText(hay
                ? sesion.getNombre() + "\n" + sesion.getComunidadNombre()
                : getString(R.string.sesion_sin_iniciar));

        boolean puede = puedeCerrarSesion();
        vista.menuLateral.opcionCerrarSesion.setEnabled(puede);
        vista.menuLateral.opcionCerrarSesion.setAlpha(puede ? 1f : 0.4f);
        vista.menuLateral.textoCerrarSesionEstado.setText(puede
                ? getString(R.string.menu_cerrar_sesion_activa, sesion.getUsuario())
                : getString(R.string.menu_cerrar_sesion_no_disponible));

        almacenamiento.resumir(resumen -> {
            if (vista == null) return;
            vista.menuLateral.textoAlmacenamientoResumen.setText(
                    getString(R.string.menu_almacenamiento_resumen,
                            resumen.total(), resumen.tamanoLegible()));
        });
    }

    private void cerrarMenu() {
        vista.cajon.closeDrawer(GravityCompat.START);
    }

    private void mostrarManuales() {
        String[] opciones = {
                getString(R.string.manual_acuicultor),
                getString(R.string.manual_administrador),
                getString(R.string.manual_instalacion),
                getString(R.string.manual_revisar_apk)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_manual)
                .setIcon(R.drawable.ic_info)
                .setItems(opciones, (dialogo, cual) -> {
                    if (cual == 0) {
                        elegirDestinoManual(
                                R.raw.manual_acuicultor_paipaytech,
                                "manual_acuicultor_paipaytech.pdf");
                    } else if (cual == 1) {
                        elegirDestinoManual(
                                R.raw.manual_administrador_paipaytech,
                                "manual_administrador_paipaytech.pdf");
                    } else if (cual == 2) {
                        elegirDestinoManual(
                                R.raw.guia_instalacion_paipaytech,
                                "guia_instalacion_paipaytech.pdf");
                    } else {
                        abrirCarpetaApk();
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void elegirDestinoManual(@RawRes int recurso, String nombre) {
        recursoManualPendiente = recurso;
        Intent destino = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_TITLE, nombre);
        selectorDestinoManual.launch(destino);
    }

    private void guardarManual(Uri destino, @RawRes int recurso) {
        try (InputStream entrada = getResources().openRawResource(recurso);
             OutputStream salida = getContentResolver().openOutputStream(destino, "w")) {
            if (salida == null) throw new IOException("Android no abrió el destino");
            byte[] bloque = new byte[16 * 1024];
            int leidos;
            while ((leidos = entrada.read(bloque)) != -1) {
                salida.write(bloque, 0, leidos);
            }
            salida.flush();
            mostrarManualGuardado(destino);
        } catch (IOException error) {
            Toast.makeText(this, R.string.manual_guardado_error, Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarManualGuardado(Uri destino) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.manual_guardado_titulo)
                .setMessage(R.string.manual_guardado_mensaje)
                .setNegativeButton(R.string.aceptar, null)
                .setPositiveButton(R.string.manual_abrir, (dialogo, cual) -> {
                    Intent abrir = new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(destino, "application/pdf")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(Intent.createChooser(abrir, getString(R.string.manual_abrir)));
                    } catch (Exception error) {
                        Toast.makeText(this, R.string.manual_sin_lector, Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void abrirCarpetaApk() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DRIVE_APK_URL)));
        } catch (Exception error) {
            Toast.makeText(this, R.string.manual_drive_error, Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------
    //  ALMACENAMIENTO
    // ------------------------------------------------------------------

    private void mostrarAlmacenamiento() {
        almacenamiento.resumir(resumen -> {
            String cuerpo = getString(R.string.almacenamiento_ocupado, resumen.tamanoLegible())
                    + "\n"
                    + getString(R.string.almacenamiento_detalle,
                            resumen.sincronizados, resumen.pendientes)
                    + "\n\n"
                    + getString(R.string.almacenamiento_aviso);

            MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.almacenamiento_titulo)
                    .setMessage(cuerpo)
                    .setNegativeButton(R.string.aceptar, null);

            if (resumen.hayQueLiberar()) {
                dialogo.setPositiveButton(
                        getString(R.string.almacenamiento_liberar, resumen.sincronizados),
                        (d, w) -> confirmarLiberar(resumen.sincronizados));
            }
            dialogo.show();
        });
    }

    private void confirmarLiberar(int cuantos) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.almacenamiento_titulo)
                .setIcon(R.drawable.ic_borrar)
                .setMessage(getString(R.string.almacenamiento_confirmar, cuantos))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (d, w) ->
                        almacenamiento.liberarSincronizados(borrados -> {
                            pintarMenuLateral();
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.almacenamiento_titulo)
                                    .setMessage(getString(
                                            R.string.almacenamiento_borrados, borrados))
                                    .setPositiveButton(R.string.aceptar, null)
                                    .show();
                        }))
                .show();
    }

    // ------------------------------------------------------------------

    private boolean mostrar(@NonNull Fragment fragmento) {
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.contenedor, fragmento)
                .commit();
        return true;
    }

    /** Permite que un fragmento mande al usuario a otra pestaña. */
    public void irASeccion(int idMenu) {
        vista.navegacionInferior.setSelectedItemId(idMenu);
    }

    private void confirmarCierreSesion() {
        almacenamiento.resumir(resumen -> {
            if (resumen.pendientes > 0) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Hay datos pendientes")
                        .setMessage("Antes de cerrar sesión debes sincronizar los "
                                + resumen.pendientes + " borradores o cambios asociados a esta cuenta. "
                                + "Así otra cuenta no podrá apropiarse de su autoría.")
                        .setPositiveButton(R.string.aceptar, null)
                        .show();
                return;
            }
            confirmarCierreSinPendientes();
        });
    }

    private void confirmarCierreSinPendientes() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_cerrar_sesion)
                .setMessage(R.string.cerrar_sesion_pregunta)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (d, w) -> {
                    // Se vuelve al login: sin una identidad activa no se crean
                    // registros cuya autoría pueda quedar ambigua.
                    new AutenticacionRepositorio(this).cerrarSesionCompleta(() -> {
                        startActivity(LoginActivity.intent(this));
                        finish();
                    });
                })
                .show();
    }

    private void advertirDispositivoSinBloqueo() {
        if (avisoDispositivoMostrado || isFinishing()) return;
        KeyguardManager seguridad = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (seguridad != null && seguridad.isDeviceSecure()) return;
        avisoDispositivoMostrado = true;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.seguridad_dispositivo_titulo)
                .setIcon(R.drawable.ic_candado)
                .setMessage(R.string.seguridad_dispositivo_mensaje)
                .setPositiveButton(R.string.aceptar, null)
                .show();
    }

    private void advertirEspacioBajo() {
        if (avisoEspacioMostrado) return;
        almacenamiento.resumir(resumen -> {
            if (!resumen.espacioBajo() || isFinishing()) return;
            avisoEspacioMostrado = true;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Poco espacio disponible")
                    .setIcon(R.drawable.ic_alerta)
                    .setMessage("Quedan " + resumen.disponibleLegible()
                            + ". Sincroniza y libera copias locales antes de registrar una jornada grande.")
                    .setPositiveButton(R.string.aceptar, null)
                    .show();
        });
    }
}
