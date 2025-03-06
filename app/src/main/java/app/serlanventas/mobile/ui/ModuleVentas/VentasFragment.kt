package app.serlanventas.mobile.ui.ModuleVentas

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.R
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
        db = AppDatabase(requireContext())

        ventasAdapter = VentasAdapter(emptyList()) { venta ->

            val detaPesoPollos = db.obtenerDetaPesoPollosPorId(venta.id)
            val pesoPollos = db.obtenerPesoPollosPorId(venta.id)

            // Mostrar detalles en consola
            Log.d("VentasFragment", "Detalle DetaPesoPollos: $detaPesoPollos")
            Log.d("VentasFragment", "Detalle PesoPollos: $pesoPollos")

            pesoPollos?.let {
                val dataNucleo = db.obtenerNucleoPorId(it.idGalpon.toInt())
                val dataGalpon = db.obtenerGalponPorId(it.idNucleo.toInt())

                Log.d("VentasFragment", "Nucleo: ${dataNucleo?.nombre}")
                Log.d("VentasFragment", "Nucleo: ${dataNucleo?.idEmpresa}")
                Log.d("VentasFragment", "Galpon: ${dataGalpon?.nombre}")
            }

            // Llamamos al método para mostrar el modal
            showModal()
        }

        binding.recyclerViewVentas.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewVentas.adapter = ventasAdapter

        cargarVentas()
    }

    // Método para cargar los datos de ventas en el RecyclerView
    private fun cargarVentas() {
        listaVentas = db.getAllDataPesoPollos()  // Asegúrate de que este método existe
        ventasAdapter.setVentas(listaVentas)
    }

    // Método para mostrar el modal sin asignar datos
    private fun showModal() {
        // Inflamos el layout del modal
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_venta_detalle, null)

        // Creamos el diálogo
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .create()

        // Mostramos el diálogo
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
