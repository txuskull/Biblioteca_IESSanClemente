package controller

import database.GestorBaseDatos
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import model.TipoPublicacion
import java.time.LocalDate

class PrestamoFormController {

    @FXML private lateinit var txtBuscarUsuario: TextField
    @FXML private lateinit var cmbUsuarios: ComboBox<ComboUsuario>
    @FXML private lateinit var cmbLibros: ComboBox<ComboLibro>
    @FXML private lateinit var datePicker: DatePicker
    @FXML private lateinit var dateDevolucion: DatePicker
    @FXML private lateinit var btnCancelar: Button
    @FXML private lateinit var btnGuardar: Button

    // clases internas pequeñitas para mostrar bien los nombres en los desplegables
    class ComboUsuario(val id: Int, val nombre: String, val tipo: String) {
        override fun toString(): String = nombre
    }

    class ComboLibro(val id: Int, val titulo: String, val tipoPublicacion: String) {
        override fun toString(): String = "$titulo [$tipoPublicacion]"
    }

    @FXML
    fun initialize() {
        // pongo la fecha de hoy por defecto
        datePicker.value = LocalDate.now()

        // cargo las listas desplegables
        cargarUsuarios(cmbUsuarios, "")
        cargarLibros(cmbLibros)

        // buscador de usuarios en tiempo real
        txtBuscarUsuario.textProperty().addListener { _, _, nuevoTexto ->
            cargarUsuarios(cmbUsuarios, nuevoTexto)
            cmbUsuarios.show() // despliego la lista para que vea las opciones
        }

        // cuando elijo libro o usuario, calculo la fecha de devolucion sola
        cmbLibros.selectionModel.selectedItemProperty().addListener { _, _, libroSeleccionado ->
            if (libroSeleccionado != null) {
                calcularFechaDevolucion()
            }
        }

        cmbUsuarios.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            calcularFechaDevolucion()
        }
    }

    // aqui aplico las REGLAS DEL NEGOCIO que se piden
    private fun calcularFechaDevolucion() {
        val libro = cmbLibros.value
        val usuario = cmbUsuarios.value

        if (libro != null && usuario != null) {
            val fechaPrestamo = datePicker.value

            // Determinar dias segun tipo de publicación y tipo de usuario
            val dias = when {
                libro.tipoPublicacion == "LIBRO" -> 7 // Libros siempre 7 días para todos
                libro.tipoPublicacion == "REVISTA" && usuario.tipo == "PROFESOR" -> 7 // Profesores tienen "privilegios" con revistas
                libro.tipoPublicacion == "REVISTA" -> 1 // Estudiantes/Personal solo 1 día para revistas
                else -> 7
            }

            // sumo los dias a la fecha de hoy y lo pongo en el calendario de devolucion
            dateDevolucion.value = fechaPrestamo.plusDays(dias.toLong())
        }
    }

    @FXML
    fun handleCancelar() {
        navegarAPrestamos()
    }

    @FXML
    fun handleGuardar() {
        val usuario = cmbUsuarios.value
        val libro = cmbLibros.value
        val fechaP = datePicker.value
        val fechaD = dateDevolucion.value

        // validacion basica de campos vacios
        if (usuario == null || libro == null || fechaP == null || fechaD == null) {
            mostrarAlerta("Faltan datos", "Debes seleccionar usuario, libro y fechas.", Alert.AlertType.WARNING)
            return
        }

        // VALIDACIÓN 1: Usuario castigado no puede llevarse nada
        if (usuarioTieneSancionActiva(usuario.id)) {
            mostrarAlerta("USUARIO SANCIONADO", "No se puede realizar el préstamo. El usuario tiene una sanción activa.", Alert.AlertType.ERROR)
            return
        }

        // VALIDACIÓN 2: Regla especial de revistas
        // si es una revista y NO es profesor...
        if (libro.tipoPublicacion == "REVISTA" && usuario.tipo != "PROFESOR") {
            // ...compruebo si ya tiene otra revista en casa
            if (usuarioTieneRevistaPrestada(usuario.id)) {
                mostrarAlerta("LÍMITE DE REVISTAS",
                    "Los estudiantes y personal solo pueden tener 1 revista prestada a la vez.\n" +
                            "Los profesores no tienen esta restricción.",
                    Alert.AlertType.ERROR)
                return
            }
        }

        // validacion logica de fechas
        if (fechaD.isBefore(fechaP)) {
            mostrarAlerta("Fecha incorrecta",
                "La fecha de devolución no puede ser anterior a la fecha de préstamo.",
                Alert.AlertType.WARNING)
            return
        }

        // si pasa todo, guardo el prestamo
        guardarPrestamo(usuario.id, libro.id, fechaP, fechaD)
    }

    // consulto a la base de datos si el usuario esta sancionado
    private fun usuarioTieneSancionActiva(idUsuario: Int): Boolean {
        var sancionado = false
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val sql = "SELECT id FROM sanciones WHERE usuario_id = ? AND estado = 'ACTIVA'"
                val ps = conn.prepareStatement(sql)
                ps.setInt(1, idUsuario)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    sancionado = true
                }
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
        return sancionado
    }

    // compruebo si ya tiene revistas sin devolver
    private fun usuarioTieneRevistaPrestada(idUsuario: Int): Boolean {
        var tieneRevista = false
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                // busco prestamos de este usuario, que sean revistas y que NO tengan fecha de devolucion real (aun no devueltos)
                val sql = """
                    SELECT COUNT(*) as total
                    FROM prestamos p
                    JOIN ejemplares e ON p.ejemplar_id = e.id
                    JOIN libros l ON e.libro_id = l.id
                    WHERE p.usuario_id = ? 
                    AND l.tipo_publicacion = 'REVISTA'
                    AND p.fecha_devolucion_real IS NULL
                """
                val ps = conn.prepareStatement(sql)
                ps.setInt(1, idUsuario)
                val rs = ps.executeQuery()
                if (rs.next() && rs.getInt("total") > 0) {
                    tieneRevista = true
                }
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
        return tieneRevista
    }

    // cargo el combo de usuarios con filtro por nombre
    private fun cargarUsuarios(cmb: ComboBox<ComboUsuario>, filtro: String) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        val seleccionPrevia = cmb.value
        cmb.items.clear()

        if (conn != null) {
            try {
                val sql = "SELECT id, nombre, tipo FROM usuarios WHERE nombre LIKE ?"
                val ps = conn.prepareStatement(sql)
                ps.setString(1, "%$filtro%")

                val rs = ps.executeQuery()
                while (rs.next()) {
                    cmb.items.add(ComboUsuario(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("tipo")
                    ))
                }
                conn.close()

                // intento mantener la seleccion si no he borrado al usuario elegido
                if (filtro.isEmpty() && seleccionPrevia != null) {
                    cmb.value = seleccionPrevia
                } else if (cmb.items.isNotEmpty()) {
                    cmb.selectionModel.selectFirst()
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun cargarLibros(cmb: ComboBox<ComboLibro>) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val sql = "SELECT id, titulo, tipo_publicacion FROM libros"
                val rs = conn.createStatement().executeQuery(sql)
                while (rs.next()) {
                    cmb.items.add(ComboLibro(
                        rs.getInt("id"),
                        rs.getString("titulo"),
                        rs.getString("tipo_publicacion")
                    ))
                }
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun guardarPrestamo(idUsuario: Int, idLibro: Int, fPrestamo: LocalDate, fDev: LocalDate) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                // PASO 1: Buscar si queda algun ejemplar fisico DISPONIBLE de ese libro
                val sqlBuscar = "SELECT id FROM ejemplares WHERE libro_id = ? AND estado = 'DISPONIBLE' LIMIT 1"
                val psBuscar = conn.prepareStatement(sqlBuscar)
                psBuscar.setInt(1, idLibro)
                val rs = psBuscar.executeQuery()

                if (rs.next()) {
                    val idEjemplar = rs.getInt("id")

                    // PASO 2: Registrar el prestamo en el historial
                    val sqlInsert = """
                        INSERT INTO prestamos (usuario_id, ejemplar_id, fecha_prestamo, fecha_devolucion_prevista)
                        VALUES (?, ?, ?, ?)
                    """
                    val psInsert = conn.prepareStatement(sqlInsert)
                    psInsert.setInt(1, idUsuario)
                    psInsert.setInt(2, idEjemplar)
                    psInsert.setString(3, fPrestamo.toString())
                    psInsert.setString(4, fDev.toString())
                    psInsert.executeUpdate()

                    // PASO 3: Marcar ese ejemplar como PRESTADO para que nadie mas lo coja
                    val sqlUpdate = "UPDATE ejemplares SET estado = 'PRESTADO' WHERE id = ?"
                    val psUpdate = conn.prepareStatement(sqlUpdate)
                    psUpdate.setInt(1, idEjemplar)
                    psUpdate.executeUpdate()

                    conn.close()

                    val alerta = Alert(Alert.AlertType.INFORMATION)
                    alerta.title = "Éxito"
                    alerta.headerText = null
                    alerta.contentText = "Préstamo registrado correctamente.\nDevolución prevista: $fDev"
                    alerta.showAndWait()

                    navegarAPrestamos()

                } else {
                    // si no hay stock, aviso
                    mostrarAlerta("Sin stock", "No hay ejemplares disponibles de esta publicación.", Alert.AlertType.ERROR)
                }
            } catch (e: Exception) {
                println(e.message)
                e.printStackTrace()
            }
        }
    }

    private fun navegarAPrestamos() {
        val stage = btnCancelar.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource("/fxml/prestamos.fxml"))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
        stage.isMaximized = true
        stage.title = "Gestión de Préstamos"
    }

    private fun mostrarAlerta(titulo: String, mensaje: String, tipo: Alert.AlertType = Alert.AlertType.WARNING) {
        val alerta = Alert(tipo)
        alerta.title = titulo
        alerta.headerText = null
        alerta.contentText = mensaje
        alerta.showAndWait()
    }
}