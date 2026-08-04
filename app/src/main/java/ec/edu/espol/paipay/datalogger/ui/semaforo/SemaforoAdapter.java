package ec.edu.espol.paipay.datalogger.ui.semaforo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.databinding.ItemSemaforoBinding;
import ec.edu.espol.paipay.datalogger.domain.EstadoAlerta;
import ec.edu.espol.paipay.datalogger.domain.LecturaEvaluada;
import ec.edu.espol.paipay.datalogger.domain.ResultadoSemaforo;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/** Una tarjeta por piscina con su color, su diagnóstico y su acción sugerida. */
public class SemaforoAdapter extends RecyclerView.Adapter<SemaforoAdapter.Tarjeta> {

    private final List<ResultadoSemaforo> datos = new ArrayList<>();

    public void actualizar(List<ResultadoSemaforo> nuevos) {
        datos.clear();
        if (nuevos != null) datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Tarjeta onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Tarjeta(ItemSemaforoBinding.inflate(
                LayoutInflater.from(padre.getContext()), padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Tarjeta tarjeta, int posicion) {
        tarjeta.pintar(datos.get(posicion));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class Tarjeta extends RecyclerView.ViewHolder {

        private final ItemSemaforoBinding v;

        Tarjeta(@NonNull ItemSemaforoBinding binding) {
            super(binding.getRoot());
            this.v = binding;
        }

        void pintar(ResultadoSemaforo resultado) {
            EstadoAlerta estado = resultado.getEstadoGlobal();
            android.content.Context contexto = v.getRoot().getContext();

            v.textoPiscina.setText(resultado.getPiscina());
            v.textoFecha.setText(FechaUtil.legibleDesdeIso(resultado.getFecha()));
            v.textoEstado.setText(contexto.getString(EstiloSemaforo.etiqueta(estado)));
            v.textoEstado.setTextColor(
                    ContextCompat.getColor(contexto, EstiloSemaforo.color(estado)));
            v.punto.setBackgroundResource(EstiloSemaforo.punto(estado));
            v.getRoot().setStrokeColor(
                    ContextCompat.getColor(contexto, EstiloSemaforo.color(estado)));
            v.getRoot().setCardBackgroundColor(
                    ContextCompat.getColor(contexto, EstiloSemaforo.fondo(estado)));

            v.textoResumen.setText(resultado.resumen());

            String accion = resultado.accionPrioritaria();
            v.textoAccion.setText(accion);
            v.bloqueAccion.setVisibility(
                    estado == EstadoAlerta.VERDE || accion.isEmpty() ? View.GONE : View.VISIBLE);

            // Detalle parámetro por parámetro
            v.contenedorLecturas.removeAllViews();
            for (LecturaEvaluada lectura : resultado.getLecturas()) {
                v.contenedorLecturas.addView(crearFila(contexto, lectura));
            }
        }

        private View crearFila(android.content.Context contexto, LecturaEvaluada lectura) {
            LinearLayout fila = new LinearLayout(contexto);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setPadding(0, 8, 0, 8);

            View punto = new View(contexto);
            LinearLayout.LayoutParams lpPunto = new LinearLayout.LayoutParams(24, 24);
            lpPunto.gravity = android.view.Gravity.CENTER_VERTICAL;
            lpPunto.rightMargin = 20;
            punto.setLayoutParams(lpPunto);
            punto.setBackgroundResource(EstiloSemaforo.punto(lectura.estado));
            fila.addView(punto);

            TextView texto = new TextView(contexto);
            texto.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            texto.setTextSize(14f);
            texto.setTextColor(ContextCompat.getColor(contexto, R.color.paipay_marron));
            texto.setText(lectura.parametro + ":  " + lectura.valorFormateado());
            fila.addView(texto);

            return fila;
        }
    }
}
