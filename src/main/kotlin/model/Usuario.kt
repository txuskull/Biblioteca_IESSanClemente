package model

data class Usuario(
    val id: Int,
    val dni: String,
    val nombre: String,
    val tipo: String,
    val email: String,
    val sancionadoHasta: String
)