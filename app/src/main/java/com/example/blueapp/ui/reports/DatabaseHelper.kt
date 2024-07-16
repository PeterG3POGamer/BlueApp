package com.example.blueapp.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "blueapp3.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        try {
            // Creación de la tabla pesopollos
            db?.execSQL("CREATE TABLE IF NOT EXISTS pesopollos (idPesoPollos INTEGER PRIMARY KEY AUTOINCREMENT, serie TEXT, fecha TEXT, totalJabas TEXT, totalPollos TEXT, totalPeso TEXT, tipo TEXT, docCliente TEXT, IdGalpon TEXT)")

            // Creación de la tabla detapp con referencia a pesopollos
            db?.execSQL("CREATE TABLE IF NOT EXISTS detapp (idDetaPP INTEGER PRIMARY KEY AUTOINCREMENT, cantJabas TEXT, cantPolllos TEXT, peso TEXT, tipo TEXT, idPesoPollo INTEGER, FOREIGN KEY(idPesoPollo) REFERENCES pesopollos(idPesoPollos))")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al crear la base de datos: ${e.message}")
            throw e
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        try {
            // Eliminación de tablas anteriores si existen
            db?.execSQL("DROP TABLE IF EXISTS pesopollos")
            db?.execSQL("DROP TABLE IF EXISTS detapp")

            // Creación de tablas nuevas
            onCreate(db)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al actualizar la base de datos: ${e.message}")
            throw e
        }
    }

    // Insertar datos de ejemplo para pesopollos y detapp con referencia
    fun insertarPesopollosYDetapp(jsonPesoPollos: JSONArray, jsonDetapp: JSONArray) {
        val db = writableDatabase
        db.beginTransaction()

        try {
            for (i in 0 until jsonPesoPollos.length()) {
                val jsonPesoPollo = jsonPesoPollos.getJSONObject(i)
                val idPesopollos = insertarPesopollos(db, jsonPesoPollo)

                // Aquí recorremos jsonDetapp y le asignamos el idPesoPollo a cada fila
                for (j in 0 until jsonDetapp.length()) {
                    val jsonDetappObj = jsonDetapp.getJSONObject(j)
                    insertarDetapp(db, jsonDetappObj, idPesopollos)
                }
            }
            db.setTransactionSuccessful()
            Log.d("DatabaseHelper", "Datos de pesopollos y detapp insertados correctamente desde JSON arrays.")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al insertar datos de pesopollos y detapp desde JSON arrays: ${e.message}")
            throw e
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    // Insertar datos en pesopollos y devolver el ID generado
    private fun insertarPesopollos(db: SQLiteDatabase, jsonPesoPollo: JSONObject): Long {
        return try {
            val contentValues = ContentValues().apply {
                put("serie", jsonPesoPollo.getString("serie"))
                put("fecha", jsonPesoPollo.getString("fecha"))
                put("totalJabas", jsonPesoPollo.getString("totalJabas"))
                put("totalPollos", jsonPesoPollo.getString("totalPollos"))
                put("totalPeso", jsonPesoPollo.getString("totalPeso"))
                put("tipo", jsonPesoPollo.getString("tipo"))
                put("docCliente", jsonPesoPollo.getString("docCliente"))
                put("IdGalpon", jsonPesoPollo.getString("IdGalpon"))
            }

            db.insert("pesopollos", null, contentValues)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al insertar datos en pesopollos: ${e.message}")
            throw e
        }
    }

    // Insertar datos en detapp utilizando el ID generado de pesopollos
    private fun insertarDetapp(db: SQLiteDatabase, jsonDetapp: JSONObject, idPesopollos: Long) {
        try {
            val contentValues = ContentValues().apply {
                put("cantJabas", jsonDetapp.getString("cantJabas"))
                put("cantPolllos", jsonDetapp.getString("cantPolllos"))
                put("peso", jsonDetapp.getString("peso"))
                put("tipo", jsonDetapp.getString("tipo"))
                put("idPesoPollo", idPesopollos.toInt())
            }

            db.insert("detapp", null, contentValues)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al insertar datos en detapp: ${e.message}")
            throw e
        }
    }

    // Obtener reportes desde pesopollos
    fun obtenerReportesPesopollos(): List<pesopollos> {
        val listaReportes = mutableListOf<pesopollos>()
        var cursor: Cursor? = null

        try {
            cursor = readableDatabase.rawQuery("SELECT * FROM pesopollos", null)

            if (cursor.moveToFirst()) {
                do {
                    val idIndex = cursor.getColumnIndex("idPesoPollos")
                    val serieIndex = cursor.getColumnIndex("serie")
                    val fechaIndex = cursor.getColumnIndex("fecha")
                    val totalJabasIndex = cursor.getColumnIndex("totalJabas")
                    val totalPollosIndex = cursor.getColumnIndex("totalPollos")
                    val totalPesoIndex = cursor.getColumnIndex("totalPeso")
                    val tipoIndex = cursor.getColumnIndex("tipo")
                    val docClienteIndex = cursor.getColumnIndex("docCliente")
                    val IdGalponIndex = cursor.getColumnIndex("IdGalpon")

                    val id = cursor.getInt(idIndex)
                    val serie = cursor.getString(serieIndex) ?: ""
                    val fecha = cursor.getString(fechaIndex) ?: ""
                    val totalJabas = cursor.getString(totalJabasIndex) ?: ""
                    val totalPollos = cursor.getString(totalPollosIndex) ?: ""
                    val totalPeso = cursor.getString(totalPesoIndex) ?: ""
                    val tipo = cursor.getString(tipoIndex) ?: ""
                    val docCliente = cursor.getString(docClienteIndex) ?: ""
                    val IdGalpon = cursor.getString(IdGalponIndex) ?: ""

                    val reporte = pesopollos(id, serie, fecha, totalJabas, totalPollos, totalPeso, tipo, docCliente, IdGalpon)
                    listaReportes.add(reporte)
                } while (cursor.moveToNext())
            } else {
                Log.d("DatabaseHelper", "No se encontraron reportes de pesopollos en la base de datos.")
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al obtener reportes de pesopollos desde la base de datos: ${e.message}")
            throw e
        } finally {
            cursor?.close()
        }

        return listaReportes
    }

    // Obtener reportes desde detapp
    fun obtenerReportesDetapp(): List<detapp> {
        val listaReportes = mutableListOf<detapp>()
        var cursor: Cursor? = null

        try {
            cursor = readableDatabase.rawQuery("SELECT * FROM detapp", null)

            if (cursor.moveToFirst()) {
                do {
                    val idIndex = cursor.getColumnIndex("idDetaPP")
                    val cantJabasIndex = cursor.getColumnIndex("cantJabas")
                    val cantPolllosIndex = cursor.getColumnIndex("cantPolllos")
                    val pesoIndex = cursor.getColumnIndex("peso")
                    val tipoIndex = cursor.getColumnIndex("tipo")
                    val idPesoPolloIndex = cursor.getColumnIndex("idPesoPollo")

                    val id = cursor.getInt(idIndex)
                    val cantJabas = cursor.getString(cantJabasIndex) ?: ""
                    val cantPolllos = cursor.getString(cantPolllosIndex) ?: ""
                    val peso = cursor.getString(pesoIndex) ?: ""
                    val tipo = cursor.getString(tipoIndex) ?: ""
                    val idPesoPollo = cursor.getInt(idPesoPolloIndex)

                    val reporte = detapp(id, cantJabas, cantPolllos, peso, tipo, idPesoPollo)
                    listaReportes.add(reporte)
                } while (cursor.moveToNext())
            } else {
                Log.d("DatabaseHelper", "No se encontraron reportes de detapp en la base de datos.")
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al obtener reportes de detapp desde la base de datos: ${e.message}")
            throw e
        } finally {
            cursor?.close()
        }

        return listaReportes
    }

}
