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

    // enlazo con los elementos de la vista
    @FXML private lateinit var btnVolver: Button
    @FXML private lateinit var txtBuscar: TextField
    @FXML private lateinit var tabla: TableView<Libro>
    @FXML private lateinit var btnNuevo: Button
    @FXML private lateinit var btnEditar: Button
    @FXML private lateinit var btnBorrar: Button

    // uso una lista observable para que la tabla se actualice sola si cambio algo
    private val listaLibros = FXCollections.observableArrayList<Libro>()
    // esta lista filtrada es un "envoltorio" para poder buscar sin perder los datos originales
    private lateinit var listaFiltrada: FilteredList<Libro>

    @FXML
    fun initialize() {
        // preparo las columnas para saber que dato va en cada sitio
        configurarColumnas()

        // cargo los libros de la base de datos
        listaLibros.setAll(cargarLibros())

        // al principio muestro todo (predicado true)
        listaFiltrada = FilteredList(listaLibros) { true }
        tabla.items = listaFiltrada

        // aqui configuro el buscador en tiempo real
        // cada vez que escribo una letra, se ejecuta este codigo
        txtBuscar.textProperty().addListener { _, _, nuevoTexto ->
            listaFiltrada.setPredicate { libro ->
                // si borro el texto, muestro todos
                if (nuevoTexto.isNullOrEmpty()) true
                else {
                    // paso todo a minusculas para no distinguir entre mayusculas y minusculas
                    val lower = nuevoTexto.lowercase()

                    // busco por titulo, autor, isbn o editorial
                    libro.titulo.lowercase().contains(lower) ||
                            (libro.autor?.lowercase()?.contains(lower) ?: false) ||
                            libro.isbn.lowercase().contains(lower) ||
                            libro.editorial.lowercase().contains(lower)
                }
            }
        }
    }

    private fun configurarColumnas() {
        // recojo las columnas del fxml por orden (0, 1, 2...)
        val colId = tabla.columns[0] as TableColumn<Libro, Int>
        val colIsbn = tabla.columns[1] as TableColumn<Libro, String>
        val colTitulo = tabla.columns[2] as TableColumn<Libro, String>
        val colStock = tabla.columns[3] as TableColumn<Libro, String>
        val colTipo = tabla.columns[4] as TableColumn<Libro, String>
        val colAutor = tabla.columns[5] as TableColumn<Libro, String>
        val colEditorial = tabla.columns[6] as TableColumn<Libro, String>

        // le digo a cada columna que propiedad del objeto Libro tiene que pintar
        colId.cellValueFactory = PropertyValueFactory("id")
        colIsbn.cellValueFactory = PropertyValueFactory("isbn")
        colTitulo.cellValueFactory = PropertyValueFactory("titulo")

        // columna especial STOCK
        colStock.cellValueFactory = PropertyValueFactory("stock")

        // si el stock empieza por "0/", significa que no quedan libros
        // asi que pinto la celda de rojo suave para avisar al conserje
        colStock.setCellFactory {
            object : TableCell<Libro, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                        style = ""
                    } else {
                        text = item
                        if (item.startsWith("0/")) {
                            // rojo claro de fondo y letra en negrita
                            style = "-fx-background-color: #ffcccc; -fx-font-weight: bold; -fx-alignment: CENTER;"
                        } else {
                            style = "-fx-alignment: CENTER;"
                        }
                    }
                }
            }
        }

        // esta columna es un enum, asi que saco el nombre en string
        colTipo.setCellValueFactory { libro ->
            javafx.beans.property.SimpleStringProperty(libro.value.tipoPublicacion.name)
        }
        colAutor.cellValueFactory = PropertyValueFactory("autor")
        colEditorial.cellValueFactory = PropertyValueFactory("editorial")
    }

    // volver al menu principal
    @FXML
    fun handleVolver() {
        navegarA("/fxml/dashboard.fxml", "Panel de Gestión")
    }

    // ir al formulario vacio para crear
    @FXML
    fun handleNuevo() {
        navegarA("/fxml/libro_form.fxml", "Nuevo Libro")
    }

    // ir al formulario relleno para editar
    @FXML
    fun handleEditar() {
        val libroSeleccionado = tabla.selectionModel.selectedItem

        if (libroSeleccionado != null) {
            val loader = FXMLLoader(javaClass.getResource("/fxml/libro_form.fxml"))
            val root = loader.load<Any>()

            // aqui recupero el controlador del formulario para pasarle el libro
            val controller = loader.getController<LibroFormController>()
            controller.cargarLibroParaEditar(libroSeleccionado)

            val stage = btnEditar.scene.window as Stage
            stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
            stage.isMaximized = true
            stage.title = "Editar Libro"
        } else {
            // si no selecciono nada, aviso
            mostrarAlerta("Aviso", "Selecciona un libro para editarlo.", Alert.AlertType.WARNING)
        }
    }

    // borrar el libro de la base de datos
    @FXML
    fun handleBorrar() {
        val libroSeleccionado = tabla.selectionModel.selectedItem

        if (libroSeleccionado != null) {
            // pregunto antes de borrar para evitar accidentes
            val alerta = Alert(Alert.AlertType.CONFIRMATION)
            alerta.title = "Confirmar borrado"
            alerta.headerText = null
            alerta.contentText = "¿Seguro que quieres eliminar: ${libroSeleccionado.titulo}?"

            val respuesta = alerta.showAndWait()
            if (respuesta.isPresent && respuesta.get() == ButtonType.OK) {
                borrarLibroDeBD(libroSeleccionado.id)
                // recargo la lista para que desaparezca de la tabla
                listaLibros.setAll(cargarLibros())
            }
        } else {
            mostrarAlerta("Aviso", "Primero selecciona un libro de la lista.", Alert.AlertType.WARNING)
        }
    }

    // funcion para traer los datos de SQLite
    private fun cargarLibros(): List<Libro> {
        val lista = mutableListOf<Libro>()
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                // esta consulta es un poco mas compleja
                // hago dos subconsultas para contar cuantas copias hay en total y cuantas estan disponibles
                // asi puedo mostrar "2/3" en la columna de stock
                val sql = """
                    SELECT l.*, 
                           (SELECT COUNT(*) FROM ejemplares e WHERE e.libro_id = l.id) as total_copias,
                           (SELECT COUNT(*) FROM ejemplares e WHERE e.libro_id = l.id AND e.estado = 'DISPONIBLE') as disponibles
                    FROM libros l
                """
                val stmt = conn.createStatement()
                val rs = stmt.executeQuery(sql)

                while (rs.next()) {
                    val tipoTexto = rs.getString("tipo_publicacion") ?: "LIBRO"
                    val tipoEnum = try {
                        model.TipoPublicacion.valueOf(tipoTexto)
                    } catch (e: Exception) { model.TipoPublicacion.LIBRO }

                    // monto el string de stock
                    val total = rs.getInt("total_copias")
                    val disp = rs.getInt("disponibles")
                    val stockStr = "$disp/$total"

                    lista.add(Libro(
                        id = rs.getInt("id"),
                        isbn = rs.getString("isbn") ?: "Sin ISBN",
                        titulo = rs.getString("titulo"),
                        tipoPublicacion = tipoEnum,

                        stock = stockStr, // aqui va el string calculado

                        temas = rs.getString("temas") ?: "Sin temas",
                        editorial = rs.getString("editorial") ?: "Sin editorial",
                        editorialDireccion = rs.getString("editorial_direccion"),
                        editorialTelefono = rs.getString("editorial_telefono"),
                        idioma = rs.getString("idioma") ?: "Español",
                        modulosRelacionados = rs.getString("modulos_relacionados"),
                        ciclosRelacionados = rs.getString("ciclos_relacionados"),
                        autor = rs.getString("autor"),
                        nacionalidadAutor = rs.getString("nacionalidad_autor"),
                        edicion = rs.getString("edicion"),
                        fechaPublicacion = rs.getString("fecha_publicacion"),
                        periodicidad = rs.getString("periodicidad")
                    ))
                }
                conn.close()
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