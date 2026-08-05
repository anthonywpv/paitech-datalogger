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
 * Detalle de UX relevante: en cuanto hay pH, amonio y nitrito, la tarjeta de
 * vista previa ya muestra el color del semáforo. Así el dato deja de ser un
 * número abstracto y se convierte en una decisión.
 *
 * Se exige el pH antes de adelantar un color porque sin él no se puede juzgar
 * el amonio: es el pH quien decide qué fracción está en su forma tóxica.
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
        mostrarPiscinaFija(vista.campoPiscinaTexto);

        TextWatcher evaluar = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { actualizarVistaPrevia(); }
        };
        vista.campoPhTexto.addTextChangedListener(evaluar);
        vista.campoAmonioTexto.addTextChangedListener(evaluar);
        vista.campoNitritoTexto.addTextChangedListener(evaluar);
        vista.campoNitratoTexto.addTextChangedListener(evaluar);

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
            vista.campoPhTexto.setText(String.valueOf(registro.ph));
            vista.campoAmonioTexto.setText(String.valueOf(registro.amonioMgL));
            vista.campoNitritoTexto.setText(String.valueOf(registro.nitritoMgL));
            vista.campoNitratoTexto.setText(String.valueOf(registro.nitratoMgL));
            vista.campoPoblacionTexto.setText(registro.poblacionEstimada == null
                    ? "" : String.valueOf(registro.poblacionEstimada));
            vista.campoObservacionTexto.setText(registro.observacion);
        });
    }

    // ---------------- VISTA PREVIA DEL SEMÁFORO ----------------

    private void actualizarVistaPrevia() {
        Double ph = numeroSuave(texto(vista.campoPhTexto));
        Double amonio = numeroSuave(texto(vista.campoAmonioTexto));
        Double nitrito = numeroSuave(texto(vista.campoNitritoTexto));

        // Sin pH no se puede juzgar el amonio, así que no se adelanta un color.
        if (ph == null || amonio == null || nitrito == null) {
            vista.vistaPreviaSemaforo.getRoot().setVisibility(View.GONE);
            return;
        }

        RegistroAgua provisional = new RegistroAgua();
        provisional.piscina = PISCINA_FIJA;
        provisional.fechaMuestreo = fechaIso;
        provisional.ph = ph;
        provisional.amonioMgL = amonio;
        provisional.nitritoMgL = nitrito;
        Double nitrato = numeroSuave(texto(vista.campoNitratoTexto));
        provisional.nitratoMgL = nitrato == null ? 0d : nitrato;

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
        final String piscina = PISCINA_FIJA;
        boolean valido = true;

        double ph = numero(vista.campoPh, texto(vista.campoPhTexto), true);
        double amonio = numero(vista.campoAmonio, texto(vista.campoAmonioTexto), true);
        double nitrito = numero(vista.campoNitrito, texto(vista.campoNitritoTexto), true);
        double nitrato = numero(vista.campoNitrato, texto(vista.campoNitratoTexto), true);
        double poblacion = numero(vista.campoPoblacion, texto(vista.campoPoblacionTexto), false);

        if (Double.isNaN(ph) || Double.isNaN(amonio) || Double.isNaN(nitrito)
                || Double.isNaN(nitrato) || Double.isNaN(poblacion)) valido = false;
        if (!valido) return;

        // Rangos físicamente posibles: atajan errores de tecleo en campo.
        if (ph < 0 || ph > 14) {
            vista.campoPh.setError("El pH va de 0 a 14");
            return;
        }
        if (amonio < 0) {
            vista.campoAmonio.setError("No puede ser negativo");
            return;
        }
        if (nitrito < 0) {
            vista.campoNitrito.setError("No puede ser negativo");
            return;
        }
        if (nitrato < 0) {
            vista.campoNitrato.setError("No puede ser negativo");
            return;
        }
        if (!esVacioOpcional(poblacion) && poblacion < 0) {
            vista.campoPoblacion.setError("No puede ser negativo");
            return;
        }
        Integer poblacionEstimada = esVacioOpcional(poblacion) ? null : (int) poblacion;

        if (registroEnEdicion != null) {
            registroEnEdicion.fechaMuestreo = fechaIso;
            registroEnEdicion.piscina = piscina;
            registroEnEdicion.ph = ph;
            registroEnEdicion.amonioMgL = amonio;
            registroEnEdicion.nitritoMgL = nitrito;
            registroEnEdicion.nitratoMgL = nitrato;
            registroEnEdicion.poblacionEstimada = poblacionEstimada;
            registroEnEdicion.observacion = texto(vista.campoObservacionTexto);

            repositorio.actualizarAgua(registroEnEdicion, id -> cerrarEdicion());
            return;
        }

        RegistroAgua r = new RegistroAgua();
        r.fechaMuestreo = fechaIso;
        r.piscina = piscina;
        r.ph = ph;
        r.amonioMgL = amonio;
        r.nitritoMgL = nitrito;
        r.nitratoMgL = nitrato;
        r.poblacionEstimada = poblacionEstimada;
        r.observacion = texto(vista.campoObservacionTexto);

        ResultadoSemaforo resultado = EvaluadorSemaforo.evaluar(r);

        repositorio.guardarAgua(r, id -> {
            String detalle = String.format(Locale.US,
                    "Piscina %s · pH %.1f · amonio %.2f mg/L · nitrito %.2f mg/L"
                            + "\nEstado del semáforo: %s",
                    r.piscina, r.ph, r.amonioMgL, r.nitritoMgL,
                    getString(EstiloSemaforo.etiqueta(resultado.getEstadoGlobal())));
            avisarGuardado(detalle);
            limpiar();
        });
    }

    private void limpiar() {
        vista.campoPhTexto.setText("");
        vista.campoAmonioTexto.setText("");
        vista.campoNitritoTexto.setText("");
        vista.campoNitratoTexto.setText("");
        vista.campoPoblacionTexto.setText("");
        vista.campoObservacionTexto.setText("");
        vista.campoPh.setError(null);
        vista.campoAmonio.setError(null);
        vista.campoNitrito.setError(null);
        vista.campoNitrato.setError(null);
        vista.campoPoblacion.setError(null);
        vista.vistaPreviaSemaforo.getRoot().setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
