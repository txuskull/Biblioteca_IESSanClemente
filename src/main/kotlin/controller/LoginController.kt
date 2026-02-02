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

    // vinculo estas variables con los id que puse en el scene builder
    @FXML private lateinit var txtUsuario: TextField
    @FXML private lateinit var txtPassword: PasswordField
    @FXML private lateinit var btnLogin: Button

    @FXML
    fun initialize() {
        // aqui iria codigo si necesitara cargar algo al arrancar la pantalla
    }

    // esta funcion salta cuando le doy al boton de entrar
    @FXML
    fun handleLogin() {
        val usuario = txtUsuario.text
        val password = txtPassword.text

        // primero compruebo que no esten vacios
        if (usuario.isBlank() || password.isBlank()) {
            mostrarAlerta("Error", "Por favor, introduce usuario y contraseña", Alert.AlertType.WARNING)
            return
        }

        // validamos usuario, contraseña Y ROL
        if (validarCredenciales(usuario, password)) {
            // si entra aqui es que es CONSERJE y la contraseña esta bien

            val stage = btnLogin.scene.window as Stage
            val loader = FXMLLoader(javaClass.getResource("/fxml/dashboard.fxml"))
            val root = loader.load<Any>()

            // cambio la escena y pongo la ventana en grande directamente
            stage.scene = Scene(root as javafx.scene.Parent, 1200.0, 700.0)
            stage.isMaximized = true
            stage.title = "Panel de Gestión - Biblioteca IES San Clemente"
        } else {
            mostrarAlerta("Error de acceso", "Usuario no autorizado o contraseña incorrecta", Alert.AlertType.ERROR)
        }
    }

    // funcion privada para consultar la base de datos
    private fun validarCredenciales(usuario: String, password: String): Boolean {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        var valido = false

        if (conn != null) {
            try {
                // consulta SQL es estricta.
                // Busco un usuario que tenga ese DNI...
                // ...que la contraseña coincida...
                // ...sea tipo 'CONSERJE'. Los alumnos no pasan.
                val sql = "SELECT * FROM usuarios WHERE dni = ? AND password = ? AND tipo = 'CONSERJE' LIMIT 1"

                val ps = conn.prepareStatement(sql)
                ps.setString(1, usuario)  // primer interrogante: el dni
                ps.setString(2, password) // segundo interrogante: la contraseña

                val rs = ps.executeQuery()

                // si la consulta encuentra algo, es que todo coincide
                if (rs.next()) {
                    valido = true
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