package com.example.blueapp.ui.BluetoothView

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.blueapp.R
import com.example.blueapp.databinding.FragmentBluetoothBinding
import com.example.blueapp.ui.Services.Logger
import com.example.blueapp.ui.ViewModel.SharedViewModel
import com.example.blueapp.ui.ViewModel.TabViewModel
import com.example.blueapp.ui.slideshow.BluetoothConnectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aflak.bluetooth.Bluetooth
import me.aflak.bluetooth.interfaces.BluetoothCallback
import me.aflak.bluetooth.interfaces.DeviceCallback
import me.aflak.bluetooth.interfaces.DiscoveryCallback

class BluetoothFragment : DialogFragment() {

    private var _binding: FragmentBluetoothBinding? = null
    private val REQUEST_ENABLE_BT = 1
    private val TAG = "BluetoothFragment"
    private var currentToast: Toast? = null


    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var logger: Logger

    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
//    private lateinit var devicesAdapter: DevicesAdapter

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

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            initializeBluetoothAdapterAndServer()
        } else {
            showToast("Se requieren permisos para usar Bluetooth")
        }
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
                    // Ocultar ProgressBar cuando se encuentra un dispositivo
                    binding.progressBar.visibility = View.GONE
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Ocultar ProgressBar cuando finaliza la búsqueda
                    binding.progressBar.visibility = View.GONE
                    showToast("Búsqueda de dispositivos finalizada")
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            showToast("Bluetooth desactivado")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            showToast("Bluetooth activado")
                            startDiscovery()
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                        BluetoothDevice.BOND_BONDED -> {
                            Log.d("Bluetooth", "Dispositivo emparejado: ${device?.name}")
                            logger.log("onReceive: Dispositivo emparejado: ${device?.name}")
                            device?.let { connectToDevice(it) }
                        }
                        BluetoothDevice.BOND_NONE -> {
                            Log.d("Bluetooth", "Dispositivo desemparejado: ${device?.name}")
                            logger.log("onReceive: Dispositivo desemparejado: ${device?.name}")
                        }
                    }
                }
            }
        }
    }

    private lateinit var tabViewModel: TabViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        val root: View = binding.root

        logger = Logger(requireContext())
        tabViewModel = ViewModelProvider(requireActivity()).get(TabViewModel::class.java)

        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bluetooth = Bluetooth(requireContext())
        bluetooth?.onStart()

        binding.progressBar.visibility = View.GONE

        setupRecyclerView()
        checkBluetoothPermissions()
        setupBluetoothCallbacks()
        setupUI()

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
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onDiscoveryFinished() {
                binding.progressBar.visibility = View.GONE
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceFound(device: BluetoothDevice) {
                if (bluetoothDevices.none { it?.address == device.address }) {
                    bluetoothDevices.add(device)
                    devicesAdapter!!.notifyDataSetChanged()
                    Log.d(TAG, "Device found: ${device.name} (${device.address})")
                    logger.log("setDiscoveryCallback: Device found: ${device.name} (${device.address})")
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
            }
        })

        bluetooth!!.setDeviceCallback(object : DeviceCallback {
            @SuppressLint("MissingPermission")
            override fun onDeviceConnected(device: BluetoothDevice) {
                showToast("Conectado a ${device.name}")
                Log.d(TAG, "Device connected: ${device.name}")
                logger.log("setDeviceCallback: Device connected: ${device.name}")
                updateUIForConnectedState()
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceDisconnected(device: BluetoothDevice, message: String) {
                showToast("Desconectado de ${device.name}")
                Log.d(TAG, "Device disconnected: ${device.name}. Message: $message")
                logger.log("setDeviceCallback: Device disconnected: ${device.name}. Message: $message")
                updateUIForDisconnectedState()
            }

            override fun onMessage(message: ByteArray) {
                val receivedMessage = String(message)
                showToast("Mensaje recibido: $receivedMessage")
                Log.d(TAG, "Message received: $receivedMessage")
                logger.log("setDeviceCallback: Message received: $receivedMessage")
            }

            override fun onError(errorCode: Int) {
                showToast("Error: $errorCode")
                Log.e(TAG, "Device error: $errorCode")
                logger.log("setDeviceCallback: Device error: $errorCode")
            }

            @SuppressLint("MissingPermission")
            override fun onConnectError(device: BluetoothDevice, message: String) {
                showToast("Error de conexión: $message")
                Log.e(TAG, "Connection error with ${device.name}: $message")
                logger.log("setDeviceCallback: Connection error with ${device.name}: $message")
            }
        })
    }

    private fun setupUI() {
        devicesAdapter = DeviceListAdapter(context, bluetoothDevices)
        binding.listDevices.adapter = devicesAdapter

        binding.btnScan.setOnClickListener {
            startDiscovery()
            checkBluetoothPermissions()
        }

        binding.btnToggleBluetooth.setOnClickListener {
            toggleBluetooth()
        }

//        binding.listDevices.setOnItemClickListener { _, _, position, _ ->
//            val device = bluetoothDevices[position]
//            if (device != null) {
//                connectToDevice(device)
//            }
//        }

        // Verificar el estado del Bluetooth al configurar la UI
        updateUIForBluetoothState()
    }

    private fun checkBluetoothPermissions() {
        if (hasBluetoothPermissions()) {
            initializeBluetoothAdapterAndServer()
        } else {
            requestMultiplePermissions.launch(bluetoothPermissions)
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return bluetoothPermissions.all { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
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
        bluetoothConnectionService = BluetoothConnectionService(requireContext(), bluetoothAdapter) { message ->
            handleReceivedMessage(message)
        }
        bluetoothConnectionService.startServer()
        startDiscovery()
    }

    private fun setupRecyclerView() {
        // Configura el adaptador para el ListView
        devicesAdapter = DeviceListAdapter(requireContext(), bluetoothDevices)
        binding.listDevices.adapter = devicesAdapter

        // Configura la acción de clic en un dispositivo
        binding.listDevices.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = bluetoothDevices[position]
            selectedDevice?.let { device ->
                pairAndConnectDevice(device)
            }
        }
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun startDiscovery() {
        bluetoothDevices.clear()
        devicesAdapter?.notifyDataSetChanged()

        // Mostrar ProgressBar
        binding.progressBar.visibility = View.VISIBLE

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
        showToast("Buscando dispositivos Bluetooth...")
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

                    dismiss()
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
                }
            }
        }
    }

    private fun handleReceivedMessage(message: String) {
        sharedViewModel.updatePesoValue(message)
        Log.d("BluetoothFragment", "Peso recibido: $message")
        logger.log("handleReceivedMessage: Peso recibido: $message")
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        requireContext().registerReceiver(receiver, filter)
    }

    private fun toggleBluetooth() {
        if (bluetooth!!.isEnabled) {
            bluetooth!!.disable()
        } else {
            bluetooth!!.enable()
        }
    }

    private fun updateUIForBluetoothState() {
        if (bluetooth!!.isEnabled) {
            updateUIForBluetoothOn()
        } else {
            updateUIForBluetoothOff()
        }
    }

    private fun updateUIForBluetoothOn() {
        if (isAdded) {
            binding.btnToggleBluetooth.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_active)
            binding.btnScan.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_active_reload)
            binding.btnScan.isEnabled = true
        }
    }

    private fun updateUIForBluetoothOff() {
        if (isAdded) {
            binding.btnToggleBluetooth.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive)
            binding.btnScan.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive_reload)
            binding.btnScan.isEnabled = false
            bluetoothDevices.clear()
            devicesAdapter?.notifyDataSetChanged()
        }
    }

    private fun updateUIForConnectedState() {
        binding.btnScan.isEnabled = false
        // Aquí puedes agregar más cambios en la UI para el estado conectado
    }

    private fun updateUIForDisconnectedState() {
        binding.btnScan.isEnabled = true
        // Aquí puedes agregar más cambios en la UI para el estado desconectado
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

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val displayMetrics = DisplayMetrics()
            dialog.window?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            val heightInDp = 600 // Aquí pones el alto en dp que deseas
            val heightInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightInDp.toFloat(), displayMetrics).toInt()

            val layoutParams = dialog.window?.attributes
            layoutParams?.width = ViewGroup.LayoutParams.WRAP_CONTENT  // Mantener el ancho como está
            layoutParams?.height = heightInPx // Ajustar el alto en píxeles
            dialog.window?.attributes = layoutParams
        }
        val closeButton: ImageButton? = view?.findViewById(R.id.btn_close_modal)
        closeButton?.setOnClickListener {
            dismiss() // Cierra el diálogo
        }
    }
}