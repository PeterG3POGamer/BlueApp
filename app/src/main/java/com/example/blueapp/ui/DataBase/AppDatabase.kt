package com.example.blueapp.ui.DataBase

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.blueapp.ui.DataBase.Entities.ClienteEntity
import com.example.blueapp.ui.DataBase.Entities.DataDetaPesoPollosEntity
import com.example.blueapp.ui.DataBase.Entities.DataPesoPollosEntity
import com.example.blueapp.ui.DataBase.Entities.PesosEntity
import com.example.blueapp.ui.DataBase.Entities.impresoraEntity
import com.example.blueapp.ui.DataBase.Entities.pesoUsedEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "blueapp2.db"
        private const val DATABASE_VERSION = 7  // Incrementamos la versión de la base de datos para reflejar el cambio

        // Table names
        private const val TABLE_DETA_PESO_POLLOS = "DataDetaPesoPollos"
        private const val TABLE_PESO_POLLOS = "DataPesoPollos"
        private const val TABLE_CLIENTE = "Cliente"
        private const val TABLE_PESOS = "ListPesos"
        private const val TABLE_IMPRESORA = "ImpresoraConfig"
        private const val TABLE_USED_PESOS = "PesoUsado"

        // Common column names
        private const val KEY_ID = "id"

        // DataDetaPesoPollos Table - column names
        private const val KEY_NUMERO_JABAS = "numeroJabas"
        private const val KEY_NUMERO_POLLOS = "numeroPollos"
        private const val KEY_PESO_KG = "pesoKg"
        private const val KEY_CON_POLLOS = "conPollos"
        private const val KEY_ID_PESO_POLLO = "idPesoPollo"  // Actualizamos el campo

        // DataPesoPollos Table - column names
        private const val KEY_SERIE = "serie"  // Nuevo campo según entidad
        private const val KEY_FECHA = "fecha"  // Nuevo campo según entidad
        private const val KEY_TOTAL_JABAS = "totalJabas"  // Nuevo campo según entidad
        private const val KEY_TOTAL_POLLOS = "totalPollos"  // Nuevo campo según entidad
        private const val KEY_TOTAL_PESO = "totalPeso"  // Nuevo campo según entidad
        private const val KEY_TIPO = "tipo"  // Actualizamos el campo
        private const val KEY_NUMERO_DOC_CLIENTE = "numeroDocCliente"
        private const val KEY_ID_GALPON = "idGalpon"
        private const val KEY_ID_NUCLEO = "idNucleo"
        private const val KEY_NOMBRE_COMPLETO = "nombreCompleto"
        private const val KEY_PRECIO_K_POLLO = "PrecioKPollo"
        private const val KEY_TOTAL_PESOJABAS = "totalPesoJabas"
        private const val KEY_TOTAL_NETO = "totalNeto"
        private const val KEY_TOTAL_PAGAR = "TotalPagar"

        // Cliente Table - column names
        private const val KEY_FECHA_REGISTRO = "fechaRegistro"

        // ListPesos Table - column names
        private const val KEY_DATA_PESO_JSON = "dataJsonPeso"
        private const val KEY_DATA_DETAPESO_JSON = "dataJsonDetaPeso"

        private const val KEY_IMPRESORA_IP = "ip"
        private const val KEY_IMPRESORA_PUERTO = "puerto"

        private const val KEY_DEVICE_NAME = "deviceName"

    }

    override fun onCreate(db: SQLiteDatabase) {
        // Creating tables
        val CREATE_TABLE_DETA_PESO_POLLOS = ("CREATE TABLE $TABLE_DETA_PESO_POLLOS("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$KEY_NUMERO_JABAS TEXT, "
                + "$KEY_NUMERO_POLLOS TEXT, "
                + "$KEY_PESO_KG TEXT, "
                + "$KEY_CON_POLLOS INTEGER, "
                + "$KEY_ID_PESO_POLLO INTEGER)")
        db.execSQL(CREATE_TABLE_DETA_PESO_POLLOS)

        val CREATE_TABLE_PESO_POLLOS = ("CREATE TABLE $TABLE_PESO_POLLOS("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$KEY_SERIE TEXT, "
                + "$KEY_FECHA TEXT, "
                + "$KEY_TOTAL_JABAS TEXT, "
                + "$KEY_TOTAL_POLLOS TEXT, "
                + "$KEY_TOTAL_PESO TEXT, "
                + "$KEY_TIPO INTEGER, "
                + "$KEY_PRECIO_K_POLLO TEXT, "
                + "$KEY_NUMERO_DOC_CLIENTE TEXT, "
                + "$KEY_ID_GALPON TEXT, "
                + "$KEY_NOMBRE_COMPLETO TEXT)")
        db.execSQL(CREATE_TABLE_PESO_POLLOS)

        val CREATE_TABLE_CLIENTE = ("CREATE TABLE $TABLE_CLIENTE("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$KEY_NUMERO_DOC_CLIENTE TEXT UNIQUE, "
                + "$KEY_NOMBRE_COMPLETO TEXT, "
                + "$KEY_FECHA_REGISTRO TEXT)")
        db.execSQL(CREATE_TABLE_CLIENTE)

        val CREATE_TABLE_PESOS = ("CREATE TABLE $TABLE_PESOS("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$KEY_NUMERO_DOC_CLIENTE TEXT UNIQUE, "
                + "$KEY_NOMBRE_COMPLETO TEXT, "
                + "$KEY_DATA_PESO_JSON TEXT, "
                + "$KEY_DATA_DETAPESO_JSON TEXT, "
                + "$KEY_FECHA_REGISTRO TEXT)")
        db.execSQL(CREATE_TABLE_PESOS)

        val CREATE_TABLE_IMPRESORA_CONFIG = ("CREATE TABLE $TABLE_IMPRESORA("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$KEY_IMPRESORA_IP TEXT, "
                + "$KEY_IMPRESORA_PUERTO TEXT)")
        db.execSQL(CREATE_TABLE_IMPRESORA_CONFIG)

        val CREATE_TABLE_USED_PESOS = ("CREATE TABLE $TABLE_USED_PESOS("
                + "$KEY_ID INTEGER PRIMARY KEY, "
                + "$KEY_DEVICE_NAME TEXT, "
                + "$KEY_DATA_PESO_JSON TEXT, "
                + "$KEY_DATA_DETAPESO_JSON TEXT, "
                + "$KEY_FECHA_REGISTRO TEXT)")
        db.execSQL(CREATE_TABLE_USED_PESOS)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < newVersion) {
            // En caso de que la versión anterior fuera 1, añadimos los nuevos campos
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DETA_PESO_POLLOS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PESO_POLLOS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CLIENTE")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PESOS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_IMPRESORA")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USED_PESOS")
            onCreate(db)
        }
    }

    // =========================================================
    // INSERTS
    // =========================================================

    // Insert functions
    fun insertDataDetaPesoPollos(data: DataDetaPesoPollosEntity): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NUMERO_JABAS, data.cantJabas)
            put(KEY_NUMERO_POLLOS, data.cantPollos)
            put(KEY_PESO_KG, data.peso)
            put(KEY_CON_POLLOS, data.tipo)
        }
        return db.insert(TABLE_DETA_PESO_POLLOS, null, values)
    }

    fun insertDataPesoPollos(data: DataPesoPollosEntity): Long {
        val db = this.writableDatabase
        val currentDate = getCurrentDateTime()
        val values = ContentValues().apply {
            put(KEY_SERIE, data.serie)
            put(KEY_FECHA, currentDate)
            put(KEY_TOTAL_JABAS, data.totalJabas)
            put(KEY_TOTAL_POLLOS, data.totalPollos)
            put(KEY_TOTAL_PESO, data.totalPeso)
            put(KEY_TIPO, data.tipo)
            put(KEY_NUMERO_DOC_CLIENTE, data.numeroDocCliente)
            put(KEY_ID_GALPON, data.idGalpon)
            put(KEY_NOMBRE_COMPLETO, data.nombreCompleto)
            put(KEY_PRECIO_K_POLLO, data.PKPollo)
        }
        return db.insert(TABLE_PESO_POLLOS, null, values)
    }

    // Inserción de Cliente
    fun insertCliente(cliente: ClienteEntity): Long {
        val db = this.writableDatabase
        val currentDate = getCurrentDateTime()

        val values = ContentValues().apply {
            put(KEY_NUMERO_DOC_CLIENTE, cliente.numeroDocCliente)
            put(KEY_NOMBRE_COMPLETO, cliente.nombreCompleto)
            put(KEY_FECHA_REGISTRO, currentDate)
        }
        return db.insert(TABLE_CLIENTE, null, values)
    }

    // Inserción de ListPesos
    fun insertListPesos(pesos: PesosEntity): Long {
        val db = this.writableDatabase
        val currentDate = getCurrentDateTime()

        val values = ContentValues().apply {
            put(KEY_NUMERO_DOC_CLIENTE, pesos.numeroDocCliente)
            put(KEY_NOMBRE_COMPLETO, pesos.nombreCompleto)
            put(KEY_DATA_PESO_JSON, pesos.dataPesoJson)
            put(KEY_DATA_DETAPESO_JSON, pesos.dataDetaPesoJson)
            put(KEY_FECHA_REGISTRO, currentDate)
        }
        return db.insert(TABLE_PESOS, null, values)
    }

    fun addImpresora(impresora: impresoraEntity): Long {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(KEY_ID, impresora.idImpresora)
            put(KEY_IMPRESORA_IP, impresora.ip)
            put(KEY_IMPRESORA_PUERTO, impresora.puerto)
        }
        return db.insert(TABLE_IMPRESORA, null, values)
    }

    fun addPesoUsed(pesoUsed: pesoUsedEntity): Long {
        val db = this.writableDatabase
        val currentDate = getCurrentDateTime()

        val values = ContentValues().apply {
            put(KEY_ID, pesoUsed.idPesoUsed)
            put(KEY_DEVICE_NAME, pesoUsed.devicedName)
            put(KEY_DATA_PESO_JSON, pesoUsed.dataPesoPollosJson)
            put(KEY_DATA_DETAPESO_JSON, pesoUsed.dataDetaPesoPollosJson)
            put(KEY_FECHA_REGISTRO, currentDate)

        }
        return db.insert(TABLE_USED_PESOS, null, values)
    }

    // =========================================================
    // GET DATE TIME
    // =========================================================

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    // =========================================================
    // GET
    // =========================================================

    fun getAllDataDetaPesoPollos(): List<DataDetaPesoPollosEntity> {
        val dataList = mutableListOf<DataDetaPesoPollosEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_DETA_PESO_POLLOS"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val data = DataDetaPesoPollosEntity(
                    idDetaPP = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    cantJabas = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NUMERO_JABAS)),
                    cantPollos = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NUMERO_POLLOS)),
                    peso = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PESO_KG)),
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CON_POLLOS))
                )
                dataList.add(data)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return dataList
    }
    fun getAllDataPesoPollos(): List<DataPesoPollosEntity> {
        val dataList = mutableListOf<DataPesoPollosEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_PESO_POLLOS"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val data = DataPesoPollosEntity(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    serie = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SERIE)),  // Leemos el nuevo campo
                    fecha = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FECHA)),  // Leemos el nuevo campo
                    totalJabas = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOTAL_JABAS)),  // Leemos el nuevo campo
                    totalPollos = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOTAL_POLLOS)),  // Leemos el nuevo campo
                    totalPeso = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOTAL_PESO)),  // Leemos el nuevo campo
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TIPO)),  // Leemos el campo actualizado
                    numeroDocCliente = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NUMERO_DOC_CLIENTE)),
                    idGalpon = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID_GALPON)),
                    idNucleo = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID_NUCLEO)),
                    nombreCompleto = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOMBRE_COMPLETO)),
                    PKPollo = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PRECIO_K_POLLO)),
                    totalPesoJabas = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOTAL_PESOJABAS)),
                    totalNeto = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOTAL_NETO)),
                    TotalPagar = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOTAL_PAGAR)),
                )
                dataList.add(data)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return dataList
    }
    fun getAllClientes(): List<ClienteEntity> {
        val clienteList = mutableListOf<ClienteEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_CLIENTE"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val cliente = ClienteEntity(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    numeroDocCliente = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NUMERO_DOC_CLIENTE)),
                    nombreCompleto = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOMBRE_COMPLETO)),
                    fechaRegistro = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FECHA_REGISTRO))
                )
                clienteList.add(cliente)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return clienteList
    }
    fun getPesosAll(): List<PesosEntity> {
        val pesosList = mutableListOf<PesosEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_PESOS"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val pesos = PesosEntity(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    idNucleo = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID_NUCLEO)),
                    idGalpon = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID_GALPON)),
                    numeroDocCliente = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NUMERO_DOC_CLIENTE)),
                    nombreCompleto = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOMBRE_COMPLETO)),
                    dataPesoJson = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATA_PESO_JSON)),
                    dataDetaPesoJson = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATA_DETAPESO_JSON)),
                    fechaRegistro = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FECHA_REGISTRO))
                )
                pesosList.add(pesos)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return pesosList
    }

    fun getPesosUsedAll(): List<pesoUsedEntity> {
        val pesosUsedList = mutableListOf<pesoUsedEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_USED_PESOS"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val pesos = pesoUsedEntity(
                    idPesoUsed = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    devicedName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEVICE_NAME)),
                    dataPesoPollosJson = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATA_PESO_JSON)),
                    dataDetaPesoPollosJson = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATA_DETAPESO_JSON)),
                    fechaRegistro = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FECHA_REGISTRO))
                )
                pesosUsedList.add(pesos)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return pesosUsedList
    }

    fun getClienteById(numeroDocCliente: String): ClienteEntity? {
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_CLIENTE WHERE $KEY_NUMERO_DOC_CLIENTE = ?"
        val cursor = db.rawQuery(selectQuery, arrayOf(numeroDocCliente))

        var cliente: ClienteEntity? = null
        if (cursor.moveToFirst()) {
            cliente = ClienteEntity(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                numeroDocCliente = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NUMERO_DOC_CLIENTE)),
                nombreCompleto = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOMBRE_COMPLETO)),
                fechaRegistro = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FECHA_REGISTRO))
            )
        }
        cursor.close()
        return cliente
    }
    fun getImpresoraById(id: String): impresoraEntity? {
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_IMPRESORA WHERE $KEY_ID = ?"
        val cursor = db.rawQuery(selectQuery, arrayOf(id))

        var impresora: impresoraEntity? = null
        if (cursor.moveToFirst()) {
            impresora = impresoraEntity(
                idImpresora = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                ip = cursor.getString(cursor.getColumnIndexOrThrow(KEY_IMPRESORA_IP)),
                puerto = cursor.getString(cursor.getColumnIndexOrThrow(KEY_IMPRESORA_PUERTO))
            )
        }
        cursor.close()
        return impresora
    }

    // =========================================================
    // UPDATE
    // =========================================================

    fun updateImpresora(impresora: impresoraEntity): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(KEY_IMPRESORA_IP, impresora.ip)
            put(KEY_IMPRESORA_PUERTO, impresora.puerto)
        }
        return db.update(
            TABLE_IMPRESORA,
            contentValues,
            "$KEY_ID = ?",
            arrayOf(impresora.idImpresora.toString())
        )
    }

    // =========================================================
    // DROPS
    // =========================================================


    // eliminar Peso por id:
    fun deletePesosById(id: Int): Int {
        val db = this.writableDatabase
        val whereClause = "$KEY_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return db.delete(TABLE_PESOS, whereClause, whereArgs)
    }

    // Eliminar un Cliente
    fun deleteCliente(clienteId: Int): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_CLIENTE, "$KEY_ID = ?", arrayOf(clienteId.toString()))
    }

    // Delete functions
    fun deleteAllData() {
        val db = this.writableDatabase
        db.delete(AppDatabase.TABLE_DETA_PESO_POLLOS, null, null)
        db.delete(AppDatabase.TABLE_PESO_POLLOS, null, null)
        db.delete(AppDatabase.TABLE_CLIENTE, null, null)
        db.close()
    }

    fun deleteAllPesoUsed() {
//        val db = this.writableDatabase
//        try {
//            db.beginTransaction()
//            db.execSQL("DELETE FROM ${AppDatabase.TABLE_USED_PESOS}")
//
//            // Reiniciar los IDs auto-incrementales
////            db.execSQL("DELETE FROM sqlite_sequence WHERE name='${AppDatabase.TABLE_USED_PESOS}'")
//
//            db.execSQL("VACUUM")
//            db.setTransactionSuccessful()
//        } catch (e: Exception) {
//            // Manejar la excepción si es necesario
//            e.printStackTrace()
//        } finally {
//            db.endTransaction()
//            db.close()
//        }
        val db = this.writableDatabase
        db.delete(AppDatabase.TABLE_USED_PESOS, null, null)
        db.close()
    }
}

