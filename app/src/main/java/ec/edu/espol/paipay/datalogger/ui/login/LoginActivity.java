package ec.edu.espol.paipay.datalogger.ui.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.repo.AutenticacionRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.databinding.ActivityLoginBinding;
import ec.edu.espol.paipay.datalogger.ui.main.MainActivity;

/**
 * Pantalla de credenciales.
 *
 * Es el punto de entrada cuando todavía no hay identidad local. El primer
 * ingreso requiere internet para validar la cuenta y descargar piscinas; luego
 * la sesión cifrada permite trabajar offline hasta el cierre explícito.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding vista;
    private AutenticacionRepositorio autenticacion;

    /** Intent para pedir credenciales desde cualquier pantalla. */
    public static Intent intent(Context contexto) {
        return new Intent(contexto, LoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SesionManager.obtener(this).haySesionActiva()) {
            abrirPrincipal();
            return;
        }

        vista = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());

        autenticacion = new AutenticacionRepositorio(this);
        vista.botonEntrar.setOnClickListener(v -> intentarIngreso());
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
            public void onExito(String nombre) {
                mostrarCargando(false);
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
                    case DATOS_DE_OTRA_CUENTA:
                        mensaje = R.string.login_error_otra_cuenta;
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
