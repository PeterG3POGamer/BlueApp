package app.serlanventas.mobile.ui.ModulesPesos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.databinding.FragmentModulePesosBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity

class PesosFragment : Fragment() {

    private var _binding: FragmentModulePesosBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var pesosAdapter: PesosAdapter
    private var listaPesos: List<PesosEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentModulePesosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización de la base de datos y el RecyclerView
        db = AppDatabase(requireContext())
        pesosAdapter = PesosAdapter(emptyList(), ::onMostrarClick, ::onEliminarClick)
        binding.recyclerViewPesos.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPesos.adapter = pesosAdapter

        // Cargar los pesos desde la base de datos
        cargarPesos()
    }

    private fun cargarPesos() {
        listaPesos = db.getTodosLosPesos()
        pesosAdapter.setPesos(listaPesos)
    }

    // Sección de manejo de clic en "Mostrar"
    private fun onMostrarClick(peso: PesosEntity) {
        val pesoDetalles = db.getPesoPorId(peso.id)

        pesoDetalles?.let {
            Log.d("PesosFragment", "Detalles del peso: $it")
        } ?: run {
            Log.d("PesosFragment", "No se encontraron detalles para el peso con ID: ${peso.id}")
        }
    }

    private fun onEliminarClick(peso: PesosEntity) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("¿Estás seguro de que deseas eliminar este peso?")
            .setPositiveButton("Sí") { dialog, id ->
                val result = db.deletePeso(peso.id)
                if (result > 0) {
                    cargarPesos()
                    Log.d("PesosFragment", "Peso eliminado con éxito.")
                } else {
                    Log.d("PesosFragment", "Error al eliminar el peso.")
                }
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
