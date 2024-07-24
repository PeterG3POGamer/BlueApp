package com.example.blueapp.ui.ImpresoraSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.blueapp.R
import com.example.blueapp.databinding.FragmentImpresoraSettingsBinding
import com.example.blueapp.ui.DataBase.AppDatabase
import com.example.blueapp.ui.DataBase.Entities.impresoraEntity
import com.example.blueapp.ui.Jabas.ManagerPost

class ImpresoraSettingsFragment : Fragment() {

    private lateinit var dbHelper: AppDatabase
    private var existingImpresora: impresoraEntity? = null

    private var _binding: FragmentImpresoraSettingsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImpresoraSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        dbHelper = AppDatabase(requireContext())

        loadPrinterSettings()

        binding.btnGuardar.setOnClickListener {
            savePrinterSettings()
        }

        return root
    }
    private fun loadPrinterSettings() {
        existingImpresora = dbHelper.getImpresoraById("1") // Replace "1" with your actual id
        if (existingImpresora != null) {
            binding.etIp.setText(existingImpresora?.ip)
            binding.etPuerto.setText(existingImpresora?.puerto)
            // Update button text and color for editing
            binding.btnGuardar.text = "Actualizar"
            binding.btnGuardar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
        } else {
            binding.etIp.text.clear()
            binding.etPuerto.text.clear()
            binding.btnGuardar.text = "Guardar"
            binding.btnGuardar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_500))
        }
    }
    private fun isValidIp(ip: String): Boolean {
        // Expresión regular para validar una IP (IPv4)
        val ipRegex = Regex("^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\$")
        return ipRegex.matches(ip)
    }
    private fun savePrinterSettings() {
        val ip = binding.etIp.text.toString()
        val puerto = binding.etPuerto.text.toString()

        if (ip.isEmpty() || puerto.isEmpty()) {
            ManagerPost.showCustomToast(
                requireContext(),
                "Por favor, complete todos los campos",
                "info"
            )
            return
        }

        if (!isValidIp(ip)) {
            ManagerPost.showCustomToast(
                requireContext(),
                "La dirección IP no es válida",
                "warning"
            )
            return
        }

        val impresora = impresoraEntity(
            idImpresora = 1, // Replace with actual id or make this dynamic
            ip = ip,
            puerto = puerto
        )

        val rowsAffected: Int
        if (existingImpresora == null) {
            // Inserción de nueva impresora
            rowsAffected = dbHelper.addImpresora(impresora).toInt()
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), "Impresora guardada exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No se pudo guardar la impresora", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Actualización de impresora existente
            rowsAffected = dbHelper.updateImpresora(impresora)
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), "Impresora actualizada exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No se pudo actualizar la impresora", Toast.LENGTH_SHORT).show()
            }
        }
        loadPrinterSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
