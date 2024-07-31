package com.example.blueapp.ui.BluetoothView

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.blueapp.R
import com.example.blueapp.databinding.FragmentBluetoothBinding
import me.aflak.bluetooth.Bluetooth
import me.aflak.bluetooth.interfaces.BluetoothCallback
import me.aflak.bluetooth.interfaces.DeviceCallback
import me.aflak.bluetooth.interfaces.DiscoveryCallback

class BluetoothFragment : Fragment() {
    private var bluetooth: Bluetooth? = null
    private var adapter: DeviceListAdapter? = null
    private var deviceList: MutableList<BluetoothDevice?> = ArrayList()
    private var _binding: FragmentBluetoothBinding? = null
    private val binding get() = _binding!!
    private var currentToast: Toast? = null

    private val TAG = "BluetoothFragment"
    private val REQUEST_ENABLE_BT = 1
    private val PAIRING_TIMEOUT = 30000L // 30 segundos

    private var isConnecting = false
    private var isPairing = false
    private var connectedDevice: BluetoothDevice? = null
    private var pairingDevice: BluetoothDevice? = null


    private val pairingReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.address == pairingDevice?.address) {
                        showToast("Solicitud de vinculación recibida para ${device?.name}")
                        Log.d(TAG, "Pairing request received for ${device?.name}")
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)

                    when (bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            if (prevBondState == BluetoothDevice.BOND_BONDING) {
                                showToast("Vinculación exitosa con ${device?.name}")
                                Log.d(TAG, "Pairing successful with ${device?.name}")
                                isPairing = false
                                pairingDevice = null
                                device?.let { connectToDevice(it) }
                            }
                        }
                        BluetoothDevice.BOND_NONE -> {
                            if (prevBondState == BluetoothDevice.BOND_BONDING) {
                                showToast("Vinculación fallida con ${device?.name}")
                                Log.d(TAG, "Pairing failed with ${device?.name}")
                                isPairing = false
                                pairingDevice = null
                            }
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
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bluetooth = Bluetooth(activity)
        binding.progressBar.visibility = View.GONE

        setupBluetoothCallbacks()
        setupUI()

        // Registrar el receptor de broadcast para los eventos de vinculación
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        requireActivity().registerReceiver(pairingReceiver, filter)
    }

    private fun setupBluetoothCallbacks() {
        bluetooth!!.setBluetoothCallback(object : BluetoothCallback {
            override fun onBluetoothTurningOn() {
                Log.d(TAG, "Bluetooth is turning on")
                showToast("Bluetooth se está encendiendo")
            }

            override fun onBluetoothTurningOff() {
                Log.d(TAG, "Bluetooth is turning off")
                showToast("Bluetooth se está apagando")
                disconnectDevice()
            }

            override fun onBluetoothOff() {
                Log.d(TAG, "Bluetooth is off")
                showToast("Bluetooth está apagado")
                updateUIForBluetoothOff()
                disconnectDevice()
            }

            override fun onBluetoothOn() {
                Log.d(TAG, "Bluetooth is on")
                showToast("Bluetooth está encendido")
                updateUIForBluetoothOn()
            }

            override fun onUserDeniedActivation() {
                Log.d(TAG, "User denied Bluetooth activation")
                showToast("Activación de Bluetooth denegada")
            }
        })

        bluetooth!!.setDiscoveryCallback(object : DiscoveryCallback {
            override fun onDiscoveryStarted() {
                showToast("Escaneo iniciado")
                binding.progressBar.visibility = View.VISIBLE
                Log.d(TAG, "Discovery started")
            }

            override fun onDiscoveryFinished() {
                showToast("Escaneo terminado")
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Discovery finished")
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceFound(device: BluetoothDevice) {
                if (deviceList.none { it?.address == device.address }) {
                    deviceList.add(device)
                    adapter!!.notifyDataSetChanged()
                    Log.d(TAG, "Device found: ${device.name} (${device.address})")
                }
            }

            @SuppressLint("MissingPermission")
            override fun onDevicePaired(device: BluetoothDevice) {
                showToast("Emparejado con ${device.name}")
                Log.d(TAG, "Device paired: ${device.name}")
                isPairing = false
                pairingDevice = null
                connectToDevice(device)
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceUnpaired(device: BluetoothDevice) {
                showToast("Desemparejado de ${device.name}")
                Log.d(TAG, "Device unpaired: ${device.name}")
                disconnectDevice()
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
                isConnecting = false
                connectedDevice = device
                updateUIForConnectedState()
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceDisconnected(device: BluetoothDevice, message: String) {
                showToast("Desconectado de ${device.name}")
                Log.d(TAG, "Device disconnected: ${device.name}. Message: $message")
                isConnecting = false
                connectedDevice = null
                updateUIForDisconnectedState()
            }

            override fun onMessage(message: ByteArray) {
                val receivedMessage = String(message)
                showToast("Mensaje recibido: $receivedMessage")
                Log.d(TAG, "Message received: $receivedMessage")
                // Aquí puedes procesar los datos recibidos
            }

            override fun onError(errorCode: Int) {
                showToast("Error: $errorCode")
                Log.e(TAG, "Device error: $errorCode")
                isConnecting = false
            }

            @SuppressLint("MissingPermission")
            override fun onConnectError(device: BluetoothDevice, message: String) {
                showToast("Error de conexión: $message")
                Log.e(TAG, "Connection error with ${device.name}: $message")
                isConnecting = false
                retryConnection(device)
            }
        })
    }

    private fun setupUI() {
        adapter = DeviceListAdapter(context, deviceList)
        binding.listDevices.adapter = adapter

        binding.btnScan.setOnClickListener {
            startBluetoothDiscovery()
        }

        binding.btnToggleBluetooth.setOnClickListener {
            toggleBluetooth()
        }

        binding.listDevices.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            if (device != null) {
                connectToDevice(device)
            }
        }

        // Verificar el estado del Bluetooth al configurar la UI
        updateUIForBluetoothState()
    }

    override fun onStart() {
        super.onStart()
        bluetooth!!.onStart()

        // Verificar si Bluetooth está habilitado al iniciar el fragmento
        if (!bluetooth!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            updateUIForBluetoothOn() // Actualiza la UI si Bluetooth ya está habilitado
        }
    }

    private fun startBluetoothDiscovery() {
        deviceList.clear()
        adapter!!.notifyDataSetChanged()
        binding.progressBar.visibility = View.VISIBLE
        bluetooth!!.startScanning()
        Log.d(TAG, "Scanning started")
    }

    private fun toggleBluetooth() {
        if (bluetooth!!.isEnabled) {
            bluetooth!!.disable()
            Log.d(TAG, "Disabling Bluetooth")
        } else {
            bluetooth!!.enable()
            Log.d(TAG, "Enabling Bluetooth")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (isConnecting || isPairing) {
            showToast("Ya se está intentando conectar o vincular a un dispositivo")
            return
        }

        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            isPairing = true
            pairingDevice = device
            showToast("Iniciando vinculación con ${device.name}...")
            Log.d(TAG, "Starting pairing process with ${device.name}")
            bluetooth!!.pair(device)

            // Configurar un timeout para la vinculación
            Handler(Looper.getMainLooper()).postDelayed({
                if (isPairing && pairingDevice == device) {
                    isPairing = false
                    pairingDevice = null
                    showToast("Tiempo de espera de vinculación agotado para ${device.name}")
                    Log.d(TAG, "Pairing timeout for ${device.name}")
                }
            }, PAIRING_TIMEOUT)
        } else {
            isConnecting = true
            showToast("Intentando conectar a ${device.name}...")
            Log.d(TAG, "Attempting to connect to ${device.name}")
            bluetooth!!.connectToDevice(device)
        }
    }


    private fun disconnectDevice() {
        connectedDevice?.let {
            bluetooth!!.disconnect()
            connectedDevice = null
            updateUIForDisconnectedState()
        }
    }

    private fun retryConnection(device: BluetoothDevice) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (connectedDevice == null) {
                showToast("Reintentando conexión...")
                connectToDevice(device)
            }
        }, 5000) // Espera 5 segundos antes de reintentar
    }

    private fun updateUIForBluetoothState() {
        if (bluetooth!!.isEnabled) {
            updateUIForBluetoothOn()
        } else {
            updateUIForBluetoothOff()
        }
    }

    private fun updateUIForBluetoothOn() {
        binding.btnToggleBluetooth.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.your_greed))
        binding.btnScan.isEnabled = true
    }

    private fun updateUIForBluetoothOff() {
        binding.btnToggleBluetooth.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
        binding.btnScan.isEnabled = false
        deviceList.clear()
        adapter!!.notifyDataSetChanged()
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
                showToast("Bluetooth activado")
                updateUIForBluetoothOn()
            } else {
                showToast("Bluetooth no activado")
                updateUIForBluetoothOff()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        bluetooth!!.onStop()
        Log.d(TAG, "Bluetooth stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(pairingReceiver)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            currentToast?.cancel()
            currentToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
            currentToast?.show()
        }
    }
}