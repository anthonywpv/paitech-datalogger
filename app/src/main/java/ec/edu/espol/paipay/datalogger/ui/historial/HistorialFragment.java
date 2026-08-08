package ec.edu.espol.paipay.datalogger.ui.historial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.repo.MovimientoRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.RegistroRepositorio;
import ec.edu.espol.paipay.datalogger.databinding.FragmentHistorialBinding;
import ec.edu.espol.paipay.datalogger.sync.RefrescoHistorialRepositorio;
import ec.edu.espol.paipay.datalogger.sync.SincronizacionWorker;
import ec.edu.espol.paipay.datalogger.ui.registro.MovimientoFragment;
import ec.edu.espol.paipay.datalogger.ui.registro.RegistroFragment;

/** Historial offline propio con jornadas y movimientos editables/anulables. */
public class HistorialFragment extends Fragment {
    private FragmentHistorialBinding vista;
    private HistorialAdapter adaptador;
    private HistorialViewModel modelo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        vista = FragmentHistorialBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle estado) {
        adaptador = new HistorialAdapter(this::mostrarAcciones);
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
            modelo.cambiarFiltro(id == R.id.filtroPendientes
                    ? HistorialViewModel.Filtro.PENDIENTES
                    : id == R.id.filtroSincronizados
                    ? HistorialViewModel.Filtro.SINCRONIZADOS
                    : HistorialViewModel.Filtro.TODOS);
        });
    }

    private void pintar(List<ItemHistorial> items) {
        if (vista == null) return;
        boolean vacio = items == null || items.isEmpty();
        vista.textoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        vista.lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
        if (vacio) vista.textoVacio.setText(mensajeVacio());
        adaptador.actualizar(items);
        int total = items == null ? 0 : items.size();
        vista.textoConteo.setText(String.format(Locale.getDefault(),
                total == 1 ? "%d registro" : "%d registros", total));
    }

    private int mensajeVacio() {
        if (modelo.filtroActual() == HistorialViewModel.Filtro.PENDIENTES) {
            return R.string.historial_vacio_pendientes;
        }
        if (modelo.filtroActual() == HistorialViewModel.Filtro.SINCRONIZADOS) {
            return R.string.historial_vacio_sincronizados;
        }
        return R.string.historial_vacio;
    }

    private void refrescarDesdeServidor() {
        new RefrescoHistorialRepositorio(requireContext()).refrescar(resultado -> {
            if (vista == null || !isAdded()) return;
            vista.refrescador.setRefreshing(false);
            int mensaje = resultado.estado == RefrescoHistorialRepositorio.Estado.OK
                    ? R.string.historial_refresco_ok
                    : resultado.estado == RefrescoHistorialRepositorio.Estado.SIN_INTERNET
                    ? R.string.historial_refresco_sin_internet
                    : R.string.historial_refresco_error;
            Toast.makeText(requireContext(), getString(mensaje), Toast.LENGTH_LONG).show();
        });
    }

    private void mostrarAcciones(ItemHistorial item) {
        if (item.uuid == null || JornadaLocal.ANULADO.equals(item.estadoLocal)
                || JornadaLocal.ANULADO_LOCAL.equals(item.estadoLocal)
                || JornadaLocal.PENDIENTE_ANULAR.equals(item.estadoLocal)) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.titulo)
                .setItems(new String[]{"Editar", "Anular"}, (dialogo, cual) -> {
                    if (cual == 0) abrirCorreccion(item);
                    else pedirAnulacion(item);
                }).show();
    }

    private void abrirCorreccion(ItemHistorial item) {
        Fragment destino = item.tipo == ItemHistorial.Tipo.MOVIMIENTO
                ? MovimientoFragment.paraEditar(item.uuid)
                : RegistroFragment.paraEditar(item.uuid);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.contenedor, destino)
                .addToBackStack("corregir_registro")
                .commit();
    }

    private void pedirAnulacion(ItemHistorial item) {
        EditText motivo = new EditText(requireContext());
        motivo.setHint("Motivo obligatorio");
        String entidad = item.tipo == ItemHistorial.Tipo.MOVIMIENTO ? "movimiento" : "jornada";
        AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Anular " + entidad)
                .setMessage("El registro no se borrará: quedará anulado con el motivo indicado.")
                .setView(motivo)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton("Anular", null)
                .create();
        dialogo.setOnShowListener(ignorado -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String texto = motivo.getText() == null
                            ? "" : motivo.getText().toString().trim();
                    if (texto.length() < 3) {
                        motivo.setError("Escribe un motivo de al menos 3 caracteres");
                        return;
                    }
                    dialogo.dismiss();
                    anular(item, texto);
                }));
        dialogo.show();
    }

    private void anular(ItemHistorial item, String motivo) {
        if (item.tipo == ItemHistorial.Tipo.MOVIMIENTO) {
            new MovimientoRepositorio(requireContext()).anular(item.uuid, motivo,
                    new MovimientoRepositorio.AlGuardar() {
                        @Override public void listo(String uuid) { anulacionLista(); }
                        @Override public void error(String mensaje, boolean lleno) { mostrarError(mensaje); }
                    });
        } else {
            new RegistroRepositorio(requireContext()).anular(item.uuid, motivo,
                    new RegistroRepositorio.AlGuardar() {
                        @Override public void listo(String uuid) { anulacionLista(); }
                        @Override public void error(String mensaje, boolean lleno) { mostrarError(mensaje); }
                    });
        }
    }

    private void anulacionLista() {
        if (!isAdded()) return;
        SincronizacionWorker.programar(requireContext());
        Toast.makeText(requireContext(),
                "Anulación guardada; se sincronizará cuando haya conexión.",
                Toast.LENGTH_LONG).show();
    }

    private void mostrarError(String mensaje) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
