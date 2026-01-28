package controller

import database.GestorBaseDatos
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.stage.Stage
import model.Prestamo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PrestamosController {

    @FXML private lateinit var btnVolver: Button
    @FXML private lateinit var tabla: TableView<Prestamo>
    @FXML private lateinit var colId: TableColumn<Prestamo, Int>
    @FXML private lateinit var colLibro: TableColumn<Prestamo, String>
    @FXML private lateinit var colUser: TableColumn<Prestamo, String>
    @FXML private lateinit var colFechaSalida: TableColumn<Prestamo, String>
    @FXML private lateinit var colFechaPrevista: TableColumn<Prestamo, String>
    @FXML private lateinit var colEstado: TableColumn<Prestamo, String>
    @FXML private lateinit var btnNuevo: Button
    @FXML private lateinit var btnDevolver: Button

    @FXML
    fun initialize() {
        configurarColumnas()
        tabla.items = cargarPrestamos()
    }

    private fun configurarColumnas() {
        colId.cellValueFactory = PropertyValueFactory("id")
        colLibro.cellValueFactory = PropertyValueFactory("tituloLibro")
        colUser.cellValueFactory = PropertyValueFactory("nombreUsuario")
        colFechaSalida.cellValueFactory = PropertyValueFactory("fechaPrestamo")
        colFechaPrevista.cellValueFactory = PropertyValueFactory("fechaDevolucionPrevista")
        colEstado.cellValueFactory = PropertyValueFactory("estado")

        colEstado.setCellFactory {
            object : TableCell<Prestamo, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = item
                    if (item == "DEVUELTO") {
                        textFill = javafx.scene.paint.Color.GREEN
                        style = "-fx-font-weight: bold;"
                    } else if (item == "ACTIVO") {
                        textFill = javafx.scene.paint.Color.BLUE
                    }
                }
            }
        }
    }

    @FXML
    fun handleVolver() {
        navegarA("/fxml/dashboard.fxml", "Panel de Gestión")
    }

    @FXML
    fun handleNuevo() {
        navegarA("/fxml/prestamo_form.fxml", "Nuevo Préstamo")
    }

    @FXML
    fun handleDevolver() {
        val seleccionado = tabla.selectionModel.selectedItem
        if (seleccionado != null && seleccionado.estado == "ACTIVO") {
            confirmarDevolucion(seleccionado)
            tabla.items = cargarPrestamos()
        } else if (seleccionado != null && seleccionado.estado == "DEVUELTO") {
            val alerta = Alert(Alert.AlertType.INFORMATION)
            alerta.contentText = "Este libro ya ha sido devuelto."
            alerta.showAndWait()
        } else {
            val alerta = Alert(Alert.AlertType.WARNING)
            alerta.contentText = "Selecciona un préstamo activo."
            alerta.showAndWait()
        }


    }

    private fun cargarPrestamos(): javafx.collections.ObservableList<Prestamo> {
        val lista = FXCollections.observableArrayList<Prestamo>()
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                val sql = """
                    SELECT p.id, u.nombre as nombre_usuario, l.titulo as titulo_libro, 
                           p.fecha_prestamo, p.fecha_devolucion_prevista, p.fecha_devolucion_real
                    FROM prestamos p
                    JOIN usuarios u ON p.usuario_id = u.id
                    JOIN ejemplares e ON p.ejemplar_id = e.id
                    JOIN libros l ON e.libro_id = l.id
                    ORDER BY p.id DESC
                """
                val rs = conn.createStatement().executeQuery(sql)

                while (rs.next()) {
                    val real = rs.getString("fecha_devolucion_real")
                    val estado = if (real != null && real.isNotEmpty()) "DEVUELTO" else "ACTIVO"

                    lista.add(Prestamo(
                        rs.getInt("id"),
                        rs.getString("titulo_libro"),
                        rs.getString("nombre_usuario"),
                        rs.getString("fecha_prestamo"),
                        rs.getString("fecha_devolucion_prevista"),
                        real,
                        estado
                    ))
                }
                conn.close()
            } catch (e: Exception) {
                println("Error SQL: ${e.message}")
            }
        }
        return lista
    }

    private fun confirmarDevolucion(prestamo: Prestamo) {
        val alerta = Alert(Alert.AlertType.CONFIRMATION)
        alerta.headerText = "Confirmar Devolución"
        alerta.contentText = "¿El usuario ha devuelto '${prestamo.tituloLibro}' hoy?"

        val resp = alerta.showAndWait()
        if (resp.isPresent && resp.get() == ButtonType.OK) {
            val gestor = GestorBaseDatos()
            val conn = gestor.getConexion()
            if (conn != null) {
                try {
                    val fechaHoy = LocalDate.now()
                    val fechaPrevista = LocalDate.parse(prestamo.fechaDevolucionPrevista)

                    // VERIFICAR SI HAY RETRASO
                    if (fechaHoy.isAfter(fechaPrevista)) {
                        val diasRetraso = ChronoUnit.DAYS.between(fechaPrevista, fechaHoy)

                        // Obtener usuario_id y tipo de publicación
                        val sqlInfo = """
                        SELECT p.usuario_id, u.tipo as tipo_usuario, l.tipo_publicacion
                        FROM prestamos p
                        JOIN usuarios u ON p.usuario_id = u.id
                        JOIN ejemplares e ON p.ejemplar_id = e.id
                        JOIN libros l ON e.libro_id = l.id
                        WHERE p.id = ?
                    """
                        val psInfo = conn.prepareStatement(sqlInfo)
                        psInfo.setInt(1, prestamo.id)
                        val rsInfo = psInfo.executeQuery()

                        if (rsInfo.next()) {
                            val userId = rsInfo.getInt("usuario_id")
                            val tipoUsuario = rsInfo.getString("tipo_usuario")
                            val tipoPublicacion = rsInfo.getString("tipo_publicacion")

                            // SOLO SANCIONAR A ESTUDIANTES (según PDF página 2)
                            if (tipoUsuario == "ESTUDIANTE") {
                                // CALCULAR DÍAS DE SANCIÓN SEGÚN TIPO
                                val diasSancion = if (tipoPublicacion == "REVISTA") {
                                    // REVISTAS: 10 días FIJOS (PDF página 3)
                                    10L
                                } else {
                                    // LIBROS: 2 × n días (PDF página 3)
                                    diasRetraso * 2
                                }

                                val fechaFinSancion = fechaHoy.plusDays(diasSancion)

                                // Insertar Sanción
                                val sqlSancion = """
                                INSERT INTO sanciones (usuario_id, motivo, fecha_inicio, fecha_fin, dias_sancion, estado)
                                VALUES (?, ?, ?, ?, ?, 'ACTIVA')
                            """
                                val psSancion = conn.prepareStatement(sqlSancion)
                                psSancion.setInt(1, userId)

                                val motivo = if (tipoPublicacion == "REVISTA") {
                                    "No devolvió revista '${prestamo.tituloLibro}' en el plazo de 1 día"
                                } else {
                                    "Retraso de $diasRetraso día(s) en '${prestamo.tituloLibro}'"
                                }

                                psSancion.setString(2, motivo)
                                psSancion.setString(3, fechaHoy.toString())
                                psSancion.setString(4, fechaFinSancion.toString())
                                psSancion.setLong(5, diasSancion)
                                psSancion.executeUpdate()

                                // Avisar con mensaje diferente según tipo
                                val aviso = Alert(Alert.AlertType.WARNING)
                                aviso.headerText = "¡Sanción Aplicada!"

                                if (tipoPublicacion == "REVISTA") {
                                    aviso.contentText = """
                                    REVISTA no devuelta a tiempo.
                                    
                                    Según el reglamento (PDF pág. 3):
                                    "En el caso de no devolver una revista en el día, 
                                    la sanción automática será de diez días."
                                    
                                    Sanción: 10 días
                                    Fin de sanción: $fechaFinSancion
                                """.trimIndent()
                                } else {
                                    aviso.contentText = """
                                    LIBRO devuelto con retraso.
                                    
                                    Días de retraso: $diasRetraso
                                    Sanción aplicada: $diasSancion días (2 × $diasRetraso)
                                    Fin de sanción: $fechaFinSancion
                                """.trimIndent()
                                }

                                aviso.showAndWait()
                            } else {
                                // NO es estudiante, no se sanciona pero informamos del retraso
                                val info = Alert(Alert.AlertType.INFORMATION)
                                info.headerText = "Devolución con retraso"
                                info.contentText = """
                                El usuario tiene $diasRetraso día(s) de retraso.
                                
                                NO se aplica sanción porque el usuario es $tipoUsuario.
                                (Solo los ESTUDIANTES reciben sanciones automáticas)
                            """.trimIndent()
                                info.showAndWait()
                            }
                        }
                    }

                    // Registrar devolución
                    val sqlUpdate = "UPDATE prestamos SET fecha_devolucion_real = ? WHERE id = ?"
                    val ps = conn.prepareStatement(sqlUpdate)
                    ps.setString(1, fechaHoy.toString())
                    ps.setInt(2, prestamo.id)
                    ps.executeUpdate()

                    // Liberar ejemplar
                    val sqlLiberar = "UPDATE ejemplares SET estado = 'DISPONIBLE' WHERE id = (SELECT ejemplar_id FROM prestamos WHERE id = ?)"
                    val ps2 = conn.prepareStatement(sqlLiberar)
                    ps2.setInt(1, prestamo.id)
                    ps2.executeUpdate()

                    conn.close()

                } catch (e: Exception) {
                    println("Error devolucion: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun navegarA(fxml: String, titulo: String) {
        val stage = btnVolver.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource(fxml))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
        stage.isMaximized = true
        stage.title = titulo
    }
}