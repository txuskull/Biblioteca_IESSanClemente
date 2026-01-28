package model

data class Prestamo(
    val id: Int,
    val tituloLibro: String,
    val nombreUsuario: String,
    val fechaPrestamo: String,
    val fechaDevolucionPrevista: String,
    val fechaDevolucionReal: String?,
    val estado: String
)