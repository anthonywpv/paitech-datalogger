package ec.edu.espol.paipay.datalogger.ui.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.repo.AutenticacionRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.databinding.ActivityLoginBinding;
import ec.edu.espol.paipay.datalogger.databinding.DialogCambiarClaveBinding;
import ec.edu.espol.paipay.datalogger.ui.main.MainActivity;

/**
 * Pantalla de credenciales.
 *
 * Es el punto de entrada cuando todavía no hay identidad local. El primer
 * ingreso requiere internet para validar la cuenta y descargar piscinas; luego
 * la sesión cifrada permite trabajar offline hasta el cierre explícito.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String EXTRA_SESION_OFFLINE_EXPIRADA = "sesion_offline_expirada";

    private ActivityLoginBinding vista;
    private AutenticacionRepositorio autenticacion;

    /** Intent para pedir credenciales desde cualquier pantalla. */
    public static Intent intent(Context contexto) {
        return new Intent(contexto, LoginActivity.class);
    }

    public static Intent intentSesionExpirada(Context contexto) {
        return intent(contexto).putExtra(EXTRA_SESION_OFFLINE_EXPIRADA, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SesionManager sesion = SesionManager.obtener(this);
        if (sesion.haySesionActiva()) {
            abrirPrincipal();
            return;
        }
        // Si Android cerró el proceso mientras se mostraba el cambio inicial,
        // se exige repetir el login; nunca se conserva la contraseña temporal.
        boolean cambioInterrumpido = sesion.requiereCambioClave();

        vista = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());

        autenticacion = new AutenticacionRepositorio(this);
        if (cambioInterrumpido) autenticacion.cerrarSesion();
        vista.botonEntrar.setOnClickListener(v -> intentarIngreso());
        if (getIntent().getBooleanExtra(EXTRA_SESION_OFFLINE_EXPIRADA, false)) {
            vista.textoMensaje.setText(R.string.sesion_offline_expirada);
            vista.textoMensaje.setVisibility(View.VISIBLE);
        }
    }

    private void intentarIngreso() {
        String usuario = vista.campoUsuarioTexto.getText() == null
                ? "" : vista.campoUsuarioTexto.getText().toString().trim();
        String clave = vista.campoClaveTexto.getText() == null
                ? "" : vista.campoClaveTexto.getText().toString();

        vista.campoUsuario.setError(null);
        vista.campoClave.setError(null);

        if (TextUtils.isEmpty(usuario) || TextUtils.isEmpty(clave)) {
            vista.textoMensaje.setText(R.string.login_error_vacio);
            vista.textoMensaje.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(usuario)) vista.campoUsuario.setError(" ");
            if (TextUtils.isEmpty(clave)) vista.campoClave.setError(" ");
            return;
        }

        mostrarCargando(true);

        autenticacion.iniciarSesion(usuario, clave, true,
                new AutenticacionRepositorio.Callback() {
            @Override
            public void onExito(String nombre, boolean requiereCambioClave) {
                mostrarCargando(false);
                if (requiereCambioClave) {
                    mostrarCambioClave(clave);
                    return;
                }
                abrirPrincipal();
            }

            @Override
            public void onError(AutenticacionRepositorio.Resultado resultado) {
                mostrarCargando(false);
                int mensaje;
                switch (resultado) {
                    case SIN_INTERNET:
                        mensaje = R.string.login_error_sin_internet;
                        break;
                    case CREDENCIALES_INVALIDAS:
                        mensaje = R.string.login_error_credenciales;
                        break;
                    case CUENTA_BLOQUEADA:
                        mensaje = R.string.login_error_bloqueado;
                        break;
                    case DATOS_DE_OTRA_CUENTA:
                        mensaje = R.string.login_error_otra_cuenta;
                        break;
                    case DATOS_PENDIENTES_DE_OTRA_COMUNIDAD:
                        mensaje = R.string.login_error_otra_comunidad;
                        break;
                    case RECHAZADO_POR_SERVIDOR:
                        mensaje = R.string.login_error_rechazado;
                        break;
                    default:
                        // Antes reutilizaba el texto de sincronización, que en
                        // esta pantalla no venía a cuento.
                        mensaje = R.string.login_error_servidor;
                        break;
                }
                vista.textoMensaje.setText(mensaje);
                vista.textoMensaje.setVisibility(View.VISIBLE);
            }
        });
    }

    private void mostrarCambioClave(String claveTemporal) {
        DialogCambiarClaveBinding formulario = DialogCambiarClaveBinding.inflate(getLayoutInflater());
        formulario.campoClaveActualTexto.setText(claveTemporal);
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cambio_clave_titulo)
                .setMessage(R.string.cambio_clave_mensaje)
                .setView(formulario.getRoot())
                .setCancelable(false)
                .setNegativeButton(R.string.cancelar, (d, w) -> {
                    autenticacion.cerrarSesion();
                    vista.campoClaveTexto.setText("");
                })
                .setPositiveButton(R.string.cambio_clave_guardar, null)
                .create();
        dialogo.setOnShowListener(ignorado -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String actual = texto(formulario.campoClaveActualTexto);
                    String nueva = texto(formulario.campoClaveNuevaTexto);
                    String confirmacion = texto(formulario.campoClaveConfirmacionTexto);
                    formulario.textoError.setVisibility(View.GONE);
                    if (nueva.length() < 8 || nueva.length() > 32) {
                        formulario.textoError.setText(R.string.cambio_clave_longitud);
                        formulario.textoError.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (!nueva.equals(confirmacion)) {
                        formulario.textoError.setText(R.string.cambio_clave_no_coincide);
                        formulario.textoError.setVisibility(View.VISIBLE);
                        return;
                    }
                    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    autenticacion.cambiarClave(actual, nueva, confirmacion,
                            new AutenticacionRepositorio.CambioClaveCallback() {
                        @Override
                        public void onExito() {
                            dialogo.dismiss();
                            vista.campoClaveTexto.setText("");
                            vista.textoMensaje.setText(R.string.cambio_clave_exito);
                            vista.textoMensaje.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(String mensaje) {
                            formulario.textoError.setText(mensaje);
                            formulario.textoError.setVisibility(View.VISIBLE);
                            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    });
                }));
        dialogo.show();
    }

    private String texto(android.widget.TextView campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    private void mostrarCargando(boolean cargando) {
        vista.progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        vista.botonEntrar.setEnabled(!cargando);
        vista.botonEntrar.setText(cargando ? getString(R.string.login_verificando)
                                           : getString(R.string.login_entrar));
        if (cargando) vista.textoMensaje.setVisibility(View.GONE);
    }

    private void abrirPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
