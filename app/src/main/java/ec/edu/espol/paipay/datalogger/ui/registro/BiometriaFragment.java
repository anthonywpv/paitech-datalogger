package ec.edu.espol.paipay.datalogger.ui.registro;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;
import ec.edu.espol.paipay.datalogger.databinding.FragmentFormBiometriaBinding;

/** Formulario de biometría de Vieja Azul: fecha, piscina, peso, talla. */
public class BiometriaFragment extends FormularioBase {

    private FragmentFormBiometriaBinding vista;

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

        vista.botonGuardar.setOnClickListener(v -> guardar());
        vista.botonLimpiar.setOnClickListener(v -> limpiar());
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

    private void guardar() {
        String piscina = codigoDePiscina(texto(vista.campoPiscinaTexto));
        boolean valido = exigir(vista.campoPiscina, piscina);

        double peso = numero(vista.campoPeso, texto(vista.campoPesoTexto), true);
        double talla = numero(vista.campoTalla, texto(vista.campoTallaTexto), true);
        double cantidad = numero(vista.campoCantidad, texto(vista.campoCantidadTexto), false);

        if (Double.isNaN(peso) || Double.isNaN(talla) || Double.isNaN(cantidad)) valido = false;
        if (!valido) return;

        if (peso <= 0) {
            vista.campoPeso.setError("El peso debe ser mayor que cero");
            return;
        }
        if (talla <= 0) {
            vista.campoTalla.setError("La talla debe ser mayor que cero");
            return;
        }

        RegistroBiometria r = new RegistroBiometria();
        r.fechaMuestreo = fechaIso;
        r.piscina = piscina;
        r.pesoGramos = peso;
        r.tallaCm = talla;
        r.cantidadMuestreada = esVacioOpcional(cantidad) ? 1 : (int) cantidad;
        r.observacion = texto(vista.campoObservacionTexto);

        repositorio.guardarBiometria(r, id -> {
            avisarGuardado(String.format(Locale.US,
                    "Piscina %s · %.1f g · %.1f cm", r.piscina, r.pesoGramos, r.tallaCm));
            limpiar();
        });
    }

    private void limpiar() {
        vista.campoPesoTexto.setText("");
        vista.campoTallaTexto.setText("");
        vista.campoCantidadTexto.setText("");
        vista.campoObservacionTexto.setText("");
        vista.campoPeso.setError(null);
        vista.campoTalla.setError(null);
        vista.campoCantidad.setError(null);
        vista.textoFactorCondicion.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
