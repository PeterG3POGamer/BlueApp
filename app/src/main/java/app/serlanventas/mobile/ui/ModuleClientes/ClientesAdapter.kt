package app.serlanventas.mobile.ui.ModuleClientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity

class ClientesAdapter(
    private var clientes: List<ClienteEntity>,
    private val onEditClick: (ClienteEntity) -> Unit,
    private val onDeleteClick: (ClienteEntity) -> Unit
) : RecyclerView.Adapter<ClientesAdapter.ClienteViewHolder>() {

    class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val documentoCliente: TextView = itemView.findViewById(R.id.documentoCliente)
        val nombreCliente: TextView = itemView.findViewById(R.id.nombreCliente)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = clientes[position]
        holder.nombreCliente.text = cliente.nombreCompleto
        holder.documentoCliente.text = cliente.numeroDocCliente

        holder.btnEditar.setOnClickListener {
            onEditClick(cliente)
        }

        holder.btnEliminar.setOnClickListener {
            onDeleteClick(cliente)
        }
    }

    override fun getItemCount() = clientes.size

    fun setClientes(clientes: List<ClienteEntity>) {
        this.clientes = clientes
        notifyDataSetChanged()
    }
}

