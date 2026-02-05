package model

enum class TipoUsuario {
    ESTUDIANTE,
    PROFESOR,
    CONSERJE,
    PERSONAL
}

data class Usuario(
    val id: Int,
    val dni: String,
    val nombre: String,
    val apellido: String = "",
    val tipo: TipoUsuario,
    val email: String,
    val fechaNacimiento: String? = null,
    val genero: String? = null,
    val telefono: String? = null,
    val direccionCalle: String? = null,
    val direccionCiudad: String? = null,
    val direccionCP: String? = null,
    val direccionProvincia: String? = null,
    val sancionadoHasta: String? = null
) {
    // Para mostrar en ComboBox y Tablas
    override fun toString(): String {
        return "$nombre $apellido ($tipo)"
    }

    // Nombre completo para mostrar
    val nombreCompleto: String
        get() = "$nombre $apellido"
}