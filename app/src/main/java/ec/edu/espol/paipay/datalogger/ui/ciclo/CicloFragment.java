package ec.edu.espol.paipay.datalogger.ui.ciclo;

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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.repo.CicloRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.ConflictoRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.DetalleConflicto;
import ec.edu.espol.paipay.datalogger.data.local.entity.ConflictoLocal;
import ec.edu.espol.paipay.datalogger.databinding.FragmentCicloBinding;
import ec.edu.espol.paipay.datalogger.domain.PrediccionCiclo;
import ec.edu.espol.paipay.datalogger.sync.SincronizacionWorker;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/** Formulario explícito de apertura/cierre con predicción cacheada y guardado offline. */
public class CicloFragment extends Fragment {
    private FragmentCicloBinding vista;
    private CicloRepositorio repositorio;
    private final List<PiscinaLocal> piscinas = new ArrayList<>();
    private final List<PiscinaDestino> destinosPiscina = new ArrayList<>();
    private CicloLocal cicloActivo;
    private PrediccionCiclo prediccion;
    private Integer poblacionPredicha;
    private long inicioMillis;
    private long cierreMillis;

    private static final List<Destino> DESTINOS = Arrays.asList(
            new Destino("VENTA", "Venta"),
            new Destino("CONSUMO", "Consumo"),
            new Destino("TRASLADO", "Traslado"),
            new Destino("MORTALIDAD_TOTAL", "Mortalidad total"),
            new Destino("OTRO", "Otro")
    );

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        vista = FragmentCicloBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle estado) {
        repositorio = new CicloRepositorio(requireContext());
        inicioMillis = FechaUtil.ahoraAlMinuto();
        cierreMillis = inicioMillis;
        pintarFechas();

        ArrayAdapter<Destino> destinos = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, DESTINOS);
        destinos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vista.selectorDestinoCierre.setAdapter(destinos);

        vista.campoInicioTexto.setOnClickListener(v -> elegirFecha(true));
        vista.campoCierreTexto.setOnClickListener(v -> elegirFecha(false));
        vista.botonCalcular.setOnClickListener(v -> calcular());
        vista.botonIniciar.setOnClickListener(v -> iniciar());
        vista.botonCerrar.setOnClickListener(v -> confirmarCierre());
        vista.botonResolverConflicto.setOnClickListener(v -> revisarConflicto());
        vista.selectorPiscina.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(android.widget.AdapterView<?> p,
                                                          View v, int pos, long id) {
                        actualizarPiscina();
                    }
                    @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
                });

        repositorio.piscinas().observe(getViewLifecycleOwner(), lista -> {
            if (vista == null) return;
            piscinas.clear();
            if (lista != null) piscinas.addAll(lista);
            ArrayAdapter<PiscinaLocal> adaptador = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, piscinas);
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            vista.selectorPiscina.setAdapter(adaptador);
            actualizarPiscina();
        });
    }

    private void actualizarPiscina() {
        if (vista == null || !(vista.selectorPiscina.getSelectedItem() instanceof PiscinaLocal)) {
            vista.textoEstadoCiclo.setText("Conéctate para descargar las piscinas.");
            return;
        }
        prediccion = null;
        poblacionPredicha = null;
        vista.botonIniciar.setEnabled(false);
        vista.textoPrediccion.setText("");
        PiscinaLocal piscina = piscinaSeleccionada();
        String agua = piscina.recordatorioAguaEstado == null
                ? "sin calcular" : piscina.recordatorioAguaEstado.replace('_', ' ');
        String biometria = piscina.recordatorioBiometriaEstado == null
                ? "sin calcular" : piscina.recordatorioBiometriaEstado.replace('_', ' ');
        vista.textoRecordatorios.setText("Agua: " + agua + "  ·  Biometría: " + biometria
                + "\nLos avisos no bloquean registros.");
        configurarDestinos(piscina);
        repositorio.activo(piscina.uuid, ciclo -> {
            if (vista == null || piscinaSeleccionada() == null
                    || !piscina.uuid.equals(piscinaSeleccionada().uuid)) return;
            cicloActivo = ciclo;
            boolean activo = ciclo != null;
            boolean conflicto = activo && CicloLocal.CONFLICTO.equals(ciclo.estadoLocal);
            vista.grupoApertura.setVisibility(activo ? View.GONE : View.VISIBLE);
            vista.grupoCierre.setVisibility(activo && !conflicto ? View.VISIBLE : View.GONE);
            vista.botonResolverConflicto.setVisibility(conflicto ? View.VISIBLE : View.GONE);
            if (activo) {
                vista.textoEstadoCiclo.setText("Ciclo " + ciclo.numero + " activo · inició con "
                        + ciclo.poblacionInicial + " peces\nEstado local: "
                        + ciclo.estadoLocal.replace('_', ' '));
            } else if (piscina.cicloActivoUuid != null) {
                vista.textoEstadoCiclo.setText(
                        "Existe un ciclo activo en el servidor. Sincroniza para descargar su detalle.");
                vista.grupoApertura.setVisibility(View.GONE);
            } else {
                vista.textoEstadoCiclo.setText("Sin ciclo activo. Puedes iniciar uno sin internet.");
            }
        });
    }

    private void revisarConflicto() {
        if (cicloActivo == null) return;
        ConflictoRepositorio conflictos = new ConflictoRepositorio(requireContext());
        conflictos.cargar(ConflictoLocal.CICLO, cicloActivo.uuid,
                new ConflictoRepositorio.AlCargar() {
                    @Override public void listo(DetalleConflicto detalle) {
                        if (!isAdded() || vista == null) return;
                        mostrarConflicto(conflictos, detalle);
                    }
                    @Override public void error(String mensaje) {
                        mostrarErrorConflicto(mensaje);
                    }
                });
    }

    private void mostrarConflicto(ConflictoRepositorio conflictos,
                                  DetalleConflicto detalle) {
        MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Conflicto de ciclo")
                .setMessage(detalle.comparacion
                        + "\n\nDescartar adopta el ciclo del servidor y conserva allí su "
                        + "historial. No se sobrescribe automáticamente ningún dato.")
                .setNegativeButton(R.string.cancelar, null)
                .setNeutralButton("Adoptar servidor", (d, w) ->
                        conflictos.descartarCambioLocal(ConflictoLocal.CICLO,
                                detalle.entidadUuid, resolucionConflicto(false)));
        if (detalle.puedeReaplicar) {
            dialogo.setPositiveButton("Reintentar mi cierre", (d, w) ->
                    conflictos.reaplicarCambioLocal(ConflictoLocal.CICLO,
                            detalle.entidadUuid, resolucionConflicto(true)));
        }
        dialogo.show();
    }

    private ConflictoRepositorio.AlResolver resolucionConflicto(boolean sincronizar) {
        return new ConflictoRepositorio.AlResolver() {
            @Override public void listo() {
                if (!isAdded() || vista == null) return;
                if (sincronizar) SincronizacionWorker.programar(requireContext());
                Toast.makeText(requireContext(), sincronizar
                                ? "El cierre quedó listo para reintentarse."
                                : "Se adoptó conscientemente el ciclo del servidor.",
                        Toast.LENGTH_LONG).show();
                actualizarPiscina();
            }
            @Override public void error(String mensaje) {
                mostrarErrorConflicto(mensaje);
            }
        };
    }

    private void mostrarErrorConflicto(String mensaje) {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("No se pudo resolver")
                .setMessage(mensaje)
                .setPositiveButton(R.string.aceptar, null)
                .show();
    }

    private void configurarDestinos(PiscinaLocal origen) {
        destinosPiscina.clear();
        destinosPiscina.add(new PiscinaDestino(null));
        for (PiscinaLocal piscina : piscinas) {
            if (!piscina.uuid.equals(origen.uuid)
                    && java.util.Objects.equals(piscina.especieNombre, origen.especieNombre)) {
                destinosPiscina.add(new PiscinaDestino(piscina));
            }
        }
        ArrayAdapter<PiscinaDestino> adaptador = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, destinosPiscina);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vista.selectorPiscinaDestino.setAdapter(adaptador);
    }

    private void calcular() {
        Integer poblacion = entero(vista.campoPoblacionTexto.getText().toString());
        if (poblacion == null || poblacion < 1) {
            vista.campoPoblacionTexto.setError("Ingresa una población mayor que cero");
            return;
        }
        PiscinaLocal piscina = piscinaSeleccionada();
        if (piscina == null) return;
        repositorio.predecir(piscina.uuid, poblacion, resultado -> {
            if (vista == null) return;
            prediccion = resultado;
            poblacionPredicha = poblacion;
            vista.botonIniciar.setEnabled(true);
            if (resultado.estimacion == null) {
                vista.textoPrediccion.setText(
                        "Sin ciclos cerrados comparables: no se muestra una cifra. Puedes iniciar el ciclo.");
            } else {
                vista.textoPrediccion.setText("Estimación: " + resultado.estimacion
                        + " peces (rango " + resultado.minimo + "–" + resultado.maximo + ")\n"
                        + resultado.ciclosUsados + " ciclos · confianza "
                        + resultado.confianza.replace('_', ' ').toLowerCase(Locale.ROOT)
                        + "\nDatos cacheados hasta: " + resultado.datosHasta);
            }
        });
    }

    private void iniciar() {
        PiscinaLocal piscina = piscinaSeleccionada();
        Integer poblacion = entero(vista.campoPoblacionTexto.getText().toString());
        if (piscina == null || poblacion == null || prediccion == null
                || !poblacion.equals(poblacionPredicha)) {
            Toast.makeText(requireContext(),
                    "Calcula nuevamente la estimación para esta población.", Toast.LENGTH_LONG).show();
            return;
        }
        Integer duracion = enteroOpcional(vista.campoDuracionTexto.getText().toString());
        if (duracion != null && (duracion < 1 || duracion > 60)) {
            vista.campoDuracionTexto.setError("Usa entre 1 y 60 meses");
            return;
        }
        repositorio.abrir(piscina, FechaUtil.isoUtc(inicioMillis), poblacion, duracion,
                texto(vista.campoObservacionesAperturaTexto), prediccion, callback("Ciclo iniciado"));
    }

    private void confirmarCierre() {
        if (cicloActivo == null) return;
        Integer poblacion = entero(vista.campoPoblacionFinalTexto.getText().toString());
        if (poblacion == null || poblacion < 0) {
            vista.campoPoblacionFinalTexto.setError("Ingresa la población final viva");
            return;
        }
        Destino destino = (Destino) vista.selectorDestinoCierre.getSelectedItem();
        if ("MORTALIDAD_TOTAL".equals(destino.valor) && poblacion != 0) {
            vista.campoPoblacionFinalTexto.setError("La mortalidad total requiere cero");
            return;
        }
        if (cierreMillis < FechaUtil.millisDesdeIsoUtc(cicloActivo.iniciadoEn, 0)) {
            Toast.makeText(requireContext(), "El cierre no puede ser anterior al inicio.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Double peso = decimalOpcional(vista.campoPesoTotalTexto.getText().toString());
        PiscinaDestino seleccion = (PiscinaDestino) vista.selectorPiscinaDestino.getSelectedItem();
        String piscinaDestino = "TRASLADO".equals(destino.valor) && seleccion != null
                && seleccion.piscina != null ? seleccion.piscina.uuid : null;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar ciclo completo")
                .setMessage("Confirma que terminó toda la cohorte. Las salidas parciales no cierran el ciclo.")
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (d, w) -> repositorio.cerrar(
                        piscinaSeleccionada(), cicloActivo, FechaUtil.isoUtc(cierreMillis),
                        destino.valor, poblacion, peso,
                        texto(vista.campoObservacionesCierreTexto), piscinaDestino,
                        callback("Ciclo cerrado")))
                .show();
    }

    private CicloRepositorio.AlGuardar callback(String mensaje) {
        return new CicloRepositorio.AlGuardar() {
            @Override public void listo(CicloLocal ciclo) {
                if (!isAdded() || vista == null) return;
                SincronizacionWorker.programar(requireContext());
                Toast.makeText(requireContext(), mensaje
                        + " y pendiente de sincronización.", Toast.LENGTH_LONG).show();
                actualizarPiscina();
            }
            @Override public void error(String detalle, boolean lleno) {
                if (!isAdded()) return;
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(lleno ? "Sin espacio suficiente" : "No se pudo guardar")
                        .setMessage(detalle)
                        .setPositiveButton(R.string.aceptar, null)
                        .show();
            }
        };
    }

    private void elegirFecha(boolean apertura) {
        long valor = apertura ? inicioMillis : cierreMillis;
        Calendar actual = FechaUtil.calendarioGuayaquil(valor);
        new DatePickerDialog(requireContext(), (d, anio, mes, dia) -> {
            Calendar seleccionada = FechaUtil.calendarioGuayaquil(valor);
            seleccionada.set(anio, mes, dia);
            new TimePickerDialog(requireContext(), (t, hora, minuto) -> {
                seleccionada.set(Calendar.HOUR_OF_DAY, hora);
                seleccionada.set(Calendar.MINUTE, minuto);
                seleccionada.set(Calendar.SECOND, 0);
                seleccionada.set(Calendar.MILLISECOND, 0);
                if (apertura) inicioMillis = seleccionada.getTimeInMillis();
                else cierreMillis = seleccionada.getTimeInMillis();
                pintarFechas();
            }, actual.get(Calendar.HOUR_OF_DAY), actual.get(Calendar.MINUTE), true).show();
        }, actual.get(Calendar.YEAR), actual.get(Calendar.MONTH),
                actual.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pintarFechas() {
        if (vista == null) return;
        vista.campoInicioTexto.setText(FechaUtil.conHora(inicioMillis));
        vista.campoCierreTexto.setText(FechaUtil.conHora(cierreMillis));
    }

    private PiscinaLocal piscinaSeleccionada() {
        Object item = vista == null ? null : vista.selectorPiscina.getSelectedItem();
        return item instanceof PiscinaLocal ? (PiscinaLocal) item : null;
    }
    private String texto(android.widget.EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
    private Integer entero(String valor) {
        try { return Integer.valueOf(valor.trim()); } catch (Exception e) { return null; }
    }
    private Integer enteroOpcional(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : entero(valor);
    }
    private Double decimalOpcional(String valor) {
        try {
            return valor == null || valor.trim().isEmpty() ? null
                    : Double.valueOf(valor.trim().replace(',', '.'));
        } catch (Exception e) { return null; }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }

    private static class Destino {
        final String valor;
        final String etiqueta;
        Destino(String valor, String etiqueta) { this.valor = valor; this.etiqueta = etiqueta; }
        @NonNull @Override public String toString() { return etiqueta; }
    }
    private static class PiscinaDestino {
        final PiscinaLocal piscina;
        PiscinaDestino(PiscinaLocal piscina) { this.piscina = piscina; }
        @NonNull @Override public String toString() {
            return piscina == null ? "Sin piscina destino (opcional)" : piscina.toString();
        }
    }
}
