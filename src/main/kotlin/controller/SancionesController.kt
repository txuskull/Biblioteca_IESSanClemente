package controller

import database.GestorBaseDatos
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.stage.Stage
import model.Sancion
import java.time.LocalDate

class SancionesController {

    @FXML private lateinit var btnVolver: Button
    @FXML private lateinit var tabla: TableView<Sancion>
    @FXML private lateinit var colUser: TableColumn<Sancion, String>
    @FXML private lateinit var colMotivo: TableColumn<Sancion, String>
    @FXML private lateinit var colDias: TableColumn<Sancion, Int>
    @FXML private lateinit var colFin: TableColumn<Sancion, String>
    @FXML private lateinit var colEstado: TableColumn<Sancion, String>
    @FXML private lateinit var btnManual: Button
    @FXML private lateinit var btnBorrar: Button

    @FXML
    fun initialize() {
        actualizarSancionesVencidas()
        configurarColumnas()
        tabla.items = cargarSanciones()
    }

    private fun configurarColumnas() {
        colUser.cellValueFactory = PropertyValueFactory("nombreUsuario")
        colMotivo.cellValueFactory = PropertyValueFactory("motivo")
        colDias.cellValueFactory = PropertyValueFactory("diasSancion")
        colFin.cellValueFactory = PropertyValueFactory("fechaFin")
        colEstado.cellValueFactory = PropertyValueFactory("estado")

        colEstado.setCellFactory {
            object : TableCell<Sancion, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = item
                    if (item == "ACTIVA") {
                        textFill = javafx.scene.paint.Color.RED
                        style = "-fx-font-weight: bold;"
                    } else if (item == "PAGADA") {
                        textFill = javafx.scene.paint.Color.GREEN
                        style = "-fx-font-weight: bold;"
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
    fun handleManual() {
        navegarA("/fxml/sancion_form.fxml", "Nueva Sanción Manual")
    }

    @FXML
    fun handleBorrar() {
        val seleccion = tabla.selectionModel.selectedItem
        if (seleccion != null) {
            val alerta = Alert(Alert.AlertType.CONFIRMATION)
            alerta.headerText = "¿Perdonar sanción?"
            alerta.contentText = "Esto eliminará la sanción de la base de datos."
            val resp = alerta.showAndWait()
            if (resp.isPresent && resp.get() == ButtonType.OK) {
                borrarSancion(seleccion.id)
                tabla.items = cargarSanciones()
            }
        } else {
            val alerta = Alert(Alert.AlertType.WARNING)
            alerta.contentText = "Selecciona una sanción de la lista."
            alerta.showAndWait()
        }
    }

    private fun actualizarSancionesVencidas() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val fechaHoy = LocalDate.now().toString()
                val sql = "UPDATE sanciones SET estado = 'PAGADA' WHERE fecha_fin < ? AND estado = 'ACTIVA'"
                val ps = conn.prepareStatement(sql)
                ps.setString(1, fechaHoy)
                val afectadas = ps.executeUpdate()
                if (afectadas > 0) {
                    println("Se han archivado automaticamente $afectadas sanciones vencidas.")
                }
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun cargarSanciones(): javafx.collections.ObservableList<Sancion> {
        val lista = FXCollections.observableArrayList<Sancion>()
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                val sql = """
                    SELECT s.id, u.nombre, s.motivo, s.fecha_inicio, s.fecha_fin, s.dias_sancion, s.estado 
                    FROM sanciones s
                    JOIN usuarios u ON s.usuario_id = u.id
                    ORDER BY s.fecha_inicio DESC
                """
                val rs = conn.createStatement().executeQuery(sql)
                while (rs.next()) {
                    lista.add(Sancion(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("motivo"),
                        rs.getString("fecha_inicio"),
                        rs.getString("fecha_fin"),
                        rs.getInt("dias_sancion"),
                        rs.getString("estado")
                    ))
                }
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
        return lista
    }

    private fun borrarSancion(id: Int) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val ps = conn.prepareStatement("DELETE FROM sanciones WHERE id = ?")
                ps.setInt(1, id)
                ps.executeUpdate()
                conn.close()
            } catch (e: Exception) {
                println(e.message)
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