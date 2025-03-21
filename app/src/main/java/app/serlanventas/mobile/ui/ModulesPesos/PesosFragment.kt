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
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentModulePesosBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost.obtenerPesosServer
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import app.serlanventas.mobile.ui.ModuleVentas.VentasFragment.ClienteAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
