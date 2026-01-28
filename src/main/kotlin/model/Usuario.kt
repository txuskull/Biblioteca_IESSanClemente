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
    val tipo: TipoUsuario,        // Campo tipo Enum
    val email: String,
    val sancionadoHasta: String?  // Campo de texto o null
) {
    // Esto hace que en los desplegables se vea bonito: "Juan (ESTUDIANTE)"
    override fun toString(): String {
        return "$nombre ($tipo)"
    }
}