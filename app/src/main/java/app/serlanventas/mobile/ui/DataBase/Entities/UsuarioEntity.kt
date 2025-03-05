package app.serlanventas.mobile.ui.DataBase.Entities

data class UsuarioEntity (
    val idUsuario: String,
    val userName: String,
    val pass: String,
    val idRol: String,
    val rolName: String,
    val idEstablecimiento: String
)