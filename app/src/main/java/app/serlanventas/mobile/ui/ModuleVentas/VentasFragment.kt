package app.serlanventas.mobile.ui.ModuleVentas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.databinding.FragmentModuleVentasBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity

class VentasFragment : Fragment() {

    private var _binding: FragmentModuleVentasBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var ventasAdapter: VentasAdapter
    private var listaVentas: List<DataPesoPollosEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentModuleVentasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Conexión a la base de datos
        db = AppDatabase(requireContext())

        // Configuración del RecyclerView
        ventasAdapter = VentasAdapter(emptyList()) { venta ->
            // Acción del botón "Mostrar"
            Log.d("VentasFragment", "Aquí se mostrarían los detalles del peso: ${venta.id}")
        }
        binding.recyclerViewVentas.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewVentas.adapter = ventasAdapter

        // Cargar los datos desde la base de datos
        cargarVentas()
    }

    private fun cargarVentas() {
        listaVentas = db.getAllDataPesoPollos()  // Asegúrate de que este método existe
        ventasAdapter.setVentas(listaVentas)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
