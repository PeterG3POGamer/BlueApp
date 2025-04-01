package app.serlanventas.mobile.ui.ModuleConfigConex

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.Entities.CaptureDeviceEntity

class DispositivosAdapter(
    private var items: List<CaptureDeviceEntity>,
    private val onItemClick: (CaptureDeviceEntity) -> Unit,
    private val onEstadoClick: (CaptureDeviceEntity) -> Unit,
    private val onEliminarClick: (CaptureDeviceEntity) -> Unit
) : RecyclerView.Adapter<DispositivosAdapter.ViewHolder>() {

    private var selectedPosition = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDispositivo: TextView = view.findViewById(R.id.txtDispositivo)
        val txtMac: TextView = view.findViewById(R.id.txtMac)
        val btnEstado: ImageButton = view.findViewById(R.id.btnEstado)
        val btnMostrar: ImageButton = view.findViewById(R.id.btnMostrar)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dispositivo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dispositivo = items[position]

        holder.txtDispositivo.text = dispositivo._nombreDispositivo
        holder.txtMac.text = dispositivo._macDispositivo

        // Configurar estado
        val estadoDrawable = if (dispositivo._estado == 1) {
            R.drawable.ic_success
        } else {
            R.drawable.ic_error
        }
        holder.btnEstado.setImageResource(estadoDrawable)

        val colorEstado = if (dispositivo._estado == 1) {
            ContextCompat.getColor(holder.itemView.context, R.color.green)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.red1)
        }
        holder.btnEstado.setColorFilter(colorEstado)

        // Configurar listeners usando setSelectedPosition
        holder.btnEstado.setOnClickListener {
            holder.getAdapterPosition().takeIf { it != RecyclerView.NO_POSITION }?.let { safePos ->
                setSelectedPosition(safePos)
                onEstadoClick(items[safePos])
            }
        }

        holder.btnMostrar.setOnClickListener {
            holder.getAdapterPosition().takeIf { it != RecyclerView.NO_POSITION }?.let { safePos ->
                setSelectedPosition(safePos)
                onItemClick(items[safePos])
            }
        }

        holder.btnEliminar.setOnClickListener {
            holder.getAdapterPosition().takeIf { it != RecyclerView.NO_POSITION }?.let { safePos ->
                setSelectedPosition(safePos)
                onEliminarClick(items[safePos])
            }
        }

        // Cambiar el fondo del elemento seleccionado
        holder.itemView.setBackgroundColor(
            if (position == selectedPosition) {
                ContextCompat.getColor(holder.itemView.context, R.color.color_gree_low)
            } else {
                ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
            }
        )
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CaptureDeviceEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition)
        }
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition)
        }
    }

    fun clearSelection() {
        val prevSelected = selectedPosition
        selectedPosition = -1
        notifyItemChanged(prevSelected)
    }
}