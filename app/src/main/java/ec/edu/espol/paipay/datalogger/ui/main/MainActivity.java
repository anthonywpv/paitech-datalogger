package ec.edu.espol.paipay.datalogger.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.repo.AutenticacionRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.databinding.ActivityMainBinding;
import ec.edu.espol.paipay.datalogger.sync.SincronizacionWorker;
import ec.edu.espol.paipay.datalogger.ui.historial.HistorialFragment;
import ec.edu.espol.paipay.datalogger.ui.registro.RegistroFragment;
import ec.edu.espol.paipay.datalogger.ui.semaforo.SemaforoFragment;
import ec.edu.espol.paipay.datalogger.ui.login.LoginActivity;
import ec.edu.espol.paipay.datalogger.ui.sync.SincronizacionFragment;

/** Contenedor principal con las cuatro secciones de la app. */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding vista;
    private SesionManager sesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vista = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());

        sesion = SesionManager.obtener(this);
        if (!sesion.haySesionActiva()) {
            irALogin();
            return;
        }

        // Se usa la Toolbar directamente (sin setSupportActionBar) para que el
        // menú declarado con app:menu conserve su propio listener.
        vista.barraHerramientas.setTitle(getString(R.string.app_name));
        vista.barraHerramientas.setSubtitle(sesion.getNombre());

        vista.barraHerramientas.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accion_cerrar_sesion) {
                confirmarCierreSesion();
                return true;
            }
            return false;
        });

        vista.navegacionInferior.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_registro) return mostrar(new RegistroFragment());
            if (id == R.id.nav_semaforo) return mostrar(new SemaforoFragment());
            if (id == R.id.nav_historial) return mostrar(new HistorialFragment());
            if (id == R.id.nav_sincronizar) return mostrar(new SincronizacionFragment());
            return false;
        });

        if (savedInstanceState == null) {
            // El semáforo es la pantalla de inicio: al abrir la app, lo primero
            // que el productor tiene que ver es si alguna piscina está en rojo.
            vista.navegacionInferior.setSelectedItemId(R.id.nav_semaforo);
        }

        // Si quedaron datos pendientes de una sesión anterior, se programa un
        // intento automático para cuando el teléfono recupere conexión.
        SincronizacionWorker.programar(this);
    }

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
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_cerrar_sesion)
                .setMessage(R.string.cerrar_sesion_pregunta)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (d, w) -> {
                    new AutenticacionRepositorio(this).cerrarSesion();
                    irALogin();
                })
                .show();
    }

    private void irALogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
