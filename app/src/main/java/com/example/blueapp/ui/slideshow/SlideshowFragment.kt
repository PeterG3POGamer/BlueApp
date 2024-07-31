package com.example.blueapp.ui.slideshow

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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.blueapp.R
import com.example.blueapp.databinding.FragmentSlideshowBinding
import com.example.blueapp.ui.BluetoothView.DeviceListAdapter
import com.example.blueapp.ui.ViewModel.SharedViewModel
import com.example.blueapp.ui.ViewModel.TabViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aflak.bluetooth.Bluetooth
import me.aflak.bluetooth.interfaces.BluetoothCallback
import me.aflak.bluetooth.interfaces.DeviceCallback
import me.aflak.bluetooth.interfaces.DiscoveryCallback

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val REQUEST_ENABLE_BT = 1
    private val TAG = "BluetoothFragment"
    private var currentToast: Toast? = null


    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

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
            Toast.makeText(requireContext(), "Se requieren permisos para usar Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestEnableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            initializeBluetoothAdapterAndServer()
        } else {
            Toast.makeText(requireContext(), "El Bluetooth debe estar habilitado para continuar", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "Búsqueda de dispositivos finalizada", Toast.LENGTH_SHORT).show()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            Toast.makeText(context, "Bluetooth desactivado", Toast.LENGTH_SHORT).show()
                        }
                        BluetoothAdapter.STATE_ON -> {
                            Toast.makeText(context, "Bluetooth activado", Toast.LENGTH_SHORT).show()
                            startDiscovery()
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                        BluetoothDevice.BOND_BONDED -> {
                            Log.d("Bluetooth", "Dispositivo emparejado: ${device?.name}")
                            device?.let { connectToDevice(it) }
                        }
                        BluetoothDevice.BOND_NONE -> {
                            Log.d("Bluetooth", "Dispositivo desemparejado: ${device?.name}")
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
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

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
            }

            override fun onBluetoothTurningOff() {
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
                }
            }

            @SuppressLint("MissingPermission")
            override fun onDevicePaired(device: BluetoothDevice) {
                showToast("Emparejado con ${device.name}")
                Log.d(TAG, "Device paired: ${device.name}")
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceUnpaired(device: BluetoothDevice) {
            }

            override fun onError(errorCode: Int) {
                showToast("Error: $errorCode")
                Log.e(TAG, "Discovery error: $errorCode")
            }
        })

        bluetooth!!.setDeviceCallback(object : DeviceCallback {
            @SuppressLint("MissingPermission")
            override fun onDeviceConnected(device: BluetoothDevice) {
                showToast("Conectado a ${device.name}")
                Log.d(TAG, "Device connected: ${device.name}")
                updateUIForConnectedState()
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceDisconnected(device: BluetoothDevice, message: String) {
                showToast("Desconectado de ${device.name}")
                Log.d(TAG, "Device disconnected: ${device.name}. Message: $message")
                updateUIForDisconnectedState()
            }

            override fun onMessage(message: ByteArray) {
                val receivedMessage = String(message)
                showToast("Mensaje recibido: $receivedMessage")
                Log.d(TAG, "Message received: $receivedMessage")
            }

            override fun onError(errorCode: Int) {
                showToast("Error: $errorCode")
                Log.e(TAG, "Device error: $errorCode")
            }

            @SuppressLint("MissingPermission")
            override fun onConnectError(device: BluetoothDevice, message: String) {
                showToast("Error de conexión: $message")
                Log.e(TAG, "Connection error with ${device.name}: $message")
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
            Toast.makeText(requireContext(), "Este dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show()
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
        bluetoothConnectionService = BluetoothConnectionService(bluetoothAdapter) { message ->
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
        Toast.makeText(requireContext(), "Buscando dispositivos Bluetooth...", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun pairAndConnectDevice(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            connectToDevice(device)
        } else {
            try {
                device.createBond()
                Toast.makeText(requireContext(), "Emparejando con ${device.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "No se pudo emparejar con ${device.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        coroutineScope.launch {
            try {
                Log.d("Bluetooth", "Intentando conectar con: ${device.name}")
                bluetoothConnectionService.connect(device)

                withContext(Dispatchers.Main) {
                    binding.deviceName.text = "Conectado a: ${device.name}"
                    sharedViewModel.updateConnectedDeviceName(device.name)
                    sharedViewModel.updateConnectedDeviceAddress(device.address.toString())
                    Toast.makeText(requireContext(), "Conectado a ${device.name}", Toast.LENGTH_SHORT).show()

                    // Redirigir al tab 1 solo si la conexión fue exitosa
                    findNavController().navigate(R.id.nav_initReportePeso)
//                    tabViewModel.setNavigateToTab(1)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Bluetooth", "Error al conectar: ${e.message}", e)
                    binding.deviceName.text = "Error de conexión"
                    Toast.makeText(requireContext(), "Error al conectar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleReceivedMessage(message: String) {
        activity?.runOnUiThread {
            sharedViewModel.updatePesoValue(message)
            Log.d("SlideshowFragment", "Peso recibido: $message")
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
        // Aplica el selector para el estado activado
        binding.btnToggleBluetooth.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_active)
        binding.btnScan.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_active_reload)
        binding.btnScan.isEnabled = true
    }

    private fun updateUIForBluetoothOff() {
        // Aplica el selector para el estado desactivado
        binding.btnToggleBluetooth.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive)
        binding.btnScan.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_inactive_reload)
        binding.btnScan.isEnabled = false
        bluetoothDevices.clear()
        devicesAdapter!!.notifyDataSetChanged()
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
        Handler(Looper.getMainLooper()).post {
            currentToast?.cancel()
            currentToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
            currentToast?.show()
        }
    }
}