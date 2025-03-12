package app.serlanventas.mobile.ui.ModulesPesos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import com.google.android.material.button.MaterialButton

class PesosAdapter(
    private var pesos: List<PesosEntity>,
    private val onMostrarClick: (PesosEntity) -> Unit,
    private val onEliminarClick: (PesosEntity) -> Unit
) : RecyclerView.Adapter<PesosAdapter.PesoViewHolder>() {

    class PesoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val estadoPeso: TextView = itemView.findViewById(R.id.estadoCliente)
        val nombreCliente: TextView = itemView.findViewById(R.id.nombreCliente)
        val documentoCliente: TextView = itemView.findViewById(R.id.documentoCliente)
        val nucleoName: TextView = itemView.findViewById(R.id.nucleo_name)
        val galponName: TextView = itemView.findViewById(R.id.galpon_name)
        val btnMostrar: MaterialButton = itemView.findViewById(R.id.btnMostrar)
        val btnEliminar: MaterialButton = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PesoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_pesos, parent, false)
        return PesoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PesoViewHolder, position: Int) {
        val peso = pesos[position]
        val db = AppDatabase(holder.itemView.context)

        val nucleoData = db.getNucleoById(peso.idNucleo.toString())
        val galponData = db.getGalponById(peso.idGalpon.toString())

        // Asigna los datos a las vistas
        holder.nombreCliente.text = peso.nombreCompleto ?: "Sin nombre"
        holder.documentoCliente.text = peso.numeroDocCliente
        holder.nucleoName.text = nucleoData!!.nombre
        holder.galponName.text = galponData!!.nombre

        // Cambiar el fondo según el estado
        when (peso.idEstado) {
            "0" -> {
                holder.estadoPeso.text = "Disponible"
                holder.estadoPeso.setBackgroundResource(R.drawable.rounded_background_success)
            }
            "1" -> {
                holder.estadoPeso.text = "Usado"
                holder.estadoPeso.setBackgroundResource(R.drawable.rounded_background_red)
            }
            "3" -> {
                holder.estadoPeso.text = "Finalizado"
                holder.estadoPeso.setBackgroundResource(R.drawable.rounded_background_blue)
            }
            else -> {
                holder.estadoPeso.text = "Desconocido"
                holder.estadoPeso.setBackgroundResource(R.drawable.rounded_background)
            }
        }

        // Configura los botones para mostrar y eliminar
        holder.btnMostrar.setOnClickListener {
            onMostrarClick(peso)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminarClick(peso)
        }
    }

    override fun getItemCount(): Int = pesos.size

    fun setPesos(pesos: List<PesosEntity>) {
        this.pesos = pesos
        notifyDataSetChanged()
    }
}
