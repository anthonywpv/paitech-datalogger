package ec.edu.espol.paipay.datalogger.ui.registro;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.databinding.FragmentFormAguaBinding;
import ec.edu.espol.paipay.datalogger.domain.EstadoAlerta;
import ec.edu.espol.paipay.datalogger.domain.EvaluadorSemaforo;
import ec.edu.espol.paipay.datalogger.domain.ResultadoSemaforo;
import ec.edu.espol.paipay.datalogger.ui.semaforo.EstiloSemaforo;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/**
 * Formulario de calidad de agua.
 *
 * Detalle de UX relevante: mientras el productor escribe la temperatura y el
 * oxígeno, la tarjeta de vista previa ya le muestra el color del semáforo.
 * Así el dato deja de ser un número abstracto y se convierte en una decisión.
 */
public class AguaFragment extends FormularioBase {

    private FragmentFormAguaBinding vista;
    private RegistroAgua registroEnEdicion;

    /** Abre el formulario para corregir una medición ya guardada. */
    public static AguaFragment paraEditar(String uuid) {
        AguaFragment f = new AguaFragment();
        f.setArguments(argumentosDeEdicion(uuid));
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentFormAguaBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(raiz, savedInstanceState);

        configurarSelectorFecha(vista.campoFechaTexto);
        cargarPiscinas(vista.campoPiscinaTexto);

        TextWatcher evaluar = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { actualizarVistaPrevia(); }
        };
        vista.campoTemperaturaTexto.addTextChangedListener(evaluar);
        vista.campoOxigenoTexto.addTextChangedListener(evaluar);
        vista.campoPhTexto.addTextChangedListener(evaluar);

        vista.botonGuardar.setOnClickListener(v -> guardar());
        vista.botonLimpiar.setOnClickListener(v -> limpiar());

        if (estaEditando()) prepararEdicion();
    }

    // ---------------- MODO CORRECCIÓN ----------------

    private void prepararEdicion() {
        vista.botonGuardar.setText(R.string.accion_guardar_cambios);
        vista.botonLimpiar.setVisibility(View.GONE);

        repositorio.aguaPorUuid(uuidEnEdicion, registro -> {
            if (registro == null || vista == null) return;
            registroEnEdicion = registro;

            fechaIso = registro.fechaMuestreo;
            vista.campoFechaTexto.setText(FechaUtil.legibleDesdeIso(fechaIso));
            vista.campoPiscinaTexto.setText(registro.piscina, false);
            vista.campoTemperaturaTexto.setText(String.valueOf(registro.temperaturaC));
            vista.campoOxigenoTexto.setText(String.valueOf(registro.oxigenoMgL));
            vista.campoPhTexto.setText(registro.ph == null ? "" : String.valueOf(registro.ph));
            vista.campoObservacionTexto.setText(registro.observacion);
        });
    }

    // ---------------- VISTA PREVIA DEL SEMÁFORO ----------------

    private void actualizarVistaPrevia() {
        Double t = numeroSuave(texto(vista.campoTemperaturaTexto));
        Double od = numeroSuave(texto(vista.campoOxigenoTexto));

        if (t == null || od == null) {
            vista.vistaPreviaSemaforo.getRoot().setVisibility(View.GONE);
            return;
        }

        RegistroAgua provisional = new RegistroAgua();
        provisional.piscina = codigoDePiscina(texto(vista.campoPiscinaTexto));
        provisional.fechaMuestreo = fechaIso;
        provisional.temperaturaC = t;
        provisional.oxigenoMgL = od;
        provisional.ph = numeroSuave(texto(vista.campoPhTexto));

        ResultadoSemaforo resultado = EvaluadorSemaforo.evaluar(provisional);
        EstadoAlerta estado = resultado.getEstadoGlobal();

        vista.vistaPreviaSemaforo.getRoot().setVisibility(View.VISIBLE);
        vista.vistaPreviaSemaforo.puntoPreview
                .setBackgroundResource(EstiloSemaforo.punto(estado));
        vista.vistaPreviaSemaforo.getRoot()
                .setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                        EstiloSemaforo.fondo(estado)));
        vista.vistaPreviaSemaforo.getRoot()
                .setStrokeColor(ContextCompat.getColor(requireContext(),
                        EstiloSemaforo.color(estado)));
        vista.vistaPreviaSemaforo.textoEstadoPreview
                .setText(getString(EstiloSemaforo.etiqueta(estado)));
        vista.vistaPreviaSemaforo.textoDiagnosticoPreview.setText(resultado.resumen());
        vista.vistaPreviaSemaforo.textoAccionPreview.setText(resultado.accionPrioritaria());
    }

    private Double numeroSuave(String valor) {
        if (valor.isEmpty()) return null;
        try {
            return Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------------- GUARDADO ----------------

    private void guardar() {
        String piscina = codigoDePiscina(texto(vista.campoPiscinaTexto));
        boolean valido = exigir(vista.campoPiscina, piscina);

        double t = numero(vista.campoTemperatura, texto(vista.campoTemperaturaTexto), true);
        double od = numero(vista.campoOxigeno, texto(vista.campoOxigenoTexto), true);
        double ph = numero(vista.campoPh, texto(vista.campoPhTexto), false);

        if (Double.isNaN(t) || Double.isNaN(od) || Double.isNaN(ph)) valido = false;
        if (!valido) return;

        // Rangos físicamente posibles: atajan errores de tecleo en campo.
        if (t < 0 || t > 45) {
            vista.campoTemperatura.setError("Valor fuera de rango (0 a 45 °C)");
            return;
        }
        if (od < 0 || od > 20) {
            vista.campoOxigeno.setError("Valor fuera de rango (0 a 20 mg/L)");
            return;
        }
        if (!esVacioOpcional(ph) && (ph < 0 || ph > 14)) {
            vista.campoPh.setError("El pH va de 0 a 14");
            return;
        }

        if (registroEnEdicion != null) {
            registroEnEdicion.fechaMuestreo = fechaIso;
            registroEnEdicion.piscina = piscina;
            registroEnEdicion.temperaturaC = t;
            registroEnEdicion.oxigenoMgL = od;
            registroEnEdicion.ph = esVacioOpcional(ph) ? null : ph;
            registroEnEdicion.observacion = texto(vista.campoObservacionTexto);

            repositorio.actualizarAgua(registroEnEdicion, id -> cerrarEdicion());
            return;
        }

        RegistroAgua r = new RegistroAgua();
        r.fechaMuestreo = fechaIso;
        r.piscina = piscina;
        r.temperaturaC = t;
        r.oxigenoMgL = od;
        r.ph = esVacioOpcional(ph) ? null : ph;
        r.observacion = texto(vista.campoObservacionTexto);

        ResultadoSemaforo resultado = EvaluadorSemaforo.evaluar(r);

        repositorio.guardarAgua(r, id -> {
            String detalle = String.format(Locale.US,
                    "Piscina %s · %.1f °C · %.1f mg/L\nEstado del semáforo: %s",
                    r.piscina, r.temperaturaC, r.oxigenoMgL,
                    getString(EstiloSemaforo.etiqueta(resultado.getEstadoGlobal())));
            avisarGuardado(detalle);
            limpiar();
        });
    }

    private void limpiar() {
        vista.campoTemperaturaTexto.setText("");
        vista.campoOxigenoTexto.setText("");
        vista.campoPhTexto.setText("");
        vista.campoObservacionTexto.setText("");
        vista.campoTemperatura.setError(null);
        vista.campoOxigeno.setError(null);
        vista.campoPh.setError(null);
        vista.vistaPreviaSemaforo.getRoot().setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
