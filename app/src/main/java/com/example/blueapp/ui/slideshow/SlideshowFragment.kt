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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blueapp.databinding.FragmentSlideshowBinding
import com.example.blueapp.ui.ViewModel.SharedViewModel
import com.example.blueapp.ui.ViewModel.TabViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private lateinit var devicesAdapter: DevicesAdapter
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
                            devicesAdapter.notifyDataSetChanged()
                        }
                    }
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

        setupRecyclerView()
        checkBluetoothPermissions()

        binding.buttonRefresh.setOnClickListener {
            checkBluetoothPermissions()
        }

        return root
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
        devicesAdapter = DevicesAdapter(bluetoothDevices) { device ->
            pairAndConnectDevice(device)
        }
        binding.recyclerViewDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewDevices.adapter = devicesAdapter
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun startDiscovery() {
        bluetoothDevices.clear()
        devicesAdapter.notifyDataSetChanged()

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
                val connectionSuccessful = bluetoothConnectionService.connect(device)

                withContext(Dispatchers.Main) {
                    binding.textSlideshow.text = "Conectado a: ${device.name}"
                    sharedViewModel.updateConnectedDeviceName(device.name)
                    sharedViewModel.updateConnectedDeviceAddress(device.address.toString())
                    Toast.makeText(requireContext(), "Conectado a ${device.name}", Toast.LENGTH_SHORT).show()

                    // Redirigir al tab 1 solo si la conexión fue exitosa
                    tabViewModel.setNavigateToTab(1)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Bluetooth", "Error al conectar: ${e.message}", e)
                    binding.textSlideshow.text = "Error de conexión"
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
        startDiscovery()
    }
}