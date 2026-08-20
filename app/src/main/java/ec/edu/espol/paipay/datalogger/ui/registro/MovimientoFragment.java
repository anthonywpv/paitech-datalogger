package ec.edu.espol.paipay.datalogger.ui.registro;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.repo.MovimientoRepositorio;
import ec.edu.espol.paipay.datalogger.databinding.FragmentMovimientoBinding;
import ec.edu.espol.paipay.datalogger.domain.ValidadorMovimiento;
import ec.edu.espol.paipay.datalogger.sync.SincronizacionWorker;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;
import ec.edu.espol.paipay.datalogger.util.SeguridadUtil;

/** Alta y corrección offline de movimientos explícitos de población. */
public class MovimientoFragment extends Fragment {
    private static final String ARG_UUID = "movimiento_uuid";

    private static final List<TipoOpcion> TIPOS = Arrays.asList(
            new TipoOpcion("MORTALIDAD", "Mortalidad"),
            new TipoOpcion("COSECHA_VENTA", "Cosecha o venta"),
            new TipoOpcion("TRASLADO", "Traslado"),
            new TipoOpcion("ESCAPE", "Escape"),
            new TipoOpcion("AJUSTE", "Ajuste")
    );

    private FragmentMovimientoBinding vista;
    private MovimientoRepositorio repositorio;
    private final List<PiscinaLocal> piscinas = new ArrayList<>();
    private String uuidActual;
    private int versionServidor;
    private long ocurridoEnMillis;
    private boolean cargado;
    private boolean guardando;

    public static MovimientoFragment paraEditar(String uuid) {
        MovimientoFragment fragmento = new MovimientoFragment();
        Bundle argumentos = new Bundle();
        argumentos.putString(ARG_UUID, uuid);
        fragmento.setArguments(argumentos);
        return fragmento;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        vista = FragmentMovimientoBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle estado) {
        repositorio = new MovimientoRepositorio(requireContext());
        ocurridoEnMillis = FechaUtil.ahoraAlMinuto();
        pintarFecha();

        ArrayAdapter<TipoOpcion> tipos = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, TIPOS);
        tipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vista.selectorTipo.setAdapter(tipos);

        ArrayAdapter<String> direcciones = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Aumentar la población", "Disminuir la población"));
        direcciones.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vista.selectorDireccionAjuste.setAdapter(direcciones);

        AdapterView.OnItemSelectedListener cambio = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> padre, View v, int posicion, long id) {
                actualizarCampos();
            }
            @Override public void onNothingSelected(AdapterView<?> padre) { }
        };
        vista.selectorTipo.setOnItemSelectedListener(cambio);
        vista.selectorDireccionAjuste.setOnItemSelectedListener(cambio);
        vista.campoFechaTexto.setOnClickListener(v -> elegirFechaHora());
        vista.botonCancelar.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        vista.botonGuardar.setOnClickListener(v -> guardar());

        repositorio.piscinas().observe(getViewLifecycleOwner(), lista -> {
            if (vista == null) return;
            piscinas.clear();
            if (lista != null) piscinas.addAll(lista);
            configurarPiscinas();
            if (!cargado) cargarInicial();
        });
    }

    private void configurarPiscinas() {
        ArrayAdapter<PiscinaLocal> origen = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, piscinas);
        origen.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vista.selectorOrigen.setAdapter(origen);

        ArrayAdapter<PiscinaLocal> destino = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, piscinas);
        destino.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vista.selectorDestino.setAdapter(destino);
    }

    private void cargarInicial() {
        if (piscinas.isEmpty()) return;
        cargado = true;
        String solicitado = getArguments() == null ? null : getArguments().getString(ARG_UUID);
        if (solicitado != null) repositorio.porUuid(solicitado, this::cargar);
        else actualizarCampos();
    }

    private void cargar(MovimientoLocal movimiento) {
        if (movimiento == null || vista == null) return;
        uuidActual = movimiento.uuid;
        versionServidor = movimiento.versionServidor;
        ocurridoEnMillis = FechaUtil.millisDesdeIsoUtc(
                movimiento.ocurridoEn, FechaUtil.ahoraAlMinuto());
        pintarFecha();
        seleccionarTipo(movimiento.tipo);
        if ("AJUSTE".equals(movimiento.tipo)) {
            vista.selectorDireccionAjuste.setSelection(
                    movimiento.piscinaDestinoUuid != null ? 0 : 1);
        }
        actualizarCampos();
        seleccionarPiscina(vista.selectorOrigen, movimiento.piscinaOrigenUuid);
        seleccionarPiscina(vista.selectorDestino, movimiento.piscinaDestinoUuid);
        vista.campoCantidadTexto.setText(String.valueOf(movimiento.cantidad));
        vista.campoObservacionesTexto.setText(movimiento.observaciones);
    }

    private void seleccionarTipo(String tipo) {
        for (int i = 0; i < TIPOS.size(); i++) {
            if (TIPOS.get(i).valor.equals(tipo)) {
                vista.selectorTipo.setSelection(i);
                return;
            }
        }
    }

    private void seleccionarPiscina(android.widget.Spinner selector, String uuid) {
        if (uuid == null) return;
        for (int i = 0; i < piscinas.size(); i++) {
            if (uuid.equals(piscinas.get(i).uuid)) {
                selector.setSelection(i);
                return;
            }
        }
    }

    private void actualizarCampos() {
        if (vista == null || vista.selectorTipo.getSelectedItem() == null) return;
        String tipo = ((TipoOpcion) vista.selectorTipo.getSelectedItem()).valor;
        boolean ajuste = "AJUSTE".equals(tipo);
        boolean traslado = "TRASLADO".equals(tipo);
        boolean entrada = ajuste && vista.selectorDireccionAjuste.getSelectedItemPosition() == 0;
        boolean salida = Arrays.asList("MORTALIDAD", "COSECHA_VENTA", "ESCAPE").contains(tipo)
                || (ajuste && vista.selectorDireccionAjuste.getSelectedItemPosition() == 1);

        vista.grupoDireccionAjuste.setVisibility(ajuste ? View.VISIBLE : View.GONE);
        vista.grupoOrigen.setVisibility(traslado || salida ? View.VISIBLE : View.GONE);
        vista.grupoDestino.setVisibility(traslado || entrada ? View.VISIBLE : View.GONE);
        vista.textoOrigen.setText(ajuste ? "Piscina que disminuye" : "Piscina de origen");
        vista.textoDestino.setText(ajuste ? "Piscina que aumenta" : "Piscina de destino");

        String ayuda;
        switch (tipo) {
            case "MORTALIDAD": ayuda = "Registra únicamente muertes observadas o confirmadas."; break;
            case "COSECHA_VENTA": ayuda = "Registra peces retirados por cosecha o venta."; break;
            case "TRASLADO": ayuda = "El origen y destino deben ser piscinas distintas de la misma especie."; break;
            case "ESCAPE": ayuda = "Registra peces perdidos por escape."; break;
            default: ayuda = "Usa el ajuste para corregir una diferencia de inventario conocida; no reemplaza una mortalidad.";
        }
        vista.textoAyuda.setText(ayuda);
    }

    private void guardar() {
        vista.campoCantidad.setError(null);
        if (piscinas.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No hay piscinas disponibles. Conéctate para actualizar el catálogo.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Integer cantidad = entero(texto(vista.campoCantidadTexto));
        if (cantidad == null || cantidad <= 0) {
            vista.campoCantidad.setError("La cantidad debe ser mayor que cero");
            return;
        }

        String tipo = ((TipoOpcion) vista.selectorTipo.getSelectedItem()).valor;
        boolean ajusteAumenta = "AJUSTE".equals(tipo)
                && vista.selectorDireccionAjuste.getSelectedItemPosition() == 0;
        boolean usaOrigen = "TRASLADO".equals(tipo)
                || Arrays.asList("MORTALIDAD", "COSECHA_VENTA", "ESCAPE").contains(tipo)
                || ("AJUSTE".equals(tipo) && !ajusteAumenta);
        boolean usaDestino = "TRASLADO".equals(tipo) || ajusteAumenta;

        PiscinaLocal origen = usaOrigen ? (PiscinaLocal) vista.selectorOrigen.getSelectedItem() : null;
        PiscinaLocal destino = usaDestino ? (PiscinaLocal) vista.selectorDestino.getSelectedItem() : null;
        if ((usaOrigen && origen == null) || (usaDestino && destino == null)) {
            Toast.makeText(requireContext(), "Selecciona las piscinas requeridas.", Toast.LENGTH_LONG).show();
            return;
        }
        if ((origen != null && origen.cicloActivoUuid == null)
                || (destino != null && destino.cicloActivoUuid == null)) {
            Toast.makeText(requireContext(),
                    "Cada piscina afectada necesita un ciclo activo.", Toast.LENGTH_LONG).show();
            return;
        }
        String errorMovimiento = ValidadorMovimiento.validar(tipo, cantidad,
                origen == null ? null : origen.uuid,
                destino == null ? null : destino.uuid,
                origen == null ? null : origen.especieNombre,
                destino == null ? null : destino.especieNombre);
        if (errorMovimiento != null) {
            Toast.makeText(requireContext(), errorMovimiento, Toast.LENGTH_LONG).show();
            return;
        }

        MovimientoLocal movimiento = new MovimientoLocal();
        movimiento.uuid = uuidActual == null ? SeguridadUtil.nuevoUuid() : uuidActual;
        movimiento.versionServidor = versionServidor;
        movimiento.tipo = tipo;
        movimiento.cantidad = cantidad;
        movimiento.piscinaOrigenUuid = origen == null ? null : origen.uuid;
        movimiento.piscinaDestinoUuid = destino == null ? null : destino.uuid;
        movimiento.cicloOrigenUuid = origen == null ? null : origen.cicloActivoUuid;
        movimiento.cicloDestinoUuid = destino == null ? null : destino.cicloActivoUuid;
        movimiento.ocurridoEn = FechaUtil.isoUtc(ocurridoEnMillis);
        movimiento.observaciones = texto(vista.campoObservacionesTexto);
        movimiento.motivoCambio = versionServidor > 0
                ? "Corrección realizada desde Android" : "";

        guardando = true;
        vista.botonGuardar.setEnabled(false);
        repositorio.guardar(movimiento, new MovimientoRepositorio.AlGuardar() {
            @Override public void listo(String uuid) {
                guardando = false;
                if (!isAdded() || vista == null) return;
                SincronizacionWorker.programar(requireContext());
                Toast.makeText(requireContext(),
                        "Movimiento guardado y pendiente de sincronizar.", Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            }

            @Override public void error(String mensaje, boolean almacenamientoLleno) {
                guardando = false;
                if (!isAdded() || vista == null) return;
                vista.botonGuardar.setEnabled(true);
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(almacenamientoLleno ? "Sin espacio suficiente" : "No se pudo guardar")
                        .setMessage(mensaje + "\n\nLa información sigue visible en el formulario.")
                        .setPositiveButton(R.string.aceptar, null).show();
            }
        });
    }

    private void elegirFechaHora() {
        Calendar actual = FechaUtil.calendarioGuayaquil(ocurridoEnMillis);
        new DatePickerDialog(requireContext(), (dialogo, anio, mes, dia) -> {
            Calendar seleccionada = FechaUtil.calendarioGuayaquil(ocurridoEnMillis);
            seleccionada.set(Calendar.YEAR, anio);
            seleccionada.set(Calendar.MONTH, mes);
            seleccionada.set(Calendar.DAY_OF_MONTH, dia);
            new TimePickerDialog(requireContext(), (tiempo, hora, minuto) -> {
                    seleccionada.set(Calendar.HOUR_OF_DAY, hora);
                    seleccionada.set(Calendar.MINUTE, minuto);
                    seleccionada.set(Calendar.SECOND, 0);
                    seleccionada.set(Calendar.MILLISECOND, 0);
                    ocurridoEnMillis = seleccionada.getTimeInMillis();
                    pintarFecha();
                }, actual.get(Calendar.HOUR_OF_DAY), actual.get(Calendar.MINUTE), true).show();
        }, actual.get(Calendar.YEAR), actual.get(Calendar.MONTH),
                actual.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pintarFecha() {
        if (vista != null) {
            vista.campoFechaTexto.setText(FechaUtil.conHora(ocurridoEnMillis));
        }
    }

    private String texto(android.widget.EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    private Integer entero(String valor) {
        try { return valor.isEmpty() ? null : Integer.valueOf(valor); }
        catch (NumberFormatException error) { return null; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }

    private static class TipoOpcion {
        final String valor;
        final String etiqueta;
        TipoOpcion(String valor, String etiqueta) {
            this.valor = valor;
            this.etiqueta = etiqueta;
        }
        @NonNull @Override public String toString() { return etiqueta; }
    }
}
