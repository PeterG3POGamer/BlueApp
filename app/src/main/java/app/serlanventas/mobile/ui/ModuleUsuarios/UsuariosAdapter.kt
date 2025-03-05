package app.serlanventas.mobile.ui.ModuleUsuarios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.Entities.UsuarioEntity
import com.google.android.material.button.MaterialButton

class UsuariosAdapter(
    private var usuarios: List<UsuarioEntity>,
    private val onMostrarClick: (UsuarioEntity) -> Unit,
    private val onEliminarClick: (UsuarioEntity) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val documentoCliente: TextView = itemView.findViewById(R.id.documentoCliente)
        val rolName: TextView = itemView.findViewById(R.id.rolName)
        val btnMostrar: MaterialButton = itemView.findViewById(R.id.btnMostrar)
        val btnEliminar: MaterialButton = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_usuarios, parent, false)
        return UsuarioViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]

        // Asignar los datos del usuario a las vistas
        holder.userName.text = usuario.userName
        holder.documentoCliente.text = usuario.idUsuario
        holder.rolName.text = usuario.rolName

        // Configurar los botones
        holder.btnMostrar.setOnClickListener {
            onMostrarClick(usuario)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminarClick(usuario)
        }
    }

    override fun getItemCount(): Int = usuarios.size

    fun setUsuarios(usuarios: List<UsuarioEntity>) {
        this.usuarios = usuarios
        notifyDataSetChanged()
    }
}
