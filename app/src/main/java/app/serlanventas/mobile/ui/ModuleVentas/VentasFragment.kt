package app.serlanventas.mobile.ui.ModuleVentas

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentModuleVentasBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.Services.generateAndOpenPDF2
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

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
            val pesoPollos = db.obtenerPesoPollosPorId(venta.id)

            pesoPollos?.let {
                val dataNucleo = db.obtenerNucleoPorId(it.idNucleo)
                val dataGalpon = db.obtenerGalponPorId(it.idGalpon)

                // Preparar los datos
                val nombreComprobante = "Nota de Venta"
                val idEmpresa = "RUC: ${dataNucleo?.idEmpresa}"
                val rsEmpresa = "MULTIGRANJAS SERLAN S.A.C."
                val correlativo = "${it.serie}-${it.numero}"
                val fecha = "FECHA: ${it.fecha.split(" ")[0]}"
                val hora = "HORA: ${it.fecha.split(" ")[1]}"
                val nombreCliente = "CLIENTE: ${it.nombreCompleto ?: "N/A"}"
                val idCliente = "N° DOC: ${it.numeroDocCliente}"
                val totalJabas = "C. DE JABAS: ${it.totalJabas}"
                val totalPollos = "C. DE POLLO: ${it.totalPollos}"
                val totalPesoJabas = "TARA: ${it.totalPesoJabas}"
                val totalPeso = "PESO BRUTO: ${it.totalPeso}"
                val totalNeto = "NETO: ${it.totalNeto}"
                val pkPollo = "PRECIO X KG: ${it.PKPollo}"
                val totalPagar = "T. A PAGAR: ${it.TotalPagar}"
                // Cálculo del peso promedio
                val psPromedio = if (it.totalPollos > "0") {
                    "PESO PROMEDIO: ${String.format("%.2f", it.totalNeto.toDouble() / it.totalPollos.toDouble())}"
                } else {
                    "PESO PROMEDIO: 0.00"
                }
                val mensaje = "¡GRACIAS POR SU COMPRA!"
                val sede = "SEDE: ${dataNucleo?.nombre} - ${dataGalpon?.nombre}"

                // Obtener los detalles de peso de pollos
                val detallesPesoPollos = db.obtenerDetaPesoPollosPorId(it.id.toString())

                // Mostrar el modal con los detalles de la venta
                showModal(venta.id, nombreComprobante, idEmpresa, rsEmpresa, correlativo, fecha, hora, nombreCliente, idCliente, totalJabas, totalPollos, totalPesoJabas, totalPeso, totalNeto, pkPollo, totalPagar, mensaje, sede, detallesPesoPollos, psPromedio)
            }
        }

        binding.recyclerViewVentas.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewVentas.adapter = ventasAdapter
        cargarVentas()
    }

    // Método para cargar los datos de ventas en el RecyclerView
    private fun cargarVentas() {
        var serie = db.getSerieDevice()
        if (serie != null){
            listaVentas = db.getAllDataPesoPollosForDevice(serie.codigo)
            ventasAdapter.setVentas(listaVentas)
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showModal(
        ventaId: Int,
        nombreComprobante: String, idEmpresa: String, rsEmpresa: String, correlativo: String,
        fecha: String, hora: String, nombreCliente: String, idCliente: String,
        totalJabas: String, totalPollos: String, totalPesoJabas: String, totalPeso: String,
        totalNeto: String, pkPollo: String, totalPagar: String, mensaje: String, sede: String,
        detallesPesoPollos: List<DataDetaPesoPollosEntity>, psPromedio: String
    ) {
        // Inflamos el layout del modal
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_venta_detalle, null)

        // Mapa de TextView y sus valores
        val textViews = mapOf(
            R.id.tvNombreComprobante to nombreComprobante,
            R.id.tviDEmpresa to idEmpresa,
            R.id.tvRSEmpresa to rsEmpresa,
            R.id.tvCorrelativo to correlativo,
            R.id.tvFecha to fecha,
            R.id.tvHora to hora,
            R.id.tvNombreCliente to nombreCliente,
            R.id.tvIdCliente to idCliente,
            R.id.tvTotalJabas to totalJabas,
            R.id.tvTotalPollos to totalPollos,
            R.id.tvTotalPesoJabas to totalPesoJabas,
            R.id.tvTotalPeso to totalPeso,
            R.id.tvTotalNeto to totalNeto,
            R.id.tvPKPollo to pkPollo,
            R.id.tvTotalPagar to totalPagar,
            R.id.tvPesoPromedio to psPromedio,
            R.id.tvMensaje to mensaje,
            R.id.tvSede to sede
        )

        // Asignamos los valores a los TextView
        textViews.forEach { (id, value) ->
            dialogView.findViewById<TextView>(id).text = value
        }

        // Configurar el RecyclerView para mostrar los detalles de pesos
        val recyclerViewDetalles = dialogView.findViewById<RecyclerView>(R.id.recyclerViewDetallesPesos)
        recyclerViewDetalles.layoutManager = LinearLayoutManager(requireContext())

        // Crear un adaptador para los detalles de pesos
        val detalleAdapter = DetallesPesosDialogAdapter(detallesPesoPollos)
        recyclerViewDetalles.adapter = detalleAdapter

        // Creamos el diálogo con dos botones: Imprimir y Cerrar
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Imprimir") { dialog, _ ->
                lifecycleScope.launch {
                    imprimirDetalleVenta(ventaId)
                }
                dialog.dismiss()
            }
            .create()

        // Mostramos el diálogo
        dialog.show()
    }

    private suspend fun imprimirDetalleVenta(ventaId: Int) {
        val pesoPollos = db.obtenerPesoPollosPorId(ventaId)
        val detallesPesoPollos = db.obtenerDetaPesoPollosPorId(ventaId.toString())

        pesoPollos?.let { it ->
            val dataNucleo = db.obtenerNucleoPorId(it.idNucleo)
            val dataGalpon = db.obtenerGalponPorId(it.idGalpon)

            val totalPollos = it.totalPollos.toIntOrNull() ?: 0
            val totalNeto = it.totalNeto.toDoubleOrNull() ?: 0.0
            val pesoPromedio = if (totalPollos > 0) String.format("%.2f", totalNeto / totalPollos) else "0.00"
            val correlativo = "${it.serie} - ${it.numero}"
            // Crear JSON
            val DATAPDF = JSONObject().apply {
                put("PESO_POLLO", JSONArray().put(JSONObject().apply {
                    put("serie", correlativo)
                    put("fecha", it.fecha)
                    put("totalJabas", it.totalJabas)
                    put("totalPollos", totalPollos.toString())
                    put("totalPeso", it.totalPeso)
                    put("tara", it.totalPesoJabas)
                    put("neto", it.totalNeto)
                    put("precio_kilo", it.PKPollo)
                    put("pesoPromedio", pesoPromedio)
                    put("total_pagar", it.TotalPagar)
                }))

                put("CLIENTE", JSONArray().put(JSONObject().apply {
                    put("dni", it.numeroDocCliente ?: "N/A")
                    put("rs", it.nombreCompleto ?: "N/A")
                }))

                put("GALPON", JSONArray().put(JSONObject().apply {
                    put("nomgal", dataGalpon?.nombre ?: "N/A")
                }))

                put("ESTABLECIMIENTO", JSONArray().put(JSONObject().apply {
                    put("nombre", dataNucleo?.nombre ?: "N/A")
                }))

                put("EMPRESA", JSONArray().put(JSONObject().apply {
                    put("nroRuc", dataNucleo?.idEmpresa ?: "N/A")
                    put("nombreComercial", "MULTIGRANJAS SERLAN S.A.C.")
                }))

                put("DETA_PESOPOLLO", JSONArray().apply {
                    detallesPesoPollos.forEach { detalle ->
                        put(JSONObject().apply {
                            put("cantJabas", detalle.cantJabas)
                            put("cantPollos", detalle.cantPollos)
                            put("peso", detalle.peso)
                            put("tipo", detalle.tipo)
                        })
                    }
                })
            }

            generateAndOpenPDF2(DATAPDF, requireContext())

        }
    }

    // Adaptador para mostrar los detalles de pesos en el diálogo
    inner class DetallesPesosDialogAdapter(
        private val detalles: List<DataDetaPesoPollosEntity>
    ) : RecyclerView.Adapter<DetallesPesosDialogAdapter.DetalleViewHolder>() {

        inner class DetalleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val idJabas: TextView = itemView.findViewById(R.id.id_jabas)
            val numeroJabas: TextView = itemView.findViewById(R.id.numero_jabas)
            val numeroPollos: TextView = itemView.findViewById(R.id.numero_pollos)
            val pesoKg: TextView = itemView.findViewById(R.id.peso_kg)
            val conPollos: TextView = itemView.findViewById(R.id.con_pollos)
            val estadoIcon: ImageView = itemView.findViewById(R.id.estado_icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalleViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_pesos, parent, false)
            return DetalleViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: DetalleViewHolder, position: Int) {
            val detalle = detalles[position]

            // Asignar los datos a las vistas
            holder.idJabas.text = "#${position + 1}"
            holder.numeroJabas.text = "${detalle.cantJabas}"
            holder.numeroPollos.text = "${detalle.cantPollos}"
            holder.pesoKg.text = "${detalle.peso} kg"

            // Configurar el estado y el ícono según el tipo
            holder.conPollos.visibility = View.VISIBLE
            if (detalle.tipo.contains("CON POLLOS", ignoreCase = true)) {
                holder.conPollos.text = "CON POLLOS"
                holder.estadoIcon.setImageResource(R.drawable.cabezapollo)
            } else {
                holder.conPollos.text = "SIN POLLOS"
                holder.estadoIcon.setImageResource(R.drawable.jabadepollo)
            }
        }

        override fun getItemCount(): Int = detalles.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

