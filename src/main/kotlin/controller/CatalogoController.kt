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
import model.Libro

class CatalogoController {

    @FXML private lateinit var btnVolver: Button
    @FXML private lateinit var txtBuscar: TextField
    @FXML private lateinit var tabla: TableView<Libro>
    @FXML private lateinit var btnNuevo: Button
    @FXML private lateinit var btnEditar: Button
    @FXML private lateinit var btnBorrar: Button

    private val listaLibros = FXCollections.observableArrayList<Libro>()
    private lateinit var listaFiltrada: FilteredList<Libro>

    @FXML
    fun initialize() {
        // IMPORTANTE: Configurar las columnas de la tabla
        configurarColumnas()

        listaLibros.setAll(cargarLibros())
        listaFiltrada = FilteredList(listaLibros) { true }
        tabla.items = listaFiltrada

        txtBuscar.textProperty().addListener { _, _, nuevoTexto ->
            listaFiltrada.setPredicate { libro ->
                if (nuevoTexto.isNullOrEmpty()) true
                else {
                    val lower = nuevoTexto.lowercase()
                    libro.titulo.lowercase().contains(lower) ||
                            (libro.autor?.lowercase()?.contains(lower) ?: false) ||
                            libro.isbn.lowercase().contains(lower) ||
                            libro.editorial.lowercase().contains(lower)
                }
            }
        }
    }

    private fun configurarColumnas() {
        val colId = tabla.columns[0] as TableColumn<Libro, Int>
        val colIsbn = tabla.columns[1] as TableColumn<Libro, String>
        val colTitulo = tabla.columns[2] as TableColumn<Libro, String>
        val colTipo = tabla.columns[3] as TableColumn<Libro, String>
        val colAutor = tabla.columns[4] as TableColumn<Libro, String>
        val colEditorial = tabla.columns[5] as TableColumn<Libro, String>

        colId.cellValueFactory = PropertyValueFactory("id")
        colIsbn.cellValueFactory = PropertyValueFactory("isbn")
        colTitulo.cellValueFactory = PropertyValueFactory("titulo")
        colAutor.cellValueFactory = PropertyValueFactory("autor")
        colEditorial.cellValueFactory = PropertyValueFactory("editorial")

        // Columna TIPO personalizada
        colTipo.setCellValueFactory { libro ->
            javafx.beans.property.SimpleStringProperty(libro.value.tipoPublicacion.name)
        }
    }

    @FXML
    fun handleVolver() {
        navegarA("/fxml/dashboard.fxml", "Panel de Gestión")
    }

    @FXML
    fun handleNuevo() {
        navegarA("/fxml/libro_form.fxml", "Nuevo Libro")
    }

    @FXML
    fun handleEditar() {
        val libroSeleccionado = tabla.selectionModel.selectedItem

        if (libroSeleccionado != null) {
            val loader = FXMLLoader(javaClass.getResource("/fxml/libro_form.fxml"))
            val root = loader.load<Any>()
            val controller = loader.getController<LibroFormController>()
            controller.cargarLibroParaEditar(libroSeleccionado)

            val stage = btnEditar.scene.window as Stage
            stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
            stage.isMaximized = true
            stage.title = "Editar Libro"
        } else {
            mostrarAlerta("Aviso", "Selecciona un libro para editarlo.", Alert.AlertType.WARNING)
        }
    }

    @FXML
    fun handleBorrar() {
        val libroSeleccionado = tabla.selectionModel.selectedItem

        if (libroSeleccionado != null) {
            val alerta = Alert(Alert.AlertType.CONFIRMATION)
            alerta.title = "Confirmar borrado"
            alerta.headerText = null
            alerta.contentText = "¿Seguro que quieres eliminar: ${libroSeleccionado.titulo}?"

            val respuesta = alerta.showAndWait()
            if (respuesta.isPresent && respuesta.get() == ButtonType.OK) {
                borrarLibroDeBD(libroSeleccionado.id)
                listaLibros.setAll(cargarLibros())
            }
        } else {
            mostrarAlerta("Aviso", "Primero selecciona un libro de la lista.", Alert.AlertType.WARNING)
        }
    }

    private fun cargarLibros(): List<Libro> {
        val lista = mutableListOf<Libro>()
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                val sql = "SELECT * FROM libros"
                val stmt = conn.createStatement()
                val rs = stmt.executeQuery(sql)

                while (rs.next()) {
                    // Convertir texto a Enum
                    val tipoTexto = rs.getString("tipo_publicacion") ?: "LIBRO"
                    val tipoEnum = try {
                        model.TipoPublicacion.valueOf(tipoTexto)
                    } catch (e: Exception) {
                        model.TipoPublicacion.LIBRO
                    }

                    lista.add(Libro(
                        id = rs.getInt("id"),
                        isbn = rs.getString("isbn") ?: "Sin ISBN",
                        titulo = rs.getString("titulo"),
                        tipoPublicacion = tipoEnum,

                        // Campos comunes
                        temas = rs.getString("temas") ?: "Sin temas",
                        editorial = rs.getString("editorial") ?: "Sin editorial",
                        editorialDireccion = rs.getString("editorial_direccion"),
                        editorialTelefono = rs.getString("editorial_telefono"),
                        idioma = rs.getString("idioma") ?: "Español",
                        modulosRelacionados = rs.getString("modulos_relacionados"),
                        ciclosRelacionados = rs.getString("ciclos_relacionados"),

                        // Solo libros
                        autor = rs.getString("autor"),
                        nacionalidadAutor = rs.getString("nacionalidad_autor"),
                        edicion = rs.getString("edicion"),
                        fechaPublicacion = rs.getString("fecha_publicacion"),

                        // Solo revistas
                        periodicidad = rs.getString("periodicidad")
                    ))
                }
                conn.close()
                println("✅ Se cargaron ${lista.size} publicaciones en el catálogo")
            } catch (e: Exception) {
                println("Error cargando libros: ${e.message}")
                e.printStackTrace()
            }
        }
        return lista
    }

    private fun borrarLibroDeBD(idLibro: Int) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                val sql = "DELETE FROM libros WHERE id = ?"
                val pstmt = conn.prepareStatement(sql)
                pstmt.setInt(1, idLibro)
                pstmt.executeUpdate()
                conn.close()
                println("Libro borrado correctamente.")
            } catch (e: Exception) {
                println("Error al borrar: ${e.message}")
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