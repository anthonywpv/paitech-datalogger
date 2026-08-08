package ec.edu.espol.paipay.datalogger.ui.registro;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.repo.RegistroRepositorio;
import ec.edu.espol.paipay.datalogger.databinding.FragmentRegistroBinding;
import ec.edu.espol.paipay.datalogger.databinding.ItemPezFormBinding;
import ec.edu.espol.paipay.datalogger.domain.ValoresKitAgua;
import ec.edu.espol.paipay.datalogger.sync.SincronizacionWorker;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;
import ec.edu.espol.paipay.datalogger.util.SeguridadUtil;

/** Formulario unificado: una jornada contiene agua, biometría o ambos bloques. */
public class RegistroFragment extends Fragment {
    private static final String ARG_UUID = "jornada_uuid";

    private FragmentRegistroBinding vista;
    private RegistroRepositorio repositorio;
    private final List<PiscinaLocal> piscinas = new ArrayList<>();
    private final List<ItemPezFormBinding> filasPeces = new ArrayList<>();
    private String uuidActual;
    private int versionServidor;
    private long capturadaEnMillis;
    private boolean guardando;
    private boolean cargado;
    private boolean permitirAutoBorrador = true;

    public static RegistroFragment paraEditar(String uuid) {
        RegistroFragment fragmento = new RegistroFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UUID, uuid);
        fragmento.setArguments(args);
        return fragmento;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentRegistroBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(raiz, savedInstanceState);
        repositorio = new RegistroRepositorio(requireContext());
        capturadaEnMillis = FechaUtil.ahoraAlMinuto();
        pintarFecha();
        configurarSelectoresAgua();

        vista.campoFechaTexto.setOnClickListener(v -> elegirFechaHora());
        vista.incluirAgua.setOnCheckedChangeListener((boton, activo) ->
                vista.grupoAgua.setVisibility(activo ? View.VISIBLE : View.GONE));
        vista.incluirBiometria.setOnCheckedChangeListener((boton, activo) -> {
            vista.grupoBiometria.setVisibility(activo ? View.VISIBLE : View.GONE);
            if (activo && filasPeces.isEmpty()) agregarPez(null);
        });
        vista.botonAgregarPez.setOnClickListener(v -> agregarPez(null));
        vista.botonBorrador.setOnClickListener(v -> guardar(false));
        vista.botonFinalizar.setOnClickListener(v -> guardar(true));
        vista.botonMovimiento.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.contenedor, new MovimientoFragment())
                        .addToBackStack("nuevo_movimiento")
                        .commit());

        repositorio.piscinas().observe(getViewLifecycleOwner(), lista -> {
            piscinas.clear();
            if (lista != null) piscinas.addAll(lista);
            ArrayAdapter<PiscinaLocal> adaptador = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, piscinas);
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            vista.selectorPiscina.setAdapter(adaptador);
            if (!cargado) cargarInicial();
        });
    }

    private void cargarInicial() {
        if (cargado || piscinas.isEmpty()) return;
        cargado = true;
        String solicitado = getArguments() == null ? null : getArguments().getString(ARG_UUID);
        if (solicitado != null) repositorio.porUuid(solicitado, this::cargar);
        else repositorio.ultimoBorrador(borrador -> {
            if (borrador != null) cargar(borrador);
            else actualizarEspecie();
        });
        vista.selectorPiscina.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { actualizarEspecie(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
    }

    private void cargar(JornadaConPeces dato) {
        if (dato == null || vista == null) return;
        JornadaLocal j = dato.jornada;
        uuidActual = j.uuid;
        versionServidor = j.versionServidor;
        permitirAutoBorrador = JornadaLocal.BORRADOR.equals(j.estadoLocal);
        capturadaEnMillis = FechaUtil.millisDesdeIsoUtc(
                j.capturadaEn, FechaUtil.ahoraAlMinuto());
        pintarFecha();
        for (int i = 0; i < piscinas.size(); i++) {
            if (piscinas.get(i).uuid.equals(j.piscinaUuid)) vista.selectorPiscina.setSelection(i);
        }
        vista.campoPoblacionTexto.setText(j.poblacionEstimada == null ? "" : String.valueOf(j.poblacionEstimada));
        vista.campoObservacionesTexto.setText(j.observaciones);
        vista.incluirAgua.setChecked(j.incluyeAgua);
        if (j.incluyeAgua) {
            vista.campoPhTexto.setText(numeroKit(j.ph, ValoresKitAgua.PH), false);
            vista.campoNitratoTexto.setText(numeroKit(j.nitrato, ValoresKitAgua.NITRATO), false);
            vista.campoNitritoTexto.setText(numeroKit(j.nitrito, ValoresKitAgua.NITRITO), false);
            vista.campoAmoniacoTotalTexto.setText(
                    numeroKit(j.amoniacoTotal, ValoresKitAgua.AMONIACO_TOTAL), false);
        }
        filasPeces.clear();
        vista.contenedorPeces.removeAllViews();
        boolean biometria = dato.peces != null && !dato.peces.isEmpty();
        vista.incluirBiometria.setChecked(biometria);
        if (biometria) for (ObservacionPezLocal pez : dato.peces) agregarPez(pez);
        vista.textoEstadoBorrador.setText(versionServidor > 0
                ? "Corrigiendo una jornada sincronizada (versión " + versionServidor + ")."
                : JornadaLocal.BORRADOR.equals(j.estadoLocal)
                    ? "Borrador recuperado. Puedes continuar donde lo dejaste."
                    : "Jornada pendiente de sincronización.");
    }

    private void agregarPez(@Nullable ObservacionPezLocal dato) {
        ItemPezFormBinding fila = ItemPezFormBinding.inflate(
                LayoutInflater.from(requireContext()), vista.contenedorPeces, false);
        filasPeces.add(fila);
        vista.contenedorPeces.addView(fila.getRoot());
        if (dato != null) {
            fila.campoPesoTexto.setText(String.valueOf(dato.pesoGramos));
            fila.campoTallaTexto.setText(String.valueOf(dato.tallaCentimetros));
        }
        fila.botonQuitar.setOnClickListener(v -> {
            vista.contenedorPeces.removeView(fila.getRoot());
            filasPeces.remove(fila);
            numerarPeces();
        });
        numerarPeces();
    }

    private void numerarPeces() {
        for (int i = 0; i < filasPeces.size(); i++) {
            filasPeces.get(i).textoNumero.setText("Pez " + (i + 1));
        }
    }

    private void guardar(boolean completar) {
        Construccion construccion = construir(completar);
        if (construccion == null) return;
        guardando = true;
        repositorio.guardar(construccion.jornada, construccion.peces, completar,
                new RegistroRepositorio.AlGuardar() {
                    @Override public void listo(String uuid) {
                        guardando = false;
                        if (!isAdded() || vista == null) return;
                        uuidActual = uuid;
                        if (completar) {
                            permitirAutoBorrador = false;
                            SincronizacionWorker.programar(requireContext());
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Jornada guardada")
                                    .setMessage("Quedó guardada en el teléfono y lista para sincronizar cuando haya conexión.")
                                    .setPositiveButton(R.string.aceptar, null).show();
                            limpiar();
                        } else {
                            permitirAutoBorrador = true;
                            vista.textoEstadoBorrador.setText("Borrador guardado en este teléfono.");
                            Toast.makeText(requireContext(), "Borrador guardado", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void error(String mensaje, boolean almacenamientoLleno) {
                        guardando = false;
                        if (!isAdded() || vista == null) return;
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(almacenamientoLleno ? "Sin espacio suficiente" : "No se pudo guardar")
                                .setMessage(mensaje + "\n\nLa información sigue visible en el formulario; libera espacio y vuelve a intentar.")
                                .setPositiveButton(R.string.aceptar, null).show();
                    }
                });
    }

    private Construccion construir(boolean completar) {
        limpiarErrores();
        if (piscinas.isEmpty() || vista.selectorPiscina.getSelectedItem() == null) {
            Toast.makeText(requireContext(), "No hay piscinas disponibles. Conéctate para actualizar el catálogo.", Toast.LENGTH_LONG).show();
            return null;
        }
        PiscinaLocal piscina = (PiscinaLocal) vista.selectorPiscina.getSelectedItem();
        Integer poblacion = entero(texto(vista.campoPoblacionTexto));
        if (completar && poblacion == null) {
            vista.campoPoblacion.setError("La población estimada es obligatoria");
            return null;
        }
        boolean agua = vista.incluirAgua.isChecked();
        boolean biometria = vista.incluirBiometria.isChecked();
        if (completar && !agua && !biometria) {
            Toast.makeText(requireContext(), "Incluye agua, biometría o ambos bloques.", Toast.LENGTH_LONG).show();
            return null;
        }

        JornadaLocal jornada = new JornadaLocal();
        jornada.uuid = uuidActual == null ? SeguridadUtil.nuevoUuid() : uuidActual;
        jornada.versionServidor = versionServidor;
        jornada.piscinaUuid = piscina.uuid;
        jornada.piscinaCodigo = piscina.codigo;
        jornada.especieNombre = piscina.especieNombre;
        jornada.capturadaEn = FechaUtil.isoUtc(capturadaEnMillis);
        jornada.poblacionEstimada = poblacion;
        jornada.observaciones = texto(vista.campoObservacionesTexto);
        jornada.incluyeAgua = agua;
        jornada.motivoCambio = versionServidor > 0 ? "Corrección realizada desde Android" : "";
        if (agua) {
            jornada.ph = decimal(texto(vista.campoPhTexto));
            jornada.nitrato = decimal(texto(vista.campoNitratoTexto));
            jornada.nitrito = decimal(texto(vista.campoNitritoTexto));
            jornada.amoniacoTotal = decimal(texto(vista.campoAmoniacoTotalTexto));
            if (completar && (jornada.ph == null || jornada.nitrato == null
                    || jornada.nitrito == null || jornada.amoniacoTotal == null)) {
                Toast.makeText(requireContext(), "Completa los cuatro parámetros del agua.", Toast.LENGTH_LONG).show();
                return null;
            }
            if ((jornada.ph != null && !ValoresKitAgua.contiene(jornada.ph, ValoresKitAgua.PH))
                    || (jornada.nitrato != null && !ValoresKitAgua.contiene(
                    jornada.nitrato, ValoresKitAgua.NITRATO))
                    || (jornada.nitrito != null && !ValoresKitAgua.contiene(
                    jornada.nitrito, ValoresKitAgua.NITRITO))
                    || (jornada.amoniacoTotal != null && !ValoresKitAgua.contiene(
                    jornada.amoniacoTotal, ValoresKitAgua.AMONIACO_TOTAL))) {
                Toast.makeText(requireContext(),
                        "Selecciona valores impresos en la tarjeta del kit.",
                        Toast.LENGTH_LONG).show();
                return null;
            }
        }

        List<ObservacionPezLocal> peces = new ArrayList<>();
        if (biometria) {
            int orden = 1;
            for (ItemPezFormBinding fila : filasPeces) {
                Double peso = decimal(texto(fila.campoPesoTexto));
                Double talla = decimal(texto(fila.campoTallaTexto));
                if (peso == null && talla == null) continue;
                if (peso == null || talla == null || peso <= 0 || talla <= 0) {
                    Toast.makeText(requireContext(), "Cada pez necesita peso y longitud mayores que cero.", Toast.LENGTH_LONG).show();
                    return null;
                }
                ObservacionPezLocal pez = new ObservacionPezLocal();
                pez.uuid = SeguridadUtil.nuevoUuid();
                pez.jornadaUuid = jornada.uuid;
                pez.orden = orden++;
                pez.pesoGramos = peso;
                pez.tallaCentimetros = talla;
                peces.add(pez);
            }
            if (completar && peces.isEmpty()) {
                Toast.makeText(requireContext(), "Agrega al menos un pez o desactiva biometría.", Toast.LENGTH_LONG).show();
                return null;
            }
        }
        return new Construccion(jornada, peces);
    }

    private void elegirFechaHora() {
        Calendar actual = FechaUtil.calendarioGuayaquil(capturadaEnMillis);
        new DatePickerDialog(requireContext(), (d, anio, mes, dia) -> {
            Calendar seleccionada = FechaUtil.calendarioGuayaquil(capturadaEnMillis);
            seleccionada.set(Calendar.YEAR, anio);
            seleccionada.set(Calendar.MONTH, mes);
            seleccionada.set(Calendar.DAY_OF_MONTH, dia);
            new TimePickerDialog(requireContext(), (t, hora, minuto) -> {
                    seleccionada.set(Calendar.HOUR_OF_DAY, hora);
                    seleccionada.set(Calendar.MINUTE, minuto);
                    seleccionada.set(Calendar.SECOND, 0);
                    seleccionada.set(Calendar.MILLISECOND, 0);
                    capturadaEnMillis = seleccionada.getTimeInMillis();
                    pintarFecha();
                }, actual.get(Calendar.HOUR_OF_DAY), actual.get(Calendar.MINUTE), true).show();
        }, actual.get(Calendar.YEAR), actual.get(Calendar.MONTH),
                actual.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pintarFecha() {
        if (vista != null) vista.campoFechaTexto.setText(FechaUtil.conHora(capturadaEnMillis));
    }

    private void actualizarEspecie() {
        Object valor = vista.selectorPiscina.getSelectedItem();
        if (valor instanceof PiscinaLocal) {
            PiscinaLocal p = (PiscinaLocal) valor;
            vista.textoEspecie.setText("Especie permanente: "
                    + (p.especieNombre == null ? "sin configurar" : p.especieNombre));
        }
    }

    private boolean hayContenido() {
        return !texto(vista.campoPoblacionTexto).isEmpty()
                || !texto(vista.campoObservacionesTexto).isEmpty()
                || vista.incluirAgua.isChecked() || vista.incluirBiometria.isChecked();
    }

    private void limpiar() {
        uuidActual = null;
        versionServidor = 0;
        permitirAutoBorrador = true;
        capturadaEnMillis = FechaUtil.ahoraAlMinuto();
        pintarFecha();
        vista.campoPoblacionTexto.setText("");
        vista.campoObservacionesTexto.setText("");
        vista.incluirAgua.setChecked(false);
        vista.incluirBiometria.setChecked(false);
        vista.campoPhTexto.setText("");
        vista.campoNitratoTexto.setText("");
        vista.campoNitritoTexto.setText("");
        vista.campoAmoniacoTotalTexto.setText("", false);
        filasPeces.clear();
        vista.contenedorPeces.removeAllViews();
        vista.textoEstadoBorrador.setText("Se guarda primero en este teléfono; no necesitas internet.");
    }

    private void limpiarErrores() { vista.campoPoblacion.setError(null); vista.campoPh.setError(null); }
    private String texto(android.widget.EditText campo) { return campo.getText() == null ? "" : campo.getText().toString().trim(); }
    private Integer entero(String valor) { try { return valor.isEmpty() ? null : Integer.valueOf(valor); } catch (Exception e) { return null; } }
    private Double decimal(String valor) { try { return valor.isEmpty() ? null : Double.valueOf(valor.replace(',', '.')); } catch (Exception e) { return null; } }
    private String numero(Double valor) { return valor == null ? "" : String.valueOf(valor); }

    private void configurarSelectoresAgua() {
        vista.campoPhTexto.setAdapter(adaptador(ValoresKitAgua.PH));
        vista.campoNitratoTexto.setAdapter(adaptador(ValoresKitAgua.NITRATO));
        vista.campoNitritoTexto.setAdapter(adaptador(ValoresKitAgua.NITRITO));
        vista.campoAmoniacoTotalTexto.setAdapter(adaptador(ValoresKitAgua.AMONIACO_TOTAL));
    }

    private ArrayAdapter<String> adaptador(String[] valores) {
        return new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, valores);
    }

    private String numeroKit(Double valor, String[] permitidos) {
        if (valor == null) return "";
        for (String permitido : permitidos) {
            if (Double.compare(valor, Double.parseDouble(permitido)) == 0) return permitido;
        }
        return numero(valor);
    }

    @Override public void onStop() {
        super.onStop();
        if (!guardando && permitirAutoBorrador && vista != null && hayContenido()) guardar(false);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        vista = null;
        filasPeces.clear();
    }

    private static class Construccion {
        final JornadaLocal jornada;
        final List<ObservacionPezLocal> peces;
        Construccion(JornadaLocal jornada, List<ObservacionPezLocal> peces) { this.jornada = jornada; this.peces = peces; }
    }
}
