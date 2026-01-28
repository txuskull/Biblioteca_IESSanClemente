package model

data class Libro(
    val id: Int,
    val isbn: String,
    val titulo: String,
    val autor: String,
    val editorial: String,
    val estado: String = "Disponible"
)