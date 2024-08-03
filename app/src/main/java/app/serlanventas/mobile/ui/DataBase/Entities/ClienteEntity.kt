package app.serlanventas.mobile.ui.DataBase.Entities

data class ClienteEntity(
    val id: Int = 0,
    val numeroDocCliente: String,
    val nombreCompleto: String,
    val fechaRegistro: String
)
