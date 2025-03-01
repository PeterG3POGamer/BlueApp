package app.serlanventas.mobile.ui.ModuleClientes

import NetworkUtils
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentModuleClientesBinding
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import app.serlanventas.mobile.ui.Services.PreLoading
import app.serlanventas.mobile.ui.Utilidades.Constants
import org.json.JSONObject


class ClientesFragment : Fragment() {
    private var _binding: FragmentModuleClientesBinding? = null
    private val binding get() = _binding!!
    private lateinit var clientesAdapter: ClientesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModuleClientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnAddClient.setOnClickListener {
            showAddClientDialog()
        }

        clientesAdapter = ClientesAdapter(emptyList(), ::showEditClientDialog, ::deleteCliente)
        binding.recyclerViewClients.adapter = clientesAdapter
        binding.recyclerViewClients.layoutManager = LinearLayoutManager(context)

        cargarClientes()
    }

    private fun showAddClientDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.modal_cliente, null)
        val etDocumentNumber = dialogView.findViewById<EditText>(R.id.inputNumeroCliente)
        val etBusinessName = dialogView.findViewById<EditText>(R.id.inputNombreCliente)
        val btnSearch = dialogView.findViewById<Button>(R.id.botonBuscar)
        val btnCancel = dialogView.findViewById<Button>(R.id.botonCancelar)
        val btnSave = dialogView.findViewById<Button>(R.id.botonGuardar)
        btnSave.isEnabled = true
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        btnSearch.visibility = View.VISIBLE

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            btnSearch.visibility = View.GONE
        }


        btnSearch.setOnClickListener {
            val preLoading = PreLoading(requireContext())
            preLoading.showPreCarga()
            val numeroCliente = etDocumentNumber.text.toString().trim()

            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                val jsonParam = JSONObject()
                jsonParam.put("numeroDocumento", numeroCliente)

                val isProduction = Constants.obtenerEstadoModo(requireContext())
                val baseUrl = Constants.getBaseUrl(isProduction)

                var baseUrlCliente = "${baseUrl}controllers/FuncionesController/buscarCliente.php"
                ManagerPost.BuscarCliente(baseUrlCliente, jsonParam.toString()) { nombreCompleto ->
                    etBusinessName.setText(nombreCompleto ?: "")

                    preLoading.hidePreCarga()
                    if (nombreCompleto.isNullOrBlank()) {
                        showCustomToast(
                            requireContext(),
                            "No se encontró el cliente, Ingrese un nombre manualmente",
                            "info"
                        )
                        etDocumentNumber.isEnabled = true
                        etBusinessName.isEnabled = true
                        btnSave.isEnabled = false
                    } else {
                        etDocumentNumber.isEnabled = false
                        etBusinessName.isEnabled = false
                        btnSave.isEnabled = true
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val documento = etDocumentNumber.text.toString().trim()
            val razonsocial = etBusinessName.text.toString().trim()
            val db = AppDatabase(requireContext())

            if (validarCampos(documento, razonsocial)) {
                val nuevoCliente = ClienteEntity(
                    numeroDocCliente = documento,
                    nombreCompleto = razonsocial,
                    fechaRegistro = ""
                )
                val clienteExistente = db.getClienteById(documento)

                if (clienteExistente == null) {
                    val insertResult = db.insertCliente(nuevoCliente)

                    if (insertResult != -1L) {
                        showCustomToast(requireContext(), "Cliente guardado exitosamente", "success")
                    } else {
                        showCustomToast(requireContext(), "Error al guardar el cliente", "error")

                    }
                    cargarClientes()
                    alertDialog.dismiss()
                }else{
                    showCustomToast(requireContext(), "Ya existe un cliente con ese documento", "info")
                }
            }
        }
        alertDialog.show()
    }

    private fun showEditClientDialog(cliente: ClienteEntity) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.modal_cliente, null)
        val etDocumentNumber = dialogView.findViewById<EditText>(R.id.inputNumeroCliente)
        val etBusinessName = dialogView.findViewById<EditText>(R.id.inputNombreCliente)
        val btnSearch = dialogView.findViewById<Button>(R.id.botonBuscar)
        val btnCancel = dialogView.findViewById<Button>(R.id.botonCancelar)
        val btnSave = dialogView.findViewById<Button>(R.id.botonGuardar)

        btnSearch.visibility = View.VISIBLE

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            btnSearch.visibility = View.GONE
        }

        etDocumentNumber.setText(cliente.numeroDocCliente)
        etBusinessName.setText(cliente.nombreCompleto)

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val documento = etDocumentNumber.text.toString().trim()
            val razonsocial = etBusinessName.text.toString().trim()

            if (validarCampos(documento, razonsocial)) {
                val db = AppDatabase(requireContext())
                val updateResult = db.updateCliente(
                    cliente.copy(
                        numeroDocCliente = documento,
                        nombreCompleto = razonsocial
                    )
                )
                if (updateResult > 0) {
                    showCustomToast(requireContext(), "Cliente actualizado exitosamente", "success")
                } else {
                    showCustomToast(requireContext(), "Error al actualizar el cliente", "error")
                }
                cargarClientes()
                alertDialog.dismiss()
            }
        }
        alertDialog.show()
    }

    private fun deleteCliente(cliente: ClienteEntity) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar este cliente?")

        builder.setPositiveButton("Eliminar") { dialog, _ ->
            val db = AppDatabase(requireContext())
            val deleteResult = db.deleteCliente(cliente.id)
            if (deleteResult > 0) {
                showCustomToast(requireContext(), "Cliente eliminado exitosamente", "success")
            } else {
                showCustomToast(requireContext(), "Error al eliminar el cliente", "error")
            }
            cargarClientes()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun cargarClientes() {
        val db = AppDatabase(requireContext())
        val clientes = db.getAllClientes()
        clientesAdapter.setClientes(clientes)
    }

    private fun validarCampos(documento: String, razonsocial: String): Boolean {
        if (documento.isEmpty()) {
            Toast.makeText(context, "El número de documento es obligatorio", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (razonsocial.isEmpty()) {
            Toast.makeText(context, "El nombre del cliente es obligatorio", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (!documento.matches("\\d+".toRegex())) {
            Toast.makeText(
                context,
                "El número de documento debe contener solo números",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (razonsocial.length < 3) {
            Toast.makeText(
                context,
                "El nombre del cliente debe tener al menos 3 caracteres",
                Toast.LENGTH_SHORT
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
