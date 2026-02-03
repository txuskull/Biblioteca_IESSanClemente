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
        // cargo los tipos de usuario del enum
        cmbTipo.items.addAll(model.TipoUsuario.values().map { it.name })
        cmbTipo.selectionModel.selectFirst()
    }

    // cargo los datos si vengo de editar uno existente
    fun cargarUsuarioParaEditar(usuario: Usuario) {
        usuarioEditar = usuario
        lblTitulo.text = "Editar Usuario"

        txtDni.text = usuario.dni
        txtDni.isDisable = true // bloqueo el dni para que no se pueda cambiar
        txtNombre.text = usuario.nombre
        txtEmail.text = usuario.email
        cmbTipo.value = usuario.tipo.name
        txtPassword.promptText = "Dejar vacio para mantener la actual"
    }

    @FXML
    fun handleCancelar() {
        navegarAUsuarios()
    }

    @FXML
    fun handleGuardar() {
        // compruebo que no dejen campos vacios importantes
        if (txtDni.text.isBlank() || txtNombre.text.isBlank()) {
            mostrarAlerta("Faltan datos", "Por favor, cubre el DNI y el Nombre.", Alert.AlertType.WARNING)
            return
        }

        if (usuarioEditar == null) {
            guardarUsuario()
        } else {
            actualizarUsuario()
        }
        // he quitado de aqui el navegarAUsuarios()
        // ahora lo llamo dentro de guardar/actualizar solo si todo sale bien
        // asi si hay error me quedo en esta pantalla para corregirlo
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

                // si ha llegado aqui es que ha guardado bien, asi que vuelvo a la lista
                navegarAUsuarios()

            } catch (e: Exception) {
                // AQUI ESTA EL CAMBIO: detecto si el error es por dni duplicado
                // sqlite devuelve un error que contiene 'UNIQUE constraint failed'
                if (e.message?.contains("UNIQUE") == true || e.message?.contains("dni") == true) {
                    mostrarAlerta("Usuario Duplicado", "Ya existe un usuario con el DNI ${txtDni.text}", Alert.AlertType.ERROR)
                } else {
                    // si es otro error raro, lo muestro tambien
                    mostrarAlerta("Error", "No se pudo guardar: ${e.message}", Alert.AlertType.ERROR)
                }
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

                // todo ok, vuelvo a la lista
                navegarAUsuarios()
            } catch (e: Exception) {
                mostrarAlerta("Error", "No se pudo actualizar: ${e.message}", Alert.AlertType.ERROR)
            }
        }
    }

    private fun navegarAUsuarios() {
        try {
            val stage = btnCancelar.scene.window as Stage
            val loader = FXMLLoader(javaClass.getResource("/fxml/usuarios.fxml"))
            // Cargar como Parent
            val root = loader.load<javafx.scene.Parent>()

            // Cambiar contenido, no ventana
            stage.scene.root = root
            stage.title = "Directorio de Usuarios"
        } catch (e: Exception) {
            println("Error navegando: ${e.message}")
        }
    }

    // funcion auxiliar para sacar ventanas emergentes rapido
    private fun mostrarAlerta(titulo: String, mensaje: String, tipo: Alert.AlertType) {
        val alerta = Alert(tipo)
        alerta.title = titulo
        alerta.headerText = null
        alerta.contentText = mensaje
        alerta.showAndWait()
    }
}