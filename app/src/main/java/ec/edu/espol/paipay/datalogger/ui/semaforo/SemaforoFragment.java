package ec.edu.espol.paipay.datalogger.ui.semaforo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.data.repo.RegistroRepositorio;
import ec.edu.espol.paipay.datalogger.databinding.FragmentSemaforoBinding;
import ec.edu.espol.paipay.datalogger.domain.EvaluadorSemaforo;
import ec.edu.espol.paipay.datalogger.domain.ResultadoSemaforo;

/**
 * ===========================================================================
 *  SEMÁFORO DE ALERTAS — INTERFAZ (FRONTEND)
 * ===========================================================================
 *
 * Toma la última medición de agua de cada piscina, la pasa por
 * {@link EvaluadorSemaforo} y la presenta como tarjetas de color.
 *
 * Las piscinas en rojo se muestran primero: lo urgente se ve sin desplazar.
 *
 * Funciona 100 % con datos locales, así que el semáforo sigue sirviendo
 * aunque en Paipayales no haya señal.
 */
public class SemaforoFragment extends Fragment {

    private FragmentSemaforoBinding vista;
    private SemaforoAdapter adaptador;

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

        adaptador = new SemaforoAdapter();
        vista.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        vista.lista.setAdapter(adaptador);

        new RegistroRepositorio(requireContext())
                .ultimaMedicionPorPiscina()
                .observe(getViewLifecycleOwner(), this::pintar);
    }

    private void pintar(List<RegistroAgua> mediciones) {
        boolean vacio = mediciones == null || mediciones.isEmpty();
        vista.textoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        vista.lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
        if (vacio) {
            adaptador.actualizar(new ArrayList<>());
            return;
        }

        List<ResultadoSemaforo> resultados = new ArrayList<>(mediciones.size());
        for (RegistroAgua medicion : mediciones) {
            resultados.add(EvaluadorSemaforo.evaluar(medicion));
        }

        // Lo más grave primero: ROJO, luego AMARILLO, luego VERDE.
        Collections.sort(resultados, (a, b) ->
                Integer.compare(b.getEstadoGlobal().ordinal(), a.getEstadoGlobal().ordinal()));

        adaptador.actualizar(resultados);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
