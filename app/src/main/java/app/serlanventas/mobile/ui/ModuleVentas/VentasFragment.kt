package app.serlanventas.mobile.ui.ModuleVentas

import NetworkUtils.isNetworkAvailable
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
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import app.serlanventas.mobile.ui.Jabas.ManagerPost.subirVentasLocales
import app.serlanventas.mobile.ui.Services.generateAndOpenPDF2
import app.serlanventas.mobile.ui.Utilidades.Constants.getBaseUrl
import com.google.android.material.button.MaterialButton
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
    private var isSyncInProgress = false

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L

    private val checkInternetRunnable = object : Runnable {
        override fun run() {
            if (isNetworkAvailable(requireContext())) {
                binding.btnSincronizarAllVentas.visibility = View.VISIBLE
            } else {
                binding.btnSincronizarAllVentas.visibility = View.GONE
                handler.postDelayed(this, checkInterval)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModuleVentasBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase(requireContext())

        setupConfigResponsiveFilter(view)

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
            if (isSyncInProgress) {
                showCustomToast(requireContext(), "Ya hay una sincronización en curso", "warning")
            }

            isSyncInProgress = true
            procesarVentaSync(venta)
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

    private fun setupConfigResponsiveFilter(view: View) {
        val filterContainer = view.findViewById<LinearLayout>(R.id.filter_container)
        val datesContainer = view.findViewById<LinearLayout>(R.id.dates_container)
        val buttonsContainer = view.findViewById<LinearLayout>(R.id.buttons_container)
        val searchButton = view.findViewById<MaterialButton>(R.id.searchButton)
        val syncButton = view.findViewById<MaterialButton>(R.id.btn_sincronizar_all_ventas)
        val clientContainer = view.findViewById<LinearLayout>(R.id.client_container)

        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        if (screenWidthDp < 600) {
            // Teléfono - diseño vertical
            filterContainer.orientation = LinearLayout.VERTICAL

            // Restablecer márgenes y pesos para evitar superposiciones
            val dateContainerParams = datesContainer.layoutParams as LinearLayout.LayoutParams
            dateContainerParams.weight = 0f
            dateContainerParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            dateContainerParams.rightMargin = 0
            datesContainer.layoutParams = dateContainerParams

            // Configurar el contenedor de botones para teléfonos
            val buttonContainerParams = buttonsContainer.layoutParams as LinearLayout.LayoutParams
            buttonContainerParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            buttonContainerParams.gravity = Gravity.CENTER
            buttonsContainer.layoutParams = buttonContainerParams

            if (screenWidthDp < 360) {
                // Dispositivos muy pequeños - hacer el botón de búsqueda más compacto
                datesContainer.orientation = LinearLayout.VERTICAL

                // Ajustar el peso para que ocupen todo el ancho cuando están en vertical
                for (i in 0 until datesContainer.childCount) {
                    val child = datesContainer.getChildAt(i)
                    if (child is LinearLayout) {
                        val params = child.layoutParams as LinearLayout.LayoutParams
                        params.width = LinearLayout.LayoutParams.MATCH_PARENT
                        params.weight = 0f
                        child.layoutParams = params
                    }
                }

                // Hacer el botón de búsqueda más compacto
                searchButton.layoutParams = (searchButton.layoutParams as LinearLayout.LayoutParams).apply {
                    width = 0
                    weight = 1f
                    rightMargin = (4 * displayMetrics.density).toInt() // Reducir el margen
                }

                // Texto más corto para el botón en pantallas muy pequeñas
                searchButton.text = "Buscar"
                searchButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f) // Texto más pequeño
                searchButton.setPadding(
                    (8 * displayMetrics.density).toInt(),
                    searchButton.paddingTop,
                    (8 * displayMetrics.density).toInt(),
                    searchButton.paddingBottom
                )

                // Ajustar el botón de sincronizar
                syncButton.layoutParams = (syncButton.layoutParams as LinearLayout.LayoutParams).apply {
                    width = (50 * displayMetrics.density).toInt() // Reducir el ancho
                    weight = 0f
                    leftMargin = (4 * displayMetrics.density).toInt() // Reducir el margen
                }

            } else {
                // Teléfonos normales
                datesContainer.orientation = LinearLayout.HORIZONTAL

                // Restablecer los pesos para las fechas en horizontal
                for (i in 0 until datesContainer.childCount) {
                    val child = datesContainer.getChildAt(i)
                    if (child is LinearLayout) {
                        val params = child.layoutParams as LinearLayout.LayoutParams
                        params.width = 0
                        params.weight = 1f
                        child.layoutParams = params
                    }
                }

                // Configurar los botones para teléfonos normales
                searchButton.layoutParams = (searchButton.layoutParams as LinearLayout.LayoutParams).apply {
                    width = 0
                    weight = 1f
                    rightMargin = (8 * displayMetrics.density).toInt()
                }

                // Restablecer el texto normal
                searchButton.text = requireContext().getString(R.string.search)
                searchButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) // Tamaño normal

                syncButton.layoutParams = (syncButton.layoutParams as LinearLayout.LayoutParams).apply {
                    width = (65 * displayMetrics.density).toInt()
                    weight = 0f
                    leftMargin = (8 * displayMetrics.density).toInt()
                }
            }

            // Asegurar que el contenedor de cliente esté visible
            clientContainer.visibility = View.VISIBLE

        } else {
            // Tablet - diseño horizontal con ajustes mejorados
            filterContainer.orientation = LinearLayout.HORIZONTAL

            // Asegurar que las fechas estén en horizontal
            datesContainer.orientation = LinearLayout.HORIZONTAL

            // Ajustar las fechas para que ocupen el espacio adecuado
            val dateContainerParams = datesContainer.layoutParams as LinearLayout.LayoutParams
            dateContainerParams.weight = 3f  // Aumentado para usar más espacio
            dateContainerParams.width = 0
            dateContainerParams.rightMargin = (16 * displayMetrics.density).toInt()
            datesContainer.layoutParams = dateContainerParams

            // Restablecer los pesos para las fechas
            for (i in 0 until datesContainer.childCount) {
                val child = datesContainer.getChildAt(i)
                if (child is LinearLayout) {
                    val params = child.layoutParams as LinearLayout.LayoutParams
                    params.width = 0
                    params.weight = 1f
                    child.layoutParams = params
                }
            }

            // Ajustar el contenedor de botones para que use el espacio necesario
            val buttonContainerParams = buttonsContainer.layoutParams as LinearLayout.LayoutParams
            buttonContainerParams.weight = 0f  // Sin peso para que solo use el espacio necesario
            buttonContainerParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            buttonContainerParams.bottomMargin = 0
            buttonsContainer.layoutParams = buttonContainerParams

            // Mantener la orientación horizontal para los botones
            buttonsContainer.orientation = LinearLayout.HORIZONTAL
            buttonsContainer.gravity = Gravity.END or Gravity.CENTER_VERTICAL

            // Restablecer el texto normal
            searchButton.text = requireContext().getString(R.string.search)
            searchButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) // Tamaño normal

            // Configurar el botón de búsqueda para tablets - tamaño adecuado
            searchButton.layoutParams = (searchButton.layoutParams as LinearLayout.LayoutParams).apply {
                width = LinearLayout.LayoutParams.WRAP_CONTENT
                height = LinearLayout.LayoutParams.WRAP_CONTENT
                weight = 0f
                rightMargin = (8 * displayMetrics.density).toInt()
            }
            // Establecer el ancho mínimo directamente en el botón
            searchButton.setMinimumWidth((100 * displayMetrics.density).toInt())

            // Configurar el botón de sincronizar para tablets
            syncButton.layoutParams = (syncButton.layoutParams as LinearLayout.LayoutParams).apply {
                width = (65 * displayMetrics.density).toInt()
                height = LinearLayout.LayoutParams.WRAP_CONTENT
                weight = 0f
                leftMargin = 0
            }

            // Asegurar que el contenedor de cliente esté visible
            clientContainer.visibility = View.VISIBLE
        }
    }

    fun procesarVentaSync(venta: DataPesoPollosEntity) {
        try {
            val baseUrl = getBaseUrl()
            val ventaNotSync = db.obtenerPesoPollosPorId(venta.id)

            if (ventaNotSync == null) {
                showCustomToast(
                    requireContext(),
                    "La venta no existe, por favor actualice la lista",
                    "warning"
                )
                return
            }

            if (ventaNotSync.idEstado == "0") {
                if (isNetworkAvailable(requireContext())) {
                    subirVentasLocales(baseUrl, venta, requireContext(), venta.id) { success ->
                        if (success) {
                            Log.d("ManagerPost", "Venta local subida correctamente")
                            showCustomToast(
                                requireContext(),
                                "Venta local subida correctamente",
                                "warning"
                            )
                            realizarBusquedaVentas()
                        } else {
                            Log.e("ManagerPost", "Error al subir venta local")
                            showCustomToast(
                                requireContext(),
                                "No se pudo subir la venta local, por favor intente de nuevo",
                                "error"
                            )
                        }
                    }
                } else {
                    showCustomToast(
                        requireContext(),
                        "No hay conexión a internet, por favor conéctese e intente nuevamente para realizar esta acción",
                        "error"
                    )
                }
            } else {
                showCustomToast(
                    requireContext(),
                    "La venta ya se encuentra sincronizada",
                    "warning"
                )
            }
        } catch (e: Exception) {
            Log.e("SyncError", "Error durante la sincronización: ${e.message}")
            showCustomToast(requireContext(), "Ocurrió un error durante la sincronización", "error")
        } finally {
            isSyncInProgress = false
        }
    }


    // Método para cargar los datos de ventas en el RecyclerView
    private fun cargarVentas() {
        val startDate = binding.startDateFilterInput.text.toString()
        val endDate = binding.endDateFilterInput.text.toString()
        var serie = db.getSerieDevice()
        if (serie != null) {
            lifecycleScope.launch {
                listaVentas = withContext(Dispatchers.IO) {
                    try {
                        db.getDataPesoPollosByDate(serie.codigo, startDate, endDate)
                    } catch (e: Exception) {
                        // Manejar errores de carga de datos
                        emptyList()
                    }
                }
                ventasAdapter.setVentas(listaVentas)
            }
        }
    }

    private var currentDialog: AlertDialog? = null

    private fun showModal(
        ventaId: Int,
        nombreComprobante: String, idEmpresa: String, rsEmpresa: String, correlativo: String,
        fecha: String, hora: String, nombreCliente: String, idCliente: String,
        totalJabas: String, totalPollos: String, totalPesoJabas: String, totalPeso: String,
        totalNeto: String, pkPollo: String, totalPagar: String, mensaje: String, sede: String,
        detallesPesoPollos: List<DataDetaPesoPollosEntity>, psPromedio: String
    ) {
        // Verificar si el diálogo ya está mostrándose
        if (currentDialog?.isShowing == true) {
            Log.d("Dialog", "Dialog is already showing")
            return
        }

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
            dialogView.findViewById<TextView>(id)?.text = value
        }

        // Configurar el RecyclerView para mostrar los detalles de pesos
        val recyclerViewDetalles = dialogView.findViewById<RecyclerView>(R.id.recyclerViewDetallesPesos)
        recyclerViewDetalles.setHasFixedSize(true)
        recyclerViewDetalles.layoutManager = LinearLayoutManager(context)

        // Creamos el diálogo con dos botones: Imprimir y Cerrar
        currentDialog = AlertDialog.Builder(context)
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
        currentDialog?.show()

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
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_detalle_pesos, parent, false)
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
                val serie = db.getSerieDevice()
                if (cliente != null && serie != null) {
                    db.getDataPesoPollosByClienteAndDate(
                        serie.codigo,
                        cliente.numeroDocCliente,
                        startDate,
                        endDate
                    )
                } else {
                    db.getDataPesoPollosByDate(serie!!.codigo, startDate, endDate)
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
            realizarBusquedaVentas()
        }
    }

    private fun realizarBusquedaVentas() {
        val selectedClientText = binding.clientFilterInput.text.toString().trim()
        val startDate = binding.startDateFilterInput.text.toString().trim()
        val endDate = binding.endDateFilterInput.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            showCustomToast(requireContext(), "Por favor seleccione un rango de fecha", "info")
        }

        // Buscar por cliente y fecha
        val selectedClient = findClienteByDisplayName(selectedClientText)
        filtrarVentasPorClienteYFecha(selectedClient)
    }

    private fun setupSynAllVentasButton() {
        binding.btnSincronizarAllVentas.setOnClickListener {
            Log.d("Sync", "Sync clicked All")

            val baseUrl = getBaseUrl()
            val ventasNotSync = db.getAllDataPesoPollosNotSync()

            if (ventasNotSync.isNotEmpty()) {
                ventasNotSync.forEach { venta ->
                    if (isNetworkAvailable(requireContext())) {
                        subirVentasLocales(baseUrl, venta, requireContext(), venta.id) { success ->
                            if (success) {
                                Log.d(
                                    "ManagerPost",
                                    "Venta local subida correctamente: ${venta.id}"
                                )
                            } else {
                                Log.e("ManagerPost", "Error al subir venta local: ${venta.id}")
                                showCustomToast(
                                    requireContext(),
                                    "Error al subir venta local: ${venta.serie}-${venta.numero}",
                                    "error"
                                )
                                return@subirVentasLocales
                            }
                            isSyncInProgress = true
                        }
                    } else {
                        showCustomToast(
                            requireContext(),
                            "¡No hay conexión a internet!\nPor favor conéctese e intente nuevamente.",
                            "error"
                        )
                    }
                }
                showCustomToast(
                    requireContext(),
                    "¡Se sincronizaron todas las ventas locales correctamente!",
                    "success"
                )
            } else {
                showCustomToast(requireContext(), "¡No hay ventas para sincronizar!", "info")
            }
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
        showDatePickerDialog(startDateInput)
        showDatePickerDialog(endDateInput)

        // Configurar el OnClickListener para el campo de fecha de inicio
        startDateInput.setOnClickListener {
            showDatePickerDialog(startDateInput, true)
        }

        // Configurar el OnClickListener para el campo de fecha de fin
        endDateInput.setOnClickListener {
            showDatePickerDialog(endDateInput, true)
        }
    }

    private fun showDatePickerDialog(editText: TextInputEditText, isCLick: Boolean = false) {
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
        if (isCLick) {
            datePickerDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()

        handler.post(checkInternetRunnable)
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(checkInternetRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        db.close()
    }
}

