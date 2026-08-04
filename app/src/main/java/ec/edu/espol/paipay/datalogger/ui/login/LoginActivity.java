package ec.edu.espol.paipay.datalogger.ui.login;

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
 * Pantalla de ingreso.
 *
 * Solo se muestra la PRIMERA vez. Si ya existe una sesión guardada,
 * la actividad se salta por completo y el productor entra directo al
 * formulario, con o sin internet.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding vista;
    private AutenticacionRepositorio autenticacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Sesión persistente: si ya inició sesión antes, no se le vuelve a pedir ---
        if (SesionManager.obtener(this).haySesionActiva()) {
            irAPrincipal();
            return;
        }

        setTheme(R.style.Theme_Paipay);
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

        autenticacion.iniciarSesion(usuario, clave, new AutenticacionRepositorio.Callback() {
            @Override
            public void onExito(String nombre) {
                mostrarCargando(false);
                irAPrincipal();
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
                    default:
                        mensaje = R.string.sync_error;
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

    private void irAPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
