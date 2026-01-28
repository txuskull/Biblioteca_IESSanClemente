package controller

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.stage.Stage
import database.GestorBaseDatos

class LoginController {

    @FXML private lateinit var txtUsuario: TextField
    @FXML private lateinit var txtPassword: PasswordField
    @FXML private lateinit var btnLogin: Button

    @FXML
    fun initialize() {
        // Configuración inicial si es necesaria
    }

    @FXML
    fun handleLogin() {
        val usuario = txtUsuario.text
        val password = txtPassword.text

        if (usuario.isBlank() || password.isBlank()) {
            mostrarAlerta("Error", "Por favor, introduce usuario y contraseña", Alert.AlertType.WARNING)
            return
        }

        if (validarCredenciales(usuario, password)) {
            // Cargar dashboard
            val stage = btnLogin.scene.window as Stage
            val loader = FXMLLoader(javaClass.getResource("/fxml/dashboard.fxml"))
            val root = loader.load<Any>()

            stage.scene = Scene(root as javafx.scene.Parent, 1200.0, 700.0)
            stage.isMaximized = true
            stage.title = "Panel de Gestión - Biblioteca IES San Clemente"
        } else {
            mostrarAlerta("Error de acceso", "Usuario o contraseña incorrectos", Alert.AlertType.ERROR)
        }
    }

    private fun validarCredenciales(usuario: String, password: String): Boolean {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        var valido = false

        if (conn != null) {
            try {
                // Por simplicidad, validamos contra cualquier usuario
                // En producción deberías tener una tabla específica de credenciales
                val sql = "SELECT * FROM usuarios WHERE dni = ? LIMIT 1"
                val ps = conn.prepareStatement(sql)
                ps.setString(1, usuario)
                val rs = ps.executeQuery()

                if (rs.next()) {
                    valido = true // Simplificado: acepta cualquier contraseña
                }
                conn.close()
            } catch (e: Exception) {
                println("Error validando credenciales: ${e.message}")
            }
        }
        return valido
    }

    private fun mostrarAlerta(titulo: String, mensaje: String, tipo: Alert.AlertType) {
        val alerta = Alert(tipo)
        alerta.title = titulo
        alerta.headerText = null
        alerta.contentText = mensaje
        alerta.showAndWait()
    }
}