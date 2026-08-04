package ec.edu.espol.paipay.datalogger.ui.registro;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;
import ec.edu.espol.paipay.datalogger.databinding.FragmentFormBiometriaBinding;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/**
 * Biometría de Vieja Azul: UN PEZ POR REGISTRO.
 *
 * El productor mide un pez, escribe peso y talla, guarda, y el formulario se
 * deja listo para el siguiente conservando fecha y piscina. Los peces medidos
 * el mismo día forman un muestreo ("M-04082026") y el contador de arriba le
 * dice cuántos lleva, que es la referencia que necesita en el borde de la
 * piscina para saber si ya completó la muestra.
 *
 * El mismo fragmento sirve para corregir un registro existente: se abre con
 * {@link #paraEditar(String)} desde el historial.
 */
public class BiometriaFragment extends FormularioBase {

    private FragmentFormBiometriaBinding vista;
    private RegistroBiometria registroEnEdicion;

    /** Abre el formulario para corregir un registro ya guardado. */
    public static BiometriaFragment paraEditar(String uuid) {
        BiometriaFragment f = new BiometriaFragment();
        f.setArguments(argumentosDeEdicion(uuid));
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentFormBiometriaBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(raiz, savedInstanceState);

        configurarSelectorFecha(vista.campoFechaTexto);
        cargarPiscinas(vista.campoPiscinaTexto);

        TextWatcher recalcular = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { mostrarFactorCondicion(); }
        };
        vista.campoPesoTexto.addTextChangedListener(recalcular);
        vista.campoTallaTexto.addTextChangedListener(recalcular);

        // Al elegir piscina cambia el muestreo en curso, así que se recuenta.
        vista.campoPiscinaTexto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refrescarConteoMuestreo(); }
        });

        vista.botonGuardar.setOnClickListener(v -> guardar());
        vista.botonLimpiar.setOnClickListener(v -> limpiar());

        if (estaEditando()) {
            prepararEdicion();
        } else {
            refrescarConteoMuestreo();
        }
    }

    // ------------------------------------------------------------------
    //  MODO CORRECCIÓN
    // ------------------------------------------------------------------

    private void prepararEdicion() {
        vista.botonGuardar.setText(R.string.accion_guardar_cambios);
        vista.botonLimpiar.setVisibility(View.GONE);

        repositorio.biometriaPorUuid(uuidEnEdicion, registro -> {
            if (registro == null || vista == null) return;
            registroEnEdicion = registro;

            fechaIso = registro.fechaMuestreo;
            vista.campoFechaTexto.setText(FechaUtil.legibleDesdeIso(fechaIso));
            vista.campoPiscinaTexto.setText(registro.piscina, false);
            vista.campoPesoTexto.setText(String.valueOf(registro.pesoGramos));
            vista.campoTallaTexto.setText(String.valueOf(registro.tallaCm));
            vista.campoObservacionTexto.setText(registro.observacion);

            vista.textoMuestreo.setText(R.string.edicion_aviso);
            vista.textoMuestreo.setVisibility(View.VISIBLE);
        });
    }

    // ------------------------------------------------------------------
    //  MUESTREO EN CURSO
    // ------------------------------------------------------------------

    @Override
    protected void onFechaCambiada() {
        refrescarConteoMuestreo();
    }

    /**
     * Muestra "Muestreo M-04082026 · P-01 — 7 peces medidos".
     * Solo tiene sentido durante el alta: al corregir, ese hueco lo ocupa el
     * aviso de que el registro volverá a quedar pendiente.
     */
    private void refrescarConteoMuestreo() {
        if (vista == null || estaEditando()) return;

        String piscina = codigoDePiscina(texto(vista.campoPiscinaTexto));
        if (TextUtils.isEmpty(piscina)) {
            vista.textoMuestreo.setVisibility(View.GONE);
            return;
        }

        final String codigo = FechaUtil.codigoMuestreo(fechaIso);
        repositorio.contarEnMuestreo(fechaIso, piscina, cuantos -> {
            if (vista == null) return;
            vista.textoMuestreo.setText(getString(
                    cuantos == 1 ? R.string.biometria_muestreo_conteo
                                 : R.string.biometria_muestreo_conteo_plural,
                    codigo, piscina, cuantos));
            vista.textoMuestreo.setVisibility(View.VISIBLE);
        });
    }

    /**
     * Retroalimentación inmediata: el productor ve el factor de condición
     * mientras escribe, sin esperar al informe técnico.
     */
    private void mostrarFactorCondicion() {
        double peso = numeroSuave(texto(vista.campoPesoTexto));
        double talla = numeroSuave(texto(vista.campoTallaTexto));

        if (Double.isNaN(peso) || Double.isNaN(talla) || peso <= 0 || talla <= 0) {
            vista.textoFactorCondicion.setVisibility(View.GONE);
            return;
        }

        double k = 100d * peso / Math.pow(talla, 3);
        String interpretacion;
        if (k < 1.4)      interpretacion = "pez delgado, revisar alimentación";
        else if (k <= 2.2) interpretacion = "condición corporal adecuada";
        else               interpretacion = "pez muy robusto, verificar la medición";

        vista.textoFactorCondicion.setText(String.format(Locale.US,
                "Factor de condición (K de Fulton): %.2f — %s", k, interpretacion));
        vista.textoFactorCondicion.setVisibility(View.VISIBLE);
    }

    private double numeroSuave(String valor) {
        if (valor.isEmpty()) return Double.NaN;
        try {
            return Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    // ------------------------------------------------------------------
    //  GUARDADO
    // ------------------------------------------------------------------

    private void guardar() {
        String piscina = codigoDePiscina(texto(vista.campoPiscinaTexto));
        boolean valido = exigir(vista.campoPiscina, piscina);

        double peso = numero(vista.campoPeso, texto(vista.campoPesoTexto), true);
        double talla = numero(vista.campoTalla, texto(vista.campoTallaTexto), true);

        if (Double.isNaN(peso) || Double.isNaN(talla)) valido = false;
        if (!valido) return;

        if (peso <= 0) {
            vista.campoPeso.setError("El peso debe ser mayor que cero");
            return;
        }
        if (talla <= 0) {
            vista.campoTalla.setError("La talla debe ser mayor que cero");
            return;
        }

        if (registroEnEdicion != null) {
            registroEnEdicion.fechaMuestreo = fechaIso;
            registroEnEdicion.piscina = piscina;
            registroEnEdicion.pesoGramos = peso;
            registroEnEdicion.tallaCm = talla;
            registroEnEdicion.observacion = texto(vista.campoObservacionTexto);

            repositorio.actualizarBiometria(registroEnEdicion, id -> cerrarEdicion());
            return;
        }

        RegistroBiometria r = new RegistroBiometria();
        r.fechaMuestreo = fechaIso;
        r.piscina = piscina;
        r.pesoGramos = peso;
        r.tallaCm = talla;
        r.observacion = texto(vista.campoObservacionTexto);

        repositorio.guardarBiometria(r, id -> {
            // No se avisa con un diálogo por cada pez: interrumpiría el ritmo de
            // medir-escribir-guardar. La confirmación es el contador subiendo.
            prepararSiguientePez();
        });
    }

    /** Deja el formulario listo para el pez siguiente, conservando el muestreo. */
    private void prepararSiguientePez() {
        if (vista == null) return;
        vista.campoPesoTexto.setText("");
        vista.campoTallaTexto.setText("");
        vista.campoObservacionTexto.setText("");
        vista.campoPeso.setError(null);
        vista.campoTalla.setError(null);
        vista.textoFactorCondicion.setVisibility(View.GONE);
        vista.campoPesoTexto.requestFocus();
        refrescarConteoMuestreo();
    }

    /** Vacía también la piscina: sirve para empezar un muestreo distinto. */
    private void limpiar() {
        prepararSiguientePez();
        vista.campoPiscinaTexto.setText("", false);
        vista.campoPiscina.setError(null);
        vista.textoMuestreo.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
