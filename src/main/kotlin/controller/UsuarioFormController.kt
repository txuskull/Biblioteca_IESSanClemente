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

    // variable para saber si estamos creando uno nuevo (null) o editando uno existente
    private var usuarioEditar: Usuario? = null

    @FXML
    fun initialize() {
        // borramos la lista manual y usamos la del Enum automatica
        // esto coge todos los valores que pusimos en TipoUsuario.kt y los convierte a texto
        cmbTipo.items.addAll(model.TipoUsuario.values().map { it.name })

        // selecciono el primero por defecto para que no quede vacio
        cmbTipo.selectionModel.selectFirst()
    }

    // esta funcion se llama cuando vengo desde el boton editar de la tabla
    fun cargarUsuarioParaEditar(usuario: Usuario) {
        usuarioEditar = usuario
        lblTitulo.text = "Editar Usuario"

        txtDni.text = usuario.dni
        txtDni.isDisable = true // el dni es la clave, no dejo que lo cambien nunca
        txtNombre.text = usuario.nombre
        txtEmail.text = usuario.email

        // selecciono en el combo el tipo que ya tenia el usuario
        cmbTipo.value = usuario.tipo.name

        txtPassword.promptText = "Dejar vacio para mantener la actual"
    }

    // volver atras sin hacer nada
    @FXML
    fun handleCancelar() {
        navegarAUsuarios()
    }

    // logica del boton guardar
    @FXML
    fun handleGuardar() {
        // validacion basica, el dni y nombre son obligatorios
        if (txtDni.text.isBlank() || txtNombre.text.isBlank()) {
            val alerta = Alert(Alert.AlertType.WARNING)
            alerta.title = "Faltan datos"
            alerta.headerText = null
            alerta.contentText = "Por favor, cubre el DNI y el Nombre."
            alerta.showAndWait()
            return
        }

        // decido si hago insert o update segun la variable de control
        if (usuarioEditar == null) {
            guardarUsuario()
        } else {
            actualizarUsuario()
        }
        navegarAUsuarios()
    }

    // insertar usuario nuevo en la base de datos
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

    // modificar usuario existente
    private fun actualizarUsuario() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                // el dni no lo actualizo porque es fijo
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

    // funcion para volver a la pantalla de lista
    private fun navegarAUsuarios() {
        val stage = btnCancelar.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource("/fxml/usuarios.fxml"))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
        stage.isMaximized = true
        stage.title = "Directorio de Usuarios"
    }
}