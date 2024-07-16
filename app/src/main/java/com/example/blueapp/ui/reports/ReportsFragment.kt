package com.example.blueapp.ui.reports

import androidx.fragment.app.Fragment

class ReportsFragment : Fragment() {
//
//    private lateinit var databaseHelper: DatabaseHelper
//    private lateinit var appDatabase: AppDatabase
//    private var db: SQLiteDatabase? = null
//    private lateinit var reportesAdapter: ReportesAdapter
//
//    private lateinit var botonRecarga: ImageButton
//    private lateinit var botonInsert: ImageButton
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_reportes, container, false)
//
//        // Initialize database and adapter
//        databaseHelper = DatabaseHelper(requireContext())
//        db = databaseHelper.writableDatabase
//        reportesAdapter = ReportesAdapter()
//
//        // UI references
//        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewReportes)
//        recyclerView.layoutManager = LinearLayoutManager(requireContext())
//        recyclerView.adapter = reportesAdapter
//
////        botonInsert = view.findViewById(R.id.boton_Insert)
////        botonInsert.setOnClickListener {
////            val jsonPesoPollos = """
////            [
////                {"serie": "00000001", "fecha": "2024-06-29", "totalJabas": "80", "totalPollos": "50", "totalPeso": "300.5", "tipo": "MIXTO", "docCliente": "95865144", "IdGalpon": "1"}
////            ]
////        """.trimIndent()
////
////            val jsonDetapp = """
////            [
////                {"cantJabas": "10", "cantPolllos": "0", "peso": "15.2", "tipo": "JABAS SIN POLLOS"},
////                {"cantJabas": "10", "cantPolllos": "0", "peso": "18.7", "tipo": "JABAS SIN POLLOS"},
////                {"cantJabas": "10", "cantPolllos": "0", "peso": "18.7", "tipo": "JABAS SIN POLLOS"},
////                {"cantJabas": "10", "cantPolllos": "0", "peso": "18.7", "tipo": "JABAS SIN POLLOS"},
////                {"cantJabas": "10", "cantPolllos": "0", "peso": "18.7", "tipo": "JABAS SIN POLLOS"},
////                {"cantJabas": "4", "cantPolllos": "10", "peso": "28.7", "tipo": "JABAS CON POLLOS"},
////                {"cantJabas": "4", "cantPolllos": "10", "peso": "38.7", "tipo": "JABAS CON POLLOS"},
////                {"cantJabas": "4", "cantPolllos": "10", "peso": "38.7", "tipo": "JABAS CON POLLOS"},
////                {"cantJabas": "4", "cantPolllos": "10", "peso": "25.7", "tipo": "JABAS CON POLLOS"},
////                {"cantJabas": "4", "cantPolllos": "10", "peso": "18.7", "tipo": "JABAS CON POLLOS"}
////            ]
////        """.trimIndent()
////
////            insertarReportesDesdeJson(jsonPesoPollos, jsonDetapp)
////        }
////
////        // Reload button
////        botonRecarga = view.findViewById(R.id.boton_ReCarga)
////        botonRecarga.setOnClickListener {
//////            cargarReportesDesdeSQLite()
////        }
//
//        // Load reports when the view is created
////        cargarReportesDesdeSQLite()
//
//        return view
//    }
//
//    private fun insertarReportesDesdeJson(jsonPesoPollos: String, jsonDetapp: String) {
//        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//
//        scope.launch {
//            try {
//                val jsonArrayPesoPollos = JSONArray(jsonPesoPollos)
//                val jsonArrayDetapp = JSONArray(jsonDetapp)
//
//                withContext(Dispatchers.Main) {
//                    databaseHelper.writableDatabase.use { db ->
//                        databaseHelper.insertarPesopollosYDetapp(jsonArrayPesoPollos, jsonArrayDetapp)
////                        cargarReportesDesdeSQLite()
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("ReportsFragment", "Error al insertar reportes desde JSON: ${e.message}")
//            }
//        }
//    }
//
////    private fun cargarReportesDesdeSQLite() {
////        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
////
////        scope.launch {
////            val reportes = databaseHelper.obtenerReportesPesopollos()
////
////            withContext(Dispatchers.Main) {
////                reportesAdapter.actualizarReportesPesopollos(reportes)
////            }
////        }
////    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        db?.close()
//    }
}
