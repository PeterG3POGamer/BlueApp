package app.serlanventas.mobile.ui.DataBase

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.UsuarioEntity
import app.serlanventas.mobile.ui.DataBase.Entities.impresoraEntity
import app.serlanventas.mobile.ui.DataBase.Entities.pesoUsedEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "SerlanVentas.db"
        private const val DATABASE_VERSION = 1

        // Table names
        private const val TABLE_DETA_PESO_POLLOS = "DataDetaPesoPollos"
        private const val TABLE_PESO_POLLOS = "DataPesoPollos"
        private const val TABLE_CLIENTE = "Cliente"
        private const val TABLE_PESOS = "ListPesos"
        private const val TABLE_IMPRESORA = "ImpresoraConfig"
        private const val TABLE_USED_PESOS = "PesoUsado"
        private const val TABLE_NUCLEO = "Nucleo"
        private const val TABLE_GALPON = "Galpon"
        private const val TABLE_USUARIO = "Usuario"

        // Common column names
        private const val KEY_ID = "id"

        // DataDetaPesoPollos Table - column names
        private const val KEY_NUMERO_JABAS = "numeroJabas"
        private const val KEY_NUMERO_POLLOS = "numeroPollos"
        private const val KEY_PESO_KG = "pesoKg"
        private const val KEY_CON_POLLOS = "conPollos"
        private const val KEY_ID_PESO_POLLO = "idPesoPollo"

        // DataPesoPollos Table - column names
        private const val KEY_SERIE = "serie"
        private const val KEY_FECHA = "fecha"
        private const val KEY_TOTAL_JABAS = "totalJabas"
        private const val KEY_TOTAL_POLLOS = "totalPollos"
        private const val KEY_TOTAL_PESO = "totalPeso"
        private const val KEY_TIPO = "tipo"
        private const val KEY_NUMERO_DOC_CLIENTE = "numeroDocCliente"
        private const val KEY_ID_GALPON = "idGalpon"
        private const val KEY_ID_NUCLEO = "idNucleo"
        private const val KEY_NOMBRE_COMPLETO = "nombreCompleto"
        private const val KEY_PRECIO_K_POLLO = "PrecioKPollo"
        private const val KEY_TOTAL_PESOJABAS = "totalPesoJabas"
        private const val KEY_TOTAL_NETO = "totalNeto"
        private const val KEY_TOTAL_PAGAR = "TotalPagar"
        private const val KEY_DNI_USUARIO = "idUsuario"
        private const val KEY_ID_ESTABLECIMIENTO = "idEstablecimiento"

        // Cliente Table - column names
        private const val KEY_FECHA_REGISTRO = "fechaRegistro"

        // ListPesos Table - column names
        private const val KEY_DATA_PESO_JSON = "dataJsonPeso"
        private const val KEY_DATA_DETAPESO_JSON = "dataJsonDetaPeso"

        private const val KEY_IMPRESORA_IP = "ip"
        private const val KEY_IMPRESORA_PUERTO = "puerto"

        private const val KEY_DEVICE_NAME = "deviceName"

        // Nucleos Table - column names
        private const val KEY_NUCLEO_NAME = "n_name"
        private const val KEY_NUCLEO_EMPRESA_RUC = "n_idEmpresa"

        // Galpones Table - column names
        private const val KEY_GALPON_NAME = "g_nombre"
        private const val KEY_GALPON_ID_ESTABLECIMIENTO = "g_idEstablecimiento"

        // Usuario Table - column names
        private const val KEY_USUARIO_NAME = "u_name"
        private const val KEY_USUARIO_PASS = "u_Pass"
        private const val KEY_USUARIO_ID_ROL = "u_idRol"
        private const val KEY_USUARIO_ROLNAME = "u_rolName"
        private const val KEY_USUARIO_ID_ESTABLECIMIENTO = "u_idEstablecimiento"
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

        val TABLE_NUCLEO = ("CREATE TABLE $TABLE_NUCLEO("
                + "$KEY_ID INTEGER PRIMARY KEY, "
                + "$KEY_NUCLEO_NAME TEXT, "
                + "$KEY_NUCLEO_EMPRESA_RUC TEXT)")
        db.execSQL(TABLE_NUCLEO)

        val TABLE_GALPON = ("CREATE TABLE $TABLE_GALPON("
                + "$KEY_ID INTEGER PRIMARY KEY, "
                + "$KEY_GALPON_NAME TEXT, "
                + "$KEY_GALPON_ID_ESTABLECIMIENTO TEXT)")
        db.execSQL(TABLE_GALPON)

        val TABLE_USUARIO = ("CREATE TABLE $TABLE_USUARIO("
                + "$KEY_ID INTEGER PRIMARY KEY, "
                + "$KEY_USUARIO_NAME TEXT, "
                + "$KEY_USUARIO_PASS TEXT, "
                + "$KEY_USUARIO_ROLNAME TEXT, "
                + "$KEY_USUARIO_ID_ROL TEXT, "
                + "$KEY_USUARIO_ID_ESTABLECIMIENTO TEXT)")
        db.execSQL(TABLE_USUARIO)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < newVersion) {
            // En caso de que la versión anterior fuera 1, añadimos los nuevos campos
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DETA_PESO_POLLOS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PESO_POLLOS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PESOS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_IMPRESORA")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USED_PESOS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NUCLEO")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GALPON")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIO")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CLIENTE")
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

    // Inserción de Nucleo
    fun insertNucleo(nucleo: NucleoEntity): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NUCLEO_NAME, nucleo.nombre)
            put(KEY_NUCLEO_EMPRESA_RUC, nucleo.idEmpresa)
        }
        return db.insert(TABLE_NUCLEO, null, values)
    }

    fun insertGalpon(galpon: GalponEntity): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_GALPON_NAME, galpon.nombre)
            put(KEY_GALPON_ID_ESTABLECIMIENTO, galpon.idEstablecimiento)
        }
        return db.insert(TABLE_GALPON, null, values)
    }

    // Inserción de Usuario
    fun insertUsuario(usuario: UsuarioEntity): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ID, usuario.idUsuario)
            put(KEY_USUARIO_NAME, usuario.userName)
            put(KEY_USUARIO_PASS, usuario.pass)
            put(KEY_USUARIO_ID_ROL, usuario.idRol)
            put(KEY_USUARIO_ROLNAME, usuario.rolName)
            put(KEY_USUARIO_ID_ESTABLECIMIENTO, usuario.idEstablecimiento)
        }
        return db.insert(TABLE_USUARIO, null, values)
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
                    numeroDocCliente = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_NUMERO_DOC_CLIENTE
                        )
                    ),
                    idGalpon = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID_GALPON)),
                    idNucleo = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID_NUCLEO)),
                    nombreCompleto = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_NOMBRE_COMPLETO
                        )
                    ),
                    PKPollo = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PRECIO_K_POLLO)),
                    totalPesoJabas = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_TOTAL_PESOJABAS
                        )
                    ),
                    totalNeto = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOTAL_NETO)),
                    TotalPagar = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOTAL_PAGAR)),
                    idUsuario = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DNI_USUARIO)),
                    idEstablecimiento = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_ID_ESTABLECIMIENTO
                        )
                    ),
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
                    numeroDocCliente = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_NUMERO_DOC_CLIENTE
                        )
                    ),
                    nombreCompleto = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_NOMBRE_COMPLETO
                        )
                    ),
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
                    numeroDocCliente = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_NUMERO_DOC_CLIENTE
                        )
                    ),
                    nombreCompleto = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_NOMBRE_COMPLETO
                        )
                    ),
                    dataPesoJson = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATA_PESO_JSON)),
                    dataDetaPesoJson = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_DATA_DETAPESO_JSON
                        )
                    ),
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
                    dataPesoPollosJson = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_DATA_PESO_JSON
                        )
                    ),
                    dataDetaPesoPollosJson = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_DATA_DETAPESO_JSON
                        )
                    ),
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
                numeroDocCliente = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        KEY_NUMERO_DOC_CLIENTE
                    )
                ),
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

    fun getUsuarioById(idUsuario: String): UsuarioEntity? {
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_USUARIO WHERE $KEY_ID = ?"
        val cursor = db.rawQuery(selectQuery, arrayOf(idUsuario))

        var usuario: UsuarioEntity? = null
        if (cursor.moveToFirst()) {
            usuario = UsuarioEntity(
                idUsuario = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                userName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_NAME)),
                pass = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_PASS)),
                idRol = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_ID_ROL)),
                rolName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_ROLNAME)),
                idEstablecimiento = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_ID_ESTABLECIMIENTO))
            )
        }
        cursor.close()
        return usuario
    }


    fun getNucleoByName(name: String): NucleoEntity? {
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_NUCLEO WHERE $KEY_NUCLEO_NAME = ?"
        val cursor = db.rawQuery(selectQuery, arrayOf(name))

        var nucleo: NucleoEntity? = null
        if (cursor.moveToFirst()) {
            nucleo = NucleoEntity(
                idEstablecimiento = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                idEmpresa = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID))
            )
        }
        cursor.close()
        return nucleo
    }

    fun getGalponByName(name: String): GalponEntity? {
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_GALPON WHERE $KEY_GALPON_NAME = ?"
        val cursor = db.rawQuery(selectQuery, arrayOf(name))

        var galpon: GalponEntity? = null
        if (cursor.moveToFirst()) {
            galpon = GalponEntity(
                idGalpon = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GALPON_NAME)),
                idEstablecimiento = cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_GALPON_ID_ESTABLECIMIENTO)),
            )
        }
        cursor.close()
        return galpon
    }

    fun getAllUsuarios(): List<UsuarioEntity> {
        val usuarioList = mutableListOf<UsuarioEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM ${TABLE_USUARIO}"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val usuario = UsuarioEntity(
                    idUsuario = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                    userName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_NAME)),
                    pass = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_PASS)),
                    idRol = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_ID_ROL)),
                    rolName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USUARIO_ROLNAME)),
                    idEstablecimiento = cursor.getString(cursor.getColumnIndexOrThrow(
                        KEY_USUARIO_ID_ESTABLECIMIENTO))
                )
                usuarioList.add(usuario)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return usuarioList
    }

    fun getAllNucleos(): List<NucleoEntity> {
        val nucleosList = mutableListOf<NucleoEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM ${TABLE_NUCLEO}"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val nucleo = NucleoEntity(
                    idEstablecimiento = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NUCLEO_NAME)),
                    idEmpresa = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NUCLEO_EMPRESA_RUC))
                )
                nucleosList.add(nucleo)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return nucleosList
    }

    fun getAllGalpones(): List<GalponEntity> {
        val galponList = mutableListOf<GalponEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM ${TABLE_GALPON}"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val galpon = GalponEntity(
                    idGalpon = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GALPON_NAME)),
                    idEstablecimiento = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GALPON_ID_ESTABLECIMIENTO))
                )
                galponList.add(galpon)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return galponList
    }

    fun getGalponesForByIdNucleo(idNucleo: String): List<GalponEntity> {
        val galponesList = mutableListOf<GalponEntity>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_GALPON WHERE $KEY_GALPON_ID_ESTABLECIMIENTO = ?"
        val cursor = db.rawQuery(selectQuery, arrayOf(idNucleo))

        if (cursor.moveToFirst()) {
            do {
                val galpon = GalponEntity(
                    idGalpon = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GALPON_NAME)),
                    idEstablecimiento = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GALPON_ID_ESTABLECIMIENTO))
                )
                galponesList.add(galpon)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return galponesList
    }


    // =========================================================
    // UPDATE
    // =========================================================

    fun updateCliente(cliente: ClienteEntity): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(KEY_NUMERO_DOC_CLIENTE, cliente.numeroDocCliente)
            put(KEY_NOMBRE_COMPLETO, cliente.nombreCompleto)
        }
        return db.update(
            TABLE_CLIENTE,
            contentValues,
            "$KEY_ID = ?",
            arrayOf(cliente.id.toString())
        )
    }

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
        db.delete(AppDatabase.TABLE_USUARIO, null, null)
        db.delete(AppDatabase.TABLE_NUCLEO, null, null)
        db.delete(AppDatabase.TABLE_GALPON, null, null)
        db.delete(AppDatabase.TABLE_IMPRESORA, null, null)
        db.delete(AppDatabase.TABLE_PESOS, null, null)
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

