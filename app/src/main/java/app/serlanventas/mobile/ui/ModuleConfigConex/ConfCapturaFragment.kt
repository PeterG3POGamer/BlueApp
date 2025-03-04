package app.serlanventas.mobile.ui.ModuleConfigConex

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.CuadroCapturaConecBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.CaptureDeviceEntity
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel

class ConfCapturaFragment : Fragment() {
    private var _binding: CuadroCapturaConecBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothAdapter: BluetoothAdapter
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
        _binding = CuadroCapturaConecBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        db = AppDatabase(requireContext())

        configurarRadioButtons()
        configurarSpinner()
        configurarBotonRefrescar()
        observarCambiosDispositivo()
        cargarDispositivosVinculados()
        configurarBotones()
        cargarRegistros()
        configurarListView()

        // Iniciar verificación periódica
        handler.postDelayed(verificacionRunnable, verificacionInterval)
    }

    private fun configurarRadioButtons() {
        binding.rgTipoConexion.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbPreviamenteConectados.id -> {
                    binding.spinnerDispositivosBluetooth.visibility = View.VISIBLE
                    cargarDispositivosVinculados()
                    mostrarAlerta("Modo de visualización", "Mostrando dispositivos previamente vinculados")
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
        binding.spinnerDispositivosBluetooth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val deviceName = parent?.getItemAtPosition(position).toString()
                bluetoothDevices[deviceName]?.let { device -> mostrarInformacionDispositivo(device, false) }
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
        // Configurar botón Guardar
        binding.btnGuardar.setOnClickListener {
            guardarConfiguracion()
        }

        // Configurar botón Actualizar
        binding.btnActualizar.setOnClickListener {
            actualizarConfiguracion()
        }

        binding.btnLimpiar.setOnClickListener {
            limpiarFormulario()
        }
    }

    private fun configurarListView() {
        // Eliminar los eventos de clic y doble clic en las filas
        // Ahora los botones en cada fila manejarán las acciones
        binding.listViewRegistros.onItemClickListener = null
        binding.listViewRegistros.setOnItemLongClickListener(null)
    }

    private fun cargarDatosEnFormulario(registro: CaptureDeviceEntity) {
        binding.edtCadenaClave.setText(registro._cadenaClave)
        binding.edtLongitud.setText(registro._longitud.toString())
        binding.edtFormatoPeso.setText(registro._formatoPeo.toString())
        binding.edtCadenaClaveCierre.setText(registro._cadenaClaveCierre)
        binding.txtNombreDispositivo.text = "Nombre: ${registro._nombreDispositivo}"
        binding.txtMacDispositivo.text = "MAC: ${registro._macDispositivo}"
    }

    private fun guardarConfiguracion() {
        try {
            val macDispositivo = obtenerMacDesdeUI()

            // Verificar si ya existe un registro con esta MAC
            val existente = db.obtenerConfCapturePorMac(macDispositivo)
            if (existente != null) {
                mostrarAlerta("Error al guardar", "Ya existe una configuración para este dispositivo. Use Actualizar en su lugar.")
                return
            }

            // Crear el dispositivo desde la UI
            val captureDevice = crearCaptureDeviceDesdeUI()

            // Validar que el nombre del dispositivo y la MAC no sean "N/A" o vacíos
            if (captureDevice._nombreDispositivo == "N/A" || captureDevice._macDispositivo == "N/A") {
                mostrarAlerta("Error", "El nombre o la MAC del dispositivo no son válidos.")
                return
            }

            // Intentamos insertar la configuración
            val resultado = db.insertarConfCapture(captureDevice)

            if (resultado > 0) {
                mostrarAlerta("Éxito", "Configuración guardada correctamente")
                limpiarFormulario()
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

            // Verificar si existe un registro con esta MAC
            val existente = db.obtenerConfCapturePorMac(macDispositivo)
            if (existente == null) {
                mostrarAlerta("Error al actualizar", "No existe una configuración para este dispositivo. Use Guardar en su lugar.")
                return
            }

            val captureDevice = crearCaptureDeviceDesdeUI()
            captureDevice._idCaptureDevice = existente._idCaptureDevice // Mantener el mismo ID

            val resultado = db.actualizarConfCapture(captureDevice)

            if (resultado > 0) {
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
                Toast.makeText(requireContext(), "Estado actualizado correctamente", Toast.LENGTH_SHORT).show()
                handler.post { cargarRegistros() } // Asegurar que se ejecuta en el hilo principal
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
        val longitud = binding.edtLongitud.text.toString().toIntOrNull() ?: 0
        val formatoPeso = binding.edtFormatoPeso.text.toString().toIntOrNull() ?: 0

        // Extraer solo el nombre del dispositivo (después de "Nombre: ")
        val nombreCompleto = binding.txtNombreDispositivo.text.toString()
        val nombreDispositivo = if (nombreCompleto.startsWith("Nombre: ")) {
            nombreCompleto.substring("Nombre: ".length)
        } else {
            nombreCompleto
        }

        // Extraer solo la MAC del dispositivo (después de "MAC: ")
        val macCompleta = binding.txtMacDispositivo.text.toString()
        val macDispositivo = if (macCompleta.startsWith("MAC: ")) {
            macCompleta.substring("MAC: ".length)
        } else {
            macCompleta
        }

        return CaptureDeviceEntity(
            _idCaptureDevice = 0, // Se asignará automáticamente o se actualizará después
            _cadenaClave = cadenaClave,
            _nombreDispositivo = nombreDispositivo,
            _macDispositivo = macDispositivo,
            _longitud = longitud,
            _formatoPeo = formatoPeso,
            _estado = 0,
            _cadenaClaveCierre = cadenaClaveCierre
        )
    }


    private fun obtenerMacDesdeUI(): String {
        val macCompleta = binding.txtMacDispositivo.text.toString()
        return if (macCompleta.startsWith("MAC: ")) {
            macCompleta.substring("MAC: ".length)
        } else {
            macCompleta
        }
    }

    private fun limpiarFormulario() {
        // Limpiar campos de texto
        binding.edtCadenaClave.setText("")
        binding.edtLongitud.setText("")
        binding.edtFormatoPeso.setText("")
        binding.edtCadenaClaveCierre.setText("")

        // Limpiar los TextViews
        binding.txtNombreDispositivo.text = "Nombre: N/A"
        binding.txtMacDispositivo.text = "MAC: N/A"
    }


    private fun cargarRegistros() {
        try {
            listaRegistros = db.obtenerTodosLosDatosConfCapture()

            if (registrosAdapter == null) {
                // Primera carga - crear nuevo adaptador
                registrosAdapter = DispositivosAdapter(requireContext(), listaRegistros)
                binding.listViewRegistros.adapter = registrosAdapter
            } else {
                // Actualizar datos existentes
                (registrosAdapter as? DispositivosAdapter)?.updateData(listaRegistros)
            }
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

            // Verificar si existe configuración para este dispositivo
            val configuracion = db.obtenerConfCapturePorMac(deviceAddress)
            if (configuracion != null) {
                cargarDatosEnFormulario(configuracion)
                // Removido el Toast innecesario
            }
        } else {
            binding.txtNombreDispositivo.text = "Nombre: N/A"
            binding.txtMacDispositivo.text = "MAC: N/A"
            mostrarEstadoConexion(false)
        }
    }

    private fun mostrarInformacionDispositivo(device: BluetoothDevice, esConexionActiva: Boolean) {
        binding.txtNombreDispositivo.text = "Nombre: ${device.name ?: "Desconocido"}"
        binding.txtMacDispositivo.text = "MAC: ${device.address}"
        mostrarEstadoConexion(esConexionActiva, device.name)

        // Verificar si existe configuración para este dispositivo
        val configuracion = db.obtenerConfCapturePorMac(device.address)
        if (configuracion != null) {
            cargarDatosEnFormulario(configuracion)
            // Removido el Toast innecesario
        } else {
            limpiarFormulario()
        }
    }

    private fun mostrarEstadoConexion(conectado: Boolean, deviceName: String? = null) {
        val mensaje = if (conectado) {
            "Conectado a: $deviceName"
        } else {
            "Sin conexión activa"
        }

        // Mostrar un indicador visual del estado de conexión
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
        handler.removeCallbacks(verificacionRunnable)
        super.onDestroyView()
        _binding = null
    }
    // Adaptador personalizado para mostrar los dispositivos con botones de acción
    inner class DispositivosAdapter(
        context: Context,
        dispositivos: List<CaptureDeviceEntity>
    ) : ArrayAdapter<CaptureDeviceEntity>(context, R.layout.item_dispositivo, ArrayList(dispositivos)) {

        private val items = ArrayList(dispositivos)
        private var selectedPosition = -1

        fun updateData(newData: List<CaptureDeviceEntity>) {
            items.clear()
            items.addAll(newData)
            notifyDataSetChanged()
        }

        fun setSelectedPosition(position: Int) {
            selectedPosition = position
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): CaptureDeviceEntity? = items.getOrNull(position)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_dispositivo, parent, false)

            val dispositivo = getItem(position) ?: return view

            // Configurar el texto del dispositivo (sin cambiar el tipo de ImageButton)
            val txtDispositivo = view.findViewById<TextView>(R.id.txtDispositivo)
            txtDispositivo.text = "${dispositivo._nombreDispositivo} (${dispositivo._macDispositivo})"

            // Configurar el ImageButton de estado (sin texto, solo cambio de color o imagen)
            val btnEstado = view.findViewById<ImageButton>(R.id.btnEstado)

            // Cambiar la imagen del ImageButton según el estado del dispositivo
            val estadoDrawable = if (dispositivo._estado == 1) {
                R.drawable.ic_success // Asegúrate de tener este recurso en tus drawables
            } else {
                R.drawable.ic_error // Asegúrate de tener este recurso en tus drawables
            }

            btnEstado.setImageResource(estadoDrawable)

            // Cambiar el color de fondo o aplicar un filtro de color, si es necesario
            val colorEstado = if (dispositivo._estado == 1) {
                android.graphics.Color.parseColor("#4CAF50") // Verde más suave
            } else {
                android.graphics.Color.parseColor("#F44336") // Rojo más suave
            }
            btnEstado.setColorFilter(colorEstado)

            // Configurar el evento de clic del ImageButton de estado
            btnEstado.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Confirmar cambio de estado")
                    .setMessage("¿Desea ${if (dispositivo._estado == 0) "activar" else "desactivar"} este dispositivo?")
                    .setPositiveButton("Sí") { _, _ ->
                        actualizarEstadoPorMac(dispositivo._macDispositivo)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            // Configurar ImageButton para mostrar detalles (al lado del ImageButton de estado)
            val btnMostrar = view.findViewById<ImageButton>(R.id.btnMostrar)
            btnMostrar.setOnClickListener {
                cargarDatosEnFormulario(dispositivo)
                setSelectedPosition(position)
                Toast.makeText(context, "Configuración cargada", Toast.LENGTH_SHORT).show()
            }

            // Configurar ImageButton para eliminar
            val btnEliminar = view.findViewById<ImageButton>(R.id.btnEliminar)
            btnEliminar.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Eliminar configuración")
                    .setMessage("¿Está seguro que desea eliminar la configuración de ${dispositivo._nombreDispositivo}?")
                    .setPositiveButton("Sí") { _, _ ->
                        val resultado = db.eliminarConfCapturePorMac(dispositivo._macDispositivo)
                        if (resultado > 0) {
                            Toast.makeText(context, "Configuración eliminada", Toast.LENGTH_SHORT).show()
                            limpiarFormulario()
                            cargarRegistros()
                        } else {
                            mostrarAlerta("Error", "No se pudo eliminar la configuración")
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            return view
        }

    }
}