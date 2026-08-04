package ec.edu.espol.paipay.datalogger.ui.historial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.databinding.FragmentHistorialBinding;
import ec.edu.espol.paipay.datalogger.sync.RefrescoHistorialRepositorio;
import ec.edu.espol.paipay.datalogger.ui.registro.AguaFragment;
import ec.edu.espol.paipay.datalogger.ui.registro.BiometriaFragment;
import ec.edu.espol.paipay.datalogger.ui.registro.LaboratorioFragment;

/** Historial completo de lo registrado, con filtro por estado de sincronización. */
public class HistorialFragment extends Fragment {

    private FragmentHistorialBinding vista;
    private HistorialAdapter adaptador;
    private HistorialViewModel modelo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentHistorialBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(raiz, savedInstanceState);

        adaptador = new HistorialAdapter(this::abrirCorreccion);
        vista.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        vista.lista.setAdapter(adaptador);

        modelo = new ViewModelProvider(this).get(HistorialViewModel.class);
        modelo.historial().observe(getViewLifecycleOwner(), this::pintar);

        vista.refrescador.setColorSchemeResources(R.color.paipay_verde_oscuro);
        vista.refrescador.setOnRefreshListener(this::refrescarDesdeServidor);

        vista.grupoFiltros.setOnCheckedStateChangeListener((grupo, seleccionados) -> {
            if (seleccionados.isEmpty()) {
                vista.filtroTodos.setChecked(true);
                return;
            }
            int id = seleccionados.get(0);
            if (id == R.id.filtroPendientes) {
                modelo.cambiarFiltro(HistorialViewModel.Filtro.PENDIENTES);
            } else if (id == R.id.filtroSincronizados) {
                modelo.cambiarFiltro(HistorialViewModel.Filtro.SINCRONIZADOS);
            } else {
                modelo.cambiarFiltro(HistorialViewModel.Filtro.TODOS);
            }
        });
    }

    private void pintar(List<ItemHistorial> items) {
        boolean vacio = items == null || items.isEmpty();
        vista.textoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        vista.lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
        adaptador.actualizar(items);

        // Los encabezados de muestreo no son registros: no deben contarse.
        int total = 0;
        if (items != null) {
            for (ItemHistorial i : items) {
                if (!i.esEncabezado()) total++;
            }
        }
        vista.textoConteo.setText(String.format(Locale.US,
                total == 1 ? "%d registro" : "%d registros", total));
    }

    /**
     * Trae de Neon los registros de este correo y los fusiona con lo local.
     * La lista se sigue pintando desde SQLite, así que si no hay señal el
     * productor no pierde de vista nada de lo que ya tenía.
     */
    private void refrescarDesdeServidor() {
        new RefrescoHistorialRepositorio(requireContext()).refrescar(resultado -> {
            if (vista == null) return;
            vista.refrescador.setRefreshing(false);

            int mensaje;
            switch (resultado.estado) {
                case OK:
                    Toast.makeText(requireContext(),
                            getString(R.string.historial_refresco_ok, resultado.traidos),
                            Toast.LENGTH_LONG).show();
                    return;
                case SIN_INTERNET:
                case SIN_SESION:
                    mensaje = R.string.historial_refresco_sin_internet;
                    break;
                default:
                    mensaje = R.string.historial_refresco_error;
                    break;
            }
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Abre el registro tocado para corregirlo.
     *
     * Al guardar, el formulario vuelve a marcarlo como pendiente; como el POST
     * hace UPSERT sobre el uuid, la siguiente sincronización actualiza la fila
     * que ya existe en Neon en vez de duplicarla.
     */
    private void abrirCorreccion(ItemHistorial item) {
        if (item.uuid == null) return;

        Fragment editor;
        switch (item.tipo) {
            case BIOMETRIA:   editor = BiometriaFragment.paraEditar(item.uuid); break;
            case AGUA:        editor = AguaFragment.paraEditar(item.uuid); break;
            case LABORATORIO: editor = LaboratorioFragment.paraEditar(item.uuid); break;
            default:          return;   // los encabezados de muestreo no se editan
        }

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.contenedor, editor)
                .addToBackStack("corregir")
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
