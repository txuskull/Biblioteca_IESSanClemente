package model

data class Sancion(
    val id: Int,
    val nombreUsuario: String,
    val motivo: String,
    val fechaInicio: String,
    val fechaFin: String,
    val diasSancion: Int,
    val estado: String
)