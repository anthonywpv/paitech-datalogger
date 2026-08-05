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
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/**
 * Formulario de ensayos de laboratorio.
 *
 * Cumple la tarea "Consolidación de Fuentes": los resultados analíticos se
 * amarran a la misma piscina y fecha que los datos in situ, de modo que en
 * Neon queden listos para cruzarse en un solo análisis.
 */
public class LaboratorioFragment extends FormularioBase {

    private FragmentFormLaboratorioBinding vista;
    private EnsayoLaboratorio ensayoEnEdicion;

    /** Abre el formulario para corregir un ensayo ya guardado. */
    public static LaboratorioFragment paraEditar(String uuid) {
        LaboratorioFragment f = new LaboratorioFragment();
        f.setArguments(argumentosDeEdicion(uuid));
        return f;
    }

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
        mostrarPiscinaFija(vista.campoPiscinaTexto);

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

        if (estaEditando()) prepararEdicion();
    }

    private void prepararEdicion() {
        vista.botonGuardar.setText(R.string.accion_guardar_cambios);
        vista.botonLimpiar.setVisibility(View.GONE);

        repositorio.laboratorioPorUuid(uuidEnEdicion, ensayo -> {
            if (ensayo == null || vista == null) return;
            ensayoEnEdicion = ensayo;

            fechaIso = ensayo.fechaMuestreo;
            vista.campoFechaTexto.setText(FechaUtil.legibleDesdeIso(fechaIso));
            vista.campoTipoMuestraTexto.setText(ensayo.tipoMuestra, false);
            vista.campoParametroTexto.setText(ensayo.parametro, false);
            vista.campoValorTexto.setText(String.valueOf(ensayo.valor));
            vista.campoUnidadTexto.setText(ensayo.unidad, false);
            vista.campoCodigoMuestraTexto.setText(ensayo.codigoMuestra);
            vista.campoLaboratorioTexto.setText(ensayo.laboratorio);
            vista.campoObservacionTexto.setText(ensayo.observacion);
        });
    }

    private void guardar() {
        final String piscina = PISCINA_FIJA;
        String tipo = texto(vista.campoTipoMuestraTexto);
        String parametro = texto(vista.campoParametroTexto);

        boolean valido = exigir(vista.campoTipoMuestra, tipo);
        valido &= exigir(vista.campoParametro, parametro);

        double valor = numero(vista.campoValor, texto(vista.campoValorTexto), true);
        if (Double.isNaN(valor)) valido = false;
        if (!valido) return;

        if (ensayoEnEdicion != null) {
            ensayoEnEdicion.fechaMuestreo = fechaIso;
            ensayoEnEdicion.piscina = piscina;
            ensayoEnEdicion.tipoMuestra = tipo;
            ensayoEnEdicion.parametro = parametro;
            ensayoEnEdicion.valor = valor;
            ensayoEnEdicion.unidad = texto(vista.campoUnidadTexto);
            ensayoEnEdicion.codigoMuestra = texto(vista.campoCodigoMuestraTexto);
            ensayoEnEdicion.laboratorio = texto(vista.campoLaboratorioTexto);
            ensayoEnEdicion.observacion = texto(vista.campoObservacionTexto);

            repositorio.actualizarLaboratorio(ensayoEnEdicion, id -> cerrarEdicion());
            return;
        }

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
