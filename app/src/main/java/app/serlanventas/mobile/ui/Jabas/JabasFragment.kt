package app.serlanventas.mobile.ui.Jabas

import NetworkUtils
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.ConnectivityManager
import android.os.Build
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentPesosBinding
import app.serlanventas.mobile.ui.BluetoothView.BluetoothFragment
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.pesoUsedEntity
import app.serlanventas.mobile.ui.Interfaces.OnItemClickListener
import app.serlanventas.mobile.ui.Jabas.ManagerPost.addListPesos
import app.serlanventas.mobile.ui.Jabas.ManagerPost.getNucleos
import app.serlanventas.mobile.ui.Jabas.ManagerPost.getSelectGalpon
import app.serlanventas.mobile.ui.Jabas.ManagerPost.setStatusUsed
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import app.serlanventas.mobile.ui.Jabas.ManagerPost.updateListPesos
import app.serlanventas.mobile.ui.Services.Logger
import app.serlanventas.mobile.ui.Services.PreLoading
import app.serlanventas.mobile.ui.Services.getAddressMacDivice.getDeviceId
import app.serlanventas.mobile.ui.Utilidades.NetworkChangeReceiver
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel
import app.serlanventas.mobile.ui.ViewModel.TabViewModel
import app.serlanventas.mobile.ui.slideshow.BluetoothConnectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

//JabasFragment.kt // INTERACTURA CON LA UI
class JabasFragment : Fragment(), OnItemClickListener {

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothConnectionService: BluetoothConnectionService? = null
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 3000L
    private var checkSendData: Boolean = false
    private var navegationTrue: Boolean = false
    private var idGalpoListaPesos = ""
    private var pesosList: List<PesosEntity> = emptyList()
    private var detallesList: List<DataDetaPesoPollosEntity> = emptyList()
    private var galponesList: List<GalponEntity> = listOf()
    private var galponIdMap = mutableMapOf<Int, String>()
    private var idPesoShared: Int = 0
    private var dataPesoPollosJson: String? = ""
    private var dataDetaPesoPollosJson: String? = ""
    private var connectedDeviceAddress: String? = null
    private var currentToast: Toast? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var dialogFragment: DialogFragment

    private lateinit var logger: Logger

    private val checkInternetRunnable = object : Runnable {
        override fun run() {
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                // Sincronizar data
//                sincronizarData()

                checkAndSendLocalData()
            }else{
                if (!NetworkUtils.isNetworkAvailable(requireContext())){
                    // Vuelve a ejecutar después del intervalo definido
                    handler.postDelayed(this, checkInterval)
                }
            }
        }
    }

    private lateinit var jabasAdapter: JabasAdapter
    private val jabasList = mutableListOf<JabasItem>()
    private lateinit var dialog: AlertDialog
    private lateinit var botonGuardar: Button
    private lateinit var botonCancelar: Button
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sharedTabViewModel: TabViewModel
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var _binding: FragmentPesosBinding? = null

    private val binding get() = _binding!!

    private fun checkAndSendLocalData() {
        val db = AppDatabase(requireContext())

        CoroutineScope(Dispatchers.IO).launch {
            val dataDetaPesoPollos = db.getAllDataDetaPesoPollos()
            val dataPesoPollos = db.getAllDataPesoPollos()

            if (dataDetaPesoPollos.isNotEmpty() && dataPesoPollos.isNotEmpty()){
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    withContext(Dispatchers.Main) {
                        showNoInternetDialog()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showSendDataConfirmationDialog(dataDetaPesoPollos, dataPesoPollos)
                    }
                }
            }
        }
    }
    @SuppressLint("SetTextI18n", "DefaultLocale", "ResourceType", "ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesosBinding.inflate(inflater, container, false)
        val root: View = binding.root
        progressBar = binding.progressBar.findViewById(R.id.progressBar)

        logger = Logger(requireContext())

        sharedTabViewModel = ViewModelProvider(requireActivity()).get(TabViewModel::class.java)

        val db = AppDatabase(requireContext())

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothConnectionService = BluetoothConnectionService(requireContext(),
            bluetoothAdapter,
            onMessageReceived = { message ->

            }
        )
        sharedViewModel.pesoValue.observe(viewLifecycleOwner) { peso ->
            val pesoFormatted = String.format("%.2f", peso.toDoubleOrNull() ?: 0.0)
            binding.inputPesoKg.setText(pesoFormatted)
            logger.log2("PesoValue: Pesos Foramteado recibido en JabaFragment -> $pesoFormatted")
        }
        showLoading()

        connectedDeviceAddress = sharedViewModel.getConnectedDeviceAddress()

        if (!connectedDeviceAddress.isNullOrBlank()){
            sharedViewModel.connectedDeviceName.observe(viewLifecycleOwner) { deviceName ->
                if (!deviceName.isNullOrEmpty()) {
                    binding.deviceConnected.text = "CONECTADO A: $deviceName"
                    binding.deviceConnected.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_active)
                } else {
                    binding.deviceConnected.text = "NO CONECTADO"
                    binding.deviceConnected.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive)
                }
            }
        }else{
            binding.deviceConnected.text = "NO CONECTADO"
            binding.deviceConnected.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive)

        }

        // Regreso de datos de la preliminar
        dataDetaPesoPollosJson = sharedViewModel.getDataDetaPesoPollosJson()
        dataPesoPollosJson = sharedViewModel.getDataPesoPollosJson()
        idPesoShared = sharedViewModel.getIdListPesos()?: 0


        jabasAdapter = JabasAdapter(jabasList, this, sharedViewModel)
        binding.recyclerViewJabas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = jabasAdapter
        }


        val pesoUsedExisted = db.getPesosUsedAll()
        if (!pesoUsedExisted.isNullOrEmpty()){
            val peso = pesoUsedExisted.first() // Asumiendo que quieres el primer registro
            idPesoShared = peso.idPesoUsed
            sharedViewModel.setIdListPesos(idPesoShared)

            dataPesoPollosJson = peso.dataPesoPollosJson
            dataDetaPesoPollosJson = peso.dataDetaPesoPollosJson
            sharedViewModel.setDataPesoPollosJson(dataPesoPollosJson.toString())
            sharedViewModel.setDataDetaPesoPollosJson(dataDetaPesoPollosJson.toString())
            db.deleteAllPesoUsed()
        }
        if (!dataDetaPesoPollosJson.isNullOrEmpty()) {
            val dataDetaPesoPollos = JSONArray(dataDetaPesoPollosJson)
            detallesList = procesarDataDetaPesoPollos(dataDetaPesoPollos)
            Log.d("JabasFragment detallesList","${detallesList}")

            val dataPesoPollos = JSONObject(dataPesoPollosJson.toString())
            distribuirDatosEnInputs(dataPesoPollos)
            detallesList.forEach { detalle ->
                val newItem = JabasItem(
                    id = detalle.idDetaPP,
                    numeroJabas = detalle.cantJabas,
                    numeroPollos = detalle.cantPollos,
                    pesoKg = detalle.peso,
                    conPollos = detalle.tipo
                )

                jabasAdapter.addItem(newItem)
            }
        }
        // Segunda condición: Procesamiento si dataPesoPollosJson no está vacío
        if (!dataPesoPollosJson.isNullOrEmpty()) {
            val dataPesoPollos = JSONObject(dataPesoPollosJson)
            distribuirDatosEnInputs(dataPesoPollos)
        }




        // Obtener los nombres de los nucleos y llenar el spinner
        getNucleos { nucleos ->
            nucleos?.let {
                showLoading()
                // Preparar nombres de los nucleos con elemento por defecto
                val nombresNucleos = mutableListOf("Seleccione Establecimiento")
                nombresNucleos.addAll(it.map { nucleo -> nucleo.nombre })

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nombresNucleos)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.selectEstablecimiento.adapter = adapter

                var nucleoIdSeleccionado = ""
                var galponIdSeleccionado = ""
                if (!dataPesoPollosJson.isNullOrEmpty()) {
                    val dataPesoPollos = JSONObject(dataPesoPollosJson)
                    nucleoIdSeleccionado = dataPesoPollos.optString("_PP_idNucleo")
                    galponIdSeleccionado = dataPesoPollos.optString("_PP_IdGalpon")
                }

                // Función para cargar y seleccionar galpones
                fun cargarYSeleccionarGalpones(idNucleo: String, galponIdToSelect: String?) {
                    Log.d("JabasFragment", "Iniciando carga de galpones para nucleo: $idNucleo")
                    Log.d("JabasFragment", "GalponIdToSelect: $galponIdToSelect")

                    getSelectGalpon(idNucleo) { galpones ->
                        galpones?.let {
                            galponesList = it
                            Log.d("JabasFragment", "Galpones cargados: ${it.map { galpon -> "${galpon.idGalpon}: ${galpon.nombre}" }}")

                            if (it.isNotEmpty()) {
                                val nombresGalpones = mutableListOf("Seleccione Galpon")
                                galponIdMap = mutableMapOf(0 to "Seleccione Galpon")

                                // Añadir los galpones manteniendo el ID como clave
                                it.forEach { galpon ->
                                    nombresGalpones.add(galpon.nombre)
                                    galponIdMap[galpon.idGalpon] = galpon.nombre
                                }

                                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nombresGalpones)
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.selectGalpon.adapter = adapter

                                if (galponIdToSelect != null) {
                                    Log.d("JabasFragment", "Intentando seleccionar galpon con ID: $galponIdToSelect")

                                    val index = nombresGalpones.indexOf(galponIdMap[galponIdToSelect.toInt()])
                                    if (index != -1) {
                                        binding.selectGalpon.setSelection(index)
                                        Log.d("JabasFragment", "Galpon seleccionado en posición: $index")
                                    } else {
                                        Log.w("JabasFragment", "galponIdToSelect no encontrado: $galponIdToSelect")
                                        binding.selectGalpon.setSelection(0)
                                    }
                                } else {
                                    Log.d("JabasFragment", "No hay galponIdToSelect. Seleccionando 'Seleccione Galpon'.")
                                    binding.selectGalpon.setSelection(0)
                                }
                                idGalpoListaPesos = ""
                            } else {
                                Log.w("SelectGalpon", "La lista de galpones está vacía para el nucleo $idNucleo")
                                val emptyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Seleccione Galpon"))
                                emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.selectGalpon.adapter = emptyAdapter

                            }
                            fetchData(2000)
                        } ?: run {
                            Log.e("SelectGalpon", "Error al obtener los galpones desde el servidor para el nucleo $idNucleo")
                            fetchData(2000)
                        }
                    }
                }

                // Seleccionar nucleo inicial y cargar galpones correspondientes
                if (nucleoIdSeleccionado.isNotEmpty()) {
                    val position = it.indexOfFirst { nucleo -> nucleo.idEstablecimiento == nucleoIdSeleccionado }
                    if (position != -1) {
                        binding.selectEstablecimiento.setSelection(position + 1)
                        cargarYSeleccionarGalpones(nucleoIdSeleccionado, galponIdSeleccionado)
                    } else {
                        binding.selectEstablecimiento.setSelection(0)
                    }
                } else {
                    binding.selectEstablecimiento.setSelection(0)
                }

                // Listener para cambios en la selección de nucleo
                binding.selectEstablecimiento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        updateSpinnerPesosIdGalpon(0, 0)
                        if (position == 0) {
                            if (jabasList.isEmpty()){
                                if (idPesoShared != 0) {
                                    limpiarCampos()
                                }else if (dataPesoPollosJson.isNullOrBlank()){
                                    limpiarCampos()
                                }
                            }else if (idPesoShared != 0){
                                if (dataPesoPollosJson.isNullOrBlank()){
                                    limpiarCampos()
                                }
                            }
                            val emptyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Seleccione Galpon"))
                            emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            binding.selectGalpon.adapter = emptyAdapter
                        } else {
                            val idNucleoSeleccionado = it[position - 1].idEstablecimiento
                            // Si es la selección inicial, usar galponIdSeleccionado, si no, usar ""
                            val galponIdToSelect = if (idNucleoSeleccionado == nucleoIdSeleccionado) galponIdSeleccionado else ""
                            if (idGalpoListaPesos.isNullOrBlank()) {
                                cargarYSeleccionarGalpones(idNucleoSeleccionado, galponIdToSelect) // Sin retraso para selecciones manuales
                            }else{
                                cargarYSeleccionarGalpones(idNucleoSeleccionado, idGalpoListaPesos)
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Manejar caso cuando no se selecciona nada
                    }
                }
                fetchData(2000)
            } ?: run {
                Log.e("JabasFragment", "Error al obtener los nucleos desde el servidor.")
                fetchData(2000)

            }
        }

        binding.selectGalpon.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        //
                    }
                    else ->{
                        val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                        val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
                        val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                        if (jabasList.isEmpty()){
                            if (idPesoShared != 0) {
                                limpiarCampos()
                            }else if (dataPesoPollosJson.isNullOrBlank()){
                                limpiarCampos()
                            }
                        }else if (idPesoShared != 0){
                            if (dataPesoPollosJson.isNullOrBlank()){
                                limpiarCampos()
                            }
                        }
                        updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Opcional: maneja el caso cuando no se selecciona nada
            }
        }

        binding.botonReloadPeso.setOnClickListener{
            val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
            val idNucleo = binding.selectEstablecimiento.selectedItemPosition
            val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0

            updateSpinnerPesosIdGalpon(idNucleo, idGalpon)

            // Cancelar el Toast actual si está visible
            currentToast?.cancel()

            // Mostrar un nuevo Toast
            currentToast = Toast.makeText(requireContext(), "Se actualizó la lista de pesos", Toast.LENGTH_SHORT)
            currentToast?.show()
        }
        binding.selectListpesos.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                    val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                    val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
                    updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                }
            }
            false
        }

        binding.selectListpesos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d("JabasFragment", "Posición seleccionada en selectListpesos: $position")
                when (position) {
                    0 -> {
                        // "Seleccione Pesos" seleccionado
                        Log.d("JabasFragment", "Opción 'Seleccione Pesos' seleccionada")
                        // Limpiar los campos o realizar alguna acción por defecto
                        if (jabasList.isEmpty()){
                            if (idPesoShared != 0) {
                                limpiarCampos()
                            }else if (dataPesoPollosJson.isNullOrBlank()){
                                limpiarCampos()
                            }
                        }else if (idPesoShared != 0){
                            if (dataPesoPollosJson.isNullOrBlank()){
                                limpiarCampos()
                            }
                        }
                        sharedViewModel.setIdListPesos(0)
                    }
                    else -> {
                        if (binding.idListPeso.text.isNullOrBlank()){
                            showLoading()
                        }
                        if (pesosList.isNotEmpty()) {
                            val selectedPesos = pesosList[position - 1]
                            Log.d("JabasFragment", "Peso seleccionado: ${selectedPesos.numeroDocCliente}/${selectedPesos.nombreCompleto}")

                            var idPeso = selectedPesos.id
                            if (idPesoShared != 0){
                                if (idPeso != idPesoShared){
                                    updatePesoStatus(idPesoShared, "NotUsed")
                                }
                            }

                            if (idPeso != 0) {
                                ManagerPost.getStautusPeso(
                                    requireContext(),
                                    idPeso
                                ) { result ->
                                    if (result != null) {
                                        val parts = result.split("|")
                                        val status = parts.getOrNull(0) ?: "0"
                                        val addresMac = parts.getOrNull(1) ?: ""

                                        var diviceName = getDeviceId(requireContext())
                                        when {
                                            status == "1" && addresMac == diviceName -> {
                                                // Proceder con las acciones existentes
                                                binding.idListPeso.text = idPeso.toString()
                                                idPesoShared = idPeso
                                                sharedViewModel.setIdListPesos(idPeso)

                                                updatePesoStatus(idPeso, "Used")

                                                // Resto del código para procesar datos (sin cambios)
                                                val dataPesoPollos = JSONObject(selectedPesos.dataPesoJson)
                                                val dataDetaPesoPollos = JSONArray(selectedPesos.dataDetaPesoJson)

                                                val idNucleo = dataPesoPollos.optString("_PP_idNucleo", "0")
                                                val idGalpon = dataPesoPollos.optString("_PP_IdGalpon", "0")

                                                idGalpoListaPesos = idGalpon
                                                Log.d("JabasFragment", "ID Núcleo en JSON: $idNucleo, ID Galpón en JSON: $idGalpon")

                                                // Seleccionar el núcleo
                                                val nucleos = (binding.selectEstablecimiento.adapter as? ArrayAdapter<*>)?.let { adapter ->
                                                    (0 until adapter.count).map { adapter.getItem(it) }
                                                } ?: listOf()
                                                val nucleoPosition = nucleos.indexOfFirst { it.toString().contains(idNucleo) }
                                                val positionToSelect = if (nucleoPosition != -1) nucleoPosition else 0
                                                binding.selectEstablecimiento.setSelection(positionToSelect)

                                                actualizarSpinnerGalpon(idGalpon)

                                                // Resto del código para procesar datos (sin cambios)
//                                                if (dataDetaPesoPollosJson.isNullOrBlank()){
                                                    limpiarTablaJabas()
                                                    detallesList = procesarDataDetaPesoPollos(dataDetaPesoPollos)
                                                    distribuirDatosEnInputs(dataPesoPollos)

                                                    detallesList.forEach { detalle ->
                                                        val newItem = JabasItem(
                                                            id = detalle.idDetaPP,
                                                            numeroJabas = detalle.cantJabas,
                                                            numeroPollos = detalle.cantPollos,
                                                            pesoKg = detalle.peso,
                                                            conPollos = detalle.tipo
                                                        )

                                                        jabasAdapter.addItem(newItem)
                                                    }
//                                                }

                                                binding.botonDeletePeso.visibility = View.VISIBLE
                                                binding.botonGuardarPeso.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.yellow)
                                                binding.botonGuardarPeso.setImageResource(R.drawable.baseline_backup_24)
                                            }
                                            status == "1" && addresMac != diviceName -> {
                                                showCustomToast(requireContext(), "Este peso está siendo usado por otro dispositivo", "info")
                                                limpiarCampos()
                                            }
                                            status == "0" && addresMac == "null" ->{
                                                // Proceder con las acciones existentes
                                                binding.idListPeso.text = idPeso.toString()
                                                idPesoShared = idPeso
                                                sharedViewModel.setIdListPesos(idPeso)

                                                updatePesoStatus(idPeso, "Used")

                                                // Resto del código para procesar datos (sin cambios)
                                                val dataPesoPollos = JSONObject(selectedPesos.dataPesoJson)
                                                val dataDetaPesoPollos = JSONArray(selectedPesos.dataDetaPesoJson)

                                                val idNucleo = dataPesoPollos.optString("_PP_idNucleo", "0")
                                                val idGalpon = dataPesoPollos.optString("_PP_IdGalpon", "0")
                                                idGalpoListaPesos = idGalpon
                                                Log.d("JabasFragment", "ID Núcleo en JSON: $idNucleo, ID Galpón en JSON: $idGalpon")

                                                // Seleccionar el núcleo
                                                val nucleos = (binding.selectEstablecimiento.adapter as? ArrayAdapter<*>)?.let { adapter ->
                                                    (0 until adapter.count).map { adapter.getItem(it) }
                                                } ?: listOf()
                                                val nucleoPosition = nucleos.indexOfFirst { it.toString().contains(idNucleo) }
                                                val positionToSelect = if (nucleoPosition != -1) nucleoPosition else 0
                                                binding.selectEstablecimiento.setSelection(positionToSelect)

                                                actualizarSpinnerGalpon(idGalpon)

                                                  // Resto del código para procesar datos (sin cambios)
//                                                if (dataDetaPesoPollosJson.isNullOrBlank()){
                                                limpiarTablaJabas()
                                                detallesList = procesarDataDetaPesoPollos(dataDetaPesoPollos)
                                                distribuirDatosEnInputs(dataPesoPollos)

                                                detallesList.forEach { detalle ->
                                                    val newItem = JabasItem(
                                                        id = detalle.idDetaPP,
                                                        numeroJabas = detalle.cantJabas,
                                                        numeroPollos = detalle.cantPollos,
                                                        pesoKg = detalle.peso,
                                                        conPollos = detalle.tipo
                                                    )

                                                    jabasAdapter.addItem(newItem)
                                                }
//                                                }

                                                binding.botonDeletePeso.visibility = View.VISIBLE
                                                binding.botonGuardarPeso.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.yellow)
                                                binding.botonGuardarPeso.setImageResource(R.drawable.baseline_backup_24)
                                            }
                                            else -> {
                                                showCustomToast(requireContext(), "Error: Este peso está siendo usado por otro dispositivo", "error")
                                                limpiarCampos()
                                                fetchData(2000)
                                            }
                                        }

                                    } else {
                                        showCustomToast(requireContext(), "Error al obtener el estado del peso", "error")
                                        fetchData(2000)
                                    }
                                }
                            }
                        fetchData(2000)
                        } else {
                            Log.d("JabasFragment", "La lista de pesos está vacía")
                            binding.botonDeletePeso.visibility = View.INVISIBLE
                            binding.botonGuardarPeso.backgroundTintList  = ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
                            binding.botonGuardarPeso.setImageResource(R.drawable.baseline_add_24)
                            fetchData(2000)
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("JabasFragment", "Nada seleccionado en selectListpesos")
            }
        }

        binding.botonDeletePeso.setOnClickListener {
            val idPeso = sharedViewModel.getIdListPesos()

            if (idPeso != null) {
                // Mostrar un cuadro de diálogo de confirmación
                AlertDialog.Builder(requireContext())
                    .setMessage("¿Estás seguro de que deseas eliminar este peso?")
                    .setPositiveButton("Sí") { dialog, which ->
                        // Si el usuario confirma, proceder con la eliminación
                        deletePeso(idPeso)
                    }
                    .setNegativeButton("No") { dialog, which ->
                        // Si el usuario cancela, cerrar el cuadro de diálogo
                        dialog.dismiss()
                    }
                    .show()
            } else {
                // Manejar el caso donde el ID no es válido
                Toast.makeText(requireContext(), "ID no válido", Toast.LENGTH_SHORT).show()
            }
        }

        binding.deviceConnected.setOnClickListener {
            if (connectedDeviceAddress.isNullOrBlank() || connectedDeviceAddress == "null") {
                // Muestra un fragmento modal si connectedDeviceAddress es null
                showModalBluetoothFragment()
            } else {
                // Muestra el diálogo de confirmación si connectedDeviceAddress no es null
                showDisconnectDialog()
            }
        }

        binding.botonGuardarPeso.setOnClickListener{
            CoroutineScope(Dispatchers.Main).launch {
                showLoading()
                val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
                var numDoc = binding.textDocCli.text.toString()
                var nombres = binding.textNomCli.text.toString()

                if (!jabasList.isNotEmpty()) {
                    showCustomToast(requireContext(),"¡La tabla esta vacía, por favor registre datos antes de enviar!","info")
                    fetchData(2000)
                } else if (numDoc.isNullOrBlank() && nombres.isNullOrBlank()){
                    showCustomToast(requireContext(),"¡Ingrese un Cliente","info")
                    fetchData(2000)
                } else if (binding.PrecioKilo.text.isNullOrBlank()){
                    showCustomToast(requireContext(),"¡Ingrese un precio","info")
                    binding.PrecioKilo.error = "Ingrese un precio"
                    fetchData(2000)
                } else if (idNucleo == 0){
                    showCustomToast(requireContext(), "¡Seleccione un nucleo!", "info")
                    fetchData(2000)
                } else if (idGalpon == 0){
                    showCustomToast(requireContext(), "¡Seleccione un galpón!", "info")
                    fetchData(2000)
                } else {
                    val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
                    val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                        id = 1,
                        serie = "",
                        fecha = "",
                        totalJabas = "",
                        totalPollos = "",
                        totalPeso = "",
                        tipo = "",
                        numeroDocCliente = binding.textDocCli.text.toString(),
                        nombreCompleto = binding.textNomCli.text.toString(),
                        idGalpon = idGalpon.toString(),
                        idNucleo = idNucleo.toString(),
                        PKPollo = binding.PrecioKilo.text.toString(),
                        totalPesoJabas = "",
                        totalNeto = "",
                        totalPagar = ""
                    )

                    dataPesoPollosJson = dataPesoPollos.toJson().toString()
                    dataDetaPesoPollosJson = JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()

                    val pesosEntity = PesosEntity(
                        id = 0,
                        idNucleo = idNucleo,
                        idGalpon = idGalpon.toInt(),
                        numeroDocCliente = dataPesoPollos.numeroDocCliente,
                        nombreCompleto = dataPesoPollos.nombreCompleto,
                        dataPesoJson = dataPesoPollosJson!!,
                        dataDetaPesoJson = dataDetaPesoPollosJson!!,
                        fechaRegistro = ""
                    )
                    if (idPesoShared == 0){
                        val success = addListPesos(requireContext(), this@JabasFragment, pesosEntity)
                        if (success) {
                            limpiarCampos()
                            updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                        }
                    }else{
                        val success = updateListPesos(requireContext(), this@JabasFragment, pesosEntity, idPesoShared)
                        if (success) {
                            limpiarCampos()
                            sharedViewModel.setContadorJabas(0)
                            _binding?.contadorJabas?.text = "= 0"
                            updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                        }
                    }
                    fetchData(2000)
                }
            }
        }



        binding.inputNumeroJabas.setText("10")
        binding.inputCantPollos.setText("0")
        binding.inputNumeroJabas.isEnabled = true
        binding.inputCantPollos.isEnabled = false
        binding.inputPesoKg.isEnabled = false

        if (!dataDetaPesoPollosJson.isNullOrBlank()){
            sharedViewModel.setBtnTrue()
            binding.checkboxConPollos.isChecked = false
            binding.botonGuardar.isEnabled = true
        }else{
            sharedViewModel.setBtnFalse()
            binding.checkboxConPollos.isChecked = false
            binding.botonGuardar.isEnabled = true
        }
        var valor = sharedViewModel.getbtnStatus()

        if (valor == false){
            binding.botonEnviar.backgroundTintList  = ContextCompat.getColorStateList(requireContext(), R.color.gray)
            binding.botonEnviar.setImageResource(R.drawable.null_24)
        }else{
            binding.botonEnviar.backgroundTintList  = ContextCompat.getColorStateList(requireContext(), R.color.your_greed)
            binding.botonEnviar.setImageResource(R.drawable.baseline_content_paste_go_24)
            binding.botonGuardar.isEnabled = true
            binding.botonGuardar.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.teal_200)
        }

        binding.botonEnviar.isEnabled = valor


        binding.checkboxConPollos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                var nJabas = sharedViewModel.getContadorJabas()
                if (nJabas == 0){
                    binding.botonGuardar.isEnabled = false
                    binding.botonGuardar.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray)
                    showCustomToast(requireContext(), "No te quedan Jabas para usar", "warning")
                } else if (nJabas!! < 0){
                    binding.botonGuardar.isEnabled = false
                    binding.botonGuardar.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray)
                    showCustomToast(requireContext(), "Te faltan ${nJabas} Jabas por registrar", "error")
                }else{
                    binding.botonGuardar.isEnabled = true
                    binding.botonGuardar.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.teal_200)
                }

                binding.inputNumeroJabas.setText("4")
                binding.inputNumeroJabas.isEnabled = true
                binding.inputCantPollos.isEnabled = true
                binding.inputCantPollos.setText("")
            } else {
                binding.botonGuardar.isEnabled = true
                binding.botonGuardar.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.teal_200)
                binding.inputNumeroJabas.setText("10")
                binding.inputCantPollos.isEnabled = false
                binding.inputCantPollos.setText("0")
            }
        }

        // esto esta en el JabasFragment.kt
        binding.botonGuardar.setOnClickListener {
            val numeroJabas = binding.inputNumeroJabas.text.toString()
            val numeroPollos = binding.inputCantPollos.text.toString()
            val pesoKg = binding.inputPesoKg.text.toString()
            val conPollos = if (binding.checkboxConPollos.isChecked) "JABAS CON POLLOS" else "JABAS SIN POLLOS"

            var isValid = true

            // Validación de número de jabas
            if (numeroJabas.isBlank() || numeroJabas.toInt() == 0) {
                binding.inputNumeroJabas.error = "Ingrese el número de jabas"
                isValid = false
            } else {
                binding.inputNumeroJabas.error = null // Limpiar error si está presente
            }

            // Validación de peso en kg
            if (pesoKg.isBlank() || pesoKg.toDouble() == 0.0) {
                binding.inputPesoKg.error = "Ingrese el peso en kg"
                isValid = false
            } else {
                binding.inputPesoKg.error = null // Limpiar error si está presente
            }

            // Validación de número de pollos si el checkbox está marcado
            if (binding.checkboxConPollos.isChecked) {
                if (numeroPollos.isBlank() || numeroPollos.toInt() == 0) {
                    binding.inputCantPollos.error = "Ingrese la cantidad de pollos"
                    isValid = false
                } else {
                    binding.inputCantPollos.error = null // Limpiar error si está presente
                }
            }

            // Si todos los campos son válidos, agregar el nuevo item
            if (isValid) {
                val newItem = JabasItem(
                    id = jabasList.size + 1,
                    numeroJabas = numeroJabas.toInt(),
                    numeroPollos = if (binding.checkboxConPollos.isChecked) numeroPollos.toInt() else 0,
                    pesoKg = pesoKg.toDouble(),
                    conPollos = conPollos
                )
                jabasAdapter.addItem(newItem)

                // Limpiar los campos de entrada después de agregar el item
                binding.inputPesoKg.text?.clear()
            }
        }

        binding.botonLimpiar.setOnClickListener {
            mostrarConfirmacionLimpiar()
        }

        binding.botonCliente.setOnClickListener {
            // Formulario Cliente
            modalCliente()
        }

        binding.botonEnviar.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                showLoading()

                val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
                val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
                val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                    id = 1,
                    serie = "",
                    fecha = "",
                    totalJabas = "",
                    totalPollos = "",
                    totalPeso = "",
                    tipo = "",
                    numeroDocCliente = binding.textDocCli.text.toString(),
                    nombreCompleto = binding.textNomCli.text.toString(),
                    idGalpon = idGalpon.toString(),
                    idNucleo = idNucleo.toString(),
                    PKPollo = binding.PrecioKilo.text.toString(),
                    totalPesoJabas = "",
                    totalNeto = "",
                    totalPagar = ""
                )
                var idEstablecimiento = binding.selectEstablecimiento.selectedItemPosition
                var precio =  dataPesoPollos.PKPollo.toDoubleOrNull()
                if (idEstablecimiento == 0){
                    showCustomToast(requireContext(), "¡Seleccione un establecimiento!", "info")
                    fetchData(500)

                }else if (dataPesoPollos.idGalpon == "0" || dataPesoPollos.idGalpon.isBlank()){
                    showCustomToast(requireContext(), "¡Seleccione un galpón!", "info")
                    fetchData(500)

                }else if (dataPesoPollos.numeroDocCliente == "" && dataPesoPollos.nombreCompleto == ""){
                    showCustomToast(requireContext(), "¡Registre un cliente!", "info")
                    fetchData(500)

                }else if (dataDetaPesoPollos.isEmpty()) {
                    showCustomToast(requireContext(), "¡La tabla esta vacía, por favor registre datos antes de enviar!", "info")
                    fetchData(500)

                }else if (dataPesoPollos.PKPollo.isBlank()) {
                    binding.PrecioKilo.error = "¡Para calcular el Total a Pagar necesitamos saber el precio por kilo!"
                    fetchData(500)

                }else if (precio == null || dataPesoPollos.PKPollo.isBlank()) {
                    binding.PrecioKilo.error = "¡Para calcular el Total a Pagar necesitamos saber el precio por kilo!"
                    fetchData(500)
//                }else if (precio <= 0) {
//                    binding.PrecioKilo.error = "¡El precio por kilo no puede ser 0!"
//                    fetchData(500)
                }else{
                    Log.d("JabasFragment", "$dataDetaPesoPollos")
                    Log.d("JabasFragment", "$dataPesoPollos")

                    dataPesoPollosJson = dataPesoPollos.toJson().toString()
                    dataDetaPesoPollosJson = JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()

                    sharedViewModel.setDataPesoPollosJson(dataPesoPollosJson!!)
                    sharedViewModel.setDataDetaPesoPollosJson(dataDetaPesoPollosJson!!)

                    navegationTrue = true
                    findNavController().navigate(R.id.nav_initPreliminar)
//                    sharedTabViewModel.setNavigateToTab(2)
                }
            }
        }
        return root
    }

    private fun showDisconnectDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("¿Deseas desconectarte del dispositivo Bluetooth?")
            .setCancelable(false)
            .setPositiveButton("Desconectar") { dialog, id ->
                showLoading()
                val deviceAddress = connectedDeviceAddress
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                val disconnected = bluetoothConnectionService?.closeConnection(device) ?: false
                if (disconnected) {
                    connectedDeviceAddress = null
                    sharedViewModel.updateConnectedDeviceAddress("")
                    binding.deviceConnected.text = "NO CONECTADO"
                    binding.deviceConnected.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive)
//                    findNavController().navigate(R.id.nav_slideshow)
                    fetchData(1000)
                } else {
                    // Puedes mostrar un mensaje de error o hacer otra cosa si no se desconectó correctamente
                    Log.e("Bluetooth", "No se pudo desconectar correctamente")
                    fetchData(1000)
                }

            }
            .setNegativeButton("Cancelar") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun showModalBluetoothFragment() {
        dialogFragment = BluetoothFragment()
        dialogFragment.show(requireActivity().supportFragmentManager, "BluetoothFragment")
    }

    private fun actualizarSpinnerGalpon(idGalpon: String) {
        val galpones = (binding.selectGalpon.adapter as? ArrayAdapter<*>)?.let { adapter ->
            (0 until adapter.count).map { adapter.getItem(it).toString() }
        } ?: listOf()

        Log.d("idGalponSelect", "Galpones disponibles: $galpones")
        Log.d("idGalponSelect", "Buscando galpón con ID: $idGalpon")

        // Convertimos idGalpon a entero
        val galponIndex = idGalpon.toIntOrNull()

        // Buscar el índice correcto en el galponIdMap
        val positionToSelect = if (galponIndex != null) {
            galponIdMap[galponIndex]?.let { galponNombre ->
                galpones.indexOf(galponNombre)
            } ?: 0
        } else {
            0
        }

        Log.d("idGalponSelect", "Posición a seleccionar: $positionToSelect")

        binding.selectGalpon.setSelection(positionToSelect)
        Log.d("idGalponSelect", "Galpón seleccionado: posición $positionToSelect, idGalpon: $idGalpon")
    }

    private fun updateSpinnerPesosIdGalpon(idNucleo : Int, idGalpon: Int) {
        val idDevice = getDeviceId(requireContext())
        // Obtener los pesos guardados en la base de datos
        ManagerPost.getListPesosByIdGalpon(idGalpon, idNucleo, idDevice) { fetchedPesosList ->
            if (fetchedPesosList != null && fetchedPesosList.isNotEmpty()) {
                // Actualizar la lista global
                pesosList = fetchedPesosList

                // Crear items para el spinner
                val spinnerItems = mutableListOf("Seleccione Pesos")
                spinnerItems.addAll(pesosList.map { "${it.id}: ${it.numeroDocCliente}/${it.nombreCompleto}" })

                // Actualizar el spinner en el hilo principal
                CoroutineScope(Dispatchers.Main).launch {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        spinnerItems
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.selectListpesos.adapter = adapter

                    // Establecer el elemento seleccionado en el Spinner
                    val positionToSelect = if (idPesoShared > 0) {
                        spinnerItems.indexOfFirst { it.startsWith("$idPesoShared:") }
                    } else {
                        -1
                    }

                    // Establecer el elemento seleccionado en el Spinner
                    if (positionToSelect != -1) {
                        binding.selectListpesos.setSelection(positionToSelect)
                    } else {
                        // Si no se encuentra el ID, seleccionar "Seleccione Pesos"
                        binding.selectListpesos.setSelection(0)
                    }
                }
                } else {
                // Si no hay datos, actualizar la lista global a una lista vacía
                pesosList = emptyList()

                // Actualizar el spinner en el hilo principal
                CoroutineScope(Dispatchers.Main).launch {
                    val spinnerItems = listOf("Seleccione Pesos", "No hay datos guardados")
                    val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, spinnerItems) {
                        override fun isEnabled(position: Int): Boolean {
                            // Deshabilitar el segundo ítem (posición 1)
                            return position != 1
                        }

                        @SuppressLint("ResourceAsColor")
                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getDropDownView(position, convertView, parent)
                            val textView = view as TextView
                            // Deshabilitar el segundo ítem (posición 1)
                            if (position == 1) {
                                textView.setTextColor(R.color.gray) // Opcional: cambiar el color del texto para indicar que está deshabilitado
                            } else {
                                textView.setTextColor(R.color.black)
                            }
                            return view
                        }
                    }
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.selectListpesos.adapter = adapter

                    Log.d("SpinnerData", "No hay datos disponibles")
                }
            }
        }
    }

    private fun deletePeso(idPeso: Int) {
        val pesoToDelete = pesosList.find { it.id == idPeso }

        if (pesoToDelete != null) {
            ManagerPost.removeListPesosId(requireContext(), idPeso) { success ->
                if (success) {
                    // Eliminar el peso de la lista local
                    pesosList = emptyList()
                    // Actualizar el spinner
                    val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                    val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
                    val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                    limpiarCampos()
                    updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                } else {
                    Toast.makeText(requireContext(), "Error al eliminar el peso", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "No se encontró el registro con ID $idPeso", Toast.LENGTH_SHORT).show()
        }
    }



    private fun showNoInternetDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Datos locales guardados")
            .setMessage("Hay datos antiguos guardados localmente, pero no hay conexión a Internet para enviarlos. Se enviaran cuando se detecte una conexión a internet")
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSendDataConfirmationDialog(dataDetaPesoPollos: List<DataDetaPesoPollosEntity>, dataPesoPollos: List<DataPesoPollosEntity>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Enviar Datos")
            .setMessage("Se ha detectado una conexión a Internet. ¿Deseas enviar los datos almacenados?")
            .setPositiveButton("Enviar") { dialog, _ ->
                // El usuario aceptó enviar los datos
                CoroutineScope(Dispatchers.IO).launch {
                    ManagerPost.sendLocalDataToServer(requireContext(), this@JabasFragment, dataDetaPesoPollos, dataPesoPollos)
                    checkSendData = true
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // El usuario canceló el envío
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    fun limpiarCampos() {
        updatePesoStatus(idPesoShared, "NotUsed")
        idPesoShared = 0
        binding.selectListpesos.setSelection(0)
        binding.idListPeso.text = ""
        idGalpoListaPesos = ""
        binding.inputPesoKg.text?.clear()
        binding.textDocCli.text = ""
        binding.textNomCli.text = ""
        binding.checkboxConPollos.isChecked = false
        binding.botonEnviar.isEnabled = false
        binding.botonEnviar.backgroundTintList  = ContextCompat.getColorStateList(requireContext(), R.color.gray)
        binding.botonEnviar.setImageResource(R.drawable.null_24)
        binding.botonGuardar.isEnabled = true
        binding.botonGuardar.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.teal_200)

        sharedViewModel.setDataPesoPollosJson("")
        sharedViewModel.setDataDetaPesoPollosJson("")
        dataPesoPollosJson = ""
        dataDetaPesoPollosJson = ""

        // LIMPIAR LOS ERRORES DE INPUTS
        binding.inputNumeroJabas.error = null
        binding.inputCantPollos.error = null
        binding.inputPesoKg.error = null
        binding.textDocCli.error = null
        binding.textNomCli.error = null

        binding.botonDeletePeso.visibility = View.INVISIBLE
        binding.botonGuardarPeso.backgroundTintList  = ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
        binding.botonGuardarPeso.setImageResource(R.drawable.baseline_add_24)

        // Limpiar Tabla
        jabasList.clear()
        jabasAdapter.notifyDataSetChanged()
        sharedViewModel.setContadorJabas(0)
        _binding?.contadorJabas?.text = "= 0"
    }

    private fun limpiarTablaJabas(){
        // Limpiar Tabla
        sharedViewModel.setDataPesoPollosJson("")
        sharedViewModel.setDataDetaPesoPollosJson("")
        dataPesoPollosJson = ""
        dataDetaPesoPollosJson = ""
        jabasList.clear()
        jabasAdapter.notifyDataSetChanged()
        sharedViewModel.setContadorJabas(0)
        binding.contadorJabas.text = "= 0"
    }

    private fun mostrarConfirmacionLimpiar() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar limpieza")
        builder.setMessage("¿Está seguro de que desea limpiar los campos?")
        builder.setPositiveButton("Sí") { dialog, which ->
            // Aquí llama a la función para limpiar los campos
            limpiarCampos()
        }
        builder.setNegativeButton("Cancelar", null)
        val dialog = builder.create()
        dialog.show()
    }

    private fun modalCliente() {
        // Inflar el layout del modal
        val dialogView = layoutInflater.inflate(R.layout.modal_cliente, null)

        // Configurar el AlertDialog
        dialog = configurarDialogo(dialogView)

        // Obtener referencias a los elementos de entrada
        val inputNumeroCliente = dialogView.findViewById<EditText>(R.id.inputNumeroCliente)
        val inputNombreCliente = dialogView.findViewById<EditText>(R.id.inputNombreCliente)

        // Obtener referencia al botón Buscar
        val botonBuscar = dialogView.findViewById<Button>(R.id.botonBuscar)

        // Configurar el botón de búsqueda
        configurarBotonBuscar(botonBuscar, inputNumeroCliente, inputNombreCliente)

        // Obtener referencias a los botones Guardar y Cancelar
        botonGuardar = dialogView.findViewById(R.id.botonGuardar)
        botonCancelar = dialogView.findViewById(R.id.botonCancelar)
        botonGuardar.isEnabled = false
        // Obtener textos de los TextViews y verificar si están vacíos o nulos
        val numeroDocumentoTextView = binding.textDocCli.text?.toString()?.trim()
        val apellidoNombreTextView = binding.textNomCli.text?.toString()?.trim()

        // Auto completar campos si hay datos disponibles en los TextViews
        val numeroDocumento = numeroDocumentoTextView ?: ""
        val apellidoNombre = apellidoNombreTextView ?: ""

        // Verificar si hay datos disponibles para auto completar
        if (numeroDocumento.isNotBlank() && apellidoNombre.isNotBlank()) {
            inputNumeroCliente.setText(numeroDocumento)
            inputNombreCliente.setText(apellidoNombre)
        }

        // Configurar listeners de los botones
        configurarBotones(inputNumeroCliente, inputNombreCliente)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No se necesita implementar
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No se necesita implementar
            }
            override fun afterTextChanged(s: Editable?) {
                // Verificar si inputNumeroCliente tiene exactamente 8 o 11 dígitos
                val numeroClienteValido = inputNumeroCliente.text.toString().length == 8 ||
                        inputNumeroCliente.text.toString().length == 11

                // Verificar si inputNombreCliente no está vacío
                val nombreClienteValido = inputNombreCliente.text.toString().length >= 5

                // Habilitar o deshabilitar el botón Guardar en función de las condiciones
                botonGuardar.isEnabled = numeroClienteValido && nombreClienteValido
            }
        }
        inputNumeroCliente.addTextChangedListener(textWatcher)
        inputNombreCliente.addTextChangedListener(textWatcher)

        // Mostrar el AlertDialog
        dialog.show()

        // Personalización adicional para mantener el diálogo abierto al hacer clic fuera del modal
        dialog.setCanceledOnTouchOutside(false) // Evitar el cierre al hacer clic fuera del modal
    }


    private fun configurarDialogo(dialogView: View): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Datos del Cliente")
            .setView(dialogView)
            .setCancelable(false) // No se puede cerrar al hacer clic fuera del modal
            .create()
    }

    // Configurar el botón de búsqueda JabasFragment.kt
    private fun configurarBotonBuscar(botonBuscar: Button, inputNumeroCliente: EditText, dialogView: View) {
        botonBuscar.setOnClickListener {
            val preLoading = PreLoading(requireContext())
            preLoading.showPreCarga()

            val numeroCliente = inputNumeroCliente.text.toString()

            // Validar número de cliente
            if (numeroCliente.length < 8 || numeroCliente.length > 11 || !numeroCliente.matches("[0-9]+".toRegex())) {
                inputNumeroCliente.error = "Ingrese un número válido (8-11 dígitos)"
                preLoading.hidePreCarga()
                return@setOnClickListener
            }

            Log.d("JabasFragment", "Cliente a Buscar: $numeroCliente")

            // Verificar disponibilidad de internet
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                val jsonParam = JSONObject()
                jsonParam.put("numeroDocumento", numeroCliente)

                ManagerPost.BuscarCliente(jsonParam.toString()) { nombreCompleto ->
                    val inputNombreCliente = dialogView.findViewById<EditText>(R.id.inputNombreCliente)
                    inputNombreCliente?.setText(nombreCompleto ?: "")
                    preLoading.hidePreCarga()
                    if (nombreCompleto.isNullOrBlank()){
                        botonGuardar.isEnabled = false
                        showCustomToast(requireContext(), "No se encontró el cliente, Ingrese un nombre manualmente", "info")
                    }else{
                        botonGuardar.isEnabled = true
                    }
                }
            } else {
                val db = AppDatabase(requireContext())
                val cliente = db.getClienteById(numeroCliente)

                if (cliente != null) {
                    val inputNombreCliente = dialogView.findViewById<EditText>(R.id.inputNombreCliente)
                    inputNombreCliente.setText(cliente.nombreCompleto)
                    preLoading.hidePreCarga()
                    botonGuardar.isEnabled = true

                } else {
                    Toast.makeText(requireContext(), "Cliente no encontrado localmente", Toast.LENGTH_SHORT).show()
                    preLoading.hidePreCarga()
                }
            }
        }
    }

    private fun configurarBotones(inputNumeroCliente: EditText, inputNombreCliente: EditText) {
        // Configurar el botón "Guardar"
        botonGuardar.setOnClickListener {
            guardarCli(inputNumeroCliente, inputNombreCliente)
        }

        // Configurar el botón "Cancelar"
        botonCancelar.setOnClickListener {
            cancelarCli()
        }
    }

    private fun guardarCli(inputNumeroCliente: EditText, inputNombreCliente: EditText) {
        val numeroCliente = inputNumeroCliente.text.toString()
        val nombreCliente = inputNombreCliente.text.toString()


        // Validar que el documento sea RUC (11 dígitos) o DNI (8 dígitos)
        if (numeroCliente.length != 8 && numeroCliente.length != 11) {
            inputNumeroCliente.error = "El documento debe ser un DNI (8 dígitos) o RUC (11 dígitos)"
            return
        }

        // Validar que los campos no estén vacíos
        if (numeroCliente.isBlank()) {
            inputNumeroCliente.error = "Ingrese el número de cliente"
            return
        }
        if (nombreCliente.isBlank()) {
            inputNombreCliente.error = "Ingrese el nombre del cliente"
            return
        }
        if (!nombreCliente.matches("[A-ZÑÁÉÍÓÚÜ\\s.]+".toRegex())) {
            inputNombreCliente.error = "Ingrese solo letras"
            return
        }

        val db = AppDatabase(requireContext())

        val existingCliente = db.getClienteById(numeroCliente)
        if (existingCliente == null){
            val nuevoCliente = ClienteEntity(
                id = 0,
                numeroDocCliente = numeroCliente,
                nombreCompleto = nombreCliente,
                fechaRegistro = ""
            )
            val insertResult = db.insertCliente(nuevoCliente)
            if (insertResult != -1L) {
                Toast.makeText(
                    requireContext(),
                    "Cliente guardado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), "Error al guardar el cliente", Toast.LENGTH_SHORT).show()
            }
        }

        // Mostrar los datos en el Logcat al presionar "Guardar"
        Log.d("JabasFragment", "Guardado - Número Cliente: $numeroCliente")
        Log.d("JabasFragment", "Guardado - Nombre Cliente: $nombreCliente")

        // Mostrar los datos en los TextViews
        binding.textDocCli.text = "$numeroCliente"
        binding.textNomCli.text = "$nombreCliente"

        dialog.dismiss() // Cerrar el modal después de cancelar
    }

    private fun cancelarCli() {
        val inputNumeroCliente = dialog.findViewById<EditText>(R.id.inputNumeroCliente)
        val inputNombreCliente = dialog.findViewById<EditText>(R.id.inputNombreCliente)

        // Limpiar campos al presionar "Cancelar"
        inputNumeroCliente?.text?.clear()
        inputNombreCliente?.text?.clear()

        dialog.dismiss() // Cerrar el modal después de cancelar
    }

    fun showPreCarga(context: Context) {
        val dialogPreCarga = Dialog(context)
        dialogPreCarga.apply {
            setContentView(R.layout.layout_barra_desplazable)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)

            val iconoCarga = findViewById<ImageView>(R.id.iconoCarga)
            val textoCarga = findViewById<TextView>(R.id.textoCarga)

            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_loading_anim)
            val rocketAnimation = when (drawable) {
                is AnimatedVectorDrawableCompat -> drawable as AnimatedVectorDrawable
                is AnimatedVectorDrawable -> drawable
                else -> throw IllegalArgumentException("Unsupported drawable type")
            }
            iconoCarga.setImageDrawable(rocketAnimation)

            rocketAnimation.start()

            textoCarga.text = "Cargando..."

            show()
        }
    }

    private fun distribuirDatosEnInputs(dataPesoPollos: JSONObject) {
        binding.textDocCli.text = dataPesoPollos.optString("_PP_docCliente", "")
        binding.textNomCli.text = dataPesoPollos.optString("_PP_nombreCompleto", "")
        binding.PrecioKilo.setText(dataPesoPollos.optString("_PP_PKPollo", ""))
    }

    private fun procesarDataDetaPesoPollos(jsonArray: JSONArray): List<DataDetaPesoPollosEntity> {
        val detalles = mutableListOf<DataDetaPesoPollosEntity>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val detalle = DataDetaPesoPollosEntity(
                idDetaPP = jsonObject.getInt("_DPP_id"),
                cantJabas = jsonObject.getInt("_DPP_cantJabas"),
                cantPollos = jsonObject.getInt("_DPP_cantPolllos"),
                peso = jsonObject.getDouble("_DPP_peso"),
                tipo = jsonObject.getString("_DPP_tipo")
            )
            detalles.add(detalle)
        }
        return detalles
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializa y registra el receptor de cambios en la red
        networkChangeReceiver = NetworkChangeReceiver { isConnected ->
            if (!isConnected) {
//                checkAndSendLocalData()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 o superior
            checkNotificationPermission()
        }
    }

    private fun checkNotificationPermission() {
        // Verificar si el permiso ya está concedido
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permiso ya concedido, puedes continuar
            enableNotificationFeatures()
        } else {
            // Mostrar cuadro de diálogo explicativo
            showPermissionExplanationDialog()
        }
    }

    private fun showPermissionExplanationDialog() {
        // Crear y mostrar un cuadro de diálogo explicativo
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Permiso de Notificaciones Necesario")
        builder.setMessage("Esta aplicación necesita permiso para mostrar notificaciones de progreso. Por favor, concede el permiso para continuar.")

        builder.setPositiveButton("Aceptar") { _, _ ->
            // Solicitar el permiso
            requestNotificationPermission()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
            // Aquí puedes manejar la cancelación, si es necesario
        }

        val dialog = builder.create()
        dialog.show()
    }
    private fun requestNotificationPermission() {
        // Solicitar el permiso
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun enableNotificationFeatures() {
        // Aquí puedes activar las características de notificaciones porque ya tienes el permiso
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableNotificationFeatures()
            } else {
                // Permiso denegado, maneja la denegación aquí si es necesario
                showCustomToast(requireContext(), "Permiso de notificaciones denegado", "error")
            }
        }
    }

    var idPesoTemp = 0
    var dataPesoPollosJsonTemp = ""
    var dataDetaPesoPollosJsonTemp = ""
    override fun onResume() {
        super.onResume()

        // Registra el receptor para cambios en la conectividad
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkChangeReceiver, filter)

        binding.botonDeletePeso.visibility = View.INVISIBLE

        if (idPesoShared == 0){
            sharedViewModel.setIdListPesos(idPesoTemp)
            idPesoShared = sharedViewModel.getIdListPesos()?: 0
        }else{
            val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
            val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
            val idNucleo = binding.selectEstablecimiento.selectedItemPosition
            updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
        }

        // Inicia la verificación periódica de la conexión a Internet
//        handler.post(checkInternetRunnable)
    }
    override fun onStop() {
        super.onStop()

        idPesoTemp = sharedViewModel.getIdListPesos()?: 0
        dataPesoPollosJsonTemp = sharedViewModel.getDataPesoPollosJson()?: ""
        dataDetaPesoPollosJsonTemp = sharedViewModel.getDataDetaPesoPollosJson()?: ""
        sharedViewModel.setDataPesoPollosJson(dataPesoPollosJsonTemp)
        sharedViewModel.setDataDetaPesoPollosJson(dataDetaPesoPollosJsonTemp)
        sharedViewModel.setIdListPesos(idPesoTemp)

    }

    override fun onPause() {
        super.onPause()
        // Desregistra el receptor para evitar fugas de memoria
        requireContext().unregisterReceiver(networkChangeReceiver)
        val db = AppDatabase(requireContext())
        val device = getDeviceId(requireContext())

        idPesoTemp = sharedViewModel.getIdListPesos()?: 0
        dataPesoPollosJsonTemp = sharedViewModel.getDataPesoPollosJson()?: ""
        dataDetaPesoPollosJsonTemp = sharedViewModel.getDataDetaPesoPollosJson()?: ""

        updatePesoStatus(idPesoTemp, "Used")
        if (!navegationTrue){
            if (idPesoTemp != 0){
                if (!dataPesoPollosJsonTemp.isNullOrBlank() && !dataDetaPesoPollosJsonTemp.isNullOrBlank()){
                    val pesoUsed = pesoUsedEntity(
                        idPesoUsed = idPesoTemp,
                        devicedName = device,
                        dataPesoPollosJson = dataPesoPollosJsonTemp,
                        dataDetaPesoPollosJson = dataDetaPesoPollosJsonTemp,
                        fechaRegistro = ""
                    )
                    db.addPesoUsed(pesoUsed)
                }else{
                    val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                    val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
                    val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                    val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
                    val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                        id = idPesoTemp,
                        serie = "",
                        fecha = "",
                        totalJabas = "",
                        totalPollos = "",
                        totalPeso = "",
                        tipo = "",
                        numeroDocCliente = binding.textDocCli.text.toString(),
                        nombreCompleto = binding.textNomCli.text.toString(),
                        idGalpon = idGalpon.toString(),
                        idNucleo = idNucleo.toString(),
                        PKPollo = binding.PrecioKilo.text.toString(),
                        totalPesoJabas = "",
                        totalNeto = "",
                        totalPagar = ""
                    )

                    dataPesoPollosJson = dataPesoPollos.toJson().toString()
                    dataDetaPesoPollosJson = JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()

                    val pesoUsed = pesoUsedEntity(
                        idPesoUsed = idPesoTemp,
                        devicedName = device,
                        dataPesoPollosJson = dataPesoPollosJson.toString(),
                        dataDetaPesoPollosJson = dataDetaPesoPollosJson.toString(),
                        fechaRegistro = ""
                    )
                    db.addPesoUsed(pesoUsed)

                    CoroutineScope(Dispatchers.Main).launch {
                        val pesosEntity = PesosEntity(
                            id = 0,
                            idNucleo = idNucleo,
                            idGalpon = idGalpon,
                            numeroDocCliente = dataPesoPollos.numeroDocCliente,
                            nombreCompleto = dataPesoPollos.nombreCompleto,
                            dataPesoJson = dataPesoPollosJson!!,
                            dataDetaPesoJson = dataDetaPesoPollosJson!!,
                            fechaRegistro = ""
                        )
                        updateListPesos(requireContext(), this@JabasFragment, pesosEntity, idPesoTemp)
                    }
                }
            }
        }else{
            val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
            val idGalpon = galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
            val idNucleo = binding.selectEstablecimiento.selectedItemPosition
            val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
            val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                id = idPesoTemp,
                serie = "",
                fecha = "",
                totalJabas = "",
                totalPollos = "",
                totalPeso = "",
                tipo = "",
                numeroDocCliente = binding.textDocCli.text.toString(),
                nombreCompleto = binding.textNomCli.text.toString(),
                idGalpon = idGalpon.toString(),
                idNucleo = idNucleo.toString(),
                PKPollo = binding.PrecioKilo.text.toString(),
                totalPesoJabas = "",
                totalNeto = "",
                totalPagar = ""
            )

            dataPesoPollosJson = dataPesoPollos.toJson().toString()
            dataDetaPesoPollosJson = JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()

            val pesoUsed = pesoUsedEntity(
                idPesoUsed = idPesoTemp,
                devicedName = device,
                dataPesoPollosJson = dataPesoPollosJson.toString(),
                dataDetaPesoPollosJson = dataDetaPesoPollosJson.toString(),
                fechaRegistro = ""
            )
            db.addPesoUsed(pesoUsed)

            CoroutineScope(Dispatchers.Main).launch {
                val pesosEntity = PesosEntity(
                    id = 0,
                    idNucleo = idNucleo,
                    idGalpon = idGalpon,
                    numeroDocCliente = dataPesoPollos.numeroDocCliente,
                    nombreCompleto = dataPesoPollos.nombreCompleto,
                    dataPesoJson = dataPesoPollosJson!!,
                    dataDetaPesoJson = dataDetaPesoPollosJson!!,
                    fechaRegistro = ""
                )
                updateListPesos(requireContext(), this@JabasFragment, pesosEntity, idPesoTemp)
            }
        }

        sharedViewModel.setDataPesoPollosJson(dataPesoPollosJsonTemp)
        sharedViewModel.setDataDetaPesoPollosJson(dataDetaPesoPollosJsonTemp)
        sharedViewModel.setIdListPesos(idPesoTemp)
        // Detiene la verificación periódica
//        handler.removeCallbacks(checkInternetRunnable)
    }

    fun updatePesoStatus(id: Int, status: String){
        if (id != 0){
            val idDevice = getDeviceId(requireContext())
            setStatusUsed(requireContext(), id, "$status", idDevice) { success ->
                if (!success) {
                    Log.d("StatusLog", "Error al cambiar el estado del peso")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        coroutineScope.cancel()
    }

    override fun onItemDeleted() {
        updateUI()
    }

    override fun onItemAdd() {
        updateUI()
    }
    private var isJabasInputValid = true
    private var currentInputJabas = 0

    private fun updateUI() {
        val nuevoContador = sharedViewModel.getContadorJabas() ?: run {
            showCustomToast(requireContext(), "Error al obtener el contador de jabas", "error")
            return
        }
        val isEditMode = !sharedViewModel.getDataPesoPollosJson().isNullOrBlank()
        val isConPollos = binding.checkboxConPollos.isChecked

        _binding?.contadorJabas?.text = "= $nuevoContador"

        if (nuevoContador != 0){
            validateInputJabas(nuevoContador)
        }
        updateSaveButton(nuevoContador, isEditMode, isConPollos)
        updateSendButton(nuevoContador, isEditMode)
        showToastMessage(nuevoContador)
    }

    private fun updateSaveButton(contador: Int, isEditMode: Boolean, isConPollos: Boolean) {
        val isEnabled = when {
            contador < 0 && !isConPollos -> true  // Habilitado si faltan jabas por registrar y no estamos agregando pollos
            contador < 0 && isConPollos -> false  // Deshabilitado si faltan jabas por registrar y estamos intentando agregar pollos
            contador == 0 && isConPollos -> false  // Deshabilitado si no hay jabas disponibles y estamos agregando pollos
            contador == 0 && !isConPollos -> true  // Habilitado si no hay jabas disponibles pero no estamos agregando pollos (agregando jabas)
            contador == 0 && isEditMode -> false  // Deshabilitado en modo edición sin jabas disponibles
            contador > 0 -> true  // Habilitado si hay jabas disponibles
            else -> false
        }

        binding.botonGuardar.isEnabled = isEnabled
        binding.botonGuardar.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (isEnabled) R.color.teal_200 else R.color.gray
        )
    }

    private fun updateSendButton(contador: Int, isEditMode: Boolean) {
        val isEnabled = contador == 0 && jabasList.isNotEmpty()

        binding.botonEnviar.isEnabled = isEnabled
        binding.botonEnviar.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (isEnabled) R.color.your_greed else R.color.gray
        )
        binding.botonEnviar.setImageResource(
            if (isEnabled) R.drawable.baseline_content_paste_go_24 else R.drawable.null_24
        )
    }

    private fun validateInputJabas(nuevoContador: Int) {
        if (binding.checkboxConPollos.isChecked) {
            val numeroJabas = _binding?.inputNumeroJabas?.text.toString().toIntOrNull() ?: 0
            currentInputJabas = numeroJabas
            if (numeroJabas > nuevoContador.coerceAtLeast(0)) {  // Usamos coerceAtLeast para manejar contadores negativos
                showCustomToast(requireContext(), "No puedes usar jabas mayores a las que tienes", "error")
                _binding?.inputNumeroJabas?.error = "Actualiza el valor aquí"
                _binding?.inputNumeroJabas?.let {
                    it.requestFocus()
                    showKeyboard(it)
                }
                isJabasInputValid = false
            } else {
                _binding?.inputNumeroJabas?.error = null
                isJabasInputValid = true
            }
        } else {
            isJabasInputValid = true
            currentInputJabas = 0
        }
    }

    private fun showToastMessage(contador: Int) {
        when {
            contador < 0 -> showCustomToast(requireContext(), "Te Faltan ${contador} Jabas por registrar!", "info")
            contador == 0 && jabasList.isEmpty() -> showCustomToast(requireContext(), "No hay jabas registradas", "info")
            contador == 0 -> showCustomToast(requireContext(), "Ya no hay Jabas por utilizar", "success")
            contador > 0 -> showCustomToast(requireContext(), "Tienes $contador Jabas por utilizar", "info")
        }
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)

        if (view is EditText) {
            // Mover el cursor al final del texto
            view.setSelection(view.text.length)
        }
    }

    // Para mostrar el ProgressBar y el fondo bloqueado
    fun showLoading() {
        _binding?.let { binding ->
            val overlay = binding.overlay.findViewById<View>(R.id.overlay)
            val progressBar = binding.progressBar.findViewById<ProgressBar>(R.id.progressBar)
            overlay.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
        }
    }

    // Para ocultar el ProgressBar y el fondo bloqueado
    fun hideLoading() {
        _binding?.let { binding ->
            val overlay = binding.overlay.findViewById<View>(R.id.overlay)
            val progressBar = binding.progressBar.findViewById<ProgressBar>(R.id.progressBar)
            overlay.visibility = View.GONE
            progressBar.visibility = View.GONE
        }
    }

    private fun fetchData(time: Long) {
        // Simulando una operación de red o carga de datos
        Handler(Looper.getMainLooper()).postDelayed({
            // Ocultar el ProgressBar después de la carga de datos
            hideLoading()
        }, time) // Simulando 2 segundos de carga
    }
}