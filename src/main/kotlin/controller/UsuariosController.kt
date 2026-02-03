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
import model.Usuario

class UsuariosController {

    @FXML private lateinit var btnVolver: Button
    @FXML private lateinit var txtBuscar: TextField
    @FXML private lateinit var tabla: TableView<Usuario>
    @FXML private lateinit var btnNuevo: Button
    @FXML private lateinit var btnEditar: Button
    @FXML private lateinit var btnBorrar: Button

    // uso dos listas, una con todos los datos y otra filtrada para buscar
    private val listaUsuarios = FXCollections.observableArrayList<Usuario>()
    private lateinit var listaFiltrada: FilteredList<Usuario>

    @FXML
    fun initialize() {
        // preparo la tabla
        configurarColumnas()

        // cargo los usuarios de la base de datos
        listaUsuarios.setAll(cargarUsuarios())

        // al principio la lista filtrada muestra todo (true)
        listaFiltrada = FilteredList(listaUsuarios) { true }
        tabla.items = listaFiltrada

        // logica del buscador: se ejecuta cada vez que escribo una letra
        txtBuscar.textProperty().addListener { _, _, nuevoTexto ->
            listaFiltrada.setPredicate { usuario ->
                // si borro el texto, muestro a todos
                if (nuevoTexto.isNullOrEmpty()) true
                else {
                    // paso a minusculas para comparar sin problemas
                    val lower = nuevoTexto.lowercase()

                    // busco por nombre, dni o email
                    usuario.nombre.lowercase().contains(lower) ||
                            usuario.dni.lowercase().contains(lower) ||
                            usuario.email.lowercase().contains(lower)
                }
            }
        }
    }

    // aqui digo que propiedad del objeto usuario va en cada columna
    private fun configurarColumnas() {
        val colDni = tabla.columns[0] as TableColumn<Usuario, String>
        val colNombre = tabla.columns[1] as TableColumn<Usuario, String>
        val colTipo = tabla.columns[2] as TableColumn<Usuario, String>
        val colEmail = tabla.columns[3] as TableColumn<Usuario, String>
        val colSancion = tabla.columns[4] as TableColumn<Usuario, String>

        colDni.cellValueFactory = PropertyValueFactory("dni")
        colNombre.cellValueFactory = PropertyValueFactory("nombre")
        colTipo.cellValueFactory = PropertyValueFactory("tipo")
        colEmail.cellValueFactory = PropertyValueFactory("email")
        colSancion.cellValueFactory = PropertyValueFactory("sancionadoHasta")
    }

    // volver al menu principal
    @FXML
    fun handleVolver() {
        navegarA("/fxml/dashboard.fxml", "Panel de Gestión")
    }

    // ir al formulario vacio para crear usuario
    @FXML
    fun handleNuevo() {
        navegarA("/fxml/usuario_form.fxml", "Nuevo Usuario")
    }

    // ir al formulario con los datos cargados para modificar
    @FXML
    fun handleEditar() {
        val usuarioSeleccionado = tabla.selectionModel.selectedItem

        if (usuarioSeleccionado != null) {
            val loader = FXMLLoader(javaClass.getResource("/fxml/usuario_form.fxml"))
            val root = loader.load<Any>()

            // recupero el controlador del formulario para pasarle el usuario
            val controller = loader.getController<UsuarioFormController>()
            controller.cargarUsuarioParaEditar(usuarioSeleccionado)

            val stage = btnEditar.scene.window as Stage
            stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
            stage.isMaximized = true
            stage.title = "Editar Usuario"
        } else {
            // si no selecciono nada, aviso
            mostrarAlerta("Atencion", "Por favor, selecciona un usuario.", Alert.AlertType.WARNING)
        }
    }

    // borrar usuario de la base de datos
    @FXML
    fun handleBorrar() {
        val usuarioSeleccionado = tabla.selectionModel.selectedItem

        if (usuarioSeleccionado != null) {
            // pido confirmacion antes de borrar
            val alerta = Alert(Alert.AlertType.CONFIRMATION)
            alerta.title = "Confirmar borrado"
            alerta.headerText = null
            alerta.contentText = "¿Seguro que quieres eliminar a: ${usuarioSeleccionado.nombre}?"

            val respuesta = alerta.showAndWait()
            if (respuesta.isPresent && respuesta.get() == ButtonType.OK) {
                borrarUsuario(usuarioSeleccionado.id)
                // recargo la lista para ver los cambios
                listaUsuarios.setAll(cargarUsuarios())
            }
        } else {
            mostrarAlerta("Atencion", "Debes seleccionar un usuario.", Alert.AlertType.WARNING)
        }
    }

    // conectar a la base de datos y traer la lista de usuarios
    private fun cargarUsuarios(): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val rs = conn.createStatement().executeQuery("SELECT * FROM usuarios")
                while (rs.next()) {
                    // convierto el string de la base de datos a mi enum de kotlin
                    val tipoTexto = rs.getString("tipo")
                    val tipoEnum = try {
                        model.TipoUsuario.valueOf(tipoTexto)
                    } catch (e: Exception) {
                        model.TipoUsuario.ESTUDIANTE
                    }

                    lista.add(model.Usuario(
                        rs.getInt("id"),
                        rs.getString("dni"),
                        rs.getString("nombre"),
                        tipoEnum,
                        rs.getString("email") ?: "",
                        rs.getString("sancionado_hasta")
                    ))
                }
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
        return lista
    }

    // ejecutar el delete en sql
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

    private fun mostrarAlerta(titulo: String, mensaje: String, tipo: Alert.AlertType) {
        val alerta = Alert(tipo)
        alerta.title = titulo
        alerta.headerText = null
        alerta.contentText = mensaje
        alerta.showAndWait()
    }
}