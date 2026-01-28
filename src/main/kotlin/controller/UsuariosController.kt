package controller

import database.GestorBaseDatos
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import model.Usuario

class UsuariosController {

    @FXML private lateinit var btnVolver: Button
    @FXML private lateinit var tabla: TableView<Usuario>
    @FXML private lateinit var btnNuevo: Button
    @FXML private lateinit var btnEditar: Button
    @FXML private lateinit var btnBorrar: Button

    @FXML
    fun initialize() {
        tabla.items = cargarUsuarios()
    }

    @FXML
    fun handleVolver() {
        navegarA("/fxml/dashboard.fxml", "Panel de Gestión")
    }

    @FXML
    fun handleNuevo() {
        navegarA("/fxml/usuario_form.fxml", "Nuevo Usuario")
    }

    @FXML
    fun handleEditar() {
        val usuarioSeleccionado = tabla.selectionModel.selectedItem

        if (usuarioSeleccionado != null) {
            val loader = FXMLLoader(javaClass.getResource("/fxml/usuario_form.fxml"))
            val root = loader.load<Any>()
            val controller = loader.getController<UsuarioFormController>()
            controller.cargarUsuarioParaEditar(usuarioSeleccionado)

            val stage = btnEditar.scene.window as Stage
            stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
            stage.isMaximized = true
            stage.title = "Editar Usuario"
        } else {
            mostrarAlerta("Atencion", "Por favor, selecciona un usuario.", Alert.AlertType.WARNING)
        }
    }

    @FXML
    fun handleBorrar() {
        val usuarioSeleccionado = tabla.selectionModel.selectedItem

        if (usuarioSeleccionado != null) {
            val alerta = Alert(Alert.AlertType.CONFIRMATION)
            alerta.title = "Confirmar borrado"
            alerta.headerText = null
            alerta.contentText = "¿Seguro que quieres eliminar a: ${usuarioSeleccionado.nombre}?"

            val respuesta = alerta.showAndWait()
            if (respuesta.isPresent && respuesta.get() == ButtonType.OK) {
                borrarUsuario(usuarioSeleccionado.id)
                tabla.items = cargarUsuarios()
            }
        } else {
            mostrarAlerta("Atencion", "Debes seleccionar un usuario.", Alert.AlertType.WARNING)
        }
    }

    private fun cargarUsuarios(): javafx.collections.ObservableList<Usuario> {
        val lista = FXCollections.observableArrayList<Usuario>()
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val rs = conn.createStatement().executeQuery("SELECT * FROM usuarios")
                while (rs.next()) {
                    lista.add(Usuario(
                        rs.getInt("id"),
                        rs.getString("dni"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.getString("email") ?: "",
                        rs.getString("sancionado_hasta") ?: "No"
                    ))
                }
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
        return lista
    }

    private fun borrarUsuario(id: Int) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val pstmt = conn.prepareStatement("DELETE FROM usuarios WHERE id = ?")
                pstmt.setInt(1, id)
                pstmt.executeUpdate()
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

    private fun mostrarAlerta(titulo: String, mensaje: String, tipo: Alert.AlertType) {
        val alerta = Alert(tipo)
        alerta.title = titulo
        alerta.headerText = null
        alerta.contentText = mensaje
        alerta.showAndWait()
    }
}