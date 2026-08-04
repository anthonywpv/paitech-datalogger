package ec.edu.espol.paipay.datalogger.ui.registro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.EnsayoLaboratorio;
import ec.edu.espol.paipay.datalogger.databinding.FragmentFormLaboratorioBinding;

/**
 * Formulario de ensayos de laboratorio.
 *
 * Cumple la tarea "Consolidación de Fuentes": los resultados analíticos se
 * amarran a la misma piscina y fecha que los datos in situ, de modo que en
 * Neon queden listos para cruzarse en un solo análisis.
 */
public class LaboratorioFragment extends FormularioBase {

    private FragmentFormLaboratorioBinding vista;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentFormLaboratorioBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(raiz, savedInstanceState);

        configurarSelectorFecha(vista.campoFechaTexto);
        cargarPiscinas(vista.campoPiscinaTexto);

        vista.campoTipoMuestraTexto.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.tipos_muestra)));

        vista.campoParametroTexto.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.parametros_laboratorio)));

        vista.campoUnidadTexto.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.unidades_laboratorio)));

        vista.botonGuardar.setOnClickListener(v -> guardar());
        vista.botonLimpiar.setOnClickListener(v -> limpiar());
    }

    private void guardar() {
        String piscina = codigoDePiscina(texto(vista.campoPiscinaTexto));
        String tipo = texto(vista.campoTipoMuestraTexto);
        String parametro = texto(vista.campoParametroTexto);

        boolean valido = exigir(vista.campoPiscina, piscina);
        valido &= exigir(vista.campoTipoMuestra, tipo);
        valido &= exigir(vista.campoParametro, parametro);

        double valor = numero(vista.campoValor, texto(vista.campoValorTexto), true);
        if (Double.isNaN(valor)) valido = false;
        if (!valido) return;

        EnsayoLaboratorio e = new EnsayoLaboratorio();
        e.fechaMuestreo = fechaIso;
        e.piscina = piscina;
        e.tipoMuestra = tipo;
        e.parametro = parametro;
        e.valor = valor;
        e.unidad = texto(vista.campoUnidadTexto);
        e.codigoMuestra = texto(vista.campoCodigoMuestraTexto);
        e.laboratorio = texto(vista.campoLaboratorioTexto);
        e.observacion = texto(vista.campoObservacionTexto);

        repositorio.guardarLaboratorio(e, id -> {
            avisarGuardado(String.format(Locale.US, "%s: %.2f %s (%s)",
                    e.parametro, e.valor, e.unidad == null ? "" : e.unidad, e.tipoMuestra));
            limpiar();
        });
    }

    private void limpiar() {
        vista.campoParametroTexto.setText("");
        vista.campoValorTexto.setText("");
        vista.campoUnidadTexto.setText("");
        vista.campoCodigoMuestraTexto.setText("");
        vista.campoObservacionTexto.setText("");
        vista.campoParametro.setError(null);
        vista.campoValor.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
