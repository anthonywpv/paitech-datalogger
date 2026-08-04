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
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/** Lista del historial con etiqueta visible de "Pendiente" o "Sincronizado". */
public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.Fila> {

    private final List<ItemHistorial> datos = new ArrayList<>();

    public void actualizar(List<ItemHistorial> nuevos) {
        datos.clear();
        if (nuevos != null) datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(ItemHistorialBinding.inflate(
                LayoutInflater.from(padre.getContext()), padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        fila.pintar(datos.get(posicion));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class Fila extends RecyclerView.ViewHolder {

        private final ItemHistorialBinding v;

        Fila(@NonNull ItemHistorialBinding binding) {
            super(binding.getRoot());
            this.v = binding;
        }

        void pintar(ItemHistorial item) {
            Context contexto = v.getRoot().getContext();

            v.textoTitulo.setText(item.titulo);
            v.textoDetalle.setText(item.detalle);
            v.textoFecha.setText(FechaUtil.legibleDesdeIso(item.fechaMuestreo));

            int icono;
            switch (item.tipo) {
                case AGUA:        icono = R.drawable.ic_gota; break;
                case LABORATORIO: icono = R.drawable.ic_laboratorio; break;
                default:          icono = R.drawable.ic_pez; break;
            }
            v.icono.setImageResource(icono);

            if (item.sincronizado) {
                v.textoEstado.setText(R.string.etiqueta_sincronizado);
                v.textoEstado.setBackgroundResource(R.drawable.chip_sincronizado);
                v.textoEstado.setTextColor(
                        ContextCompat.getColor(contexto, R.color.estado_sincronizado));
            } else {
                v.textoEstado.setText(R.string.etiqueta_pendiente);
                v.textoEstado.setBackgroundResource(R.drawable.chip_pendiente);
                v.textoEstado.setTextColor(
                        ContextCompat.getColor(contexto, R.color.estado_pendiente));
            }
        }
    }
}
