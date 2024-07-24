package com.example.blueapp.ui.preliminar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.blueapp.R
import com.example.blueapp.ui.DataBase.Entities.DataDetaPesoPollosEntity
import com.example.blueapp.ui.DataBase.Entities.DataPesoPollosEntity

class ReportesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var pesopollosReportes: List<DataPesoPollosEntity> = emptyList()
    private var detappReportes: List<DataDetaPesoPollosEntity> = emptyList()

    companion object {
        private const val TYPE_PESOPOLLOS = 1
        private const val TYPE_DETAPP = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_PESOPOLLOS -> {
                val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_reporte, parent, false)
                pesopollosViewHolder(itemView)
            }
            TYPE_DETAPP -> {
                val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_detapp, parent, false)
                detappViewHolder(itemView)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_PESOPOLLOS -> {
                val pesopollosHolder = holder as pesopollosViewHolder
                pesopollosHolder.bind(pesopollosReportes[position])
            }
            TYPE_DETAPP -> {
                val detappHolder = holder as detappViewHolder
                detappHolder.bind(detappReportes[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return pesopollosReportes.size + detappReportes.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < pesopollosReportes.size) {
            TYPE_PESOPOLLOS
        } else {
            TYPE_DETAPP
        }
    }

    fun actualizarReportesPesopollos(nuevosReportes: List<DataPesoPollosEntity>) {
        pesopollosReportes = nuevosReportes
        notifyDataSetChanged()
    }

    fun actualizarReportesDetapp(nuevosReportes: List<DataDetaPesoPollosEntity>) {
        detappReportes = nuevosReportes
        notifyDataSetChanged()
    }

    class pesopollosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewId: TextView = itemView.findViewById(R.id.textViewId)
        private val textViewSerie: TextView = itemView.findViewById(R.id.textViewSerie)
        private val textViewFecha: TextView = itemView.findViewById(R.id.textViewFecha)
        private val textViewTipo: TextView = itemView.findViewById(R.id.textViewTipo)
        private val textViewDocCliente: TextView = itemView.findViewById(R.id.textViewDocCliente)

        fun bind(pesopollos: DataPesoPollosEntity) {
            textViewId.text = pesopollos.id.toString()
            textViewSerie.text = pesopollos.serie
            textViewFecha.text = pesopollos.fecha
            textViewTipo.text = pesopollos.tipo
            textViewDocCliente.text = pesopollos.numeroDocCliente
        }
    }

    class detappViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewId: TextView = itemView.findViewById(R.id.textViewId)
        private val textViewCantJabas: TextView = itemView.findViewById(R.id.textViewjabas)
        private val textViewCantPolllos: TextView = itemView.findViewById(R.id.textViewPollos)
        private val textViewPeso: TextView = itemView.findViewById(R.id.textViewPeso)
        private val textViewTipo: TextView = itemView.findViewById(R.id.textViewTipo)

        fun bind(detapp: DataDetaPesoPollosEntity) {
            textViewId.text = detapp.idDetaPP.toString()
            textViewCantJabas.text = detapp.cantJabas.toString()
            textViewCantPolllos.text = detapp.cantPollos.toString()
            textViewPeso.text = detapp.peso.toString()
            textViewTipo.text = detapp.tipo
        }
    }
}
