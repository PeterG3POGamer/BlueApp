package app.serlanventas.mobile.ui.ModuleGalpones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity

class GalponesAdapter(
    private var galpones: List<GalponEntity>,
    private val nucleosMap: Map<String, String>,
    private val onEditClick: (GalponEntity) -> Unit,
    private val onDeleteClick: (GalponEntity) -> Unit
) : RecyclerView.Adapter<GalponesAdapter.GlaponViewHolder>() {

    class GlaponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreGalpon: TextView = itemView.findViewById(R.id.nombreGalpon)
        val nombreNucleo: TextView = itemView.findViewById(R.id.nombreNucleo) // Change ID to match your layout
        val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GlaponViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_galpon, parent, false)
        return GlaponViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GlaponViewHolder, position: Int) {
        val galpon = galpones[position]
        holder.nombreGalpon.text = galpon.nombre
        holder.nombreNucleo.text = nucleosMap[galpon.idEstablecimiento] ?: "Unknown Nucleo"

        holder.btnEditar.setOnClickListener {
            onEditClick(galpon)
        }

        holder.btnEliminar.setOnClickListener {
            onDeleteClick(galpon)
        }
    }

    override fun getItemCount() = galpones.size

    fun setGalpones(galpones: List<GalponEntity>) {
        this.galpones = galpones
        notifyDataSetChanged()
    }
}


