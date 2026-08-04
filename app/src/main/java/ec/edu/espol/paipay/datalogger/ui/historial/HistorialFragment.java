package ec.edu.espol.paipay.datalogger.ui.historial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.databinding.FragmentHistorialBinding;

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

        adaptador = new HistorialAdapter();
        vista.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        vista.lista.setAdapter(adaptador);

        modelo = new ViewModelProvider(this).get(HistorialViewModel.class);
        modelo.historial().observe(getViewLifecycleOwner(), this::pintar);

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

        int total = items == null ? 0 : items.size();
        vista.textoConteo.setText(String.format(Locale.US,
                total == 1 ? "%d registro" : "%d registros", total));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
