package app.serlanventas.mobile.ui.ModuleVentas

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentModuleVentasBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.Services.generateAndOpenPDF2
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class VentasFragment : Fragment() {

    private var _binding: FragmentModuleVentasBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var ventasAdapter: VentasAdapter
    private var listaVentas: List<DataPesoPollosEntity> = emptyList()
    private var listaClientes: List<ClienteEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModuleVentasBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase(requireContext())

        // Definir las funciones para manejar los clics
        val onMostrarClick: (DataPesoPollosEntity) -> Unit = { venta ->
            lifecycleScope.launch {
                val pesoPollos = withContext(Dispatchers.IO) {
                    val pesoData = db.obtenerPesoPollosPorId(venta.id)
                    val dataNucleo = pesoData?.let { db.obtenerNucleoPorId(it.idNucleo) }
                    val dataGalpon = pesoData?.let { db.obtenerGalponPorId(it.idGalpon) }
                    val detallesPesoPollos =
                        pesoData?.let { db.obtenerDetaPesoPollosPorId(it.id.toString()) }
                            ?: emptyList()

                    Triple(pesoData, Pair(dataNucleo, dataGalpon), detallesPesoPollos)
                }

                val (pollosData, nucleoGalpon, detalles) = pesoPollos
                val (dataNucleo, dataGalpon) = nucleoGalpon

                pollosData?.let { it ->
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
                    val psPromedio = if (it.totalPollos > "0") {
                        "PESO PROMEDIO: ${
                            String.format(
                                "%.2f",
                                it.totalNeto.toDouble() / it.totalPollos.toDouble()
                            )
                        }"
                    } else {
                        "PESO PROMEDIO: 0.00"
                    }
                    val mensaje = "¡GRACIAS POR SU COMPRA!"
                    val sede = "SEDE: ${dataNucleo?.nombre} - ${dataGalpon?.nombre}"

                    val detallesPesoPollos = db.obtenerDetaPesoPollosPorId(it.id.toString())

                    showModal(
                        venta.id,
                        nombreComprobante,
                        idEmpresa,
                        rsEmpresa,
                        correlativo,
                        fecha,
                        hora,
                        nombreCliente,
                        idCliente,
                        totalJabas,
                        totalPollos,
                        totalPesoJabas,
                        totalPeso,
                        totalNeto,
                        pkPollo,
                        totalPagar,
                        mensaje,
                        sede,
                        detallesPesoPollos,
                        psPromedio
                    )
                }
            }
        }

        val onSyncClick: (DataPesoPollosEntity) -> Unit = { venta ->
           Log.d("Sync", "Sync clicked for venta ID: ${venta.id}")
            Toast.makeText(requireContext(), "Proximamente...", Toast.LENGTH_SHORT).show()
        }

        // Crear la instancia del adaptador con las funciones definidas
        ventasAdapter = VentasAdapter(emptyList(), onMostrarClick, onSyncClick)

        setupDateFilters()
        setupSearchButton()
        setupSynAllVentasButton()
        setupClientDropdown()

        binding.recyclerViewVentas.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewVentas.adapter = ventasAdapter
        cargarVentas()
    }


    // Método para cargar los datos de ventas en el RecyclerView
    private fun cargarVentas() {
        var serie = db.getSerieDevice()
        if (serie != null) {
            listaVentas = db.getAllDataPesoPollosForDevice(serie.codigo)
            ventasAdapter.setVentas(listaVentas)
        }
    }

    private fun showModal(
        ventaId: Int,
        nombreComprobante: String, idEmpresa: String, rsEmpresa: String, correlativo: String,
        fecha: String, hora: String, nombreCliente: String, idCliente: String,
        totalJabas: String, totalPollos: String, totalPesoJabas: String, totalPeso: String,
        totalNeto: String, pkPollo: String, totalPagar: String, mensaje: String, sede: String,
        detallesPesoPollos: List<DataDetaPesoPollosEntity>, psPromedio: String
    ) {
        // Inflamos el layout del modal
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.fragment_venta_detalle, null)

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
            dialogView.findViewById<TextView>(id)?.text = value
        }

        // Configurar el RecyclerView para mostrar los detalles de pesos
        val recyclerViewDetalles =
            dialogView.findViewById<RecyclerView>(R.id.recyclerViewDetallesPesos)
        recyclerViewDetalles.setHasFixedSize(true)
        recyclerViewDetalles.layoutManager = LinearLayoutManager(context)

        // Creamos el diálogo con dos botones: Imprimir y Cerrar
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Imprimir") { dialog, _ ->
                lifecycleScope.launch {
                    imprimirDetalleVenta(ventaId)
                }
                dialog.dismiss()
            }
            .create()

        // Crear un adaptador para los detalles de pesos
        val detalleAdapter = DetallesPesosDialogAdapter(detallesPesoPollos)
        recyclerViewDetalles.adapter = detalleAdapter

        // Mostramos el diálogo
        dialog.show()

        // Logs adicionales para depuración
        Log.d("Dialog", "Dialog shown successfully")
    }

    private suspend fun imprimirDetalleVenta(ventaId: Int) {
        val pesoPollos = db.obtenerPesoPollosPorId(ventaId)
        val detallesPesoPollos = db.obtenerDetaPesoPollosPorId(ventaId.toString())

        pesoPollos?.let { it ->
            val dataNucleo = db.obtenerNucleoPorId(it.idNucleo)
            val dataGalpon = db.obtenerGalponPorId(it.idGalpon)

            val totalPollos = it.totalPollos.toIntOrNull() ?: 0
            val totalNeto = it.totalNeto.toDoubleOrNull() ?: 0.0
            val pesoPromedio =
                if (totalPollos > 0) String.format("%.2f", totalNeto / totalPollos) else "0.00"
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
            val fechaPeso: TextView = itemView.findViewById(R.id.fecha_peso)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalleViewHolder {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.list_detalle_pesos, parent, false)
            return DetalleViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: DetalleViewHolder, position: Int) {
            val detalle = detalles[position]

            // Asignar los datos a las vistas
            holder.idJabas.text = "#${position + 1}"
            holder.numeroJabas.text = "${detalle.cantJabas}"
            holder.numeroPollos.text = "${detalle.cantPollos}"
            holder.pesoKg.text = "${detalle.peso} kg"

            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            try {
                // Parsear la cadena a un objeto Date
                val fechaPesoDate = inputFormat.parse(detalle.fechaPeso)

                // Formatear la fecha
                val dateFormat = SimpleDateFormat("MMM dd, yyyy")
                val date = dateFormat.format(fechaPesoDate)

                // Formatear la hora
                val timeFormat = SimpleDateFormat("HH:mm:ss")
                val time = timeFormat.format(fechaPesoDate)

                // Establecer el texto con la fecha y la hora
                holder.fechaPeso.text = "$date\n$time"
            } catch (e: Exception) {
                // Manejar el caso donde el formato de la fecha no es el esperado
                holder.fechaPeso.text = "Fecha no válida"
            }


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

    // Configurar el AutoCompleteTextView para clientes
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun setupClientDropdown() {
        lifecycleScope.launch {
            // Cargar la lista de clientes en segundo plano
            listaClientes = withContext(Dispatchers.IO) {
                try {
                    db.getAllClientes()
                } catch (e: Exception) {
                    // Manejar errores de carga de datos
                    emptyList()
                }
            }

            // Configurar el adaptador con la lista de clientes
            val clienteAdapter = ClienteAdapter(requireContext(), listaClientes)
            binding.clientFilterInput.setAdapter(clienteAdapter)

            // Configurar el OnItemClickListener para manejar la selección de un cliente
            binding.clientFilterInput.setOnItemClickListener { parent, _, position, _ ->
                val cliente = parent.getItemAtPosition(position) as ClienteEntity
                binding.clientFilterInput.setText("${cliente.numeroDocCliente} - ${cliente.nombreCompleto}")
                filtrarVentasPorClienteYFecha(cliente)
            }

            // Configurar el OnTouchListener para manejar el clic y mostrar la lista desplegable
            binding.clientFilterInput.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    // Mover el cursor al final del texto
                    binding.clientFilterInput.post {
                        binding.clientFilterInput.setSelection(binding.clientFilterInput.text.length)
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.clientFilterInput.selectAll()
                        binding.clientFilterInput.setText("")
                    }, 300)


                    // Mostrar el teclado y la lista desplegable
                    binding.clientFilterInput.showKeyboard()
                    binding.clientFilterInput.requestFocus()
                    binding.clientFilterInput.showDropDown()

                }
                true
            }

            // Configurar el OnFocusChangeListener para mostrar el teclado pero no la lista desplegable
            binding.clientFilterInput.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        // Mover el cursor al final del texto
                        binding.clientFilterInput.post {
                            binding.clientFilterInput.setSelection(binding.clientFilterInput.text.length)
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.clientFilterInput.selectAll()
                        }, 300)

                        // Mostrar el teclado
                        binding.clientFilterInput.showKeyboard()
                    }
                }

            // Configurar el threshold para mostrar la lista desplegable después de ingresar un carácter
            binding.clientFilterInput.threshold = 1

            // Configurar el OnTextChangedListener para mostrar la lista desplegable cuando se escribe
            binding.clientFilterInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().isNotEmpty()) {
                        binding.clientFilterInput.showDropDown()
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    // Extensión para mostrar el teclado
    fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        this.requestFocus()
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun filtrarVentasPorClienteYFecha(cliente: ClienteEntity?) {
        val startDate = binding.startDateFilterInput.text.toString()
        val endDate = binding.endDateFilterInput.text.toString()

        lifecycleScope.launch {
            listaVentas = withContext(Dispatchers.IO) {
                if (cliente != null) {
                    db.getDataPesoPollosByClienteAndDate(
                        cliente.numeroDocCliente,
                        startDate,
                        endDate
                    )
                } else {
                    db.getDataPesoPollosByDate(startDate, endDate)
                }
            }
            ventasAdapter.setVentas(listaVentas)
        }
    }

    private fun findClienteByDisplayName(displayName: String): ClienteEntity? {
        // Buscar el cliente en la lista basándose en el texto mostrado (númeroDocCliente - nombreCompleto)
        return listaClientes.find { cliente ->
            "${cliente.numeroDocCliente} - ${cliente.nombreCompleto}" == displayName
        }
    }

    private fun setupSearchButton() {
        binding.searchButton.setOnClickListener {
            val selectedClientText = binding.clientFilterInput.text.toString().trim()
            val startDate = binding.startDateFilterInput.text.toString().trim()
            val endDate = binding.endDateFilterInput.text.toString().trim()

            // Buscar por cliente y fecha
            val selectedClient = findClienteByDisplayName(selectedClientText)
            filtrarVentasPorClienteYFecha(selectedClient)
        }
    }

    private fun setupSynAllVentasButton() {
        binding.btnSincronizarAllVentas.setOnClickListener {
            Log.d("Sync", "Sync clicked All")
            Toast.makeText(requireContext(), "Proximamente...", Toast.LENGTH_SHORT).show()
//            var baseUrl = Constants.getBaseUrl()
//            val ventasNotSync = db.getAllDataPesoPollosNotSync()
//            subirVentasLocales(baseUrl, ventasNotSync, requireContext(), null)
        }
    }

    // First, create a Client adapter class
    class ClienteAdapter(context: Context, private val clientes: List<ClienteEntity>) :
        ArrayAdapter<ClienteEntity>(
            context,
            android.R.layout.simple_dropdown_item_1line,
            clientes
        ) {

        // Lista filtrada que se muestra en el AutoCompleteTextView
        private var filteredClientes: List<ClienteEntity> = clientes

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_dropdown_item_1line, parent, false)

            val cliente = filteredClientes[position]
            val textView = view.findViewById<TextView>(android.R.id.text1)
            textView.text = "${cliente.numeroDocCliente} - ${cliente.nombreCompleto}"

            return view
        }

        override fun getCount(): Int {
            return filteredClientes.size
        }

        override fun getItem(position: Int): ClienteEntity {
            return filteredClientes[position]
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val results = FilterResults()

                    // Filtrar la lista de clientes basándose en el texto ingresado
                    val filteredList = if (constraint.isNullOrEmpty()) {
                        clientes // Mostrar todos los clientes si no hay restricción
                    } else {
                        clientes.filter { cliente ->
                            // Buscar en el número de documento o en el nombre completo
                            cliente.numeroDocCliente.contains(
                                constraint.toString(),
                                ignoreCase = true
                            ) ||
                                    cliente.nombreCompleto.contains(
                                        constraint.toString(),
                                        ignoreCase = true
                                    )
                        }
                    }

                    results.values = filteredList
                    results.count = filteredList.size
                    return results
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    @Suppress("UNCHECKED_CAST")
                    filteredClientes = results?.values as? List<ClienteEntity> ?: emptyList()
                    notifyDataSetChanged() // Notificar cambios para actualizar la vista
                }
            }
        }
    }

    private fun setupDateFilters() {
        val startDateInput = binding.startDateFilterInput
        val endDateInput = binding.endDateFilterInput

        // Configurar el OnClickListener para el campo de fecha de inicio
        startDateInput.setOnClickListener {
            showDatePickerDialog(startDateInput)
        }

        // Configurar el OnClickListener para el campo de fecha de fin
        endDateInput.setOnClickListener {
            showDatePickerDialog(endDateInput)
        }
    }

    private fun showDatePickerDialog(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Crear el DatePickerDialog
        val datePickerDialog =
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editText.setText(dateFormat.format(selectedDate.time))
            }, year, month, day)

        // Prellenar el campo con la fecha actual si está vacío
        if (editText.text.isNullOrEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            editText.setText(dateFormat.format(calendar.time))
        }

        // Mostrar el DatePickerDialog
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

