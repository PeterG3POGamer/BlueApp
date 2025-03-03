package app.serlanventas.mobile.ui.ModuleGalpones

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentModuleGalponesBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast

class GalponesFragment : Fragment() {

    private var _binding: FragmentModuleGalponesBinding? = null
    private val binding get() = _binding!!
    private lateinit var galponesAdapter: GalponesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModuleGalponesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddGalpon.setOnClickListener {
            showAddGalponDialog()
        }

        val db = AppDatabase(requireContext())
        val nucleos = db.getAllNucleos()
        val nucleosMap = nucleos.associateBy({ it.idEstablecimiento }, { it.nombre })

        // Initialize the adapter with the nucleos map
        galponesAdapter = GalponesAdapter(emptyList(), nucleosMap, ::showEditGalponDialog, ::deleteGalpon)
        binding.recyclerViewGalpon.adapter = galponesAdapter
        binding.recyclerViewGalpon.layoutManager = LinearLayoutManager(context)

        cargarGalpones()
    }

    private fun showAddGalponDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.modal_galpon, null)
        val etGalponName = dialogView.findViewById<EditText>(R.id.inputNombreGalpon)
        val spinnerNucleo = dialogView.findViewById<Spinner>(R.id.spinnerNucleo)
        val btnCancel = dialogView.findViewById<Button>(R.id.botonCancelar)
        val btnSave = dialogView.findViewById<Button>(R.id.botonGuardar)

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // Populate the Spinner with data from the database
        val db = AppDatabase(requireContext())
        val nucleos = db.getAllNucleos()
        val nucleoNames = nucleos.map { it.nombre }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nucleoNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNucleo.adapter = adapter

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val nombreGalpon = etGalponName.text.toString().trim()
            val selectedNucleoName = spinnerNucleo.selectedItem.toString()
            val selectedNucleo = nucleos.find { it.nombre == selectedNucleoName }

            if (validarCampos(nombreGalpon, selectedNucleoName)) {
                val nuevoGalpon = GalponEntity(
                    idGalpon = 0,
                    nombre = nombreGalpon,
                    idEstablecimiento = selectedNucleo?.idEstablecimiento ?: ""
                )

                val insertResult = db.insertGalpon(nuevoGalpon)
                if (insertResult != -1L) {
                    showCustomToast(requireContext(), "Galpón guardado exitosamente", "success")
                    cargarGalpones()
                    alertDialog.dismiss()
                } else {
                    showCustomToast(requireContext(), "Error al guardar el galpón", "error")
                }
            }
        }
        alertDialog.show()
    }


    private fun showEditGalponDialog(galpon: GalponEntity) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.modal_galpon, null)
        val etGalponName = dialogView.findViewById<EditText>(R.id.inputNombreGalpon)
        val spinnerNucleo = dialogView.findViewById<Spinner>(R.id.spinnerNucleo)
        val btnCancel = dialogView.findViewById<Button>(R.id.botonCancelar)
        val btnSave = dialogView.findViewById<Button>(R.id.botonGuardar)

        etGalponName.setText(galpon.nombre)

        // Populate the Spinner with data from the database
        val db = AppDatabase(requireContext())
        val nucleos = db.getAllNucleos()
        val nucleoNames = nucleos.map { it.nombre }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nucleoNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNucleo.adapter = adapter

        // Set the current selection if editing
        val currentNucleoIndex = nucleoNames.indexOfFirst { it == galpon.idEstablecimiento }
        if (currentNucleoIndex != -1) {
            spinnerNucleo.setSelection(currentNucleoIndex)
        }

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val nombreGalpon = etGalponName.text.toString().trim()
            val selectedNucleoName = spinnerNucleo.selectedItem.toString()
            val selectedNucleo = nucleos.find { it.nombre == selectedNucleoName }

            if (validarCampos(nombreGalpon, selectedNucleoName)) {
                val updateResult = db.updateGalpon(
                    galpon.copy(
                        nombre = nombreGalpon,
                        idEstablecimiento = selectedNucleo?.idEstablecimiento ?: ""
                    )
                )
                if (updateResult > 0) {
                    showCustomToast(requireContext(), "Galpón actualizado exitosamente", "success")
                } else {
                    showCustomToast(requireContext(), "Error al actualizar el galpón", "error")
                }
                cargarGalpones()
                alertDialog.dismiss()
            }
        }
        alertDialog.show()
    }


    private fun deleteGalpon (galpon: GalponEntity) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar este galpón?")

        builder.setPositiveButton("Eliminar") { dialog, _ ->
            val db = AppDatabase(requireContext())
            val deleteResult = db.deleteGalpon(galpon.idGalpon)
            if (deleteResult > 0) {
                showCustomToast(requireContext(), "Galpón eliminado exitosamente", "success")
            } else {
                showCustomToast(requireContext(), "Error al eliminar el galpón", "error")
            }
            cargarGalpones()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun cargarGalpones() {
        val db = AppDatabase(requireContext())
        val galpones = db.getAllGalpones()
        galponesAdapter.setGalpones(galpones)
    }

    private fun validarCampos(nombreGalpon: String, idNucleo: String): Boolean {
        if (nombreGalpon.isEmpty()) {
            Toast.makeText(context, "El número de galpón es obligatorio", LENGTH_SHORT)
                .show()
            return false
        }

        if (nombreGalpon.isEmpty()) {
            Toast.makeText(context, "El nombre del galpón es obligatorio", LENGTH_SHORT)
                .show()
            return false
        }

        if (nombreGalpon.length < 3) {
            Toast.makeText(
                context,
                "El nombre del galpón debe tener al menos 3 caracteres",
                LENGTH_SHORT
            ).show()
            return false
        }


        if (idNucleo.isEmpty()) {
            Toast.makeText(context, "El núcleo del galpón es obligatorio", LENGTH_SHORT)
                .show()
            return false
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}