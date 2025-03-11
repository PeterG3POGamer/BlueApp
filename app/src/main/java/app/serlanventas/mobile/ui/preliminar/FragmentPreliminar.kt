package app.serlanventas.mobile.ui.preliminar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.pesoUsedEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost
import app.serlanventas.mobile.ui.Jabas.ManagerPost.removeListPesosId
import app.serlanventas.mobile.ui.Jabas.ManagerPost.saveLocally
import app.serlanventas.mobile.ui.Services.getAddressMacDivice
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel
import app.serlanventas.mobile.ui.ViewModel.TabViewModel
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject

class FragmentPreliminar : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var tabViewModel: TabViewModel
    private lateinit var reportesAdapter: ReportesAdapter
    private val editTextIds = listOf(
        R.id.inputDniCliente,
        R.id.inputNomCliente,
        R.id.inputCantPollo,
        R.id.inputPesoBruto,
        R.id.inputTara,
        R.id.inputNeto,
        R.id.inputKlPollo,
        R.id.inputTotalPagar,
        R.id.inputPesoPromedioPollo
    )

    companion object {
        private const val REQUEST_CODE_STORAGE_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_peso_preliminar, container, false)
        val boton_Volver = view.findViewById<MaterialButton>(R.id.boton_Volver)
        val boton_Procesar = view.findViewById<MaterialButton>(R.id.boton_Procesar)
        reportesAdapter = ReportesAdapter()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewReportes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = reportesAdapter

        tabViewModel = ViewModelProvider(requireActivity()).get(TabViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Observar los datos del ViewModel y actualizar los inputs

        sharedViewModel.getDataDetaPesoPollosJson()?.takeIf { it.isNotEmpty() }
            ?.let { dataDetaPesoPollosJson ->
                val dataDetaPesoPollos = JSONArray(dataDetaPesoPollosJson)
                val detallesList = procesarDataDetaPesoPollos(dataDetaPesoPollos)

                val (totalJabas, totalPollos, totalPesoPollos, totalPesoJabas, neto) = calcularTotales(
                    detallesList
                )
                sharedViewModel.setTotales(totalJabas, totalPollos, totalPesoPollos, neto)


                sharedViewModel.getDataPesoPollosJson()?.takeIf { it.isNotEmpty() }
                    ?.let { dataPesoPollosJson ->
                        val dataPesoPollos = JSONObject(dataPesoPollosJson)
                        val pesoKPollo = dataPesoPollos.getDouble("_PP_PKPollo")
                        val TotalPagar = pesoKPollo * neto

                        val promedioPesoPollo = if (totalPollos > 0) neto / totalPollos else 0.0

                        dataPesoPollos.put("_PP_totalJabas", totalJabas)
                        dataPesoPollos.put("_PP_totalPollos", totalPollos)
                        dataPesoPollos.put("_PP_totalPeso", formatDecimal(totalPesoPollos))
                        dataPesoPollos.put("_PP_totalNeto", formatDecimal(neto))
                        dataPesoPollos.put("_PP_totalPesoJabas", formatDecimal(totalPesoJabas))
                        dataPesoPollos.put("_PP_PKPollo", formatDecimal(pesoKPollo))
                        dataPesoPollos.put("_PP_TotalPagar", formatDecimal(TotalPagar))
                        dataPesoPollos.put("_PP_PromedioPollo", formatDecimal(promedioPesoPollo))

                        sharedViewModel.setDataPesoPollosJson(dataPesoPollos.toString())

                        distribuirDatosEnInputs(dataPesoPollos, view)
                    }
                // Actualizar los detalles en el adapter
                reportesAdapter.actualizarReportesDetapp(detallesList)
            }

        requestStoragePermission()
        bloquearInputs(view)

        boton_Volver.setOnClickListener {
            limpiarCampos()
            findNavController().navigate(R.id.action_nav_initPreliminar_to_nav_initReportePeso)
//            tabViewModel.setNavigateToTab(1)

        }

        boton_Procesar.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                requestStoragePermission()
                procesarDatos()
            } else {
                procesarDatos()
            }
            db.deleteAllPesoUsed()
        }


        return view
    }


    fun limpiarCampos() {
        view?.findViewById<EditText>(R.id.inputDniCliente)?.setText("")
        view?.findViewById<EditText>(R.id.inputNomCliente)?.setText("")
        view?.findViewById<EditText>(R.id.inputCantPollo)?.setText("")
        view?.findViewById<EditText>(R.id.inputPesoBruto)?.setText("")
        view?.findViewById<EditText>(R.id.inputTara)?.setText("")
        view?.findViewById<EditText>(R.id.inputNeto)?.setText("")
        view?.findViewById<EditText>(R.id.inputKlPollo)?.setText("")
        view?.findViewById<EditText>(R.id.inputTotalPagar)?.setText("")
        view?.findViewById<EditText>(R.id.inputPesoPromedioPollo)?.setText("")
        reportesAdapter.actualizarReportesDetapp(emptyList())
    }

    private fun procesarDatos() {
        val dataPesoPollosJson = sharedViewModel.getDataPesoPollosJson()
        val dataDetaPesoPollosJson = sharedViewModel.getDataDetaPesoPollosJson()

        Log.d("Preliminar dataPesoPollosJson", dataPesoPollosJson.toString());
        Log.d("Preliminar dataDetaPesoPollosJson", dataDetaPesoPollosJson.toString());

        if (!dataDetaPesoPollosJson.isNullOrEmpty() && !dataPesoPollosJson.isNullOrEmpty()) {
        }
        val dataPesoPollos = JSONObject(dataPesoPollosJson)
        val dataDetaPesoPollos = JSONArray(dataDetaPesoPollosJson)

        var numeroDocCliente = dataPesoPollos.optString("_PP_docCliente")
        var nombreCompleto = dataPesoPollos.optString("_PP_nombreCompleto", null)
        var idNucleo = dataPesoPollos.optString("_PP_idNucleo")

        val dataPesoPollosEntity = procesarDataPesoPollos(dataPesoPollos)
        val dataDetaPesoPollosList = convertirObejtoPesoPollos(dataDetaPesoPollos)

        // Enviar datos al servidor
        val idPeso = sharedViewModel.getIdListPesos()

//      sendDataToServer(requireContext(), JabasFragment(), dataDetaPesoPollosList, dataPesoPollosEntity)

        saveLocally(
            requireContext(),
            dataDetaPesoPollosList, dataPesoPollosEntity, numeroDocCliente, nombreCompleto, idNucleo
        )


        // CAMBIA EL ESTADO UNA VES DE HAYA COBRADO
        val idDevice = getAddressMacDivice.getDeviceId(requireContext())
        if (idPeso != 0) {
            ManagerPost.setStatusUsed(
                requireContext(),
                idPeso!!,
                "NotUsed",
                idDevice
            ) { success ->
                if (!success) {
                    Log.d("StatusLog", "Error al cambiar el estado del peso")
                }
            }

            removeListPesosId(requireContext(), idPeso) { success ->
                if (success) {

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al eliminar el peso",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            sharedViewModel.setIdListPesos(0)
        }

        Toast.makeText(requireContext(), "Procesando...", Toast.LENGTH_SHORT).show()
        limpiarCampos()
        val antiguoJson = sharedViewModel.getDataPesoPollosJson()

        // Conviertes el JSON antiguo a un objeto JSONObject si no está vacío
        val jsonAntiguo = if (antiguoJson!!.isNotEmpty()) {
            JSONObject(antiguoJson)
        } else {
            JSONObject()
        }


        // Creas un nuevo objeto JSONObject combinando los datos antiguos y nuevos
        val nuevoJson = JSONObject()
        nuevoJson.put("_PP_id", jsonAntiguo.optInt("_PP_id", 0))
        nuevoJson.put("_PP_idNucleo", jsonAntiguo.optString("_PP_idNucleo", ""))
        nuevoJson.put("_PP_IdGalpon", jsonAntiguo.optString("_PP_IdGalpon", ""))
        nuevoJson.put("_PP_PKPollo", jsonAntiguo.optString("_PP_PKPollo", ""))

        // Actualizas el JSON en tu ViewModel
        sharedViewModel.setDataPesoPollosJson(nuevoJson.toString())
        sharedViewModel.setDataDetaPesoPollosJson("")

        db.deleteAllPesoUsed()

        findNavController().navigate(R.id.action_nav_initPreliminar_to_nav_initReportePeso)
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, proceed with your app logic
                procesarDatos()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun procesarDataPesoPollos(jsonObject: JSONObject): DataPesoPollosEntity {
        return DataPesoPollosEntity(
            id = jsonObject.getInt("_PP_id"),
            serie = jsonObject.optString("_PP_serie", "1234"),
            numero = jsonObject.optString("_PP_numero", "0"),
            fecha = jsonObject.optString("_PP_fecha", ""),
            totalJabas = jsonObject.optString("_PP_totalJabas"),
            totalPollos = jsonObject.optString("_PP_totalPollos"),
            totalPeso = jsonObject.optString("_PP_totalPeso"),
            tipo = jsonObject.optString("_PP_tipo"),
            numeroDocCliente = jsonObject.optString("_PP_docCliente"),
            nombreCompleto = jsonObject.optString("_PP_nombreCompleto", null),
            idGalpon = jsonObject.optString("_PP_IdGalpon"),
            idNucleo = jsonObject.optString("_PP_idNucleo"),
            PKPollo = jsonObject.optString("_PP_PKPollo"),
            totalPesoJabas = jsonObject.optString("_PP_totalPesoJabas"),
            totalNeto = jsonObject.optString("_PP_totalNeto"),
            TotalPagar = jsonObject.optString("_PP_TotalPagar"),
            idUsuario = jsonObject.optString("_PP_idUsuario"),
            idEstado = jsonObject.optString("_PP_idEstado"),
        )
    }

    // Convierte JSONArray a List<DataDetaPesoPollosEntity>
    private fun convertirObejtoPesoPollos(jsonArray: JSONArray): List<DataDetaPesoPollosEntity> {
        val detalles = mutableListOf<DataDetaPesoPollosEntity>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val detalle = DataDetaPesoPollosEntity(
                idDetaPP = jsonObject.getInt("_DPP_id"),
                cantJabas = jsonObject.getInt("_DPP_cantJabas"),
                cantPollos = jsonObject.getInt("_DPP_cantPolllos"),
                peso = jsonObject.getDouble("_DPP_peso"),
                tipo = jsonObject.getString("_DPP_tipo"),
                idPesoPollo = jsonObject.optString("_DPP_idPesoPollo", ""),
                fechaPeso = jsonObject.optString("_DPP_fechPeso", "")

            )
            detalles.add(detalle)
        }
        return detalles
    }

    data class TotalesData(
        val totalJabas: Int,
        val totalPollos: Int,
        val totalPeso: Double,
        val totalPesoJabas: Double,
        val neto: Double
    )

    private fun calcularTotales(dataDetaPesoPollos: List<DataDetaPesoPollosEntity>): TotalesData {
        var totalJabas = 0
        var totalPollos = 0
        var totalPesoPollos = 0.0
        var totalPesoJabas = 0.0

        dataDetaPesoPollos.forEach { detaPesoPollo ->
            if (detaPesoPollo.tipo == "JABAS CON POLLOS") {
                totalPollos += detaPesoPollo.cantJabas * detaPesoPollo.cantPollos
                totalPesoPollos += detaPesoPollo.peso.toDouble()
            } else {
                totalJabas += detaPesoPollo.cantJabas.toInt()
                totalPesoJabas += detaPesoPollo.peso.toDouble()
            }
        }

        val neto = totalPesoPollos - totalPesoJabas

        return TotalesData(totalJabas, totalPollos, totalPesoPollos, totalPesoJabas, neto)
    }

    private fun distribuirDatosEnInputs(dataPesoPollos: JSONObject, view: View) {
        view.findViewById<EditText>(R.id.inputDniCliente)
            ?.setText(dataPesoPollos.optString("_PP_docCliente"))
        view.findViewById<EditText>(R.id.inputNomCliente)
            ?.setText(dataPesoPollos.optString("_PP_nombreCompleto"))

        view.findViewById<EditText>(R.id.inputCantPollo)?.setText(
            formatDecimal(
                dataPesoPollos.optString("_PP_totalPollos").replace(",", ".").toDoubleOrNull()
            )
        )
        view.findViewById<EditText>(R.id.inputPesoBruto)?.setText(
            formatDecimal(
                dataPesoPollos.optString("_PP_totalPeso").replace(",", ".").toDoubleOrNull()
            )
        )
        view.findViewById<EditText>(R.id.inputTara)?.setText(
            formatDecimal(
                dataPesoPollos.optString("_PP_totalPesoJabas").replace(",", ".").toDoubleOrNull()
            )
        )
        view.findViewById<EditText>(R.id.inputNeto)?.setText(
            formatDecimal(
                dataPesoPollos.optString("_PP_totalNeto").replace(",", ".").toDoubleOrNull()
            )
        )
        view.findViewById<EditText>(R.id.inputKlPollo)?.setText(
            formatDecimal(
                dataPesoPollos.optString("_PP_PKPollo").replace(",", ".").toDoubleOrNull()
            )
        )
        view.findViewById<EditText>(R.id.inputTotalPagar)?.setText(
            formatDecimal(
                dataPesoPollos.optString("_PP_TotalPagar").replace(",", ".").toDoubleOrNull()
            )
        )
        view.findViewById<EditText>(R.id.inputPesoPromedioPollo)?.setText(
            formatDecimal(
                dataPesoPollos.optString("_PP_PromedioPollo").replace(",", ".").toDoubleOrNull()
            )
        )
    }


    private fun formatDecimal(number: Double?): String {
        return number?.let { String.format("%.2f", it) } ?: ""
    }

    private fun procesarDataDetaPesoPollos(jsonArray: JSONArray): List<DataDetaPesoPollosEntity> {
        val detalles = mutableListOf<DataDetaPesoPollosEntity>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val detalle = DataDetaPesoPollosEntity(
                idDetaPP = jsonObject.getInt("_DPP_id"),
                cantJabas = jsonObject.getInt("_DPP_cantJabas"),
                cantPollos = jsonObject.getInt("_DPP_cantPolllos"),
                peso = jsonObject.getDouble("_DPP_peso"),
                tipo = jsonObject.getString("_DPP_tipo"),
                idPesoPollo = jsonObject.optString("_DPP_idPesoPollo", ""),
                fechaPeso = jsonObject.optString("_DPP_fechPeso", "")

            )
            detalles.add(detalle)
        }
        return detalles
    }

    private fun bloquearInputs(view: View) {
        editTextIds.forEach { editTextId ->
            view.findViewById<EditText>(editTextId)?.isEnabled = false
        }
    }

    override fun onStop() {
        super.onStop()
        var idPesoTemp = sharedViewModel.getIdListPesos() ?: 0
        var dataPesoPollosJsonTemp = sharedViewModel.getDataPesoPollosJson() ?: ""
        var dataDetaPesoPollosJsonTemp = sharedViewModel.getDataDetaPesoPollosJson() ?: ""
        val device = getAddressMacDivice.getDeviceId(requireContext())

        if (idPesoTemp != 0) {
            if (!dataPesoPollosJsonTemp.isNullOrBlank() && !dataDetaPesoPollosJsonTemp.isNullOrBlank()) {
                val pesoUsed = pesoUsedEntity(
                    idPesoUsed = idPesoTemp,
                    devicedName = device,
                    dataPesoPollosJson = dataPesoPollosJsonTemp,
                    dataDetaPesoPollosJson = dataDetaPesoPollosJsonTemp,
                    fechaRegistro = ""
                )
                db.deleteAllPesoUsed()
                db.addPesoUsed(pesoUsed)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }
}
