package app.serlanventas.mobile.ui.ModuleConfigConex

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentConexConfigBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.CaptureDeviceEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost.insertarConfigConexion
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel
import app.serlanventas.mobile.ui.slideshow.BluetoothConnectionService

class ConfigConexFragment : Fragment() {
    private var _binding: FragmentConexConfigBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothConnectionService: BluetoothConnectionService? = null
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val bluetoothDevices = mutableMapOf<String, BluetoothDevice>()
    private lateinit var db: AppDatabase
    private val handler = Handler(Looper.getMainLooper())
    private val verificacionInterval = 15000L // 15 segundos
    private var registrosAdapter: DispositivosAdapter? = null
    private var listaRegistros: List<CaptureDeviceEntity> = emptyList()

    private val verificacionRunnable = object : Runnable {
        override fun run() {
            if (binding.rbConectadoAhora.isChecked) {
                verificarDispositivoConectadoActual()
            }
            handler.postDelayed(this, verificacionInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConexConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        db = AppDatabase(requireContext())

        configurarRadioButtons()
        configurarSpinner()
        configurarBotonRefrescar()
        observarCambiosDispositivo()
        cargarDispositivosVinculados()
        configurarBotones()
        cargarRegistros()
        configurarRecyclerView()

        // Iniciar verificación periódica
        handler.postDelayed(verificacionRunnable, verificacionInterval)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothConnectionService = BluetoothConnectionService(requireContext(),
            bluetoothAdapter,
            onMessageReceived = { message ->
            }
        )

        sharedViewModel.pesoValue.observe(viewLifecycleOwner) { peso ->
            if (!peso.isNullOrBlank()) {
                val pesoFormatted = peso.toDoubleOrNull()?.toString() ?: "0.00"
                binding.etPesaje.setText(pesoFormatted)
            }
        }

        var accumulatedText = ""

        sharedViewModel.rawData.observe(viewLifecycleOwner) { rawData ->
            accumulatedText += rawData

            if (accumulatedText.length >= 800) {
                binding.etPesoCrudo.text = ""
                accumulatedText = ""
            } else {
                binding.etPesoCrudo.setText(accumulatedText)
                binding.scrollViewCrudo.post {
                    binding.scrollViewCrudo.fullScroll(View.FOCUS_DOWN)
                }
            }
        }

        // Configura el acordeón
        binding.tvTitle.setOnClickListener {
            if (binding.accordionContent.visibility == View.VISIBLE) {
                binding.accordionContent.visibility = View.GONE
            } else {
                binding.accordionContent.visibility = View.VISIBLE
            }
        }
    }

    private fun configurarRecyclerView() {
        registrosAdapter = DispositivosAdapter(listaRegistros,
            onItemClick = { dispositivo ->
                cargarDatosEnFormulario(dispositivo)
            },
            onEstadoClick = { dispositivo ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar cambio de estado")
                    .setMessage("¿Desea ${if (dispositivo._estado == 0) "activar" else "desactivar"} este dispositivo?")
                    .setPositiveButton("Sí") { _, _ ->
                        actualizarEstadoPorMac(dispositivo._macDispositivo)
                    }
                    .setNegativeButton("No", null)
                    .show()
            },
            onEliminarClick = { dispositivo ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar configuración")
                    .setMessage("¿Está seguro que desea eliminar la configuración de ${dispositivo._nombreDispositivo}?")
                    .setPositiveButton("Sí") { _, _ ->
                        val resultado = db.eliminarConfCapturePorMac(dispositivo._macDispositivo)
                        if (resultado > 0) {
                            Toast.makeText(context, "Configuración eliminada", Toast.LENGTH_SHORT).show()
                            limpiarFormulario(false)
                            cargarRegistros()
                        } else {
                            mostrarAlerta("Error", "No se pudo eliminar la configuración")
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )

        binding.recyclerViewRegistros.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = registrosAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun configurarRadioButtons() {
        binding.rgTipoConexion.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbPreviamenteConectados.id -> {
                    binding.spinnerDispositivosBluetooth.visibility = View.VISIBLE
                    cargarDispositivosVinculados()
                    mostrarAlerta(
                        "Modo de visualización",
                        "Mostrando dispositivos previamente vinculados"
                    )
                }

                binding.rbConectadoAhora.id -> {
                    binding.spinnerDispositivosBluetooth.visibility = View.GONE
                    verificarDispositivoConectadoActual()
                    mostrarAlerta("Modo de visualización", "Verificando conexión actual")
                }
            }
        }
    }

    private fun configurarSpinner() {
        binding.spinnerDispositivosBluetooth.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val deviceName = parent?.getItemAtPosition(position).toString()
                    bluetoothDevices[deviceName]?.let { device ->
                        mostrarInformacionDispositivo(
                            device,
                            false
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun configurarBotonRefrescar() {
        binding.btnRefreshBluetooth.setOnClickListener {
            if (binding.rbPreviamenteConectados.isChecked) {
                cargarDispositivosVinculados()
                mostrarAlerta("Actualización", "Lista de dispositivos actualizada")
            } else {
                verificarDispositivoConectadoActual()
                mostrarAlerta("Actualización", "Verificando conexión actual...")
            }
        }
    }

    private fun configurarBotones() {
        binding.btnGuardar.setOnClickListener { guardarConfiguracion() }
        binding.btnInfo.setOnClickListener { abrirModalInfo() }
        binding.btnDataBloque.setOnClickListener { cambiarBloque() }
        binding.btnActualizar.setOnClickListener { actualizarConfiguracion() }
        binding.btnLimpiar.setOnClickListener { limpiarFormulario(false) }
    }

    private fun abrirModalInfo() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_info_bloques, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun cambiarBloque() {
        val currentText = binding.btnDataBloque.text.toString()

        if (currentText == "ENTERO") {
            binding.btnDataBloque.text = "DISCREPANTE"
            binding.btnDataBloque.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    R.color.color_orange_low
                )
            )
            binding.btnDataBloque.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    R.color.black
                )
            )
        } else {
            binding.btnDataBloque.text = "ENTERO"
            binding.btnDataBloque.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    R.color.color_gree_low
                )
            )
            binding.btnDataBloque.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    R.color.black
                )
            )
        }
    }

    private fun cargarDatosEnFormulario(registro: CaptureDeviceEntity) {
        binding.btnActualizar.visibility = View.VISIBLE
        binding.btnGuardar.visibility = View.GONE
        binding.edtLongitud.setText(registro._longitud?.toString() ?: "")
        binding.edtFormatoPeso.setText(registro._formatoPeo?.toString() ?: "")
        binding.edtCadenaClaveCierre.setText(registro._cadenaClaveCierre ?: "")
        binding.txtNombreDispositivo.text = "Nombre: ${registro._nombreDispositivo ?: "N/A"}"
        binding.txtMacDispositivo.text = "MAC: ${registro._macDispositivo ?: "N/A"}"

        when (registro._bloque) {
            "2" -> {
                binding.btnDataBloque.text = "ENTERO"
                binding.btnDataBloque.setBackgroundColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.color_gree_low
                    )
                )
                binding.btnDataBloque.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.black
                    )
                )
            }

            "1" -> {
                binding.btnDataBloque.text = "DISCREPANTE"
                binding.btnDataBloque.setBackgroundColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.color_orange_low
                    )
                )
                binding.btnDataBloque.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.black
                    )
                )
            }

            else -> {
                binding.btnDataBloque.text = "NO DETECTADO"
                binding.btnDataBloque.setBackgroundColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.black_4
                    )
                )
                binding.btnDataBloque.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.white
                    )
                )
            }
        }
    }

    private fun guardarConfiguracion() {
        try {
            val macDispositivo = obtenerMacDesdeUI()
            val existente = db.obtenerConfCapturePorMac(macDispositivo)
            if (existente != null) {
                mostrarAlerta(
                    "Error al guardar",
                    "Ya existe una configuración para este dispositivo. Use Actualizar en su lugar."
                )
                return
            }

            val captureDevice = crearCaptureDeviceDesdeUI()
            if (captureDevice._nombreDispositivo == "N/A" || captureDevice._macDispositivo == "N/A") {
                mostrarAlerta("Error", "El nombre o la MAC del dispositivo no son válidos.")
                return
            }

            val resultado = db.insertarConfCapture(captureDevice)
            if (resultado > 0) {
                insertarConfigConexion(requireContext(), captureDevice) { success ->
                    if (success) {
                        //
                    }
                }
                limpiarFormulario(false)
                cargarRegistros()
            } else {
                mostrarAlerta("Error", "No se pudo guardar la configuración")
            }
        } catch (e: Exception) {
            mostrarAlerta("Error", "Error al guardar: ${e.message}")
        }
    }

    private fun actualizarConfiguracion() {
        try {
            val macDispositivo = obtenerMacDesdeUI()
            val existente = db.obtenerConfCapturePorMac(macDispositivo)
            if (existente == null) {
                mostrarAlerta(
                    "Error al actualizar",
                    "No existe una configuración para este dispositivo. Use Guardar en su lugar."
                )
                return
            }

            val captureDevice = crearCaptureDeviceDesdeUI()
            captureDevice._idCaptureDevice = existente._idCaptureDevice

            val resultado = db.actualizarConfCapture(captureDevice)
            if (resultado > 0) {
                insertarConfigConexion(requireContext(), captureDevice) { success ->
                    if (success) {
                        //
                    }
                }
                mostrarAlerta("Éxito", "Configuración actualizada correctamente")
                cargarRegistros()
            } else {
                mostrarAlerta("Error", "No se pudo actualizar la configuración")
            }
        } catch (e: Exception) {
            mostrarAlerta("Error", "Error al actualizar: ${e.message}")
        }
    }

    private fun actualizarEstadoPorMac(mac: String) {
        try {
            val resultado = db.actualizarEstadoPorMac(mac)
            if (resultado > 0) {
                Toast.makeText(
                    requireContext(),
                    "Estado actualizado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
                sharedViewModel.actualizarPeso("0.00")
                val configuracion = db.obtenerConfCapturePorMac(mac)
                if (configuracion != null) {
                    cargarDatosEnFormulario(configuracion)
                } else {
                    limpiarFormulario(true)
                }
                handler.post { cargarRegistros() }
            } else {
                mostrarAlerta("Error", "No se pudo actualizar el estado")
            }
        } catch (e: Exception) {
            mostrarAlerta("Error", "Error al actualizar estado: ${e.message}")
        }
    }

    private fun crearCaptureDeviceDesdeUI(): CaptureDeviceEntity {
        val cadenaClave = binding.edtCadenaClave.text.toString()
        val cadenaClaveCierre = binding.edtCadenaClaveCierre.text.toString()

        val Databloque = when (binding.btnDataBloque.text.toString()) {
            "ENTERO" -> "2"
            "DISCREPANTE" -> "1"
            else -> "0"
        }

        val longitud = binding.edtLongitud.text.toString().toIntOrNull() ?: 0
        val formatoPeso = binding.edtFormatoPeso.text.toString().toIntOrNull() ?: 0

        val nombreCompleto = binding.txtNombreDispositivo.text.toString()
        val nombreDispositivo = nombreCompleto.removePrefix("Nombre: ")

        val macCompleta = binding.txtMacDispositivo.text.toString()
        val macDispositivo = macCompleta.removePrefix("MAC: ")

        return CaptureDeviceEntity(
            _idCaptureDevice = 0,
            _cadenaClave = cadenaClave,
            _nombreDispositivo = nombreDispositivo,
            _macDispositivo = macDispositivo,
            _longitud = longitud,
            _formatoPeo = formatoPeso,
            _estado = 0,
            _cadenaClaveCierre = cadenaClaveCierre,
            _bloque = Databloque,
            _isSync = "0"
        )
    }

    private fun obtenerMacDesdeUI(): String {
        return binding.txtMacDispositivo.text.toString().removePrefix("MAC: ")
    }

    private fun limpiarFormulario(estado: Boolean = false) {
        binding.edtCadenaClave.setText("")
        binding.edtLongitud.setText("")
        binding.edtFormatoPeso.setText("")
        binding.edtCadenaClaveCierre.setText("")

        binding.btnActualizar.visibility = View.GONE
        binding.btnGuardar.visibility = View.VISIBLE

        if (!estado) {
            binding.txtNombreDispositivo.text = "Nombre: N/A"
            binding.txtMacDispositivo.text = "MAC: N/A"
        }
        binding.btnDataBloque.text = "NO DETECTADO"
        binding.btnDataBloque.setBackgroundColor(
            ContextCompat.getColor(
                binding.root.context,
                R.color.black_4
            )
        )
        binding.btnDataBloque.setTextColor(
            ContextCompat.getColor(
                binding.root.context,
                R.color.white
            )
        )
        registrosAdapter?.clearSelection()
    }

    private fun cargarRegistros() {
        try {
            listaRegistros = db.obtenerTodosLosDatosConfCapture()
            registrosAdapter?.updateData(listaRegistros)
        } catch (e: Exception) {
            mostrarAlerta("Error", "Error al cargar registros: ${e.message}")
        }
    }

    private fun observarCambiosDispositivo() {
        sharedViewModel.connectedDeviceName.observe(viewLifecycleOwner) { deviceName ->
            deviceName?.let {
                binding.txtNombreDispositivo.text = "Nombre: $it"
                mostrarAlerta("Conexión Bluetooth", "Dispositivo conectado: $it")
            }
        }

        sharedViewModel.connectedDeviceAddress.observe(viewLifecycleOwner) { address ->
            address?.let {
                binding.txtMacDispositivo.text = "MAC: $it"
            }
        }
    }

    private fun cargarDispositivosVinculados() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            mostrarAlerta(
                "Permisos requeridos",
                "Es necesario habilitar los permisos solicitados por la aplicación para poder llevar a cabo la captura de los datos de los pesos."
            )
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            actualizarUIBluetoothDesactivado()
            mostrarAlerta("Estado Bluetooth", "Bluetooth desactivado")
            return
        }

        val bondedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        bluetoothDevices.clear()

        if (bondedDevices.isEmpty()) {
            actualizarUISinDispositivos()
            mostrarAlerta("Dispositivos vinculados", "No hay dispositivos vinculados")
        } else {
            actualizarUIConDispositivos(bondedDevices)
        }
    }

    private fun actualizarUIBluetoothDesactivado() {
        binding.txtNombreDispositivo.text = "Nombre: Bluetooth desactivado"
        binding.txtMacDispositivo.text = "MAC: Bluetooth desactivado"
        binding.spinnerDispositivosBluetooth.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Bluetooth desactivado")
        )
    }

    private fun actualizarUISinDispositivos() {
        binding.spinnerDispositivosBluetooth.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("No hay dispositivos vinculados")
        )
        binding.txtNombreDispositivo.text = "Nombre: No hay dispositivos vinculados"
        binding.txtMacDispositivo.text = "MAC: No hay dispositivos vinculados"
    }

    @SuppressLint("MissingPermission")
    private fun actualizarUIConDispositivos(bondedDevices: Set<BluetoothDevice>) {
        try {
            val deviceNames = bondedDevices.map { device ->
                val name = device.name ?: "Dispositivo sin nombre"
                bluetoothDevices[name] = device
                name
            }

            binding.spinnerDispositivosBluetooth.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                deviceNames
            )

            deviceNames.firstOrNull()?.let { deviceName ->
                bluetoothDevices[deviceName]?.let { device ->
                    mostrarInformacionDispositivo(device, false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mostrarAlerta("Error", "Error al cargar los dispositivos")
        }
    }

    private fun verificarDispositivoConectadoActual() {
        if (!isAdded || isDetached || _binding == null) {
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            actualizarUIBluetoothDesactivado()
            return
        }

        val deviceName = sharedViewModel.connectedDeviceName.value
        val deviceAddress = sharedViewModel.connectedDeviceAddress.value

        if (deviceName != null && deviceAddress != null) {
            binding.txtNombreDispositivo.text = "Nombre: $deviceName"
            binding.txtMacDispositivo.text = "MAC: $deviceAddress"
            mostrarEstadoConexion(true, deviceName)

            val configuracion = db.obtenerConfCapturePorMac(deviceAddress)
            if (configuracion != null) {
                cargarDatosEnFormulario(configuracion)
            }
        } else {
            binding.txtNombreDispositivo.text = "Nombre: N/A"
            binding.txtMacDispositivo.text = "MAC: N/A"
            mostrarEstadoConexion(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun mostrarInformacionDispositivo(device: BluetoothDevice, esConexionActiva: Boolean) {
        binding.txtNombreDispositivo.text = "Nombre: ${device.name ?: "N/A"}"
        binding.txtMacDispositivo.text = "MAC: ${device.address}"
        mostrarEstadoConexion(esConexionActiva, device.name)

        val configuracion = db.obtenerConfCapturePorMac(device.address)
        if (configuracion != null) {
            cargarDatosEnFormulario(configuracion)
        } else {
            limpiarFormulario(true)
        }
    }

    private fun mostrarEstadoConexion(conectado: Boolean, deviceName: String? = null) {
        val mensaje = if (conectado) {
            "Conectado a: $deviceName"
        } else {
            "Sin conexión activa"
        }

        activity?.runOnUiThread {
            val colorEstado = if (conectado) {
                android.graphics.Color.GREEN
            } else {
                android.graphics.Color.RED
            }
            binding.txtNombreDispositivo.setTextColor(colorEstado)
        }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        activity?.runOnUiThread {
            try {
                AlertDialog.Builder(requireContext())
                    .setTitle(titulo)
                    .setMessage(mensaje)
                    .setPositiveButton("Aceptar", null)
                    .create()
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}