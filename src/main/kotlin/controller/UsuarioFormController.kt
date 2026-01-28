package controller

import database.GestorBaseDatos
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import model.Usuario

class UsuarioFormController {

    @FXML private lateinit var txtDni: TextField
    @FXML private lateinit var txtNombre: TextField
    @FXML private lateinit var txtEmail: TextField
    @FXML private lateinit var txtPassword: PasswordField
    @FXML private lateinit var cmbTipo: ComboBox<String>
    @FXML private lateinit var btnCancelar: Button
    @FXML private lateinit var btnGuardar: Button
    @FXML private lateinit var lblTitulo: Label

    private var usuarioEditar: Usuario? = null

    @FXML
    fun initialize() {
        cmbTipo.items.addAll("ESTUDIANTE", "PROFESOR")
        cmbTipo.selectionModel.selectFirst()
    }

    fun cargarUsuarioParaEditar(usuario: Usuario) {
        usuarioEditar = usuario
        lblTitulo.text = "Editar Usuario"

        txtDni.text = usuario.dni
        txtDni.isDisable = true
        txtNombre.text = usuario.nombre
        txtEmail.text = usuario.email
        cmbTipo.value = usuario.tipo
        txtPassword.promptText = "Dejar vacio para mantener la actual"
    }

    @FXML
    fun handleCancelar() {
        navegarAUsuarios()
    }

    @FXML
    fun handleGuardar() {
        if (txtDni.text.isBlank() || txtNombre.text.isBlank()) {
            val alerta = Alert(Alert.AlertType.WARNING)
            alerta.title = "Faltan datos"
            alerta.headerText = null
            alerta.contentText = "Por favor, cubre el DNI y el Nombre."
            alerta.showAndWait()
            return
        }

        if (usuarioEditar == null) {
            guardarUsuario()
        } else {
            actualizarUsuario()
        }
        navegarAUsuarios()
    }

    private fun guardarUsuario() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val sql = "INSERT INTO usuarios (dni, nombre, tipo, email) VALUES (?,?,?,?)"
                val ps = conn.prepareStatement(sql)
                ps.setString(1, txtDni.text)
                ps.setString(2, txtNombre.text)
                ps.setString(3, cmbTipo.value)
                ps.setString(4, txtEmail.text)
                ps.executeUpdate()
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun actualizarUsuario() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val sql = "UPDATE usuarios SET nombre=?, tipo=?, email=? WHERE id=?"
                val ps = conn.prepareStatement(sql)
                ps.setString(1, txtNombre.text)
                ps.setString(2, cmbTipo.value)
                ps.setString(3, txtEmail.text)
                ps.setInt(4, usuarioEditar!!.id)
                ps.executeUpdate()
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun navegarAUsuarios() {
        val stage = btnCancelar.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource("/fxml/usuarios.fxml"))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
        stage.isMaximized = true
        stage.title = "Directorio de Usuarios"
    }
}