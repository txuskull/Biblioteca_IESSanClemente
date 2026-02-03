package controller

import database.GestorBaseDatos
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
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
    @FXML private lateinit var txtBuscar: TextField

    @FXML
    fun initialize() {
        // preparo las columnas para que sepan que dato poner
        configurarColumnas()

        // cargo los datos de la base de datos en una lista observable
        val listaPrestamos = FXCollections.observableArrayList<Prestamo>()
        listaPrestamos.setAll(cargarPrestamos())

        // envuelvo la lista en un filtro para poder buscar
        val listaFiltrada = FilteredList(listaPrestamos) { true }
        tabla.items = listaFiltrada

        // busqueda en tiempo real cuando escribo
        txtBuscar.textProperty().addListener { _, _, nuevoTexto ->
            listaFiltrada.setPredicate { prestamo ->
                if (nuevoTexto.isNullOrEmpty()) true
                else {
                    // paso todo a minusculas para ignorar mayusculas/minusculas
                    val lower = nuevoTexto.lowercase()
                    prestamo.nombreUsuario.lowercase().contains(lower) ||
                            prestamo.tituloLibro.lowercase().contains(lower) ||
                            prestamo.fechaPrestamo.contains(nuevoTexto)
                }
            }
        }
    }

    private fun configurarColumnas() {
        // asocio cada columna con el atributo de la clase Prestamo
        colId.cellValueFactory = PropertyValueFactory("id")
        colLibro.cellValueFactory = PropertyValueFactory("tituloLibro")
        colUser.cellValueFactory = PropertyValueFactory("nombreUsuario")
        colFechaSalida.cellValueFactory = PropertyValueFactory("fechaPrestamo")
        colFechaPrevista.cellValueFactory = PropertyValueFactory("fechaDevolucionPrevista")
        colEstado.cellValueFactory = PropertyValueFactory("estado")

        // personalizo la columna de estado para que se vea mas bonita
        colEstado.setCellFactory {
            object : TableCell<Prestamo, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = item
                    if (item == "DEVUELTO") {
                        // verde y negrita si ya esta devuelto
                        textFill = javafx.scene.paint.Color.GREEN
                        style = "-fx-font-weight: bold;"
                    } else if (item == "ACTIVO") {
                        // azul si aun lo tiene el usuario
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


    // funcion para gestionar la devolucion de un libro
    @FXML
    fun handleDevolver() {
        val seleccionado = tabla.selectionModel.selectedItem

        // solo puedo devolver si esta seleccionado y esta activo
        if (seleccionado != null && seleccionado.estado == "ACTIVO") {
            confirmarDevolucion(seleccionado)

            // despues de devolver, recargo la tabla entera para ver los cambios
            val nuevaLista = cargarPrestamos()
            // actualizar lista filtrada
            (tabla.items as? FilteredList<Prestamo>)?.let { filtrada ->
                (filtrada.source as? javafx.collections.ObservableList<Prestamo>)?.setAll(nuevaLista)
            }
        } else if (seleccionado != null && seleccionado.estado == "DEVUELTO") {
            // si ya estaba devuelto aviso al usuario
            val alerta = Alert(Alert.AlertType.INFORMATION)
            alerta.contentText = "Este libro ya ha sido devuelto."
            alerta.showAndWait()
        } else {
            // si no selecciono nada aviso
            val alerta = Alert(Alert.AlertType.WARNING)
            alerta.contentText = "Selecciona un préstamo activo."
            alerta.showAndWait()
        }
    }

    // consulta sql para traer todos los prestamos con joins para sacar nombres reales
    private fun cargarPrestamos(): javafx.collections.ObservableList<Prestamo> {
        val lista = FXCollections.observableArrayList<Prestamo>()
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                // uno las tablas prestamos, usuarios, ejemplares y libros
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
                    // calculo el estado segun si tiene fecha real o no
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

    // logica principal de la devolucion y calculo de multas
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

                    // compruebo si hay retraso
                    if (fechaHoy.isAfter(fechaPrevista)) {
                        val diasRetraso = ChronoUnit.DAYS.between(fechaPrevista, fechaHoy)

                        // necesito saber quien es el usuario y que tipo de libro es
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

                            // sancion estudiantes
                            if (tipoUsuario == "ESTUDIANTE") {
                                // calculo la sancion segun el tipo de publicacion
                                val diasSancion = if (tipoPublicacion == "REVISTA") {
                                    // revistas siempre 10 dias
                                    10L
                                } else {
                                    // libros es el doble de los dias de retraso
                                    diasRetraso * 2
                                }

                                val fechaFinSancion = fechaHoy.plusDays(diasSancion)

                                // inserto la multa en la base de datos
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

                                // aviso de que se ha aplicado una multa automatica
                                val aviso = Alert(Alert.AlertType.WARNING)
                                aviso.headerText = "¡Sanción Aplicada!"

                                if (tipoPublicacion == "REVISTA") {
                                    aviso.contentText = """
                                    REVISTA no devuelta a tiempo.
                                    
                                    Según el reglamento:
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
                                // si no es estudiante solo informo
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

                    // actualizo la fecha real de devolucion en el prestamo
                    val sqlUpdate = "UPDATE prestamos SET fecha_devolucion_real = ? WHERE id = ?"
                    val ps = conn.prepareStatement(sqlUpdate)
                    ps.setString(1, fechaHoy.toString())
                    ps.setInt(2, prestamo.id)
                    ps.executeUpdate()

                    // libero el ejemplar para que otro lo pueda pedir
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

    // funcion para cambiar de ventana
    private fun navegarA(fxml: String, titulo: String) {
        // cojo la ventana actual
        val stage = btnVolver.scene.window as Stage

        val loader = FXMLLoader(javaClass.getResource(fxml))
        val root = loader.load<javafx.scene.Parent>()

        // en vez de crear una 'new Scene', le digo a la escena actual
        // que cambie su contenido (root) por el nuevo
        stage.scene.root = root

        // esto mantiene el tamaño y el maximizado que ya tenias
        stage.title = titulo
    }
}