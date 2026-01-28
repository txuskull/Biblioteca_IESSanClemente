package controller

import database.GestorBaseDatos
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import java.time.LocalDate

class PrestamoFormController {

    @FXML private lateinit var txtBuscarUsuario: TextField
    @FXML private lateinit var cmbUsuarios: ComboBox<ComboUsuario>
    @FXML private lateinit var cmbLibros: ComboBox<ComboLibro>
    @FXML private lateinit var datePicker: DatePicker
    @FXML private lateinit var dateDevolucion: DatePicker
    @FXML private lateinit var btnCancelar: Button
    @FXML private lateinit var btnGuardar: Button

    class ComboUsuario(val id: Int, val nombre: String) {
        override fun toString(): String = nombre
    }

    class ComboLibro(val id: Int, val titulo: String) {
        override fun toString(): String = titulo
    }

    @FXML
    fun initialize() {
        datePicker.value = LocalDate.now()
        dateDevolucion.value = LocalDate.now().plusDays(7) // 7 días por defecto

        cargarUsuarios(cmbUsuarios, "")
        cargarLibros(cmbLibros)

        txtBuscarUsuario.textProperty().addListener { _, _, nuevoTexto ->
            cargarUsuarios(cmbUsuarios, nuevoTexto)
            cmbUsuarios.show()
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

        if (usuario == null || libro == null || fechaP == null || fechaD == null) {
            mostrarAlerta("Faltan datos", "Debes seleccionar usuario, libro y fechas.")
        } else {
            if (usuarioTieneSancionActiva(usuario.id)) {
                mostrarAlerta("USUARIO SANCIONADO", "No se puede realizar el prestamo. El alumno tiene una sancion activa.")
            } else {
                guardarPrestamo(usuario.id, libro.id, fechaP, fechaD)
            }
        }
    }

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

    private fun cargarUsuarios(cmb: ComboBox<ComboUsuario>, filtro: String) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        val seleccionPrevia = cmb.value
        cmb.items.clear()

        if (conn != null) {
            try {
                val sql = "SELECT id, nombre FROM usuarios WHERE tipo = 'ESTUDIANTE' AND nombre LIKE ?"
                val ps = conn.prepareStatement(sql)
                ps.setString(1, "%$filtro%")

                val rs = ps.executeQuery()
                while (rs.next()) {
                    cmb.items.add(ComboUsuario(rs.getInt("id"), rs.getString("nombre")))
                }
                conn.close()

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
                val sql = "SELECT id, titulo FROM libros"
                val rs = conn.createStatement().executeQuery(sql)
                while (rs.next()) {
                    cmb.items.add(ComboLibro(rs.getInt("id"), rs.getString("titulo")))
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
                // Buscar ejemplar disponible
                val sqlBuscar = "SELECT id FROM ejemplares WHERE libro_id = ? AND estado = 'DISPONIBLE' LIMIT 1"
                val psBuscar = conn.prepareStatement(sqlBuscar)
                psBuscar.setInt(1, idLibro)
                val rs = psBuscar.executeQuery()

                if (rs.next()) {
                    val idEjemplar = rs.getInt("id")

                    // Registrar préstamo
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

                    // Marcar ejemplar como prestado
                    val sqlUpdate = "UPDATE ejemplares SET estado = 'PRESTADO' WHERE id = ?"
                    val psUpdate = conn.prepareStatement(sqlUpdate)
                    psUpdate.setInt(1, idEjemplar)
                    psUpdate.executeUpdate()

                    conn.close()

                    val alerta = Alert(Alert.AlertType.INFORMATION)
                    alerta.title = "Exito"
                    alerta.headerText = null
                    alerta.contentText = "Prestamo registrado correctamente."
                    alerta.showAndWait()

                    navegarAPrestamos()

                } else {
                    mostrarAlerta("Sin stock", "No hay ejemplares disponibles de este libro.")
                }
            } catch (e: Exception) {
                println(e.message)
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

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val alerta = Alert(Alert.AlertType.WARNING)
        alerta.title = titulo
        alerta.headerText = null
        alerta.contentText = mensaje
        alerta.showAndWait()
    }
}