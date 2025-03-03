package app.serlanventas.mobile.ui.ModuleNucleos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentModuleNucleosBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast

class NucleosFragment : Fragment() {

    private var _binding: FragmentModuleNucleosBinding? = null
    private val binding get() = _binding!!
    private lateinit var nucleosAdapter: NucleosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModuleNucleosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddNucleo.setOnClickListener {
            showAddGalponDialog()
        }

        nucleosAdapter = NucleosAdapter(emptyList(), ::showEditNucleoDialog, ::deleteNucleo)
        binding.recyclerViewNucleo.adapter = nucleosAdapter
        binding.recyclerViewNucleo.layoutManager = LinearLayoutManager(context)

        cargarNucleos()
    }

    private fun showAddGalponDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.modal_nucleo, null)
        val etNucleoName = dialogView.findViewById<EditText>(R.id.inputNombreNucleo)
        val btnCancel = dialogView.findViewById<Button>(R.id.botonCancelar)
        val btnSave = dialogView.findViewById<Button>(R.id.botonGuardar)

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val db = AppDatabase(requireContext())

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val nombreNucleo = etNucleoName.text.toString().trim()

            if (validarCampos(nombreNucleo)) {
                val nuevoNucleo = NucleoEntity(
                    idEstablecimiento = "",
                    nombre = nombreNucleo,
                    idEmpresa = ""
                )

                val insertResult = db.insertNucleo(nuevoNucleo)
                if (insertResult != -1L) {
                    showCustomToast(requireContext(), "Galpón guardado exitosamente", "success")
                    cargarNucleos()
                    alertDialog.dismiss()
                } else {
                    showCustomToast(requireContext(), "Error al guardar el galpón", "error")
                }
            }
        }
        alertDialog.show()
    }


    private fun showEditNucleoDialog(nucleo: NucleoEntity) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.modal_nucleo, null)
        val etNucleoName = dialogView.findViewById<EditText>(R.id.inputNombreNucleo)
        val btnCancel = dialogView.findViewById<Button>(R.id.botonCancelar)
        val btnSave = dialogView.findViewById<Button>(R.id.botonGuardar)

        etNucleoName.setText(nucleo.nombre)

        val db = AppDatabase(requireContext())

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val nombreNucleo = etNucleoName.text.toString().trim()

            if (validarCampos(nombreNucleo)) {
                val updateResult = db.updateNucleo(
                    nucleo.copy(
                        nombre = nombreNucleo
                    )
                )
                if (updateResult > 0) {
                    showCustomToast(requireContext(), "Galpón actualizado exitosamente", "success")
                } else {
                    showCustomToast(requireContext(), "Error al actualizar el galpón", "error")
                }
                cargarNucleos()
                alertDialog.dismiss()
            }
        }
        alertDialog.show()
    }


    private fun deleteNucleo (nucleo: NucleoEntity) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar este núcleo?")

        builder.setPositiveButton("Eliminar") { dialog, _ ->
            val db = AppDatabase(requireContext())
            val deleteResult = db.deleteNucleo(nucleo.idEstablecimiento)
            if (deleteResult > 0) {
                showCustomToast(requireContext(), "Núcleo eliminado exitosamente", "success")
            } else {
                showCustomToast(requireContext(), "Error al eliminar el núcleo", "error")
            }
            cargarNucleos()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun cargarNucleos() {
        val db = AppDatabase(requireContext())
        val galpones = db.getAllNucleos()
        nucleosAdapter.setNucleos(galpones)
    }

    private fun validarCampos(nombreNucleo: String): Boolean {
        if (nombreNucleo.isEmpty()) {
            Toast.makeText(context, "El número de núcleo es obligatorio", LENGTH_SHORT)
                .show()
            return false
        }

        if (nombreNucleo.length < 3) {
            Toast.makeText(
                context,
                "El nombre del núcleo debe tener al menos 3 caracteres",
                LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}