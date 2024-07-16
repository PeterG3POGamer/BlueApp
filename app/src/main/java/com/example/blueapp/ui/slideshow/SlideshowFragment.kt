package com.example.blueapp.ui.slideshow

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blueapp.R
import com.example.blueapp.databinding.FragmentPesosBinding
import com.example.blueapp.databinding.FragmentSlideshowBinding
import com.example.blueapp.ui.Jabas.JabasFragment
import com.example.blueapp.ui.Utilidades.Constants
import com.example.blueapp.ui.ViewModel.SharedViewModel
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val REQUEST_ENABLE_BT = 1
    private val PERMISSIONS_REQUEST_CODE = 1001

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var bluetoothConnectionService: BluetoothConnectionService

    // Permisos de Bluetooth para Android 12 y superiores
    private val bluetoothPermissions12 = arrayOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Permisos de Bluetooth para Android 11 y anteriores
    private val bluetoothPermissions11 = arrayOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission", "NotifyDataSetChanged")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d("Bluetooth", "Dispositivo encontrado: ${it.name} - ${it.address}")
                        if (!bluetoothDevices.contains(it)) {
                            bluetoothDevices.add(it)
                            devicesAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
        } else {
            initializeBluetoothAdapterAndServer()
        }

        // Configurar el botón para actualizar dispositivos Bluetooth
        binding.buttonRefresh.setOnClickListener {
            startDiscovery()
        }

        return root
    }


    // Este método solo será llamado si se han otorgado los permisos
    private fun initializeBluetoothAdapterAndServer() {
        // Inicialización del adaptador de Bluetooth
        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Este dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show()
        } else {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                bluetoothConnectionService = BluetoothConnectionService(bluetoothAdapter) { message ->
                    handleReceivedMessage(message)
                }
                initializeBluetoothServer()
                setupBluetooth()
            }
        }
    }

    private fun setupRecyclerView() {
        devicesAdapter = DevicesAdapter(bluetoothDevices) { device ->
            pairDevice(device)
            connectToDevice(device)
        }
        binding.recyclerViewDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewDevices.adapter = devicesAdapter
    }

    private fun hasBluetoothPermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissions12
        } else {
            bluetoothPermissions11
        }
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissions12
        } else {
            bluetoothPermissions11
        }
        requestPermissions(requiredPermissions, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeBluetoothAdapterAndServer()
            } else {
                Toast.makeText(requireContext(), "Se requieren permisos para usar Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeBluetoothServer() {
        bluetoothConnectionService = BluetoothConnectionService(bluetoothAdapter) { message ->
            handleReceivedMessage(message)
        }
        bluetoothConnectionService.startServer()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                initializeBluetoothAdapterAndServer()
            } else {
                Toast.makeText(requireContext(), "El Bluetooth debe estar habilitado para continuar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBluetooth() {
        startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
        Toast.makeText(requireContext(), "Se á recargado la lista de dispositivos bluetooth", Toast.LENGTH_LONG).show()


        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireContext().registerReceiver(receiver, filter)
    }

    @SuppressLint("MissingPermission")
    private fun pairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod("createBond")
            method.invoke(device)
            Toast.makeText(requireContext(), "Emparejando con ${device.name}", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_initReportePeso)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "No se pudo emparejar con ${device.name}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothConnectionService.connect(device)
        binding.textSlideshow.text = "Dispositivo: $device"
        sharedViewModel.updateConnectedDeviceName(device.name)
    }

    private fun handleReceivedMessage(message: String) {
        // Mostrar el mensaje en el hilo principal
        activity?.runOnUiThread {
            // Actualizar el texto con el mensaje de peso recibido
//            binding.textReceivedMessage.text = "Peso recibido: $message kg"
//            Log.d("SlidesshowFragment", "Peso recibido: $message")
            sharedViewModel.updatePesoValue(message)
        }
    }

    private fun startSendingRandomWeightData() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                // Generar un peso aleatorio entre 50 y 100 kg
                val randomWeight = Random.nextDouble(50.0, 100.0)
                // Enviar el peso aleatorio
                bluetoothConnectionService.sendWeightData(randomWeight)
                Log.d("SlidesshowFragment", "Peso enviado: $randomWeight")
                // Planificar la siguiente ejecución después de 1 segundo (1000 ms)
                handler.postDelayed(this, 1000)
            }
        }
        // Iniciar el envío repetido de datos
        handler.post(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        _binding = null
        requireContext().unregisterReceiver(receiver)
//        bluetoothConnectionService.closeConnection()
    }
}