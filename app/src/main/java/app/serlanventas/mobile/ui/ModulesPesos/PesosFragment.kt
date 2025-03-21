package app.serlanventas.mobile.ui.ModulesPesos

import android.annotation.SuppressLint
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentModulePesosBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost.obtenerPesosServer
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import app.serlanventas.mobile.ui.ModuleVentas.VentasFragment.ClienteAdapter
import app.serlanventas.mobile.ui.preliminar.FragmentPreliminar.TotalesData
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

class PesosFragment : Fragment() {

    private var _binding: FragmentModulePesosBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var pesosAdapter: PesosAdapter
    private var listaPesos: List<PesosEntity> = emptyList()
    private var listaClientes: List<ClienteEntity> = emptyList()

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

        setupConfigResponsiveFilter(view)

        binding.btnSincronizarPesos.setOnClickListener {
            binding.btnSincronizarPesos.isEnabled = false
            obtenerPesosServer(requireContext()) { success ->
                if (success) {
                    showCustomToast(requireContext(), "Pesos sincronizados con éxito", "success")
                } else {
                    showCustomToast(requireContext(), "Error al sincronizar pesos", "error")
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.btnSincronizarPesos.isEnabled = true
                    cargarPesos()
                }, 500)
            }
        }

        setupDateFilters()
        setupSearchButton()
        setupClientDropdown()

        pesosAdapter = PesosAdapter(emptyList(), ::onMostrarClick, ::onEliminarClick)
        binding.recyclerViewPesos.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPesos.adapter = pesosAdapter
        cargarPesos()
    }

    private fun setupConfigResponsiveFilter(view: View) {
        val filterContainer = view.findViewById<LinearLayout>(R.id.filter_container)
        val datesContainer = view.findViewById<LinearLayout>(R.id.dates_container)
        val buttonsContainer = view.findViewById<LinearLayout>(R.id.buttons_container)
        val searchButton = view.findViewById<MaterialButton>(R.id.searchButton)
        val syncButton = view.findViewById<MaterialButton>(R.id.btn_sincronizar_pesos)
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
                searchButton.layoutParams =
                    (searchButton.layoutParams as LinearLayout.LayoutParams).apply {
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
                syncButton.layoutParams =
                    (syncButton.layoutParams as LinearLayout.LayoutParams).apply {
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
                searchButton.layoutParams =
                    (searchButton.layoutParams as LinearLayout.LayoutParams).apply {
                        width = 0
                        weight = 1f
                        rightMargin = (8 * displayMetrics.density).toInt()
                    }

                // Restablecer el texto normal
                searchButton.text = requireContext().getString(R.string.search)
                searchButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) // Tamaño normal

                syncButton.layoutParams =
                    (syncButton.layoutParams as LinearLayout.LayoutParams).apply {
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
            searchButton.layoutParams =
                (searchButton.layoutParams as LinearLayout.LayoutParams).apply {
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

    private fun cargarPesos() {
        val startDate = binding.startDateFilterInput.text.toString()
        val endDate = binding.endDateFilterInput.text.toString()

        lifecycleScope.launch {
            listaPesos = withContext(Dispatchers.IO) {
                try {
                    db.getTempPesoByDate(startDate, endDate)
                } catch (e: Exception) {
                    // Manejar errores de carga de datos
                    emptyList()
                }
            }
            pesosAdapter.setPesos(listaPesos)
        }

    }

    // Sección de manejo de clic en "Mostrar"
    @SuppressLint("DefaultLocale")
    private fun onMostrarClick(peso: PesosEntity) {
        val pesoDetalles = db.getPesoPorId(peso.id)
        if (pesoDetalles != null) {
            // Convertir JSON a objetos
            val dataPesoObjeto = parseDataPesoJson(pesoDetalles.dataPesoJson)
            val dataDetaPesoList = parseDataDetaPesoJson(pesoDetalles.dataDetaPesoJson)
            val dataNucleo = db.obtenerNucleoPorId(dataPesoObjeto.idNucleo)
            val dataGalpon = db.obtenerGalponPorId(dataPesoObjeto.idGalpon)

            val (totalJ, totalP, totalPesoPollos, totalPesoJ, neto) = calcularTotales(
                dataDetaPesoList
            )

            val nombreComprobante = "DETALLE DE PESO"
            val idEmpresa = "RUC: ${dataNucleo?.idEmpresa}"
            val rsEmpresa = "MULTIGRANJAS SERLAN S.A.C."
            val correlativo = "${dataPesoObjeto.serie}-${dataPesoObjeto.numero}"
            val fechaParts = pesoDetalles.fechaRegistro.split(" ")
            val fecha = if (fechaParts.size > 0) "FECHA: ${fechaParts[0]}" else "FECHA: N/A"
            val hora = if (fechaParts.size > 1) "HORA: ${fechaParts[1]}" else "HORA: N/A"
            val nombreCliente = "CLIENTE: ${dataPesoObjeto.nombreCompleto ?: "N/A"}"
            val idCliente = "N° DOC: ${dataPesoObjeto.numeroDocCliente}"
            val totalJabas = "C. DE JABAS: $totalJ"
            val totalPollos = "C. DE POLLO: $totalP"
            val totalPesoJabas = "TARA: $totalPesoJ"
            val totalPeso = "PESO BRUTO: $totalPesoPollos"
            val totalNeto = "NETO: $neto"
            val pkPollo = "PRECIO X KG: ${dataPesoObjeto.PKPollo}"
            val totalPagar = "T. A PAGAR: ${dataPesoObjeto.TotalPagar.coerceAtLeast("0.0")}"
            val psPromedio = if (totalP > 0) {
                "PESO PROMEDIO: ${
                    String.format(
                        "%.2f",
                        neto / totalP
                    )
                }"
            } else {
                "PESO PROMEDIO: 0.00"
            }
            val mensaje = "¡GRACIAS POR SU COMPRA!"
            val sede = "SEDE: ${dataNucleo?.nombre} - ${dataGalpon?.nombre}"


            showModal(
                dataPesoObjeto.id,
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
                dataDetaPesoList,
                psPromedio
            )

        }
    }

    private fun calcularTotales(dataDetaPesoPollos: List<DataDetaPesoPollosEntity>): TotalesData {
        var totalJabas = 0
        var totalPollos = 0
        var totalPesoPollos = 0.0
        var totalPesoJabas = 0.0

        dataDetaPesoPollos.forEach { detaPesoPollo ->
            if (detaPesoPollo.tipo == "JABAS CON POLLOS") {
                totalPollos += detaPesoPollo.cantJabas * detaPesoPollo.cantPollos
                totalPesoPollos += detaPesoPollo.peso.toDouble()
            } else {
                totalJabas += detaPesoPollo.cantJabas.toInt()
                totalPesoJabas += detaPesoPollo.peso.toDouble()
            }
        }

        if(totalPollos == 0){
            totalPesoPollos = totalPesoJabas
        }

        val neto = (totalPesoPollos - totalPesoJabas).coerceAtLeast(0.0)

        return TotalesData(totalJabas, totalPollos, totalPesoPollos, totalPesoJabas, neto)
    }

    private var currentDialog: android.app.AlertDialog? = null
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
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_detalle_temp_pesos, null)

        // Mapa de TextView y sus valores
        val textViews = mapOf(
            R.id.tvNombreComprobante to nombreComprobante,
            R.id.tviDEmpresa to idEmpresa,
            R.id.tvRSEmpresa to rsEmpresa, -
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
        currentDialog = android.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
//            .setNeutralButton("Imprimir") { dialog, _ ->
//                lifecycleScope.launch {
//                    imprimirDetalleVenta(ventaId)
//                }
//                dialog.dismiss()
//            }
            .create()

        // Crear un adaptador para los detalles de pesos
        val detalleAdapter = DetallesTempPesosDialogAdapter(detallesPesoPollos)
        recyclerViewDetalles.adapter = detalleAdapter

        // Mostramos el diálogo
        currentDialog?.show()

        // Logs adicionales para depuración
        Log.d("Dialog", "Dialog shown successfully")
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
                filtrarPesoPorClienteYFecha(cliente)
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

    private var isDatePickerShowing = false

    private fun setupDateFilters() {
        val startDateInput = binding.startDateFilterInput
        val endDateInput = binding.endDateFilterInput
        showDatePickerDialog(startDateInput)
        showDatePickerDialog(endDateInput)

        startDateInput.setOnClickListener {
            if (!isDatePickerShowing) {
                showDatePickerDialog(startDateInput, true)
            }
        }

        endDateInput.setOnClickListener {
            if (!isDatePickerShowing) {
                showDatePickerDialog(endDateInput, true)
            }
        }
    }

    private fun showDatePickerDialog(editText: TextInputEditText, isClick: Boolean = false) {
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
                isDatePickerShowing = false
            }, year, month, day)

        datePickerDialog.setOnDismissListener {
            isDatePickerShowing = false
        }

        // Prellenar el campo con la fecha actual si está vacío
        if (editText.text.isNullOrEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            editText.setText(dateFormat.format(calendar.time))
        }

        // Mostrar el DatePickerDialog
        if (isClick && !isDatePickerShowing) {
            isDatePickerShowing = true
            datePickerDialog.show()
        }
    }

    private fun filtrarPesoPorClienteYFecha(cliente: ClienteEntity?) {
        val startDate = binding.startDateFilterInput.text.toString()
        val endDate = binding.endDateFilterInput.text.toString()

        lifecycleScope.launch {
            listaPesos = withContext(Dispatchers.IO) {
                if (cliente != null) {
                    db.getTemPesosByClienteAndDate(
                        cliente.numeroDocCliente,
                        startDate,
                        endDate
                    )
                } else {
                    db.getTempPesoByDate(startDate, endDate)
                }
            }
            pesosAdapter.setPesos(listaPesos)
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
        filtrarPesoPorClienteYFecha(selectedClient)
    }

    /*
        Procesar datos JSON a entidades
    */
    fun parseDataPesoJson(dataPesoJson: String): DataPesoPollosEntity {
        val dataPesoJsonObject = JSONObject(dataPesoJson)
        return DataPesoPollosEntity(
            id = dataPesoJsonObject.getInt("_PP_id"),
            serie = dataPesoJsonObject.getString("_PP_serie"),
            numero = dataPesoJsonObject.getString("_PP_numero"),
            fecha = dataPesoJsonObject.getString("_PP_fecha"),
            totalJabas = dataPesoJsonObject.getString("_PP_totalJabas"),
            totalPollos = dataPesoJsonObject.getString("_PP_totalPollos"),
            totalPeso = dataPesoJsonObject.getString("_PP_totalPeso"),
            tipo = dataPesoJsonObject.getString("_PP_tipo"),
            numeroDocCliente = dataPesoJsonObject.getString("_PP_docCliente"),
            nombreCompleto = dataPesoJsonObject.getString("_PP_nombreCompleto"),
            idGalpon = dataPesoJsonObject.getString("_PP_IdGalpon"),
            idNucleo = dataPesoJsonObject.getString("_PP_idNucleo"),
            PKPollo = dataPesoJsonObject.getString("_PP_PKPollo"),
            totalPesoJabas = dataPesoJsonObject.getString("_PP_totalPesoJabas"),
            totalNeto = dataPesoJsonObject.getString("_PP_totalNeto"),
            TotalPagar = dataPesoJsonObject.getString("_PP_TotalPagar"),
            idUsuario = dataPesoJsonObject.getString("_PP_idUsuario"),
            idEstado = dataPesoJsonObject.getString("_PP_idEstado")
        )
    }

    fun parseDataDetaPesoJson(dataDetaPesoJson: String): List<DataDetaPesoPollosEntity> {
        val dataDetaPesoArray = JSONArray(dataDetaPesoJson)
        val dataDetaPesoList = mutableListOf<DataDetaPesoPollosEntity>()
        for (i in 0 until dataDetaPesoArray.length()) {
            val detalleJsonObject = dataDetaPesoArray.getJSONObject(i)
            val dataDetaPesoObjeto = DataDetaPesoPollosEntity(
                idDetaPP = detalleJsonObject.getInt("_DPP_id"),
                cantJabas = detalleJsonObject.getInt("_DPP_cantJabas"),
                cantPollos = detalleJsonObject.getInt("_DPP_cantPolllos"),
                peso = detalleJsonObject.getDouble("_DPP_peso"),
                tipo = detalleJsonObject.getString("_DPP_tipo"),
                idPesoPollo = detalleJsonObject.getString("_DPP_idPesoPollo"),
                fechaPeso = detalleJsonObject.getString("_DPP_fechaPeso")
            )
            dataDetaPesoList.add(dataDetaPesoObjeto)
        }
        return dataDetaPesoList
    }

    inner class DetallesTempPesosDialogAdapter(
        private val detalles: List<DataDetaPesoPollosEntity>
    ) : RecyclerView.Adapter<DetallesTempPesosDialogAdapter.DetalleViewHolder>() {

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

