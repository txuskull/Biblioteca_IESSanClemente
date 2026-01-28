package model

enum class TipoPublicacion {
    LIBRO,
    REVISTA
}

data class Libro(
    val id: Int,
    val isbn: String,
    val titulo: String,
    val tipoPublicacion: TipoPublicacion,

    // Campos comunes
    val temas: String,
    val editorial: String,
    val editorialDireccion: String?,
    val editorialTelefono: String?,
    val idioma: String,
    val modulosRelacionados: String?,
    val ciclosRelacionados: String?,

    // Solo para LIBROS
    val autor: String?,
    val nacionalidadAutor: String?,
    val edicion: String?,
    val fechaPublicacion: String?,

    // Solo para REVISTAS
    val periodicidad: String?,
    val stock: String = "0/0"
) {
    // Para mostrar en ComboBox y listas
    override fun toString(): String {
        return "$titulo ($isbn)"
    }

    // Helper para saber si es revista
    fun esRevista(): Boolean = tipoPublicacion == TipoPublicacion.REVISTA

    // Helper para saber si es libro
    fun esLibro(): Boolean = tipoPublicacion == TipoPublicacion.LIBRO
}