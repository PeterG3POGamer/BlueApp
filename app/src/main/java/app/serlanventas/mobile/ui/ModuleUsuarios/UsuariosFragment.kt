package app.serlanventas.mobile.ui.ModuleUsuarios

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.databinding.FragmentModuleUsuariosBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.UsuarioEntity

class UsuariosFragment : Fragment() {

    private var _binding: FragmentModuleUsuariosBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var usuariosAdapter: UsuariosAdapter
    private var listaUsuarios: List<UsuarioEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentModuleUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase(requireContext())
        usuariosAdapter = UsuariosAdapter(emptyList(), ::onMostrarClick, ::onEliminarClick)
        binding.recyclerViewUsuarios.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewUsuarios.adapter = usuariosAdapter
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        listaUsuarios = db.getAllUsuarios()
        usuariosAdapter.setUsuarios(listaUsuarios)
    }

    private fun onMostrarClick(usuario: UsuarioEntity) {
        Log.d("UsuariosFragment", "Aquí se mostrarían los detalles del usuario: ${usuario.userName}")
    }

    private fun onEliminarClick(usuario: UsuarioEntity): Boolean {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setMessage("¿Estás seguro de que deseas eliminar este usuario?")
            .setPositiveButton("Sí") { dialog, id ->
                val result = db.eliminarUsuarioById(usuario.idUsuario)
                if (result) {
                    cargarUsuarios()
                    Log.d("UsuariosFragment", "Usuario eliminado con éxito.")
                } else {
                    Log.d("UsuariosFragment", "Error al eliminar el usuario.")
                }
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        builder.create().show()
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
