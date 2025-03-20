package app.serlanventas.mobile.ui.Jabas

import NetworkUtils
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentPesosBinding
import app.serlanventas.mobile.ui.BluetoothView.BluetoothFragment
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.pesoUsedEntity
import app.serlanventas.mobile.ui.DataSyncManager.DataSyncManager
import app.serlanventas.mobile.ui.Interfaces.OnItemClickListener
import app.serlanventas.mobile.ui.Interfaces.ProgressCallback
import app.serlanventas.mobile.ui.Jabas.ManagerPost.addListPesos
import app.serlanventas.mobile.ui.Jabas.ManagerPost.getNucleos
import app.serlanventas.mobile.ui.Jabas.ManagerPost.getSelectGalpon
import app.serlanventas.mobile.ui.Jabas.ManagerPost.obtenerPesosServer
import app.serlanventas.mobile.ui.Jabas.ManagerPost.removeListPesosId
import app.serlanventas.mobile.ui.Jabas.ManagerPost.setStatusUsed
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import app.serlanventas.mobile.ui.Jabas.ManagerPost.updateListPesos
import app.serlanventas.mobile.ui.Services.Logger
import app.serlanventas.mobile.ui.Services.getAddressMacDivice.getDeviceId
import app.serlanventas.mobile.ui.Utilidades.Constants
import app.serlanventas.mobile.ui.Utilidades.Constants.getCurrentDateTime
import app.serlanventas.mobile.ui.Utilidades.NetworkChangeReceiver
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel
import app.serlanventas.mobile.ui.ViewModel.TabViewModel
import app.serlanventas.mobile.ui.slideshow.BluetoothConnectionService
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

//JabasFragment.kt // INTERACTURA CON LA UI
class JabasFragment : Fragment(), OnItemClickListener, ProgressCallback {

    private lateinit var db: AppDatabase

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothConnectionService: BluetoothConnectionService? = null
    private lateinit var networkChangeReceiver: NetworkChangeReceiver

    private var isProduction: Boolean = false
    private var baseUrl: String = ""
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var dataSyncManager: DataSyncManager
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L
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
    private lateinit var progressBar: ImageView

    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>

    private lateinit var logger: Logger

    private val checkInternetRunnable = object : Runnable {
        override fun run() {
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                binding.btnSincronizarPesos.visibility = View.VISIBLE
            } else {
                binding.btnSincronizarPesos.visibility = View.GONE
                handler.postDelayed(this, checkInterval)
            }
        }
    }


    private lateinit var jabasAdapter: JabasAdapter
    private val jabasList = mutableListOf<JabasItem>()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sharedTabViewModel: TabViewModel
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var _binding: FragmentPesosBinding? = null

    private val binding get() = _binding!!

    @SuppressLint("SetTextI18n", "DefaultLocale", "ResourceType", "ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesosBinding.inflate(inflater, container, false)
        val root: View = binding.root
        progressBar = binding.loadingGif

        val baseUrl = Constants.getBaseUrl()

        logger = Logger(requireContext())

        sharedTabViewModel = ViewModelProvider(requireActivity()).get(TabViewModel::class.java)


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothConnectionService = BluetoothConnectionService(requireContext(),
            bluetoothAdapter,
            onMessageReceived = { message ->

            }
        )
        sharedViewModel.pesoValue.observe(viewLifecycleOwner) { peso ->
            if (!peso.isNullOrBlank()) {
                val pesoFormatted = peso.toDoubleOrNull()?.toString() ?: "0.00"
                binding.inputPesoKg.setText(pesoFormatted)
//                logger.log2("PesoValue: Peso Formateado recibido en JabaFragment -> $pesoFormatted")
            }
        }

        showLoading()

        connectedDeviceAddress = sharedViewModel.getConnectedDeviceAddress()

        if (!connectedDeviceAddress.isNullOrBlank()) {
            sharedViewModel.connectedDeviceName.observe(viewLifecycleOwner) { deviceName ->
                if (!deviceName.isNullOrEmpty()) {
                    binding.deviceConnected.text = "CONECTADO A: $deviceName"
                    binding.deviceConnectedLayout.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.button_background_active
                    )
                } else {
                    binding.deviceConnected.text = "NO CONECTADO"
                    binding.deviceConnectedLayout.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.button_background_inactive
                    )
                }
            }
        } else {
            binding.deviceConnected.text = "NO CONECTADO"
            binding.deviceConnectedLayout.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive)

        }

        // Regreso de datos de la preliminar
        dataDetaPesoPollosJson = sharedViewModel.getDataDetaPesoPollosJson()
        dataPesoPollosJson = sharedViewModel.getDataPesoPollosJson()
        idPesoShared = sharedViewModel.getIdListPesos() ?: 0


        jabasAdapter = JabasAdapter(jabasList, this, sharedViewModel)
        binding.recyclerViewJabas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = jabasAdapter
        }

        binding.selectGalpon.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> {
                        //
                    }

                    else -> {
                        val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                        val idGalpon =
                            galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull()
                                ?: 0
                        val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                        updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Opcional: maneja el caso cuando no se selecciona nada
            }
        }
        binding.accordionHeader.setOnClickListener {
            if (binding.accordionContent.visibility == View.GONE) {
                binding.accordionContent.visibility = View.VISIBLE
                binding.arrow.setImageResource(R.drawable.ic_arrow_up)
            } else {
                binding.accordionContent.visibility = View.GONE
                binding.arrow.setImageResource(R.drawable.ic_arrow_down)
            }
        }

        binding.selectListpesos.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                    val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                    val idGalpon =
                        galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull()
                            ?: 0
                    updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                }
            }
            false
        }

        binding.selectListpesos.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    Log.d("JabasFragment", "Posición seleccionada en selectListpesos: $position")
                    when (position) {
                        0 -> {
                            // "Lista de Pesos" seleccionado
                            Log.d("JabasFragment", "Opción 'Lista de Pesos' seleccionada")
                            sharedViewModel.setIdListPesos(0)
                        }

                        else -> {
                            if (binding.idListPeso.text.isNullOrBlank()) {
                                showLoading()
                            }
                            if (pesosList.isNotEmpty()) {
                                val selectedPesos = pesosList[position - 1]
                                Log.d(
                                    "JabasFragment",
                                    "Peso seleccionado: ${selectedPesos.numeroDocCliente}/${selectedPesos.nombreCompleto}"
                                )

                                var idPeso = selectedPesos.id
                                if (idPesoShared != 0) {
                                    if (idPeso != idPesoShared) {
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
                                                    val dataPesoPollos =
                                                        JSONObject(selectedPesos.dataPesoJson)
                                                    val dataDetaPesoPollos =
                                                        JSONArray(selectedPesos.dataDetaPesoJson)

                                                    val idNucleo = dataPesoPollos.optString(
                                                        "_PP_idNucleo",
                                                        "0"
                                                    )
                                                    val idGalpon = dataPesoPollos.optString(
                                                        "_PP_IdGalpon",
                                                        "0"
                                                    )

                                                    idGalpoListaPesos = idGalpon
                                                    Log.d(
                                                        "JabasFragment",
                                                        "ID Núcleo en JSON: $idNucleo, ID Galpón en JSON: $idGalpon"
                                                    )

                                                    limpiarTablaJabas()
                                                    detallesList = procesarDataDetaPesoPollos(
                                                        dataDetaPesoPollos
                                                    )
                                                    distribuirDatosEnInputs(dataPesoPollos)

                                                    detallesList.forEach { detalle ->
                                                        val newItem = JabasItem(
                                                            id = detalle.idDetaPP,
                                                            numeroJabas = detalle.cantJabas,
                                                            numeroPollos = detalle.cantPollos,
                                                            pesoKg = detalle.peso,
                                                            conPollos = detalle.tipo,
                                                            idPesoPollo = detalle.idPesoPollo,
                                                            fechaPeso = detalle.fechaPeso
                                                        )

                                                        jabasAdapter.addItem(newItem)
                                                    }

                                                    binding.botonDeletePeso.visibility =
                                                        View.VISIBLE
                                                    binding.botonGuardarPeso.backgroundTintList =
                                                        ContextCompat.getColorStateList(
                                                            requireContext(),
                                                            R.color.yellow
                                                        )
                                                    binding.botonGuardarPeso.setImageResource(R.drawable.baseline_backup_24)
                                                }

                                                status == "1" && addresMac != diviceName -> {
                                                    showCustomToast(
                                                        requireContext(),
                                                        "Este peso está siendo usado por otro dispositivo",
                                                        "info"
                                                    )
                                                    limpiarCampos()
                                                    limpiarClientes()
                                                }

                                                status == "0" && addresMac == "null" -> {
                                                    // Proceder con las acciones existentes
                                                    binding.idListPeso.text = idPeso.toString()
                                                    idPesoShared = idPeso
                                                    sharedViewModel.setIdListPesos(idPeso)

                                                    updatePesoStatus(idPeso, "Used")

                                                    // Resto del código para procesar datos (sin cambios)
                                                    val dataPesoPollos =
                                                        JSONObject(selectedPesos.dataPesoJson)
                                                    val dataDetaPesoPollos =
                                                        JSONArray(selectedPesos.dataDetaPesoJson)

                                                    val idNucleo = dataPesoPollos.optString(
                                                        "_PP_idNucleo",
                                                        "0"
                                                    )
                                                    val idGalpon = dataPesoPollos.optString(
                                                        "_PP_IdGalpon",
                                                        "0"
                                                    )
                                                    idGalpoListaPesos = idGalpon
                                                    Log.d(
                                                        "JabasFragment",
                                                        "ID Núcleo en JSON: $idNucleo, ID Galpón en JSON: $idGalpon"
                                                    )

                                                    limpiarTablaJabas()
                                                    detallesList = procesarDataDetaPesoPollos(
                                                        dataDetaPesoPollos
                                                    )
                                                    distribuirDatosEnInputs(dataPesoPollos)

                                                    detallesList.forEach { detalle ->
                                                        val newItem = JabasItem(
                                                            id = detalle.idDetaPP,
                                                            numeroJabas = detalle.cantJabas,
                                                            numeroPollos = detalle.cantPollos,
                                                            pesoKg = detalle.peso,
                                                            conPollos = detalle.tipo,
                                                            idPesoPollo = detalle.idPesoPollo,
                                                            fechaPeso = detalle.fechaPeso
                                                        )

                                                        jabasAdapter.addItem(newItem)
                                                    }
                                                    calcularTotalPagar()

                                                    binding.botonDeletePeso.visibility =
                                                        View.VISIBLE
                                                    binding.botonGuardarPeso.backgroundTintList =
                                                        ContextCompat.getColorStateList(
                                                            requireContext(),
                                                            R.color.yellow
                                                        )
                                                    binding.botonGuardarPeso.setImageResource(R.drawable.baseline_backup_24)
                                                }

                                                else -> {
                                                    showCustomToast(
                                                        requireContext(),
                                                        "Error: Este peso está siendo usado por otro dispositivo",
                                                        "error"
                                                    )
                                                    limpiarCampos()
                                                    limpiarClientes()
                                                    fetchData(2000)
                                                }
                                            }

                                        } else {
                                            Log.d(
                                                "JabasFragment",
                                                "Error al obtener el estado del peso"
                                            )
                                            fetchData(2000)
                                        }
                                    }
                                }
                                fetchData(2000)
                            } else {
                                Log.d("JabasFragment", "La lista de pesos está vacía")
                                binding.botonDeletePeso.visibility = View.INVISIBLE
                                binding.botonGuardarPeso.backgroundTintList =
                                    ContextCompat.getColorStateList(
                                        requireContext(),
                                        R.color.purple_500
                                    )
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

        binding.deviceConnectedLayout.setOnClickListener {
            if (connectedDeviceAddress.isNullOrBlank() || connectedDeviceAddress == "null") {
                // Muestra un fragmento modal si connectedDeviceAddress es null
                showModalBluetoothFragment()
            } else {
                // Muestra el diálogo de confirmación si connectedDeviceAddress no es null
                showDisconnectDialog()
            }
        }

        binding.botonGuardarPeso.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                val idGalpon =
                    galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull()
                        ?: 0
                var numDoc = binding.textDocCli.text.toString()
                var nombres = binding.textNomCli.text.toString()

                if (!jabasList.isNotEmpty()) {
                    showCustomToast(
                        requireContext(),
                        "¡La tabla esta vacía, por favor registre datos antes de enviar!",
                        "info"
                    )
                    fetchData(2000)
                } else if (numDoc.isBlank() && nombres.isBlank()) {
                    showCustomToast(requireContext(), "¡Ingrese el Cliente", "info")
                    binding.textDocCli.requestFocus()
                    fetchData(2000)
                } else if (binding.PrecioKilo.text.isNullOrBlank()) {
                    showCustomToast(requireContext(), "¡Ingrese un precio", "info")
                    binding.PrecioKilo.error = "Ingrese un precio"
                    binding.PrecioKilo.requestFocus()
                    fetchData(2000)
                } else if (idNucleo == 0) {
                    showCustomToast(requireContext(), "¡Seleccione un nucleo!", "info")
                    fetchData(2000)
                } else if (idGalpon == 0) {
                    showCustomToast(requireContext(), "¡Seleccione un galpón!", "info")
                    fetchData(2000)
                } else {
                    showLoading()

                    val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
                    val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                        id = 1,
                        serie = "",
                        numero = "",
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
                        totalPagar = binding.totalPagarPreview.text.toString(),
                        idUsuario = "",
                        idEstado = "0",
                    )

                    dataPesoPollosJson = dataPesoPollos.toJson().toString()
                    dataDetaPesoPollosJson =
                        JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()
                    val serieDevice = db.getSerieDevice()

                    val pesosEntity = PesosEntity(
                        id = 0,
                        idNucleo = idNucleo,
                        idGalpon = idGalpon,
                        numeroDocCliente = dataPesoPollos.numeroDocCliente,
                        nombreCompleto = dataPesoPollos.nombreCompleto,
                        dataPesoJson = dataPesoPollosJson!!,
                        dataDetaPesoJson = dataDetaPesoPollosJson!!,
                        idEstado = "0",
                        isSync = "0",
                        devicedName = "",
                        serieDevice = serieDevice!!.codigo,
                        fechaRegistro = ""
                    )
                    if (idPesoShared == 0) {
                        val success =
                            addListPesos(requireContext(), this@JabasFragment, pesosEntity)
                        if (success) {
                            limpiarCampos()
                            limpiarClientes()
                            updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                        }
                    } else {
                        val success = updateListPesos(
                            requireContext(),
                            this@JabasFragment,
                            pesosEntity,
                            idPesoShared
                        )
                        if (success) {
                            limpiarCampos()
                            limpiarClientes()
                            sharedViewModel.setContadorJabas(0)
                            _binding?.contadorJabas?.text = "= 0"
                            updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                        }
                    }
                    fetchData(2000)
                }
            }
        }

        binding.btnSincronizarPesos.setOnClickListener {
            var warningMsg = ""
            if (idPesoShared != 0) {
                warningMsg =
                    "Advertencia: Usted esta usando un peso, es posible que la lista sea diferente al momento de sincronizar. "
            }
            // Mostrar el cuadro de diálogo de confirmación
            AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Sincronización")
                .setMessage("${warningMsg}\n¿Estás seguro de que deseas sincronizar los pesos?")
                .setPositiveButton("Sincronizar") { _, _ ->
                    // Código a ejecutar si el usuario confirma
                    binding.btnSincronizarPesos.isEnabled = false
                    showLoading()
                    val idPesoUtilizado = sharedViewModel.getIdListPesos() ?: 0
                    if (idPesoUtilizado > 0) {
                        limpiarCampos()
                        limpiarClientes()
                    }

                    val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                    val idGalpon =
                        galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull()
                            ?: 0
                    val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                    idPesoShared = idPesoUtilizado

                    obtenerPesosServer(requireContext()) { success ->
                        if (success) {
                            showCustomToast(
                                requireContext(),
                                "Pesos sincronizados con éxito",
                                "success"
                            )
                            updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                        } else {
                            showCustomToast(requireContext(), "Error al sincronizar pesos", "error")
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.btnSincronizarPesos.isEnabled = true
                            hideLoading()
                        }, 500)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.inputNumeroJabas.setText("10")
        binding.inputCantPollos.setText("0")
        binding.inputNumeroJabas.isEnabled = true
        binding.inputCantPollos.isEnabled = false

        binding.inputPesoKg.isEnabled = true // pesotrue

        if (!dataDetaPesoPollosJson.isNullOrBlank()) {
            sharedViewModel.setBtnTrue()
            binding.checkboxConPollos.isChecked = false
            binding.botonGuardar.isEnabled = true
        } else {
            sharedViewModel.setBtnFalse()
            binding.checkboxConPollos.isChecked = false
            binding.botonGuardar.isEnabled = true
        }
        var valor = sharedViewModel.getBtnStatus()

        if (valor == false) {
            binding.botonEnviar.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.gray)
            binding.botonEnviar.setImageResource(R.drawable.null_24)
        } else {
            binding.botonEnviar.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.your_greed)
            binding.botonEnviar.setImageResource(R.drawable.baseline_content_paste_go_24)
            binding.botonGuardar.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
            binding.botonGuardar.isEnabled = true
        }

        binding.botonEnviar.isEnabled = valor


        binding.checkboxConPollos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                var nJabas = sharedViewModel.getContadorJabas()
                if (nJabas == 0) {
                    binding.botonGuardar.backgroundTintList =
                        ContextCompat.getColorStateList(requireContext(), R.color.gray)
                    binding.botonGuardar.isEnabled = false
                    showCustomToast(requireContext(), "No te quedan Jabas para usar", "warning")
                } else if (nJabas!! < 0) {
                    binding.botonGuardar.backgroundTintList =
                        ContextCompat.getColorStateList(requireContext(), R.color.gray)
                    binding.botonGuardar.isEnabled = false
                    showCustomToast(
                        requireContext(),
                        "Te faltan ${nJabas} Jabas por registrar",
                        "error"
                    )
                } else {
                    binding.botonGuardar.backgroundTintList =
                        ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
                    binding.botonGuardar.isEnabled = true
                }

                binding.inputNumeroJabas.setText("4")
                binding.inputNumeroJabas.isEnabled = true
                binding.inputCantPollos.isEnabled = true
                binding.inputCantPollos.setText("")
                binding.inputCantPollos.requestFocus()
                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(
                    binding.inputCantPollos,
                    InputMethodManager.SHOW_IMPLICIT
                )
            } else {
                binding.botonGuardar.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
                binding.botonGuardar.isEnabled = true
                binding.inputNumeroJabas.setText("10")
                binding.inputCantPollos.isEnabled = false
                binding.inputCantPollos.setText("0")
                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.inputCantPollos.windowToken, 0)
            }
        }

        binding.PrecioKilo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                calcularTotalPagar()
            }
        })

        // esto esta en el JabasFragment.kt
        binding.botonGuardar.setOnClickListener {
            val numeroJabas = binding.inputNumeroJabas.text.toString()
            val numeroPollos = binding.inputCantPollos.text.toString()
            val pesoKg = binding.inputPesoKg.text.toString()
            val conPollos =
                if (binding.checkboxConPollos.isChecked) "JABAS CON POLLOS" else "JABAS SIN POLLOS"

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
                    binding.inputCantPollos.requestFocus()
                    isValid = false
                } else {
                    binding.inputCantPollos.error = null // Limpiar error si está presente
                }
            }

            // Si todos los campos son válidos, agregar el nuevo item
            if (isValid) {
                val currentTime = getCurrentDateTime()
                val newItem = JabasItem(
                    id = jabasList.size + 1,
                    numeroJabas = numeroJabas.toInt(),
                    numeroPollos = if (binding.checkboxConPollos.isChecked) numeroPollos.toInt() else 0,
                    pesoKg = pesoKg.toDouble(),
                    conPollos = conPollos,
                    idPesoPollo = "",
                    fechaPeso = currentTime
                )
                jabasAdapter.addItem(newItem)

                // Limpiar los campos de entrada después de agregar el item
                binding.inputPesoKg.text?.clear()
                calcularTotalPagar()

            }
        }

        binding.botonLimpiar.setOnClickListener {
            mostrarConfirmacionLimpiar()
        }

        binding.botonCliente.setOnClickListener {
            showLoading()

            val numeroCliente = binding.textDocCli.text.toString()

            // Validar número de cliente
            if (numeroCliente.length < 8 || numeroCliente.length > 11 || !numeroCliente.matches("[0-9]+".toRegex())) {
                binding.textDocCli.error = "Ingrese un número válido (8-11 dígitos)"
                binding.textDocCli.requestFocus()
                hideLoading()
                return@setOnClickListener
            }

            Log.d("JabasFragment", "Cliente a Buscar: $numeroCliente")

            // Verificar disponibilidad de internet
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                val cliente = db.getClienteById(numeroCliente)

                if (cliente != null) {
                    binding.textNomCli.setText(cliente.nombreCompleto)

                    binding.textDocCli.isEnabled = false
                    binding.textNomCli.isEnabled = false
                    binding.botonCliente.isEnabled = false
                    // Actualizar la vista con los datos del cliente desde la base de datos
                    hideLoading()
                } else {
                    val jsonParam = JSONObject()
                    jsonParam.put("numeroDocumento", numeroCliente)

                    var baseUrlCliente =
                        "${baseUrl}controllers/FuncionesController/buscarCliente.php"
                    ManagerPost.BuscarCliente(
                        baseUrlCliente,
                        jsonParam.toString()
                    ) { nombreCompleto ->
                        binding.textNomCli.setText(nombreCompleto ?: "")

                        hideLoading()
                        if (nombreCompleto.isNullOrBlank()) {
                            showCustomToast(
                                requireContext(),
                                "No se encontró el cliente, Ingrese un nombre manualmente",
                                "info"
                            )
                            binding.textDocCli.isEnabled = true
                            binding.textNomCli.isEnabled = true
                            binding.botonCliente.isEnabled = true
                        } else {
                            binding.textDocCli.isEnabled = false
                            binding.textNomCli.isEnabled = false
                            binding.botonCliente.isEnabled = false

                            // Deshabilitar campos de entrada
                            binding.textDocCli.isEnabled = false
                            binding.textNomCli.isEnabled = false
                            binding.botonCliente.isEnabled = false

                            val nuevoCliente = ClienteEntity(
                                numeroDocCliente = numeroCliente,
                                nombreCompleto = nombreCompleto.toString(),
                                fechaRegistro = ""
                            )

                            val cliente = db.getClienteById(numeroCliente)

                            if (cliente == null) {
                                // Mostrar un diálogo de confirmación
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Guardar Cliente localmente")
                                    .setMessage("¿Desea guardar al cliente $numeroCliente / $nombreCompleto?")
                                    .setPositiveButton("Sí") { dialog, _ ->
                                        // Insertar el cliente si el usuario confirma
                                        val insertResult = db.insertCliente(nuevoCliente)

                                        if (insertResult != -1L) {
                                            showCustomToast(
                                                requireContext(),
                                                "Cliente guardado exitosamente",
                                                "success"
                                            )
                                        } else {
                                            showCustomToast(
                                                requireContext(),
                                                "Error al guardar el cliente",
                                                "error"
                                            )
                                        }
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton("No") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .show()
                            }
                        }
                    }
                }
            } else {
                val cliente = db.getClienteById(numeroCliente)

                if (cliente != null) {
                    binding.textNomCli.setText(cliente.nombreCompleto)
                    binding.textDocCli.isEnabled = false
                    binding.textNomCli.isEnabled = false
                    binding.botonCliente.isEnabled = false

                    hideLoading()
                } else {
                    binding.textDocCli.isEnabled = true
                    binding.textNomCli.isEnabled = true
                    binding.botonCliente.isEnabled = true

                    showCustomToast(
                        requireContext(),
                        "No se encontró el cliente, Ingrese un nombre manualmente",
                        "info"
                    )
                    hideLoading()
                }
            }
        }

        // Configuración del botón Limpiar
        binding.botonLimpiarCliente.setOnClickListener {
            showDialogLimpiarCliente()
        }

        binding.botonEnviar.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val sharedPreferences =
                    requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val dni = sharedPreferences.getString("dni", "") ?: ""

                val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
                val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                val idGalpon =
                    galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull()
                        ?: 0
                val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                    id = 1,
                    serie = "",
                    numero = "",
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
                    totalPagar = "",
                    idUsuario = dni,
                    idEstado = "0",
                )
                var idEstablecimiento = binding.selectEstablecimiento.selectedItemPosition
                var precio = dataPesoPollos.PKPollo.toDoubleOrNull()
                if (idEstablecimiento == 0) {
                    showCustomToast(requireContext(), "¡Seleccione un Núcleo!", "info")
                    fetchData(500)
                    toggleAcordionisGone()

                } else if (dataPesoPollos.idGalpon == "0" || dataPesoPollos.idGalpon.isBlank()) {
                    showCustomToast(requireContext(), "¡Seleccione un galpón!", "info")
                    fetchData(500)
                    toggleAcordionisGone()

                } else if (dataPesoPollos.numeroDocCliente == "" && dataPesoPollos.nombreCompleto == "") {
                    showCustomToast(requireContext(), "¡Registre un cliente!", "info")
                    fetchData(500)
                    toggleAcordionisGone()
                } else if (dataDetaPesoPollos.isEmpty()) {
                    showCustomToast(
                        requireContext(),
                        "¡La tabla esta vacía, por favor registre datos antes de enviar!",
                        "info"
                    )
                    fetchData(500)

                } else if (dataPesoPollos.PKPollo.isBlank()) {
                    binding.PrecioKilo.error =
                        "¡Para calcular el Total a Pagar necesitamos saber el precio por kilo!"
                    binding.PrecioKilo.requestFocus()
                    fetchData(500)

                } else if (precio == null || dataPesoPollos.PKPollo.isBlank() || dataPesoPollos.PKPollo.toDouble() <= 0) {
                    binding.PrecioKilo.error =
                        "¡Para calcular el Total a Pagar necesitamos saber el precio por kilo!"
                    fetchData(500)
                    binding.PrecioKilo.requestFocus()
                } else {
                    showLoading()

                    toggleAcordionnotGone()
                    Log.d("JabasFragment", "$dataDetaPesoPollos")
                    Log.d("JabasFragment", "$dataPesoPollos")

                    dataPesoPollosJson = dataPesoPollos.toJson().toString()
                    dataDetaPesoPollosJson =
                        JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()

                    sharedViewModel.setDataPesoPollosJson(dataPesoPollosJson!!)
                    sharedViewModel.setDataDetaPesoPollosJson(dataDetaPesoPollosJson!!)

                    navegationTrue = true
                    findNavController().navigate(R.id.nav_initPreliminar)
                }
            }
        }
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Posponer la carga de datos hasta que el fragmento esté listo
        view.post {
            cargarDatosUsadosPeso()
        }
    }

    private fun cargarDatosUsadosPeso() {
        val pesoUsedExisted = db.getPesosUsedAll()
        if (!pesoUsedExisted.isNullOrEmpty()) {
            val peso = pesoUsedExisted.first()
            idPesoShared = peso.idPesoUsed
            sharedViewModel.setIdListPesos(idPesoShared)

            dataPesoPollosJson = peso.dataPesoPollosJson
            dataDetaPesoPollosJson = peso.dataDetaPesoPollosJson
            sharedViewModel.setDataPesoPollosJson(dataPesoPollosJson.toString())
            sharedViewModel.setDataDetaPesoPollosJson(dataDetaPesoPollosJson.toString())
            db.deleteAllPesoUsed()
        } else {
            dataPesoPollosJson = sharedViewModel.getDataPesoPollosJson()
            dataDetaPesoPollosJson = sharedViewModel.getDataDetaPesoPollosJson()
        }

        if (!dataDetaPesoPollosJson.isNullOrEmpty()) {
            val dataDetaPesoPollos = JSONArray(dataDetaPesoPollosJson)
            detallesList = procesarDataDetaPesoPollos(dataDetaPesoPollos)
            Log.d("JabasFragment detallesList", "${detallesList}")

            if (idPesoShared == 0) {
                detallesList.forEach { detalle ->
                    val newItem = JabasItem(
                        id = detalle.idDetaPP,
                        numeroJabas = detalle.cantJabas,
                        numeroPollos = detalle.cantPollos,
                        pesoKg = detalle.peso,
                        conPollos = detalle.tipo,
                        idPesoPollo = detalle.idPesoPollo,
                        fechaPeso = detalle.fechaPeso
                    )

                    jabasAdapter.addItem(newItem)
                }
            }

            val dataPesoPollos = JSONObject(dataPesoPollosJson.toString())
            distribuirDatosEnInputs(dataPesoPollos)
        }
        // Segunda condición: Procesamiento si dataPesoPollosJson no está vacío
        if (!dataPesoPollosJson.isNullOrEmpty()) {
            val dataPesoPollos = JSONObject(dataPesoPollosJson)
            distribuirDatosEnInputs(dataPesoPollos)
            toggleAcordionisGone()
        }
        calcularTotalPagar()

        // Obtener los nombres de los nucleos y llenar el spinner
        getNucleos(baseUrl, requireContext()) { nucleos ->
            nucleos?.let {
                showLoading()
                // Preparar nombres de los nucleos con elemento por defecto
                val nombresNucleos = mutableListOf("Núcleos")
                nombresNucleos.addAll(it.map { nucleo -> nucleo.nombre })

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    nombresNucleos
                )
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


                    getSelectGalpon(idNucleo, baseUrl, requireContext()) { galpones ->
                        galpones?.let {
                            galponesList = it
                            Log.d(
                                "JabasFragment",
                                "Galpones cargados: ${it.map { galpon -> "${galpon.idGalpon}: ${galpon.nombre}" }}"
                            )

                            if (it.isNotEmpty()) {
                                val nombresGalpones = mutableListOf("Galpones")
                                galponIdMap = mutableMapOf(0 to "Galpones")

                                // Añadir los galpones manteniendo el ID como clave
                                it.forEach { galpon ->
                                    nombresGalpones.add(galpon.nombre)
                                    galponIdMap[galpon.idGalpon] = galpon.nombre
                                }

                                val adapter = ArrayAdapter(
                                    requireContext(),
                                    android.R.layout.simple_spinner_item,
                                    nombresGalpones
                                )
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.selectGalpon.adapter = adapter

                                if (galponIdToSelect != null) {
                                    Log.d(
                                        "JabasFragment",
                                        "Intentando seleccionar galpon con ID: $galponIdToSelect"
                                    )

                                    val index =
                                        nombresGalpones.indexOf(galponIdMap[galponIdToSelect.toInt()])
                                    if (index != -1) {
                                        binding.selectGalpon.setSelection(index)
                                        Log.d(
                                            "JabasFragment",
                                            "Galpon seleccionado en posición: $index"
                                        )
                                    } else {
                                        Log.w(
                                            "JabasFragment",
                                            "galponIdToSelect no encontrado: $galponIdToSelect"
                                        )
                                        binding.selectGalpon.setSelection(0)
                                    }
                                } else {
                                    Log.d(
                                        "JabasFragment",
                                        "No hay galponIdToSelect. Seleccionando 'Galpones'."
                                    )
                                    binding.selectGalpon.setSelection(0)
                                }
                                idGalpoListaPesos = ""
                            } else {
                                Log.w(
                                    "SelectGalpon",
                                    "La lista de galpones está vacía para el nucleo $idNucleo"
                                )
                                val emptyAdapter = ArrayAdapter(
                                    requireContext(),
                                    android.R.layout.simple_spinner_item,
                                    listOf("Galpones")
                                )
                                emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.selectGalpon.adapter = emptyAdapter

                            }
                            fetchData(2000)
                        } ?: run {
                            Log.e(
                                "SelectGalpon",
                                "Error al obtener los galpones desde el servidor para el nucleo $idNucleo"
                            )
                            fetchData(2000)
                        }
                    }
                }

                // Seleccionar nucleo inicial y cargar galpones correspondientes
                if (nucleoIdSeleccionado.isNotEmpty()) {
                    val position =
                        it.indexOfFirst { nucleo -> nucleo.idEstablecimiento == nucleoIdSeleccionado }
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
                binding.selectEstablecimiento.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            updateSpinnerPesosIdGalpon(0, 0)
                            if (position == 0) {
                                if (jabasList.isEmpty()) {
                                    if (idPesoShared != 0) {
                                        limpiarCampos()
                                        limpiarClientes()
                                    } else if (dataPesoPollosJson.isNullOrBlank()) {
                                        limpiarCampos()
                                        limpiarClientes()
                                    }
                                } else if (idPesoShared != 0) {
                                    if (dataPesoPollosJson.isNullOrBlank()) {
                                        limpiarCampos()
                                        limpiarClientes()
                                    }
                                }
                                val emptyAdapter = ArrayAdapter(
                                    requireContext(),
                                    android.R.layout.simple_spinner_item,
                                    listOf("Galpones")
                                )
                                emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.selectGalpon.adapter = emptyAdapter
                            } else {
                                val idNucleoSeleccionado = it[position - 1].idEstablecimiento
                                // Si es la selección inicial, usar galponIdSeleccionado, si no, usar ""
                                val galponIdToSelect =
                                    if (idNucleoSeleccionado == nucleoIdSeleccionado) galponIdSeleccionado else ""
                                if (idGalpoListaPesos.isNullOrBlank()) {
                                    cargarYSeleccionarGalpones(
                                        idNucleoSeleccionado,
                                        galponIdToSelect
                                    ) // Sin retraso para selecciones manuales
                                } else {
                                    cargarYSeleccionarGalpones(
                                        idNucleoSeleccionado,
                                        idGalpoListaPesos
                                    )
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
    }

    private fun calcularTotalPagar() {
        val precioKiloText = binding.PrecioKilo.text.toString()

        if (precioKiloText.isNotBlank()) {
            val precioKilo = precioKiloText.toDoubleOrNull()

            if (precioKilo != null) {
                var totalPesoPollos = 0.0
                var totalPesoJabas = 0.0

                jabasList.forEach { jaba ->
                    if (jaba.conPollos == "JABAS CON POLLOS") {
                        totalPesoPollos += jaba.pesoKg
                    } else {
                        totalPesoJabas += jaba.pesoKg
                    }
                }

                val neto = totalPesoPollos - totalPesoJabas
                val TotalPagar = neto * precioKilo

                binding.totalPagarPreview.setText(String.format("%.2f", TotalPagar))
            } else {
                binding.totalPagarPreview.setText("0.00")
            }
        } else {
            binding.totalPagarPreview.setText("0.00")
        }
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

                try {
                    val disconnected = bluetoothConnectionService?.closeConnection(device) ?: false
                    if (disconnected) {
                        Log.i("Bluetooth", "Desconexión exitosa")
                    } else {
                        Log.w("Bluetooth", "Desconexión fallida, pero se actualizará el estado")
                    }
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error al desconectar: ${e.message}")
                } finally {
                    // Actualiza el estado de la conexión independientemente del resultado
                    connectedDeviceAddress = null
                    sharedViewModel.updateConnectedDeviceAddress("")
                    binding.deviceConnected.text = "NO CONECTADO"
                    binding.deviceConnected.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.button_background_inactive
                    )
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
        checkBluetoothPermissions { permissionsGranted ->
            if (permissionsGranted) {
                // Si los permisos están concedidos, muestra el fragmento
                val dialogFragment = BluetoothFragment()
                dialogFragment.show(requireActivity().supportFragmentManager, "BluetoothFragment")
            } else {
                // Si los permisos no están concedidos, no muestres el fragmento
            }
        }
    }

    private fun updateSpinnerPesosIdGalpon(idNucleo: Int, idGalpon: Int) {
        val idDevice = getDeviceId(requireContext())
        val baseUrl = Constants.getBaseUrl()
        // Obtener los pesos guardados en la base de datos
        ManagerPost.getListPesosByIdGalpon(
            baseUrl,
            requireContext(),
            idGalpon,
            idNucleo,
            idDevice,
        ) { fetchedPesosList ->
            if (fetchedPesosList != null && fetchedPesosList.isNotEmpty()) {
                // Actualizar la lista global
                pesosList = fetchedPesosList

                // Crear items para el spinner
                val spinnerItems = mutableListOf("Lista de Pesos")
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
                        // Si no se encuentra el ID, seleccionar "Lista de Pesos"
                        binding.selectListpesos.setSelection(0)
                    }
                }
            } else {
                // Si no hay datos, actualizar la lista global a una lista vacía
                pesosList = emptyList()

                // Actualizar el spinner en el hilo principal
                CoroutineScope(Dispatchers.Main).launch {
                    val spinnerItems = listOf("Lista de Pesos", "No hay datos guardados")
                    val adapter = object : ArrayAdapter<String>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        spinnerItems
                    ) {
                        override fun isEnabled(position: Int): Boolean {
                            // Deshabilitar el segundo ítem (posición 1)
                            return position != 1
                        }

                        @SuppressLint("ResourceAsColor")
                        override fun getDropDownView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
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
            removeListPesosId(requireContext(), idPeso) { success ->
                if (success) {
                    // Eliminar el peso de la lista local
                    pesosList = emptyList()
                    // Actualizar el spinner
                    val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                    val idGalpon =
                        galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull()
                            ?: 0
                    val idNucleo = binding.selectEstablecimiento.selectedItemPosition
                    limpiarCampos()
                    limpiarClientes()
                    updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al eliminar el peso",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                requireContext(),
                "No se encontró el peso, por favor intente de nuevo",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("SpinnerData", "No se encontró el registro con ID $idPeso")
        }
    }

    fun showDialogLimpiarCliente() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("¿Que acción quiere realizar?")

        // Establecer los botones en el diálogo
        builder.setPositiveButton("Limpiar") { dialog, which ->
            // Limpiar los campos de entrada
            binding.textDocCli.setText("")
            binding.textNomCli.setText("")
            binding.textDocCli.isEnabled = true
            binding.textNomCli.isEnabled = true
            binding.botonCliente.isEnabled = true

            // Colocar el foco en el campo Número de Documento
            binding.textDocCli.requestFocus()

            // Abrir el teclado
            Handler(Looper.getMainLooper()).postDelayed({
                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(
                    binding.textDocCli,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }, 100)
        }

        builder.setNeutralButton("Editar") { dialog, which ->
            // Habilitar solo el campo de Razón Social (Nombre)
            binding.textNomCli.isEnabled = true
        }

        builder.setNegativeButton("Cancelar") { dialog, which ->
            // Cerrar el diálogo sin realizar ninguna acción
            dialog.dismiss()
        }

        // Crear y mostrar el diálogo
        val dialog = builder.create()
        dialog.show()
    }

    fun limpiarClientes() {
        binding.textNomCli.setText("")
        binding.textDocCli.setText("")

        binding.textDocCli.isEnabled = true
        binding.textNomCli.isEnabled = true
        binding.botonCliente.isEnabled = true
    }

    fun limpiarCampos() {
        updatePesoStatus(idPesoShared, "NotUsed")
        idPesoShared = 0
        idPesoTemp = 0
        binding.selectListpesos.setSelection(0)
        binding.idListPeso.text = ""
        idGalpoListaPesos = ""
        binding.inputPesoKg.text?.clear()
        binding.checkboxConPollos.isChecked = false
        binding.botonEnviar.isEnabled = false
        binding.botonEnviar.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.gray)
        binding.botonEnviar.setImageResource(R.drawable.null_24)
        binding.botonGuardar.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
        binding.botonGuardar.isEnabled = true

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
        binding.botonGuardarPeso.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
        binding.botonGuardarPeso.setImageResource(R.drawable.baseline_add_24)

        // Limpiar Tabla
        jabasList.clear()
        jabasAdapter.notifyDataSetChanged()
        sharedViewModel.setContadorJabas(0)
        _binding?.contadorJabas?.text = "= 0"

        calcularTotalPagar()
    }

    private fun limpiarTablaJabas() {
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
            limpiarCampos()
            limpiarClientes()
        }
        builder.setNegativeButton("Cancelar", null)
        val dialog = builder.create()
        dialog.show()
    }

    private fun distribuirDatosEnInputs(dataPesoPollos: JSONObject) {
        val docCliente = dataPesoPollos.optString("_PP_docCliente", "")
        val nombreCliente = dataPesoPollos.optString("_PP_nombreCompleto", "")
        val precioKilo = dataPesoPollos.optString("_PP_PKPollo", "")

        binding.textDocCli.setText(docCliente)
        binding.textNomCli.setText(nombreCliente)
        binding.PrecioKilo.setText(precioKilo)

        binding.textDocCli.isEnabled = false
        binding.textNomCli.isEnabled = false
        binding.botonCliente.isEnabled = false

        // Deshabilitar los campos solo si tienen datos
        if (docCliente.isEmpty() || nombreCliente.isEmpty() || (docCliente.isEmpty() || nombreCliente.isEmpty())) {
            binding.textDocCli.isEnabled = true
            binding.textNomCli.isEnabled = true
            binding.botonCliente.isEnabled = true
        }
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
                tipo = jsonObject.getString("_DPP_tipo"),
                idPesoPollo = jsonObject.optString("_DPP_idPesoPollo", ""),
                fechaPeso = jsonObject.optString("_DPP_fechaPeso", "")
            )
            detalles.add(detalle)
        }
        return detalles
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseUrl = Constants.getBaseUrl()
        dataSyncManager = DataSyncManager(requireContext())
        db = AppDatabase(requireContext())
        var isLoggedIn = true
        // Inicializa y registra el receptor de cambios en la red
        networkChangeReceiver = NetworkChangeReceiver { isConnected ->
            if (isConnected) {
//                dataSyncManager.checkSincronizarData(baseUrl, isLoggedIn, this) { success ->
//                    if (success) {
                //
//                    }
//                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 o superior
            checkNotificationPermission()
        }

        requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    enableBluetoothFeatures()
                } else {
                    showPermissionDeniedDialog()
                }
            }

        // Verificar permisos de Bluetooth al crear el fragmento
        checkBluetoothPermissions { arePermissionsGranted ->
            if (arePermissionsGranted) {
                enableBluetoothFeatures()
            }
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


    ////////////////////////////////////////////////////////////////////////////////////////
    //BLUETOOTH PRINCIPAL PERMISOS
    ////////////////////////////////////////////////////////////////////////////////////////
    private fun checkBluetoothPermissions(onPermissionsChecked: (Boolean) -> Unit) {
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        if (bluetoothPermissions.all { permission ->
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            onPermissionsChecked(true)
        } else {
            // Mostrar el diálogo de explicación de permisos primero
            showBluetoothPermissionExplanationDialog(bluetoothPermissions, onPermissionsChecked)
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permisos Necesarios")
            .setMessage("Los permisos de Bluetooth son necesarios para usar esta función. Por favor, habilítelos en la configuración de la aplicación.")
            .setPositiveButton("Ir a Configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showBluetoothPermissionExplanationDialog(
        permissions: Array<String>,
        onPermissionsChecked: (Boolean) -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permisos de Bluetooth Requeridos")
            .setMessage("Esta aplicación necesita permisos de Bluetooth para funcionar correctamente. ¿Desea concederlos?")
            .setPositiveButton("Conceder") { _, _ ->
                requestMultiplePermissions.launch(permissions)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                onPermissionsChecked(false)
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun enableBluetoothFeatures() {
        // Habilitar características de Bluetooth aquí
    }
    ////////////////////////////////////////////////////////////////////////////////////////

    var idPesoTemp = 0
    var dataPesoPollosJsonTemp = ""
    var dataDetaPesoPollosJsonTemp = ""
    override fun onResume() {
        super.onResume()

        // Registra el receptor para cambios en la conectividad
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkChangeReceiver, filter)

        binding.botonDeletePeso.visibility = View.INVISIBLE

        if (idPesoShared == 0) {
            sharedViewModel.setIdListPesos(idPesoTemp)
            idPesoShared = sharedViewModel.getIdListPesos() ?: 0
        } else {
            val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
            val idGalpon =
                galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
            val idNucleo = binding.selectEstablecimiento.selectedItemPosition
            updateSpinnerPesosIdGalpon(idNucleo, idGalpon)
        }

        // Inicia la verificación periódica de la conexión a Internet
        handler.post(checkInternetRunnable)
    }

    override fun onStop() {
        super.onStop()

        idPesoTemp = sharedViewModel.getIdListPesos() ?: 0
        dataPesoPollosJsonTemp = sharedViewModel.getDataPesoPollosJson() ?: ""
        dataDetaPesoPollosJsonTemp = sharedViewModel.getDataDetaPesoPollosJson() ?: ""
        sharedViewModel.setDataPesoPollosJson(dataPesoPollosJsonTemp)
        sharedViewModel.setDataDetaPesoPollosJson(dataDetaPesoPollosJsonTemp)
        sharedViewModel.setIdListPesos(idPesoTemp)

    }

    override fun onPause() {
        super.onPause()
        // Desregistra el receptor para evitar fugas de memoria
        requireContext().unregisterReceiver(networkChangeReceiver)

        val device = getDeviceId(requireContext())

        idPesoTemp = sharedViewModel.getIdListPesos() ?: 0
        dataPesoPollosJsonTemp = sharedViewModel.getDataPesoPollosJson() ?: ""
        dataDetaPesoPollosJsonTemp = sharedViewModel.getDataDetaPesoPollosJson() ?: ""

        updatePesoStatus(idPesoTemp, "Used")
        if (!navegationTrue) {
            // SI NAVEGA A OTRA VENTANA DEL MENU
            if (idPesoTemp != 0) {
                if (!dataPesoPollosJsonTemp.isNullOrBlank() && !dataDetaPesoPollosJsonTemp.isNullOrBlank()) {
                    val pesoUsed = pesoUsedEntity(
                        idPesoUsed = idPesoTemp,
                        devicedName = device,
                        dataPesoPollosJson = dataPesoPollosJsonTemp,
                        dataDetaPesoPollosJson = dataDetaPesoPollosJsonTemp,
                        fechaRegistro = ""
                    )
                    db.addPesoUsed(pesoUsed)
                } else {
                    val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                    val idGalpon =
                        galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull()
                            ?: 0
                    val idNucleo = binding.selectEstablecimiento.selectedItemPosition

                    val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
                    val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                        id = idPesoTemp,
                        serie = "",
                        numero = "",
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
                        totalPagar = binding.totalPagarPreview.text.toString(),
                        idUsuario = "",
                        idEstado = "0",
                    )

                    dataPesoPollosJson = dataPesoPollos.toJson().toString()
                    dataDetaPesoPollosJson =
                        JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()

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
                            idEstado = "1",
                            isSync = "",
                            serieDevice = "",
                            devicedName = device,
                            fechaRegistro = ""
                        )
                        updateListPesos(
                            requireContext(),
                            this@JabasFragment,
                            pesosEntity,
                            idPesoTemp
                        )
                    }
                }
            } else {
                val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
                val idGalpon =
                    galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull()
                        ?: 0
                val idNucleo = binding.selectEstablecimiento.selectedItemPosition

                val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
                val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                    id = idPesoTemp,
                    serie = "",
                    numero = "",
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
                    totalPagar = binding.totalPagarPreview.text.toString(),
                    idUsuario = "",
                    idEstado = "0",
                )

                dataPesoPollosJsonTemp = dataPesoPollos.toJson().toString()
                dataDetaPesoPollosJsonTemp =
                    JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()

                val pesoUsed = pesoUsedEntity(
                    idPesoUsed = idPesoTemp,
                    devicedName = device,
                    dataPesoPollosJson = dataPesoPollosJsonTemp,
                    dataDetaPesoPollosJson = dataDetaPesoPollosJsonTemp,
                    fechaRegistro = ""
                )
                db.addPesoUsed(pesoUsed)
            }
        } else {
            // Si se redirige a preliminar
            val galponNombreSeleccionado = binding.selectGalpon.selectedItem.toString()
            val idGalpon =
                galponIdMap.filterValues { it == galponNombreSeleccionado }.keys.firstOrNull() ?: 0
            val idNucleo = binding.selectEstablecimiento.selectedItemPosition
            val dataDetaPesoPollos = ManagerPost.captureData(jabasList)
            val dataPesoPollos = ManagerPost.captureDataPesoPollos(
                id = idPesoTemp,
                serie = "",
                numero = "",
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
                totalPagar = binding.totalPagarPreview.text.toString(),
                idUsuario = "",
                idEstado = "0",
            )

            dataPesoPollosJson = dataPesoPollos.toJson().toString()
            dataDetaPesoPollosJson = JSONArray(dataDetaPesoPollos.map { it.toJson() }).toString()

            if (!dataPesoPollosJson.isNullOrBlank() && !dataDetaPesoPollosJson.isNullOrBlank()) {
                val pesoUsed = pesoUsedEntity(
                    idPesoUsed = idPesoTemp,
                    devicedName = device,
                    dataPesoPollosJson = dataPesoPollosJsonTemp,
                    dataDetaPesoPollosJson = dataDetaPesoPollosJsonTemp,
                    fechaRegistro = ""
                )
                db.addPesoUsed(pesoUsed)
            }

            CoroutineScope(Dispatchers.Main).launch {
                val pesosEntity = PesosEntity(
                    id = 0,
                    idNucleo = idNucleo,
                    idGalpon = idGalpon,
                    numeroDocCliente = dataPesoPollos.numeroDocCliente,
                    nombreCompleto = dataPesoPollos.nombreCompleto,
                    dataPesoJson = dataPesoPollosJson!!,
                    dataDetaPesoJson = dataDetaPesoPollosJson!!,
                    idEstado = "1",
                    isSync = "",
                    serieDevice = "",
                    devicedName = device,
                    fechaRegistro = ""
                )
                updateListPesos(requireContext(), this@JabasFragment, pesosEntity, idPesoTemp)
            }
        }

        sharedViewModel.setDataPesoPollosJson(dataPesoPollosJsonTemp)
        sharedViewModel.setDataDetaPesoPollosJson(dataDetaPesoPollosJsonTemp)
        sharedViewModel.setIdListPesos(idPesoTemp)
        // Detiene la verificación periódica
        handler.removeCallbacks(checkInternetRunnable)
    }

    fun updatePesoStatus(id: Int, status: String) {
        if (id != 0) {
            val idDevice = getDeviceId(requireContext())
            setStatusUsed(requireContext(), id, "$status", idDevice) { success ->
                if (success) {
                    db.deleteAllPesoUsed()
                } else {
                    Log.d("StatusLog", "Error al cambiar el estado del peso")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        coroutineScope.cancel()
        db.close()
    }

    override fun onItemDeleted(id: Int) {
        // Find the position of the item to be deleted
        val position = jabasList.indexOfFirst { it.id == id }
        if (position != -1) {
            // Remove the item from the data source
            jabasAdapter.deleteItem(position)
            // Update the UI to reflect the changes
            updateUI()
        }
    }

    override fun onItemAdd() {
        updateUI()
    }

    private var isJabasInputValid = true
    private var currentInputJabas = 0

    private fun updateUI() {
        val nuevoContador = sharedViewModel.getContadorJabas() ?: run {
//            showCustomToast(requireContext(), "Error al obtener el contador de jabas", "error")
            return
        }
        val isEditMode = !sharedViewModel.getDataPesoPollosJson().isNullOrBlank()
        val isConPollos = binding.checkboxConPollos.isChecked

        _binding?.contadorJabas?.text = "= $nuevoContador"

        if (nuevoContador != 0) {
            validateInputJabas(nuevoContador)
        }
        updateSaveButton(nuevoContador, isEditMode, isConPollos)
        updateSendButton(nuevoContador, isEditMode)
        showToastMessage(nuevoContador)
        calcularTotalPagar()

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
            if (isEnabled) R.color.purple_500 else R.color.gray
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
                showCustomToast(
                    requireContext(),
                    "No puedes usar jabas mayores a las que tienes",
                    "error"
                )
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
            contador < 0 -> showCustomToast(
                requireContext(),
                "Te Faltan ${contador} Jabas por registrar!",
                "info"
            )

            contador == 0 && jabasList.isEmpty() -> showCustomToast(
                requireContext(),
                "No hay jabas registradas",
                "info"
            )

            contador == 0 -> showCustomToast(
                requireContext(),
                "Ya no hay Jabas por utilizar",
                "success"
            )

            contador > 0 -> showCustomToast(
                requireContext(),
                "Tienes $contador Jabas por utilizar",
                "info"
            )
        }
    }

    private fun showKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)

        if (view is EditText) {
            view.setSelection(view.text.length)
        }
    }

    // Para mostrar el ProgressBar y el fondo bloqueado
    fun showLoading() {
        _binding?.let { binding ->
            val overlay = binding.overlay.findViewById<View>(R.id.overlay)
            val loadingGif = binding.loadingGif.findViewById<ImageView>(R.id.loadingGif)
            overlay.visibility = View.VISIBLE
            loadingGif.visibility = View.VISIBLE

            loadingGif.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.purple_500), // Color deseado
                PorterDuff.Mode.SRC_IN // Modo de mezcla
            )

            // Cargar el GIF usando Glide
            Glide.with(this)
                .asGif()
                .load(R.drawable.icon_loader)
                .into(loadingGif)
        }
    }

    // Para ocultar el ProgressBar y el fondo bloqueado
    fun hideLoading() {
        _binding?.let { binding ->
            val overlay = binding.overlay.findViewById<View>(R.id.overlay)
            val loadingGif = binding.loadingGif.findViewById<ImageView>(R.id.loadingGif)
            overlay.visibility = View.GONE
            loadingGif.visibility = View.GONE

            // Detener la animación del GIF
            Glide.with(this).clear(loadingGif)
        }
    }


    fun toggleAcordionisGone() {
        if (binding.accordionContent.visibility == View.GONE) {
            binding.accordionContent.visibility = View.VISIBLE
            binding.arrow.setImageResource(R.drawable.ic_arrow_up)
        }
    }

    fun toggleAcordionnotGone() {
        if (binding.accordionContent.visibility != View.GONE) {
            binding.accordionContent.visibility = View.GONE
            binding.arrow.setImageResource(R.drawable.ic_arrow_down)
        }
    }

    private fun fetchData(time: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            hideLoading()
        }, time)
    }

    override fun onProgressUpdate(message: String) {
        //
    }
}