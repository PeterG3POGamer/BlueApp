package app.serlanventas.mobile.ui.ModuleConfigConex

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import app.serlanventas.mobile.databinding.FragmentConexConfigBinding
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel
import app.serlanventas.mobile.ui.slideshow.BluetoothConnectionService

class ConfigConexFragment : Fragment() {
    private var _binding: FragmentConexConfigBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothConnectionService: BluetoothConnectionService? = null
    private val sharedViewModel: SharedViewModel by activityViewModels()

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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothConnectionService = BluetoothConnectionService(requireContext(),
            bluetoothAdapter,
            onMessageReceived = { message ->
            }
        )
        sharedViewModel.pesoValue.observe(viewLifecycleOwner) { peso ->
            val pesoFormatted = peso.toDoubleOrNull()?.toString() ?: "0.00"
            binding.etPesaje.setText(pesoFormatted)
        }


        sharedViewModel.rawData.observe(viewLifecycleOwner) { rawData ->
            binding.etPesoCrudo.setText(rawData)
        }

        // Configura el acordeón
        binding.tvTitle.setOnClickListener {
            if (binding.accordionContent.visibility == View.VISIBLE) {
                binding.accordionContent.visibility = View.GONE
            } else {
                binding.accordionContent.visibility = View.VISIBLE
            }
        }

        // Muestra la configuración de conexión por defecto
        mostrarFragmento(ConfCapturaFragment())

        binding.btnConfConexion.setOnClickListener {
            mostrarFragmento(ConfConexionFragment())
        }

        binding.btnConfCaptura.setOnClickListener {
            mostrarFragmento(ConfCapturaFragment())
        }
    }

    // Método para proporcionar acceso al BluetoothConnectionService desde otros fragmentos
    fun getBluetoothConnectionService(): BluetoothConnectionService? {
        return bluetoothConnectionService
    }

    private fun mostrarFragmento(fragment: Fragment) {
        childFragmentManager.commit {
            replace(binding.contenedorConfiguracion.id, fragment)
            setReorderingAllowed(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
