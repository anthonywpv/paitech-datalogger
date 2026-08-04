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

/**
 * Pantalla de credenciales.
 *
 * NO es el punto de entrada de la app: registrar datos en campo no exige cuenta
 * ni señal. Esta pantalla la abre la sección de sincronización cuando hace falta
 * saber quién sube los datos, y devuelve RESULT_OK para que la subida continúe
 * donde se quedó.
 *
 * La sesión sigue siendo persistente: se pide una sola vez y a partir de ahí las
 * siguientes subidas no vuelven a preguntar, hasta que el productor cierre
 * sesión desde el menú.
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

        // Si ya hay sesión no hay nada que preguntar: se devuelve el control.
        if (SesionManager.obtener(this).haySesionActiva()) {
            setResult(RESULT_OK);
            finish();
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

        autenticacion.iniciarSesion(usuario, clave, new AutenticacionRepositorio.Callback() {
            @Override
            public void onExito(String nombre) {
                mostrarCargando(false);
                setResult(RESULT_OK);
                finish();
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
}
