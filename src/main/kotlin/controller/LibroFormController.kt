package controller

import database.GestorBaseDatos
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.stage.Stage
import model.Libro
import model.TipoPublicacion

class LibroFormController {

    @FXML private lateinit var txtIsbn: TextField
    @FXML private lateinit var txtTitulo: TextField
    @FXML private lateinit var cmbTipo: ComboBox<String>
    @FXML private lateinit var txtTemas: TextArea
    @FXML private lateinit var txtEditorial: TextField
    @FXML private lateinit var txtEditorialDir: TextField
    @FXML private lateinit var txtEditorialTel: TextField
    @FXML private lateinit var cmbIdioma: ComboBox<String>
    @FXML private lateinit var txtModulos: TextField
    @FXML private lateinit var txtCiclos: TextField

    // Campos SOLO para LIBROS
    @FXML private lateinit var txtAutor: TextField
    @FXML private lateinit var txtNacionalidad: TextField
    @FXML private lateinit var txtEdicion: TextField
    @FXML private lateinit var txtFechaPublicacion: TextField

    // Campos SOLO para REVISTAS
    @FXML private lateinit var cmbPeriodicidad: ComboBox<String>

    @FXML private lateinit var btnCancelar: Button
    @FXML private lateinit var btnGuardar: Button
    @FXML private lateinit var lblTitulo: Label

    // GridPane para ocultar/mostrar filas
    @FXML private lateinit var gridForm: GridPane

    // Ocultar campos

    @FXML private lateinit var lblAutor: Label
    @FXML private lateinit var lblNacionalidad: Label
    @FXML private lateinit var lblEdicion: Label
    @FXML private lateinit var lblFechaPublicacion: Label
    @FXML private lateinit var lblPeriodicidad: Label

    private var libroEditar: Libro? = null

    @FXML
    fun initialize() {
        // Configurar ComboBox de tipo
        cmbTipo.items.addAll("LIBRO", "REVISTA")
        cmbTipo.selectionModel.selectFirst()

        // Configurar ComboBox de idioma
        cmbIdioma.items.addAll("Español", "Inglés", "Francés", "Alemán", "Portugués", "Gallego")
        cmbIdioma.selectionModel.select("Español")

        // Configurar ComboBox de periodicidad
        cmbPeriodicidad.items.addAll("Diaria", "Semanal", "Quincenal", "Mensual", "Bimensual", "Trimestral")
        cmbPeriodicidad.selectionModel.select("Mensual")

        // Listener para mostrar/ocultar campos según tipo
        cmbTipo.selectionModel.selectedItemProperty().addListener { _, _, nuevoTipo ->
            actualizarCamposSegunTipo(nuevoTipo)
        }

        // Inicializar vista según tipo por defecto
        actualizarCamposSegunTipo("LIBRO")
    }

    private fun actualizarCamposSegunTipo(tipo: String) {
        val esLibro = tipo == "LIBRO"

        if (esLibro) {
            // Mostrar campos de LIBRO
            lblAutor.isVisible = true
            lblAutor.isManaged = true
            txtAutor.isVisible = true
            txtAutor.isManaged = true

            lblNacionalidad.isVisible = true
            lblNacionalidad.isManaged = true
            txtNacionalidad.isVisible = true
            txtNacionalidad.isManaged = true

            lblEdicion.isVisible = true
            lblEdicion.isManaged = true
            txtEdicion.isVisible = true
            txtEdicion.isManaged = true

            lblFechaPublicacion.isVisible = true
            lblFechaPublicacion.isManaged = true
            txtFechaPublicacion.isVisible = true
            txtFechaPublicacion.isManaged = true

            // Ocultar campos de REVISTA
            lblPeriodicidad.isVisible = false
            lblPeriodicidad.isManaged = false
            cmbPeriodicidad.isVisible = false
            cmbPeriodicidad.isManaged = false

        } else {
            // Ocultar campos de LIBRO
            lblAutor.isVisible = false
            lblAutor.isManaged = false
            txtAutor.isVisible = false
            txtAutor.isManaged = false

            lblNacionalidad.isVisible = false
            lblNacionalidad.isManaged = false
            txtNacionalidad.isVisible = false
            txtNacionalidad.isManaged = false

            lblEdicion.isVisible = false
            lblEdicion.isManaged = false
            txtEdicion.isVisible = false
            txtEdicion.isManaged = false

            lblFechaPublicacion.isVisible = false
            lblFechaPublicacion.isManaged = false
            txtFechaPublicacion.isVisible = false
            txtFechaPublicacion.isManaged = false

            // Mostrar campos de REVISTA
            lblPeriodicidad.isVisible = true
            lblPeriodicidad.isManaged = true
            cmbPeriodicidad.isVisible = true
            cmbPeriodicidad.isManaged = true
        }
    }

    fun cargarLibroParaEditar(libro: Libro) {
        libroEditar = libro
        lblTitulo.text = "Editar Publicación"

        txtIsbn.text = libro.isbn
        txtIsbn.isDisable = true
        txtTitulo.text = libro.titulo
        cmbTipo.value = libro.tipoPublicacion.name

        // Campos comunes
        txtTemas.text = libro.temas
        txtEditorial.text = libro.editorial
        txtEditorialDir.text = libro.editorialDireccion ?: ""
        txtEditorialTel.text = libro.editorialTelefono ?: ""
        cmbIdioma.value = libro.idioma
        txtModulos.text = libro.modulosRelacionados ?: ""
        txtCiclos.text = libro.ciclosRelacionados ?: ""

        // Campos de libro
        if (libro.esLibro()) {
            txtAutor.text = libro.autor ?: ""
            txtNacionalidad.text = libro.nacionalidadAutor ?: ""
            txtEdicion.text = libro.edicion ?: ""
            txtFechaPublicacion.text = libro.fechaPublicacion ?: ""
        }

        // Campos de revista
        if (libro.esRevista()) {
            cmbPeriodicidad.value = libro.periodicidad ?: "Mensual"
        }

        actualizarCamposSegunTipo(libro.tipoPublicacion.name)
    }

    @FXML
    fun handleCancelar() {
        navegarACatalogo()
    }

    @FXML
    fun handleGuardar() {
        if (txtIsbn.text.isEmpty() || txtTitulo.text.isEmpty()) {
            val alerta = Alert(Alert.AlertType.WARNING)
            alerta.title = "Datos incompletos"
            alerta.headerText = null
            alerta.contentText = "El ISBN y el Título son obligatorios."
            alerta.showAndWait()
        } else {
            if (libroEditar == null) {
                guardarNuevo()
            } else {
                actualizarExistente()
            }
            navegarACatalogo()
        }
    }

    private fun guardarNuevo() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val tipo = cmbTipo.value
                val esLibro = tipo == "LIBRO"

                val sql = if (esLibro) {
                    """
                    INSERT INTO libros 
                    (isbn, titulo, tipo_publicacion, temas, editorial, editorial_direccion, editorial_telefono, 
                     idioma, modulos_relacionados, ciclos_relacionados, autor, nacionalidad_autor, edicion, fecha_publicacion)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """
                } else {
                    """
                    INSERT INTO libros 
                    (isbn, titulo, tipo_publicacion, temas, editorial, editorial_direccion, editorial_telefono, 
                     idioma, modulos_relacionados, ciclos_relacionados, periodicidad)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """
                }

                val pstmt = conn.prepareStatement(sql)
                pstmt.setString(1, txtIsbn.text)
                pstmt.setString(2, txtTitulo.text)
                pstmt.setString(3, tipo)
                pstmt.setString(4, txtTemas.text)
                pstmt.setString(5, txtEditorial.text)
                pstmt.setString(6, txtEditorialDir.text)
                pstmt.setString(7, txtEditorialTel.text)
                pstmt.setString(8, cmbIdioma.value)
                pstmt.setString(9, txtModulos.text)
                pstmt.setString(10, txtCiclos.text)

                if (esLibro) {
                    pstmt.setString(11, txtAutor.text)
                    pstmt.setString(12, txtNacionalidad.text)
                    pstmt.setString(13, txtEdicion.text)
                    pstmt.setString(14, txtFechaPublicacion.text)
                } else {
                    pstmt.setString(11, cmbPeriodicidad.value)
                }

                pstmt.executeUpdate()

                // Crear ejemplares automáticos
                val generatedKeys = pstmt.generatedKeys
                if (generatedKeys.next()) {
                    val nuevoId = generatedKeys.getInt(1)
                    generarEjemplaresParaPublicacion(conn, nuevoId, tipo)
                }

                conn.close()
            } catch (e: Exception) {
                println(e.message)
                e.printStackTrace()
            }
        }
    }

    private fun generarEjemplaresParaPublicacion(conn: java.sql.Connection, idLibro: Int, tipo: String) {
        try {
            if (tipo == "LIBRO") {
                // LIBROS: crear 3 ejemplares
                val sqlInsert = """
                INSERT INTO ejemplares (libro_id, codigo_ejemplar, numero_revista, fecha_adquisicion, estado) 
                VALUES (?, ?, ?, ?, 'DISPONIBLE')
            """
                val ps = conn.prepareStatement(sqlInsert)

                for (i in 1..3) {
                    ps.setInt(1, idLibro)
                    ps.setString(2, "AUTO-$idLibro-$i")
                    ps.setNull(3, java.sql.Types.INTEGER) // numero_revista = NULL para libros
                    ps.setString(4, java.time.LocalDate.now().toString())
                    ps.executeUpdate()
                }

                println("✅ Se crearon 3 ejemplares para el libro ID: $idLibro")
            } else {
                // REVISTAS: crear 1 ejemplar con número 1
                // (El usuario puede añadir más números manualmente después)
                val sqlInsert = """
                INSERT INTO ejemplares (libro_id, codigo_ejemplar, numero_revista, fecha_adquisicion, estado) 
                VALUES (?, ?, ?, ?, 'DISPONIBLE')
            """
                val ps = conn.prepareStatement(sqlInsert)

                ps.setInt(1, idLibro)
                ps.setString(2, "REV-$idLibro-1")
                ps.setInt(3, 1) // Empieza con número 1
                ps.setString(4, java.time.LocalDate.now().toString())
                ps.executeUpdate()

                println("✅ Se creó 1 ejemplar (Nº 1) para la revista ID: $idLibro")
            }
        } catch (e: Exception) {
            println("Error creando ejemplares: ${e.message}")
        }
    }

    private fun actualizarExistente() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val tipo = cmbTipo.value
                val esLibro = tipo == "LIBRO"

                val sql = """
                    UPDATE libros SET 
                    titulo=?, tipo_publicacion=?, temas=?, editorial=?, editorial_direccion=?, editorial_telefono=?,
                    idioma=?, modulos_relacionados=?, ciclos_relacionados=?,
                    autor=?, nacionalidad_autor=?, edicion=?, fecha_publicacion=?, periodicidad=?
                    WHERE id=?
                """

                val ps = conn.prepareStatement(sql)
                ps.setString(1, txtTitulo.text)
                ps.setString(2, tipo)
                ps.setString(3, txtTemas.text)
                ps.setString(4, txtEditorial.text)
                ps.setString(5, txtEditorialDir.text)
                ps.setString(6, txtEditorialTel.text)
                ps.setString(7, cmbIdioma.value)
                ps.setString(8, txtModulos.text)
                ps.setString(9, txtCiclos.text)

                if (esLibro) {
                    ps.setString(10, txtAutor.text)
                    ps.setString(11, txtNacionalidad.text)
                    ps.setString(12, txtEdicion.text)
                    ps.setString(13, txtFechaPublicacion.text)
                    ps.setString(14, null)
                } else {
                    ps.setString(10, null)
                    ps.setString(11, null)
                    ps.setString(12, null)
                    ps.setString(13, null)
                    ps.setString(14, cmbPeriodicidad.value)
                }

                ps.setInt(15, libroEditar!!.id)
                ps.executeUpdate()
                conn.close()
            } catch (e: Exception) {
                println(e.message)
                e.printStackTrace()
            }
        }
    }

    private fun navegarACatalogo() {
        val stage = btnCancelar.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource("/fxml/catalogo.fxml"))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
        stage.isMaximized = true
        stage.title = "Catálogo de Libros"
    }
}