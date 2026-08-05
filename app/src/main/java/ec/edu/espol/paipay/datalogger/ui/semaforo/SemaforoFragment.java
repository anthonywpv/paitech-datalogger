package ec.edu.espol.paipay.datalogger.ui.semaforo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;
import ec.edu.espol.paipay.datalogger.data.repo.RegistroRepositorio;
import ec.edu.espol.paipay.datalogger.databinding.FragmentSemaforoBinding;
import ec.edu.espol.paipay.datalogger.databinding.ItemAlertaBinding;
import ec.edu.espol.paipay.datalogger.domain.EstadoAlerta;
import ec.edu.espol.paipay.datalogger.domain.EvaluadorSemaforo;
import ec.edu.espol.paipay.datalogger.domain.LecturaEvaluada;
import ec.edu.espol.paipay.datalogger.domain.ResultadoSemaforo;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/**
 * ===========================================================================
 *  SEMÁFORO DE ALERTAS — INTERFAZ (FRONTEND)
 * ===========================================================================
 *
 * Pantalla de inicio. Muestra ALERTAS, no explicaciones: el productor abre la
 * app para saber si algo va mal, no para leer qué parámetros se miden.
 *
 * Siempre hay algo que ver. Cuando todo está bien también se dice, en verde:
 * una pantalla que solo habla cuando hay problemas deja al productor sin saber
 * si es que no pasa nada o si es que la app no está funcionando.
 *
 * Dos bloques en un solo desplazamiento:
 *   · Piscina      — agua (ciclo del nitrógeno), mortalidad y biometría.
 *   · Lombricultura — todavía en desarrollo.
 *
 * Funciona 100 % con datos locales, así que sigue sirviendo sin señal.
 */
public class SemaforoFragment extends Fragment {

    private FragmentSemaforoBinding vista;
    private RegistroRepositorio repositorio;

    private List<RegistroAgua> aguas = new ArrayList<>();
    private List<RegistroBiometria> biometrias = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentSemaforoBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(raiz, savedInstanceState);
        repositorio = new RegistroRepositorio(requireContext());

        vista.tituloPiscina.setText(R.string.semaforo_piscina_titulo);

        repositorio.aguas().observe(getViewLifecycleOwner(), lista -> {
            aguas = lista == null ? new ArrayList<>() : lista;
            repintar();
        });
        repositorio.biometrias().observe(getViewLifecycleOwner(), lista -> {
            biometrias = lista == null ? new ArrayList<>() : lista;
            repintar();
        });
    }

    // ------------------------------------------------------------------

    private void repintar() {
        if (vista == null) return;
        vista.contenedorPiscina.removeAllViews();
        vista.contenedorLombricultura.removeAllViews();

        pintarAgua();
        pintarPeces();
        pintarLombricultura();
    }

    /** Estado del agua: una alerta por parámetro, más la mortalidad. */
    private void pintarAgua() {
        RegistroAgua ultima = aguas.isEmpty() ? null : aguas.get(0);

        if (ultima == null) {
            vista.subtituloPiscina.setText(R.string.semaforo_sin_medicion);
            alerta(vista.contenedorPiscina, EstadoAlerta.SIN_DATO,
                    getString(R.string.semaforo_sin_datos),
                    getString(R.string.semaforo_sin_datos_detalle), null);
            return;
        }

        vista.subtituloPiscina.setText(getString(R.string.semaforo_ultima_medicion,
                FechaUtil.legibleDesdeIso(ultima.fechaMuestreo)));

        ResultadoSemaforo resultado = EvaluadorSemaforo.evaluar(ultima);

        // Resumen arriba: el color global y qué hacer primero.
        alerta(vista.contenedorPiscina, resultado.getEstadoGlobal(),
                getString(EstiloSemaforo.etiqueta(resultado.getEstadoGlobal())),
                resultado.resumen(), resultado.accionPrioritaria());

        // Mortalidad: necesita la medición anterior, así que no sale de evaluar().
        LecturaEvaluada mortalidad = EvaluadorSemaforo.evaluarMortalidad(
                poblacionAnterior(), ultima.poblacionEstimada);
        if (mortalidad != null) {
            alerta(vista.contenedorPiscina, mortalidad.estado,
                    mortalidad.parametro, mortalidad.diagnostico, mortalidad.recomendacion);
        }

        // Detalle parámetro a parámetro, lo bueno y lo malo.
        for (LecturaEvaluada l : resultado.getLecturas()) {
            alerta(vista.contenedorPiscina, l.estado,
                    l.parametro + "   ·   " + l.valorFormateado(),
                    l.diagnostico, l.estado == EstadoAlerta.VERDE ? null : l.recomendacion);
        }
    }

    /** Población anotada en la medición anterior a la última, si la hubo. */
    private Integer poblacionAnterior() {
        // aguas viene ordenada por creado_en DESC: la 0 es la última.
        for (int i = 1; i < aguas.size(); i++) {
            if (aguas.get(i).poblacionEstimada != null) return aguas.get(i).poblacionEstimada;
        }
        return null;
    }

    /** Cómo va la biometría: si hay muestreo reciente y con cuántos peces. */
    private void pintarPeces() {
        if (biometrias.isEmpty()) {
            alerta(vista.contenedorPiscina, EstadoAlerta.SIN_DATO,
                    getString(R.string.semaforo_peces_titulo),
                    getString(R.string.semaforo_peces_sin_datos), null);
            return;
        }

        String muestreo = biometrias.get(0).codigoMuestreo();
        int enElMuestreo = 0;
        double sumaPeso = 0;
        for (RegistroBiometria r : biometrias) {
            if (r.codigoMuestreo().equals(muestreo)) {
                enElMuestreo++;
                sumaPeso += r.pesoGramos;
            }
        }

        // Una muestra corta no invalida el dato, pero conviene decirlo: con
        // pocos peces el promedio se mueve mucho con un solo ejemplar raro.
        boolean muestraCorta = enElMuestreo < 10;
        String detalle = getString(R.string.semaforo_peces_detalle,
                enElMuestreo,
                String.format(Locale.getDefault(), "%.1f", sumaPeso / enElMuestreo),
                FechaUtil.legibleDesdeIso(biometrias.get(0).fechaMuestreo));

        alerta(vista.contenedorPiscina,
                muestraCorta ? EstadoAlerta.AMARILLO : EstadoAlerta.VERDE,
                getString(R.string.semaforo_peces_titulo),
                detalle,
                muestraCorta ? getString(R.string.semaforo_peces_muestra_corta) : null);
    }

    private void pintarLombricultura() {
        alerta(vista.contenedorLombricultura, EstadoAlerta.SIN_DATO,
                getString(R.string.semaforo_lombricultura_estado),
                getString(R.string.semaforo_lombricultura_detalle), null);
    }

    // ------------------------------------------------------------------

    /** Añade una tarjeta de alerta coloreada según el estado. */
    private void alerta(LinearLayout destino, EstadoAlerta estado,
                        String titulo, String detalle, @Nullable String accion) {
        ItemAlertaBinding a = ItemAlertaBinding.inflate(
                LayoutInflater.from(requireContext()), destino, false);

        a.textoAlertaTitulo.setText(titulo);
        a.textoAlertaDetalle.setText(detalle);
        a.puntoAlerta.setBackgroundResource(EstiloSemaforo.punto(estado));
        a.tarjetaAlerta.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), EstiloSemaforo.fondo(estado)));
        a.tarjetaAlerta.setStrokeColor(
                ContextCompat.getColor(requireContext(), EstiloSemaforo.color(estado)));

        if (accion != null && !accion.isEmpty()) {
            a.textoAlertaAccion.setText(accion);
            a.textoAlertaAccion.setVisibility(View.VISIBLE);
        }

        destino.addView(a.getRoot());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
