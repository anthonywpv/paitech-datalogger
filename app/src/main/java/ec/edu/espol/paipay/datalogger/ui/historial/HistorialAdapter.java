package ec.edu.espol.paipay.datalogger.ui.historial;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.databinding.ItemHistorialBinding;
import ec.edu.espol.paipay.datalogger.databinding.ItemHistorialEncabezadoBinding;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/**
 * Lista del historial: encabezados de muestreo, registros con su etiqueta de
 * "Pendiente" o "Sincronizado", y un toque para corregir.
 */
public class HistorialAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_ENCABEZADO = 0;
    private static final int TIPO_REGISTRO = 1;

    public interface AlTocarRegistro {
        void tocado(ItemHistorial item);
    }

    private final List<ItemHistorial> datos = new ArrayList<>();
    private final AlTocarRegistro alTocar;

    public HistorialAdapter(AlTocarRegistro alTocar) {
        this.alTocar = alTocar;
    }

    public void actualizar(List<ItemHistorial> nuevos) {
        datos.clear();
        if (nuevos != null) datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int posicion) {
        return datos.get(posicion).esEncabezado() ? TIPO_ENCABEZADO : TIPO_REGISTRO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        LayoutInflater inflador = LayoutInflater.from(padre.getContext());
        if (tipo == TIPO_ENCABEZADO) {
            return new Encabezado(
                    ItemHistorialEncabezadoBinding.inflate(inflador, padre, false));
        }
        return new Fila(ItemHistorialBinding.inflate(inflador, padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder soporte, int posicion) {
        ItemHistorial item = datos.get(posicion);
        if (soporte instanceof Encabezado) {
            ((Encabezado) soporte).pintar(item);
        } else {
            ((Fila) soporte).pintar(item, alTocar);
        }
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    // ------------------------------------------------------------------

    static class Encabezado extends RecyclerView.ViewHolder {

        private final ItemHistorialEncabezadoBinding v;

        Encabezado(@NonNull ItemHistorialEncabezadoBinding binding) {
            super(binding.getRoot());
            this.v = binding;
        }

        void pintar(ItemHistorial item) {
            Context contexto = v.getRoot().getContext();
            // En el encabezado, "titulo" es el código y "detalle" el conteo.
            v.textoMuestreo.setText(contexto.getString(R.string.historial_muestreo, item.titulo));
            v.textoConteoMuestreo.setText(contexto.getString(
                    R.string.historial_muestreo_conteo, Integer.parseInt(item.detalle)));
        }
    }

    static class Fila extends RecyclerView.ViewHolder {

        private final ItemHistorialBinding v;

        Fila(@NonNull ItemHistorialBinding binding) {
            super(binding.getRoot());
            this.v = binding;
        }

        void pintar(ItemHistorial item, AlTocarRegistro alTocar) {
            Context contexto = v.getRoot().getContext();

            v.textoTitulo.setText(item.titulo);
            v.textoDetalle.setText(item.detalle);
            v.textoFecha.setText(FechaUtil.legibleDesdeIso(item.fechaMuestreo));

            int icono;
            switch (item.tipo) {
                case MOVIMIENTO: icono = R.drawable.ic_piscina; break;
                default: icono = R.drawable.ic_formulario; break;
            }
            v.icono.setImageResource(icono);

            if (item.sincronizado) {
                if ("ANULADO".equals(item.estadoLocal)
                        || "ANULADO_LOCAL".equals(item.estadoLocal)) {
                    v.textoEstado.setText("Anulado");
                } else {
                    v.textoEstado.setText(R.string.etiqueta_sincronizado);
                }
                v.textoEstado.setBackgroundResource(R.drawable.chip_sincronizado);
                v.textoEstado.setTextColor(
                        ContextCompat.getColor(contexto, R.color.estado_sincronizado));
            } else {
                if ("CONFLICTO".equals(item.estadoLocal)) {
                    v.textoEstado.setText("Conflicto");
                } else if ("BORRADOR".equals(item.estadoLocal)) {
                    v.textoEstado.setText("Borrador");
                } else if ("PENDIENTE_ANULAR".equals(item.estadoLocal)) {
                    v.textoEstado.setText("Por anular");
                } else {
                    v.textoEstado.setText(R.string.etiqueta_pendiente);
                }
                v.textoEstado.setBackgroundResource(R.drawable.chip_pendiente);
                v.textoEstado.setTextColor(
                        ContextCompat.getColor(contexto, R.color.estado_pendiente));
            }

            v.getRoot().setOnClickListener(v -> {
                if (alTocar != null) alTocar.tocado(item);
            });
        }
    }
}
