package app.serlanventas.mobile.ui.ModuleVentas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import com.google.android.material.button.MaterialButton

class VentasAdapter(
    private var ventas: List<DataPesoPollosEntity>,
    private val onMostrarClick: (DataPesoPollosEntity) -> Unit
) : RecyclerView.Adapter<VentasAdapter.VentaViewHolder>() {

    class VentaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCliente: TextView = itemView.findViewById(R.id.nombreCliente)
        val documentoCliente: TextView = itemView.findViewById(R.id.documentoCliente)
        val serieNumero: TextView = itemView.findViewById(R.id.serieNumero)
        val fecha: TextView = itemView.findViewById(R.id.fecha)
        val totalPeso: TextView = itemView.findViewById(R.id.totalPeso)
        val totalPagar: TextView = itemView.findViewById(R.id.totalPagar)
        val btnMostrar: MaterialButton = itemView.findViewById(R.id.btnMostrar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_ventas, parent, false)
        return VentaViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
        val venta = ventas[position]

        // Asignar los datos del objeto a las vistas
        holder.nombreCliente.text = venta.nombreCompleto ?: "Sin nombre"
        holder.documentoCliente.text = venta.numeroDocCliente
        holder.serieNumero.text = "${venta.serie}-${venta.numero}"
        holder.fecha.text = "REGISTRO: ${venta.fecha}"
        holder.totalPeso.text = "PS BRUTO: ${venta.totalPeso} kg"
        holder.totalPagar.text = "PAGO TOTAL: ${venta.TotalPagar}"

        // Configurar el botón "Mostrar"
        holder.btnMostrar.setOnClickListener {
            onMostrarClick(venta)
        }
    }

    override fun getItemCount(): Int = ventas.size

    fun setVentas(ventas: List<DataPesoPollosEntity>) {
        this.ventas = ventas
        notifyDataSetChanged()
    }
}
