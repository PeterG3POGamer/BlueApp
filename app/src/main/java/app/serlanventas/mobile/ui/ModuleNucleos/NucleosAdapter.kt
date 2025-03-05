package app.serlanventas.mobile.ui.ModuleNucleos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity

class NucleosAdapter(
    private var nucleos: List<NucleoEntity>,
    private val onEditClick: (NucleoEntity) -> Unit,
    private val onDeleteClick: (NucleoEntity) -> Unit
) : RecyclerView.Adapter<NucleosAdapter.GlaponViewHolder>() {

    class GlaponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreNucleo: TextView = itemView.findViewById(R.id.nombreNucleo)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GlaponViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nucleo, parent, false)
        return GlaponViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GlaponViewHolder, position: Int) {
        val nucleo = nucleos[position]
        holder.nombreNucleo.text = nucleo.nombre

        holder.btnEditar.setOnClickListener {
            onEditClick(nucleo)
        }

        holder.btnEliminar.setOnClickListener {
            onDeleteClick(nucleo)
        }
    }

    override fun getItemCount() = nucleos.size

    fun setNucleos(nucleos: List<NucleoEntity>) {
        this.nucleos = nucleos
        notifyDataSetChanged()
    }
}


