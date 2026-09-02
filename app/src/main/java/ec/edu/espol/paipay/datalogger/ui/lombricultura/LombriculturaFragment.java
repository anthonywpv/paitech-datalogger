package ec.edu.espol.paipay.datalogger.ui.lombricultura;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.CamaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ConflictoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.data.repo.AlmacenamientoRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.ConflictoRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.DetalleConflicto;
import ec.edu.espol.paipay.datalogger.data.repo.LombriculturaRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.databinding.FragmentLombriculturaBinding;
import ec.edu.espol.paipay.datalogger.sync.SincronizacionWorker;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/** Ciclos y registros de cama con funcionamiento completo sin conexión. */
public class LombriculturaFragment extends Fragment {
    private FragmentLombriculturaBinding vista;
    private LombriculturaRepositorio repositorio;
    private AlmacenamientoRepositorio almacenamiento;
    private SesionManager sesion;
    private final List<CamaLocal> camas = new ArrayList<>();
    private List<RegistroLombriculturaLocal> registros = new ArrayList<>();
    private CicloLombriculturaLocal cicloActivo;
    private CicloLombriculturaLocal cicloEdicion;
    private RegistroLombriculturaLocal registroEditado;
    private long inicioMillis;
    private long registroMillis;
    private long cierreMillis;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        vista = FragmentLombriculturaBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle estado) {
        super.onViewCreated(raiz, estado);
        repositorio = new LombriculturaRepositorio(requireContext());
        almacenamiento = new AlmacenamientoRepositorio(requireContext());
        sesion = SesionManager.obtener(requireContext());
        inicioMillis = FechaUtil.ahoraAlMinuto();
        registroMillis = inicioMillis;
        cierreMillis = inicioMillis;
        pintarFechas();

        vista.campoInicioTexto.setOnClickListener(v -> elegirFecha(Fecha.INICIO));
        vista.campoFechaRegistroTexto.setOnClickListener(v -> elegirFecha(Fecha.REGISTRO));
        vista.campoCierreTexto.setOnClickListener(v -> elegirFecha(Fecha.CIERRE));
        vista.botonPhMenos.setOnClickListener(v -> cambiarPh(-0.01d));
        vista.botonPhMas.setOnClickListener(v -> cambiarPh(0.01d));
        vista.botonIniciarCiclo.setOnClickListener(v -> iniciarCiclo());
        vista.botonGuardarRegistro.setOnClickListener(v -> guardarRegistro());
        vista.botonCancelarEdicion.setOnClickListener(v -> cancelarEdicion());
        vista.botonCerrarCiclo.setOnClickListener(v -> confirmarCierre());
        vista.botonResolverConflictoCiclo.setOnClickListener(v -> resolverConflictoCiclo());
        vista.selectorCama.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(android.widget.AdapterView<?> p,
                                                          View v, int pos, long id) {
                        cancelarEdicion();
                        actualizarCama();
                        pintarHistorial();
                    }
                    @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
                });

        repositorio.camas().observe(getViewLifecycleOwner(), lista -> {
            if (vista == null) return;
            String seleccionAnterior = camaSeleccionada() == null
                    ? null : camaSeleccionada().uuid;
            camas.clear();
            if (lista != null) camas.addAll(lista);
            ArrayAdapter<CamaLocal> adaptador = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, camas);
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            vista.selectorCama.setAdapter(adaptador);
            if (seleccionAnterior != null) {
                for (int i = 0; i < camas.size(); i++) {
                    if (seleccionAnterior.equals(camas.get(i).uuid)) {
                        vista.selectorCama.setSelection(i);
                        break;
                    }
                }
            }
            actualizarCama();
        });
        repositorio.registros().observe(getViewLifecycleOwner(), lista -> {
            registros = lista == null ? new ArrayList<>() : lista;
            pintarHistorial();
        });
    }

    private void actualizarCama() {
        CamaLocal cama = camaSeleccionada();
        if (vista == null) return;
        if (cama == null) {
            cicloActivo = null;
            vista.textoEstadoCiclo.setText(
                    "No hay camas disponibles para esta comunidad. Conéctate para actualizar.");
            vista.grupoApertura.setVisibility(View.GONE);
            vista.grupoRegistro.setVisibility(View.GONE);
            vista.grupoCierre.setVisibility(View.GONE);
            return;
        }
        repositorio.cicloActivo(cama.uuid, ciclo -> {
            if (vista == null || camaSeleccionada() == null
                    || !cama.uuid.equals(camaSeleccionada().uuid)) return;
            cicloActivo = ciclo;
            cicloEdicion = ciclo;
            boolean activo = ciclo != null;
            boolean conflicto = activo
                    && CicloLombriculturaLocal.CONFLICTO.equals(ciclo.estadoLocal);
            vista.grupoApertura.setVisibility(activo || cama.cicloActivoUuid != null
                    ? View.GONE : View.VISIBLE);
            vista.grupoRegistro.setVisibility(activo && !conflicto ? View.VISIBLE : View.GONE);
            vista.grupoCierre.setVisibility(activo && !conflicto ? View.VISIBLE : View.GONE);
            vista.botonResolverConflictoCiclo.setVisibility(conflicto ? View.VISIBLE : View.GONE);
            if (conflicto) {
                vista.textoEstadoCiclo.setText("El ciclo tiene un conflicto 409 pendiente de decisión.");
            } else if (activo) {
                vista.textoEstadoCiclo.setText("Ciclo " + ciclo.numero + " activo · conteo inicial "
                        + ciclo.conteoInicial + "\nEstado local: "
                        + ciclo.estadoLocal.replace('_', ' '));
            } else if (cama.cicloActivoUuid != null) {
                vista.textoEstadoCiclo.setText(
                        "Existe un ciclo activo en el servidor. Sincroniza para descargarlo.");
            } else {
                vista.textoEstadoCiclo.setText("Sin ciclo activo. Puedes iniciar uno sin Internet.");
            }
        });
    }

    private void iniciarCiclo() {
        CamaLocal cama = camaSeleccionada();
        Integer conteo = entero(texto(vista.campoConteoInicialTexto));
        if (cama == null) return;
        if (conteo == null || conteo < 0) {
            vista.campoConteoInicialTexto.setError("Ingresa un conteo entero igual o mayor que cero");
            return;
        }
        if (!hayEspacio()) return;
        repositorio.iniciarCiclo(cama, FechaUtil.isoUtc(inicioMillis), conteo,
                texto(vista.campoObservacionesAperturaTexto), ciclo -> {
                    SincronizacionWorker.programar(requireContext());
                    Toast.makeText(requireContext(),
                            "Ciclo guardado en el teléfono y pendiente de sincronización.",
                            Toast.LENGTH_LONG).show();
                    limpiarApertura();
                    actualizarCama();
                });
    }

    private void guardarRegistro() {
        CamaLocal cama = camaSeleccionada();
        CicloLombriculturaLocal ciclo = registroEditado == null ? cicloActivo : cicloEdicion;
        String phTexto = texto(vista.campoPhSueloTexto).replace(',', '.');
        Double ph = decimal(phTexto);
        Integer conteo = entero(texto(vista.campoConteoTexto));
        vista.campoPhSuelo.setError(null);
        vista.campoConteo.setError(null);
        if (cama == null || ciclo == null) {
            Toast.makeText(requireContext(), "La cama necesita un ciclo válido.", Toast.LENGTH_LONG).show();
            return;
        }
        if (ph == null || ph < 0d || ph > 14d || !maximoDosDecimales(phTexto)) {
            vista.campoPhSuelo.setError("Ingresa un pH entre 0 y 14, con máximo 2 decimales");
            return;
        }
        if (conteo == null || conteo < 0) {
            vista.campoConteo.setError("Ingresa el número real de lombrices, igual o mayor que cero");
            return;
        }
        long inicio = FechaUtil.millisDesdeIsoUtc(ciclo.iniciadoEn, Long.MIN_VALUE);
        long cierre = FechaUtil.millisDesdeIsoUtc(ciclo.cerradoEn, Long.MAX_VALUE);
        if (registroMillis < inicio || registroMillis > cierre) {
            Toast.makeText(requireContext(),
                    "La fecha del registro debe estar dentro del ciclo.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!hayEspacio()) return;
        repositorio.guardarRegistro(cama, ciclo, registroEditado,
                FechaUtil.isoUtc(registroMillis), ph, conteo,
                texto(vista.campoObservacionesRegistroTexto), guardado -> {
                    SincronizacionWorker.programar(requireContext());
                    Toast.makeText(requireContext(), registroEditado == null
                                    ? "Registro guardado sin conexión."
                                    : "Corrección guardada y pendiente de sincronización.",
                            Toast.LENGTH_LONG).show();
                    cancelarEdicion();
                    limpiarRegistro();
                });
    }

    private void confirmarCierre() {
        if (cicloActivo == null || camaSeleccionada() == null) return;
        Integer conteo = entero(texto(vista.campoConteoFinalTexto));
        if (conteo == null || conteo < 0) {
            vista.campoConteoFinalTexto.setError(
                    "Ingresa el conteo final real, igual o mayor que cero");
            return;
        }
        if (cierreMillis < FechaUtil.millisDesdeIsoUtc(cicloActivo.iniciadoEn, 0)) {
            Toast.makeText(requireContext(), "El cierre no puede preceder al inicio.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar ciclo de la cama")
                .setMessage("El conteo final puede ser menor, igual o mayor al inicial. "
                        + "Confirma que corresponde al número realmente observado.")
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (d, w) -> {
                    if (!hayEspacio()) return;
                    repositorio.cerrarCiclo(camaSeleccionada(), cicloActivo,
                            FechaUtil.isoUtc(cierreMillis), conteo,
                            texto(vista.campoObservacionesCierreTexto), ciclo -> {
                                SincronizacionWorker.programar(requireContext());
                                Toast.makeText(requireContext(),
                                        "Cierre guardado y pendiente de sincronización.",
                                        Toast.LENGTH_LONG).show();
                                limpiarCierre();
                                actualizarCama();
                            });
                }).show();
    }

    private void pintarHistorial() {
        if (vista == null) return;
        vista.contenedorHistorial.removeAllViews();
        CamaLocal cama = camaSeleccionada();
        int visibles = 0;
        if (cama != null) {
            for (RegistroLombriculturaLocal registro : registros) {
                if (!cama.uuid.equals(registro.camaUuid)
                        || RegistroLombriculturaLocal.ANULADO.equals(registro.estadoLocal)) continue;
                TextView fila = new TextView(requireContext());
                int espacio = (int) (12 * getResources().getDisplayMetrics().density);
                fila.setPadding(espacio, espacio, espacio, espacio);
                fila.setTextColor(getResources().getColor(R.color.paipay_marron, null));
                fila.setBackgroundResource(R.drawable.chip_neutro);
                fila.setText(FechaUtil.conHoraDesdeIsoUtc(registro.capturadaEn)
                        + " · pH " + numero(registro.phSuelo)
                        + " · " + registro.conteoLombrices + " lombrices\n"
                        + registro.estadoLocal.replace('_', ' '));
                fila.setOnClickListener(v -> opcionesRegistro(registro));
                LinearLayoutParams.aplicar(fila, espacio);
                vista.contenedorHistorial.addView(fila);
                visibles++;
                if (visibles >= 50) break;
            }
        }
        if (visibles == 0) {
            TextView vacio = new TextView(requireContext());
            vacio.setText("Todavía no existen registros en esta cama.");
            vacio.setTextColor(getResources().getColor(R.color.paipay_gris_bajo, null));
            vista.contenedorHistorial.addView(vacio);
        }
    }

    private void opcionesRegistro(RegistroLombriculturaLocal registro) {
        boolean autor = registro.autorCorreo.equalsIgnoreCase(sesion.getUsuario());
        boolean conflicto = RegistroLombriculturaLocal.CONFLICTO.equals(registro.estadoLocal);
        List<String> opciones = new ArrayList<>();
        if (conflicto) opciones.add("Resolver conflicto 409");
        if (autor && !conflicto) opciones.add("Editar");
        if (autor && !conflicto) opciones.add("Anular");
        opciones.add("Cerrar");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Registro · " + FechaUtil.conHoraDesdeIsoUtc(registro.capturadaEn))
                .setMessage("pH del suelo: " + numero(registro.phSuelo)
                        + "\nConteo real: " + registro.conteoLombrices
                        + "\nObservaciones: " + vacio(registro.observaciones)
                        + "\nAutor: " + registro.autorCorreo)
                .setItems(opciones.toArray(new String[0]), (d, indice) -> {
                    String opcion = opciones.get(indice);
                    if (opcion.startsWith("Resolver")) resolverConflictoRegistro(registro);
                    else if ("Editar".equals(opcion)) editar(registro);
                    else if ("Anular".equals(opcion)) pedirAnulacion(registro);
                }).show();
    }

    private void editar(RegistroLombriculturaLocal registro) {
        repositorio.ciclo(registro.cicloUuid, ciclo -> {
            if (ciclo == null || vista == null) {
                Toast.makeText(requireContext(), "No se encontró el ciclo local del registro.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            registroEditado = registro;
            cicloEdicion = ciclo;
            registroMillis = FechaUtil.millisDesdeIsoUtc(
                    registro.capturadaEn, FechaUtil.ahoraAlMinuto());
            vista.campoFechaRegistroTexto.setText(FechaUtil.conHora(registroMillis));
            vista.campoPhSueloTexto.setText(numero(registro.phSuelo));
            vista.campoConteoTexto.setText(String.valueOf(registro.conteoLombrices));
            vista.campoObservacionesRegistroTexto.setText(registro.observaciones);
            vista.textoTituloRegistro.setText("Corregir registro");
            vista.botonCancelarEdicion.setVisibility(View.VISIBLE);
            vista.grupoRegistro.setVisibility(View.VISIBLE);
        });
    }

    private void pedirAnulacion(RegistroLombriculturaLocal registro) {
        final android.widget.EditText motivo = new android.widget.EditText(requireContext());
        motivo.setHint("Motivo obligatorio");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Anular registro")
                .setMessage("La anulación queda auditada; no borra silenciosamente el dato.")
                .setView(motivo)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (d, w) -> {
                    String texto = motivo.getText() == null ? "" : motivo.getText().toString().trim();
                    if (texto.length() < 3) {
                        Toast.makeText(requireContext(), "El motivo requiere al menos 3 caracteres.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    repositorio.anular(registro, texto, guardado -> {
                        SincronizacionWorker.programar(requireContext());
                        Toast.makeText(requireContext(), "Anulación guardada.",
                                Toast.LENGTH_LONG).show();
                    });
                }).show();
    }

    private void resolverConflictoCiclo() {
        if (cicloActivo != null) resolverConflicto(
                ConflictoLocal.CICLO_LOMBRICULTURA, cicloActivo.uuid, true);
    }

    private void resolverConflictoRegistro(RegistroLombriculturaLocal registro) {
        resolverConflicto(ConflictoLocal.REGISTRO_LOMBRICULTURA, registro.uuid, false);
    }

    private void resolverConflicto(String tipo, String uuid, boolean ciclo) {
        ConflictoRepositorio conflictos = new ConflictoRepositorio(requireContext());
        conflictos.cargar(tipo, uuid, new ConflictoRepositorio.AlCargar() {
            @Override public void listo(DetalleConflicto detalle) {
                if (!isAdded()) return;
                MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Conflicto 409 de lombricultura")
                        .setMessage(detalle.comparacion)
                        .setNegativeButton(R.string.cancelar, null)
                        .setNeutralButton("Adoptar servidor", (d, w) ->
                                conflictos.descartarCambioLocal(tipo, uuid,
                                        resolucionConflicto(false)));
                if (detalle.puedeReaplicar) {
                    dialogo.setPositiveButton(ciclo ? "Reintentar cierre" : "Reaplicar mi cambio",
                            (d, w) -> conflictos.reaplicarCambioLocal(tipo, uuid,
                                    resolucionConflicto(true)));
                }
                dialogo.show();
            }
            @Override public void error(String mensaje) { mostrarError(mensaje); }
        });
    }

    private ConflictoRepositorio.AlResolver resolucionConflicto(boolean sincronizar) {
        return new ConflictoRepositorio.AlResolver() {
            @Override public void listo() {
                if (sincronizar) SincronizacionWorker.programar(requireContext());
                actualizarCama();
                Toast.makeText(requireContext(), "Decisión guardada.", Toast.LENGTH_LONG).show();
            }
            @Override public void error(String mensaje) { mostrarError(mensaje); }
        };
    }

    private void mostrarError(String mensaje) {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext()).setTitle("No se pudo completar")
                .setMessage(mensaje).setPositiveButton(R.string.aceptar, null).show();
    }

    private void cancelarEdicion() {
        registroEditado = null;
        cicloEdicion = cicloActivo;
        if (vista == null) return;
        vista.textoTituloRegistro.setText("Nuevo registro");
        vista.botonCancelarEdicion.setVisibility(View.GONE);
        if (cicloActivo == null) vista.grupoRegistro.setVisibility(View.GONE);
    }

    private void cambiarPh(double incremento) {
        Double actual = decimal(texto(vista.campoPhSueloTexto).replace(',', '.'));
        if (actual == null) actual = 7d;
        double nuevo = Math.max(0d, Math.min(14d, actual + incremento));
        vista.campoPhSueloTexto.setText(String.format(Locale.US, "%.2f", nuevo));
    }

    private void elegirFecha(Fecha tipo) {
        long valor = tipo == Fecha.INICIO ? inicioMillis
                : tipo == Fecha.REGISTRO ? registroMillis : cierreMillis;
        Calendar actual = FechaUtil.calendarioGuayaquil(valor);
        new DatePickerDialog(requireContext(), (d, anio, mes, dia) -> {
            Calendar seleccionada = FechaUtil.calendarioGuayaquil(valor);
            seleccionada.set(anio, mes, dia);
            new TimePickerDialog(requireContext(), (t, hora, minuto) -> {
                seleccionada.set(Calendar.HOUR_OF_DAY, hora);
                seleccionada.set(Calendar.MINUTE, minuto);
                seleccionada.set(Calendar.SECOND, 0);
                seleccionada.set(Calendar.MILLISECOND, 0);
                if (tipo == Fecha.INICIO) inicioMillis = seleccionada.getTimeInMillis();
                else if (tipo == Fecha.REGISTRO) registroMillis = seleccionada.getTimeInMillis();
                else cierreMillis = seleccionada.getTimeInMillis();
                pintarFechas();
            }, actual.get(Calendar.HOUR_OF_DAY), actual.get(Calendar.MINUTE), true).show();
        }, actual.get(Calendar.YEAR), actual.get(Calendar.MONTH),
                actual.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pintarFechas() {
        if (vista == null) return;
        vista.campoInicioTexto.setText(FechaUtil.conHora(inicioMillis));
        vista.campoFechaRegistroTexto.setText(FechaUtil.conHora(registroMillis));
        vista.campoCierreTexto.setText(FechaUtil.conHora(cierreMillis));
    }

    private boolean hayEspacio() {
        if (!almacenamiento.espacioCritico()) return true;
        mostrarError("No queda espacio suficiente para garantizar el guardado. Libera espacio e inténtalo otra vez.");
        return false;
    }

    private void limpiarApertura() {
        vista.campoConteoInicialTexto.setText("");
        vista.campoObservacionesAperturaTexto.setText("");
    }

    private void limpiarRegistro() {
        registroMillis = FechaUtil.ahoraAlMinuto();
        vista.campoFechaRegistroTexto.setText(FechaUtil.conHora(registroMillis));
        vista.campoPhSueloTexto.setText("");
        vista.campoConteoTexto.setText("");
        vista.campoObservacionesRegistroTexto.setText("");
    }

    private void limpiarCierre() {
        cierreMillis = FechaUtil.ahoraAlMinuto();
        vista.campoCierreTexto.setText(FechaUtil.conHora(cierreMillis));
        vista.campoConteoFinalTexto.setText("");
        vista.campoObservacionesCierreTexto.setText("");
    }

    private CamaLocal camaSeleccionada() {
        Object item = vista == null ? null : vista.selectorCama.getSelectedItem();
        return item instanceof CamaLocal ? (CamaLocal) item : null;
    }

    private String texto(android.widget.EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
    private Integer entero(String valor) {
        try { return Integer.valueOf(valor.trim()); } catch (Exception error) { return null; }
    }
    private Double decimal(String valor) {
        try { return Double.valueOf(valor.trim()); } catch (Exception error) { return null; }
    }
    private boolean maximoDosDecimales(String valor) {
        int punto = valor.indexOf('.');
        return punto < 0 || valor.length() - punto - 1 <= 2;
    }
    private String numero(double valor) {
        String texto = String.format(Locale.US, "%.2f", valor);
        return texto.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    private String vacio(String valor) {
        return valor == null || valor.trim().isEmpty() ? "Sin observaciones" : valor;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }

    private enum Fecha { INICIO, REGISTRO, CIERRE }

    /** Evita repetir conversión dp y conserva separación entre filas dinámicas. */
    private static final class LinearLayoutParams {
        private static void aplicar(TextView vista, int margen) {
            android.widget.LinearLayout.LayoutParams parametros =
                    new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            parametros.bottomMargin = margen / 2;
            vista.setLayoutParams(parametros);
        }
    }
}
