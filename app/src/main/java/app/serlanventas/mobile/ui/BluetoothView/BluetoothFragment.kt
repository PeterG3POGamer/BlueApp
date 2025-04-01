package app.serlanventas.mobile.ui.BluetoothView

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.fragment.findNavController
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentBluetoothBinding
import app.serlanventas.mobile.ui.Services.Logger
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel
import app.serlanventas.mobile.ui.slideshow.BluetoothConnectionService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aflak.bluetooth.Bluetooth
import me.aflak.bluetooth.interfaces.BluetoothCallback
import me.aflak.bluetooth.interfaces.DeviceCallback
import me.aflak.bluetooth.interfaces.DiscoveryCallback

@Suppress("DEPRECATION")
class BluetoothFragment : Fragment() {

    private var bluetoothDialog: AlertDialog? = null
    private var _binding: FragmentBluetoothBinding? = null
    private val REQUEST_ENABLE_BT = 1

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_CHECK_SETTINGS = 0x1

    private val TAG = "BluetoothFragment"
    private var currentToast: Toast? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var logger: Logger

    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var bluetooth: Bluetooth? = null
    private var devicesAdapter: DeviceListAdapter? = null
    private var bluetoothDevices: MutableList<BluetoothDevice?> = ArrayList()

    private lateinit var bluetoothConnectionService: BluetoothConnectionService

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val requestEnableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            initializeBluetoothAdapterAndServer()
        } else {
            showToast("El Bluetooth debe estar habilitado para continuar")
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!bluetoothDevices.contains(it)) {
                            bluetoothDevices.add(it)
                            devicesAdapter?.notifyDataSetChanged()
                        }
                    }
                    // Actualizar ProgressBar del diálogo
                    bluetoothDialog?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.GONE
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Actualizar ProgressBar del diálogo
                    bluetoothDialog?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.GONE
                    showToast("Búsqueda de dispositivos finalizada")
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            showToast("Bluetooth desactivado")
                            updateUIForBluetoothOff()
                        }
                        BluetoothAdapter.STATE_ON -> {
                            showToast("Bluetooth activado")
                            updateUIForBluetoothOn()
                            startDiscovery()
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                        BluetoothDevice.BOND_BONDED -> {
                            logger.log("onReceive: Dispositivo emparejado: ${device?.name}")
                            connectToDevice(device!!)
                        }
                        BluetoothDevice.BOND_NONE -> {
                            logger.log("onReceive: Dispositivo desemparejado: ${device?.name}")
                        }
                    }
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var checkProgressBarRunnable: Runnable


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        val root: View = binding.root

        logger = Logger(requireContext())

        sharedViewModel.pesoValue.distinctUntilChanged().observe(viewLifecycleOwner) { message ->
            handleReceivedMessage(message)
        }

        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }

    private fun setupBluetoothCallbacks() {
        bluetooth!!.setBluetoothCallback(object : BluetoothCallback {
            override fun onBluetoothTurningOn() {
                updateUIForBluetoothOn()
            }

            override fun onBluetoothTurningOff() {
                updateUIForBluetoothOff()
            }

            override fun onBluetoothOff() {
                updateUIForBluetoothOff()
            }

            override fun onBluetoothOn() {
                updateUIForBluetoothOn()
            }

            override fun onUserDeniedActivation() {
            }
        })

        bluetooth!!.setDiscoveryCallback(object : DiscoveryCallback {
            override fun onDiscoveryStarted() {
                bluetoothDialog?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.VISIBLE
            }

            override fun onDiscoveryFinished() {
                bluetoothDialog?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.GONE
                handler.removeCallbacks(checkProgressBarRunnable)
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceFound(device: BluetoothDevice) {
                val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    device.name ?: device.alias ?: "Desconocido"
                } else {
                    device.name ?: "Desconocido"
                }

                if (bluetoothDevices.none { it?.address == device.address }) {
                    bluetoothDevices.add(device)
                    devicesAdapter?.notifyDataSetChanged()

                    Log.d(TAG, "Device found: $deviceName (${device.address})")
                    logger.log("setDiscoveryCallback: Device found: $deviceName (${device.address})")
                }
            }

            @SuppressLint("MissingPermission")
            override fun onDevicePaired(device: BluetoothDevice) {
                showToast("Emparejado con ${device.name}")
                Log.d(TAG, "Device paired: ${device.name}")
                logger.log("setDiscoveryCallback: Device paired: ${device.name}")
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceUnpaired(device: BluetoothDevice) {
            }

            override fun onError(errorCode: Int) {
                showToast("Error: $errorCode")
                Log.e(TAG, "Discovery error: $errorCode")
                logger.log("setDiscoveryCallback: Discovery error: $errorCode")
                bluetoothDialog?.dismiss()
                handler.removeCallbacks(checkProgressBarRunnable)
            }
        })

        bluetooth!!.setDeviceCallback(object : DeviceCallback {
            @SuppressLint("MissingPermission")
            override fun onDeviceConnected(device: BluetoothDevice) {
                showToast("Conectado a ${device.name}")
                Log.d(TAG, "Device connected: ${device.name}")
                logger.log("setDeviceCallback: Device connected: ${device.name}")

                // Actualizar nombre del dispositivo en el diálogo
                bluetoothDialog?.findViewById<TextView>(R.id.device_name)?.text =
                    "Conectado a: ${device.name}"

                updateUIForBluetoothOn()
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceDisconnected(device: BluetoothDevice, message: String) {
                showToast("Desconectado de ${device.name}")
                Log.d(TAG, "Device disconnected: ${device.name}. Message: $message")
                logger.log("setDeviceCallback: Device disconnected: ${device.name}. Message: $message")

                // Actualizar nombre del dispositivo en el diálogo
                bluetoothDialog?.findViewById<TextView>(R.id.device_name)?.text =
                    "No conectado"

                updateUIForBluetoothOff()
            }

            override fun onMessage(message: ByteArray) {
                val receivedMessage = String(message)
                showToast("Mensaje recibido: $receivedMessage")
                Log.d(TAG, "Message received: $receivedMessage")
                logger.log("setDeviceCallback: Message received: $receivedMessage")
                bluetoothDialog?.dismiss()
            }

            override fun onError(errorCode: Int) {
                showToast("Error: $errorCode")
                Log.e(TAG, "Device error: $errorCode")
                logger.log("setDeviceCallback: Device error: $errorCode")
                bluetoothDialog?.dismiss()
            }

            @SuppressLint("MissingPermission")
            override fun onConnectError(device: BluetoothDevice, message: String) {
                showToast("Error de conexión: $message")
                Log.e(TAG, "Connection error with ${device.name}: $message")
                logger.log("setDeviceCallback: Connection error with ${device.name}: $message")
                bluetoothDialog?.dismiss()
            }
        })
    }

    private fun checkBluetoothPermissions() {
        if (isAdded && context != null) {
            if (hasBluetoothPermissions()) {
                initializeBluetoothAdapterAndServer()
            } else {
                requestBluetoothPermissions()
            }
        } else {
            Log.e("BluetoothFragment", "Fragmento no adjunto a una actividad")
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        val context = context ?: return false
        return bluetoothPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            initializeBluetoothAdapterAndServer()
        } else {
            showToast("Se requieren permisos para usar Bluetooth")
        }
    }

    private fun requestBluetoothPermissions() {
        if (isAdded && context != null) {
            if (!hasBluetoothPermissions()) {
                // Mostrar un diálogo explicando por qué se necesitan los permisos de Bluetooth
                AlertDialog.Builder(requireContext())
                    .setTitle("Permisos de Bluetooth Requeridos")
                    .setMessage("Esta aplicación necesita permisos de Bluetooth para funcionar correctamente. ¿Desea concederlos?")
                    .setPositiveButton("Conceder") { _, _ ->
                        // Solicitar permisos de Bluetooth
                        requestMultiplePermissions.launch(bluetoothPermissions)
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        // Manejar la negativa del usuario
                        showToast("Permisos de Bluetooth no concedidos")
                        dialog?.dismiss()
                    }
                    .setOnDismissListener {
                        // Manejar el cierre del diálogo
                        showToast("Diálogo de permisos cerrado")
                    }
                    .setCancelable(false)
                    .show()
            } else {
                // Permisos ya concedidos, proceder con la inicialización de Bluetooth
                initializeBluetoothAdapterAndServer()
            }
        } else {
            Log.e("BluetoothFragment", "Fragmento no adjunto a una actividad")
        }
    }


    private fun initializeBluetoothAdapterAndServer() {
        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            showToast("Este dispositivo no soporta Bluetooth")
            logger.log("initializeBluetoothAdapterAndServer: Este dispositivo no soporta Bluetooth")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)
        } else {
            setupBluetoothService()
        }
    }

    private fun setupBluetoothService() {
        if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            bluetoothConnectionService = BluetoothConnectionService(requireContext(), bluetoothAdapter) { message ->
                sharedViewModel.updatePesoValue(message.processedValue)
                sharedViewModel.updateRawData(message.rawData)
            }
            bluetoothConnectionService.startServer()
            startDiscovery()
        }
    }

    private fun setupRecyclerView(view: View) {
        val listDevices = view.findViewById<ListView>(R.id.list_devices)
        devicesAdapter = DeviceListAdapter(requireContext(), bluetoothDevices)
        listDevices.adapter = devicesAdapter

        listDevices.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = bluetoothDevices[position]
            selectedDevice?.let { device ->
                pairAndConnectDevice(device)
            }
        }
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun startDiscovery() {
        if (::bluetoothAdapter.isInitialized) {
            bluetoothDevices.clear()
            devicesAdapter?.notifyDataSetChanged()

            // Mostrar ProgressBar
            bluetoothDialog?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility =
                View.VISIBLE

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
            showToast("Buscando dispositivos Bluetooth...")
        }else {
            // Maneja el caso cuando el adaptador no está inicializado
            Log.e("BluetoothFragment", "Bluetooth adapter not initialized")
        }
    }

    @SuppressLint("MissingPermission")
    private fun pairAndConnectDevice(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            connectToDevice(device)
        } else {
            try {
                device.createBond()
                showToast("Emparejando con ${device.name}")

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("No se pudo emparejar con ${device.name}")
                logger.log("pairAndConnectDevice: No se pudo emparejar con ${device.name}, $e")
                bluetoothDialog?.dismiss()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        coroutineScope.launch {
            try {
                Log.d("Bluetooth", "Intentando conectar con: ${device.name}")
                logger.log("connectToDevice: Intentando conectar con: ${device.name}")
                bluetoothConnectionService.connect(device)

                withContext(Dispatchers.Main) {
                    binding.deviceName.text = "Conectado a: ${device.name}"
                    sharedViewModel.updateConnectedDeviceName(device.name)
                    sharedViewModel.updateConnectedDeviceAddress(device.address.toString())
                    showToast("Conectado a ${device.name}")

                    bluetoothDialog?.hide()
                    // Redirigir al tab 1 solo si la conexión fue exitosa
                    findNavController().navigate(R.id.nav_initReportePeso)
//                    tabViewModel.setNavigateToTab(1)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Bluetooth", "Error al conectar: ${e.message}", e)
                    logger.log("connectToDevice: Error al conectar: ${e.message}", e)
                    binding.deviceName.text = "Error de conexión"
                    showToast("Error al conectar: ${e.message}")
                    bluetoothDialog?.dismiss()
                }
            }
        }
    }

    private fun handleReceivedMessage(message: String) {
        sharedViewModel.updatePesoValue(message)
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun checkGPSStatus() {
        if (!isGPSEnabled()) {
            requestGPSActivation()
        } else {
            Log.d(TAG, "GPS está activado")
        }
    }

    private fun requestGPSActivation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // GPS está activado o el usuario acaba de activarlo
            Log.d(TAG, "GPS activado exitosamente")
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // La ubicación no está activada y hay una resolución disponible
                try {
                    // Muestra el diálogo solicitando activar el GPS
                    exception.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignorar el error
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        requireContext().registerReceiver(receiver, filter)

        checkGPSStatus()
    }

    private fun toggleBluetooth() {
        if (isAdded && context != null) {
            if (bluetooth!!.isEnabled) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetooth!!.disable()
                } else {
                    requestBluetoothPermissions()
                }
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetooth!!.enable()
                } else {
                    requestBluetoothPermissions()
                }
            }
        }
    }


    private fun updateUIForBluetoothState() {
        if (hasBluetoothPermissions()) {
            if (bluetooth!!.isEnabled) {
                updateUIForBluetoothOn()
            } else {
                updateUIForBluetoothOff()
            }
        } else {
            // Si no se tienen los permisos, solicitar permisos
            requestBluetoothPermissions()
        }
    }


    private fun updateUIForBluetoothOn() {
        bluetoothDialog?.let { dialog ->
            dialog.findViewById<ImageButton>(R.id.btn_toggle_bluetooth)?.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.button_background_active)
            dialog.findViewById<ImageButton>(R.id.btn_scan)?.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.button_background_active_reload)
            dialog.findViewById<ImageButton>(R.id.btn_scan)?.isEnabled = true
        }
    }

    private fun updateUIForBluetoothOff() {
        bluetoothDialog?.let { dialog ->
            dialog.findViewById<ImageButton>(R.id.btn_toggle_bluetooth)?.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive)
            dialog.findViewById<ImageButton>(R.id.btn_scan)?.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive_reload)
            dialog.findViewById<ImageButton>(R.id.btn_scan)?.isEnabled = false
            bluetoothDevices.clear()
            devicesAdapter?.notifyDataSetChanged()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (bluetooth!!.isEnabled) {
                updateUIForBluetoothOn()
            } else {
                updateUIForBluetoothOff()
            }
        }

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d(TAG, "El usuario activó el GPS")
                    // El GPS fue activado, puedes continuar con tu lógica aquí
                }
                Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "El usuario no activó el GPS")
                    // El usuario decidió no activar el GPS, puedes manejarlo aquí
                }
            }
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            context?.let {
                currentToast?.cancel()
                currentToast = Toast.makeText(it, message, Toast.LENGTH_SHORT)
                currentToast?.show()
            }
        }
    }

    fun showBluetoothDialog(context: Context) {
        if (isAdded) {
            if (bluetoothDialog == null) {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.fragment_bluetooth, null)
                val builder = AlertDialog.Builder(context)
                builder.setView(dialogView)

                bluetoothDialog = builder.create()
                bluetoothDialog?.setOnCancelListener {
                    // Handle dialog cancellation if needed
                }

                // Initialize all Bluetooth components here
                initializeBluetoothComponents(dialogView)
            }

            bluetoothDialog?.show()
        } else {
            Log.e("BluetoothFragment", "Fragment not attached to FragmentManager")
        }
    }

    private fun initializeBluetoothComponents(dialogView: View) {
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize Bluetooth
        bluetooth = Bluetooth(requireContext())
        bluetooth?.onStart()

        // Setup progress bar runnable
        checkProgressBarRunnable = object : Runnable {
            override fun run() {
                if (isAdded && dialogView != null &&
                    dialogView.findViewById<ProgressBar>(R.id.progress_bar).visibility == View.VISIBLE) {
                    checkBluetoothPermissions()
                }
            }
        }

        // Set initial progress bar visibility
        dialogView.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE

        // Setup UI components
        setupDialogUI(dialogView)

        // Setup Bluetooth callbacks
        setupBluetoothCallbacks()

        // Check permissions
        checkBluetoothPermissions()
    }

    private fun setupDialogUI(view: View) {
        val btnToggleBluetooth = view.findViewById<ImageButton>(R.id.btn_toggle_bluetooth)
        val btnScan = view.findViewById<ImageButton>(R.id.btn_scan)
        val btnCloseModal = view.findViewById<ImageButton>(R.id.btn_close_modal)
        val listDevices = view.findViewById<ListView>(R.id.list_devices)
        val deviceName = view.findViewById<TextView>(R.id.device_name)

        // Initialize devices adapter
        devicesAdapter = DeviceListAdapter(requireContext(), bluetoothDevices)
        listDevices.adapter = devicesAdapter

        listDevices.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = bluetoothDevices[position]
            selectedDevice?.let { device ->
                pairAndConnectDevice(device)
            }
        }

        btnToggleBluetooth.setOnClickListener {
            toggleBluetooth()
        }

        btnScan.setOnClickListener {
            checkBluetoothPermissions()
        }

        btnCloseModal.setOnClickListener {
            handler.removeCallbacks(checkProgressBarRunnable)
            bluetoothDialog?.dismiss()
        }

        handler.postDelayed(checkProgressBarRunnable, 3000)
    }

    override fun onPause() {
        super.onPause()
        bluetoothDialog?.dismiss()
    }
}