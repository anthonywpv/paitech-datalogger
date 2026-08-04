package ec.edu.espol.paipay.datalogger.ui.sync;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.databinding.DialogoConfirmarSyncBinding;
import ec.edu.espol.paipay.datalogger.databinding.FragmentSincronizacionBinding;
import ec.edu.espol.paipay.datalogger.sync.ResultadoSincronizacion;
import ec.edu.espol.paipay.datalogger.sync.ResumenPendientes;
import ec.edu.espol.paipay.datalogger.sync.SincronizacionRepositorio;
import ec.edu.espol.paipay.datalogger.ui.login.LoginActivity;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;
import ec.edu.espol.paipay.datalogger.util.RedUtil;

/**
 * ===========================================================================
 *  PANTALLA DE SINCRONIZACIÓN
 * ===========================================================================
 *
 * Implementa el requisito literal del proyecto: al pulsar [Sincronizar], antes
 * de subir nada, se le advierte al productor que necesita conexión estable y se
 * le enumeran exactamente los datos guardados localmente que se van a enviar a
 * la Base de Datos principal. Solo tras su confirmación explícita comienza la
 * subida.
 */
public class SincronizacionFragment extends Fragment {

    private FragmentSincronizacionBinding vista;
    private SincronizacionRepositorio sincronizacion;
    private SesionManager sesion;

    /**
     * Vuelta de la pantalla de credenciales. Si el productor se identificó, la
     * subida continúa sola: no tiene que volver a pulsar [Sincronizar].
     */
    private final ActivityResultLauncher<Intent> pedirCredenciales =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() == Activity.RESULT_OK) {
                            pedirConfirmacion();
                        }
                        refrescar();
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentSincronizacionBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(raiz, savedInstanceState);

        sincronizacion = new SincronizacionRepositorio(requireContext());
        sesion = SesionManager.obtener(requireContext());

        vista.botonSincronizar.setOnClickListener(v -> iniciarSubida());
        refrescar();
    }

    @Override
    public void onResume() {
        super.onResume();
        refrescar();
    }

    // ---------------------------------------------------------------
    //  Estado de la pantalla
    // ---------------------------------------------------------------

    private void refrescar() {
        pintarConexion();

        sincronizacion.contarPendientes(resumen -> {
            if (vista == null) return;

            vista.textoTotalPendientes.setText(String.valueOf(resumen.total()));

            if (resumen.hayPendientes()) {
                vista.textoDetallePendientes.setText(resumen.detalle());
                vista.botonSincronizar.setEnabled(true);
            } else {
                vista.textoDetallePendientes.setText(R.string.sync_sin_pendientes);
                vista.botonSincronizar.setEnabled(false);
            }

            long ultima = sesion.getUltimaSincronizacion();
            vista.textoUltimaSync.setText(ultima < 0
                    ? getString(R.string.sync_nunca)
                    : getString(R.string.sync_ultima, FechaUtil.conHora(ultima)));
        });
    }

    private void pintarConexion() {
        boolean conectado = RedUtil.hayInternet(requireContext());

        vista.textoConexion.setText(conectado
                ? R.string.sync_estado_conectado
                : R.string.sync_estado_sin_conexion);
        vista.puntoConexion.setBackgroundResource(conectado
                ? R.drawable.punto_verde
                : R.drawable.punto_rojo);
        vista.tarjetaConexion.setStrokeColor(ContextCompat.getColor(requireContext(),
                conectado ? R.color.semaforo_verde : R.color.semaforo_rojo));
        vista.tarjetaConexion.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                conectado ? R.color.semaforo_verde_fondo : R.color.semaforo_rojo_fondo));
    }

    // ---------------------------------------------------------------
    //  Credenciales
    // ---------------------------------------------------------------

    /**
     * Puerta de entrada de [Sincronizar].
     *
     * Registrar no exige cuenta, pero subir sí: la base principal tiene que
     * saber quién manda cada dato. Si todavía no hay sesión se piden las
     * credenciales y, al volver, la subida sigue sola.
     */
    private void iniciarSubida() {
        if (sesion.haySesionActiva()) {
            pedirConfirmacion();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sync_credenciales_titulo)
                .setIcon(R.drawable.ic_candado)
                .setMessage(R.string.sync_credenciales_mensaje)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.sync_credenciales_entrar,
                        (d, w) -> pedirCredenciales.launch(
                                LoginActivity.intent(requireContext())))
                .show();
    }

    // ---------------------------------------------------------------
    //  Diálogo de confirmación previo
    // ---------------------------------------------------------------

    private void pedirConfirmacion() {
        sincronizacion.contarPendientes(resumen -> {
            if (vista == null) return;

            if (!resumen.hayPendientes()) {
                avisoSimple(R.string.sync_titulo, getString(R.string.sync_sin_pendientes));
                return;
            }

            DialogoConfirmarSyncBinding cuerpo = DialogoConfirmarSyncBinding
                    .inflate(LayoutInflater.from(requireContext()));

            cuerpo.textoListaDatos.setText(resumen.detalle());

            if (RedUtil.esDatosMoviles(requireContext())) {
                cuerpo.bloqueAvisoDatos.setVisibility(View.VISIBLE);
                cuerpo.textoAvisoDatos.setText(
                        "Estás usando datos móviles. La subida consume muy pocos megas, "
                                + "pero con wifi es más segura.");
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.sync_dialogo_titulo)
                    .setIcon(R.drawable.ic_nube_subir)
                    .setView(cuerpo.getRoot())
                    .setNegativeButton(R.string.sync_dialogo_cancelar, null)
                    .setPositiveButton(R.string.sync_dialogo_confirmar,
                            (d, w) -> ejecutarSincronizacion(resumen))
                    .show();
        });
    }

    // ---------------------------------------------------------------
    //  Subida
    // ---------------------------------------------------------------

    private void ejecutarSincronizacion(ResumenPendientes resumen) {
        mostrarProgreso(true, getString(R.string.sync_en_proceso));

        sincronizacion.sincronizar(new SincronizacionRepositorio.CallbackSincronizacion() {
            @Override
            public void progreso(String mensaje) {
                if (vista == null) return;
                vista.textoProgreso.setText(mensaje);
            }

            @Override
            public void terminado(ResultadoSincronizacion resultado) {
                if (vista == null) return;
                mostrarProgreso(false, null);
                informarResultado(resultado);
                refrescar();
            }
        });
    }

    private void informarResultado(ResultadoSincronizacion resultado) {
        switch (resultado.estado) {
            case EXITO:
                avisoSimple(R.string.sync_exito, String.format(Locale.US,
                        "Se subieron %d registro(s) a la base principal.\n\n"
                                + "Tus datos siguen guardados también en el teléfono.",
                        resultado.subidos));
                break;

            case PARCIAL:
                avisoSimple(R.string.sync_parcial, String.format(Locale.US,
                        "Se subieron %d registro(s), pero %d no pudieron enviarse.\n\n"
                                + "Vuelve a intentarlo cuando la señal esté más estable. "
                                + "Nada se perdió.",
                        resultado.subidos, resultado.fallidos));
                break;

            case SIN_INTERNET:
                avisoSimple(R.string.sync_titulo, getString(R.string.sync_sin_internet));
                break;

            case SIN_PENDIENTES:
                avisoSimple(R.string.sync_titulo, getString(R.string.sync_sin_pendientes));
                break;

            case SESION_EXPIRADA:
                // La cookie ya no sirve. Se olvida y se vuelve a pedir en el
                // sitio, en vez de mandar al productor a buscar el menú.
                sesion.cerrarSesion();
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.sync_parcial)
                        .setIcon(R.drawable.ic_candado)
                        .setMessage(R.string.sync_sesion_expirada)
                        .setNegativeButton(R.string.cancelar, null)
                        .setPositiveButton(R.string.sync_credenciales_entrar,
                                (d, w) -> pedirCredenciales.launch(
                                        LoginActivity.intent(requireContext())))
                        .show();
                break;

            default:
                String detalle = getString(R.string.sync_error);
                if (resultado.mensajeTecnico != null) {
                    detalle += "\n\nDetalle técnico: " + resultado.mensajeTecnico;
                }
                avisoSimple(R.string.sync_parcial, detalle);
                break;
        }
    }

    private void mostrarProgreso(boolean visible, String mensaje) {
        vista.bloqueProgreso.setVisibility(visible ? View.VISIBLE : View.GONE);
        vista.botonSincronizar.setEnabled(!visible);
        if (mensaje != null) vista.textoProgreso.setText(mensaje);
    }

    private void avisoSimple(int titulo, String mensaje) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton(R.string.aceptar, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
