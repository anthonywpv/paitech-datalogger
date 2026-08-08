package ec.edu.espol.paipay.datalogger.ui.semaforo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.SemaforoLocal;
import ec.edu.espol.paipay.datalogger.data.repo.RegistroRepositorio;
import ec.edu.espol.paipay.datalogger.databinding.FragmentSemaforoBinding;
import ec.edu.espol.paipay.datalogger.databinding.ItemAlertaBinding;
import ec.edu.espol.paipay.datalogger.domain.EstadoAlerta;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/** Semáforo comunitario cacheado: funciona offline y no descarga el historial ajeno. */
public class SemaforoFragment extends Fragment {
    private FragmentSemaforoBinding vista;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        vista = FragmentSemaforoBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override public void onViewCreated(@NonNull View raiz, @Nullable Bundle estado) {
        new RegistroRepositorio(requireContext()).semaforos()
                .observe(getViewLifecycleOwner(), this::repintar);
    }

    private void repintar(List<SemaforoLocal> datos) {
        if (vista == null) return;
        vista.contenedorPiscina.removeAllViews();
        vista.contenedorLombricultura.removeAllViews();
        if (datos == null || datos.isEmpty()) {
            vista.tituloPiscina.setText(R.string.semaforo_piscina_titulo);
            vista.subtituloPiscina.setText("Sin semáforo comunitario cacheado");
            alerta(vista.contenedorPiscina, EstadoAlerta.SIN_DATO,
                    "Sin datos del servidor",
                    "Conéctate y sincroniza una vez para descargar la última jornada comunitaria.", null);
        } else {
            SemaforoLocal dato = datos.get(0);
            vista.tituloPiscina.setText(dato.piscinaNombre);
            vista.subtituloPiscina.setText(dato.capturadaEn == null ? "Sin mediciones"
                    : "Última jornada comunitaria: " + fecha(dato.capturadaEn)
                    + (dato.autorNombre == null ? "" : " · " + dato.autorNombre));
            EstadoAlerta estado = estado(dato.estado);
            alerta(vista.contenedorPiscina, estado,
                    dato.estado == null ? "SIN DATOS" : dato.estado,
                    dato.resumen == null ? "Umbrales provisionales de v1.3." : dato.resumen,
                    "Los rangos químicos aún deben confirmarse con el equipo de biología.");
            if (dato.ph != null) {
                alerta(vista.contenedorPiscina, estado, "Valores de la última jornada",
                        String.format(Locale.getDefault(),
                                "pH %.2f · nitrato %.3f ppm · nitrito %.3f ppm · amoníaco total %.3f ppm",
                                dato.ph, dato.nitrato, dato.nitrito, dato.amoniacoTotal), null);
            }
        }
        alerta(vista.contenedorLombricultura, EstadoAlerta.SIN_DATO,
                getString(R.string.semaforo_lombricultura_estado),
                getString(R.string.semaforo_lombricultura_detalle), null);
    }

    private EstadoAlerta estado(String valor) {
        if ("ROJO".equals(valor)) return EstadoAlerta.ROJO;
        if ("AMARILLO".equals(valor)) return EstadoAlerta.AMARILLO;
        if ("VERDE".equals(valor)) return EstadoAlerta.VERDE;
        return EstadoAlerta.SIN_DATO;
    }

    private String fecha(String iso) {
        return FechaUtil.conHoraDesdeIsoUtc(iso);
    }

    private void alerta(LinearLayout destino, EstadoAlerta estado,
                        String titulo, String detalle, @Nullable String accion) {
        ItemAlertaBinding a = ItemAlertaBinding.inflate(LayoutInflater.from(requireContext()), destino, false);
        a.textoAlertaTitulo.setText(titulo);
        a.textoAlertaDetalle.setText(detalle);
        a.puntoAlerta.setBackgroundResource(EstiloSemaforo.punto(estado));
        a.tarjetaAlerta.setCardBackgroundColor(ContextCompat.getColor(requireContext(), EstiloSemaforo.fondo(estado)));
        a.tarjetaAlerta.setStrokeColor(ContextCompat.getColor(requireContext(), EstiloSemaforo.color(estado)));
        if (accion != null && !accion.isEmpty()) {
            a.textoAlertaAccion.setText(accion);
            a.textoAlertaAccion.setVisibility(View.VISIBLE);
        }
        destino.addView(a.getRoot());
    }

    @Override public void onDestroyView() { super.onDestroyView(); vista = null; }
}
