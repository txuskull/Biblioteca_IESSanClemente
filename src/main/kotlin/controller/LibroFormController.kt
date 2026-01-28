package controller

import database.GestorBaseDatos
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import model.Libro

class LibroFormController {

    @FXML private lateinit var txtIsbn: TextField
    @FXML private lateinit var txtTitulo: TextField
    @FXML private lateinit var cmbTipo: ComboBox<String>
    @FXML private lateinit var txtAutor: TextField
    @FXML private lateinit var txtNacionalidad: TextField
    @FXML private lateinit var txtEditorial: TextField
    @FXML private lateinit var txtEditorialDir: TextField
    @FXML private lateinit var txtEditorialTel: TextField
    @FXML private lateinit var txtTemas: TextArea
    @FXML private lateinit var btnCancelar: Button
    @FXML private lateinit var btnGuardar: Button
    @FXML private lateinit var lblTitulo: Label

    private var libroEditar: Libro? = null

    @FXML
    fun initialize() {
        cmbTipo.items.addAll("LIBRO", "REVISTA")
        cmbTipo.selectionModel.selectFirst()
    }

    fun cargarLibroParaEditar(libro: Libro) {
        libroEditar = libro
        lblTitulo.text = "Editar Publicacion"

        txtIsbn.text = libro.isbn
        txtIsbn.isDisable = true
        txtTitulo.text = libro.titulo
        txtAutor.text = libro.autor
        txtEditorial.text = libro.editorial

        // Cargar detalles adicionales de la BD
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val sql = "SELECT * FROM libros WHERE id = ?"
                val pstmt = conn.prepareStatement(sql)
                pstmt.setInt(1, libro.id)
                val rs = pstmt.executeQuery()
                if (rs.next()) {
                    // Aquí cargarías campos adicionales si existieran
                }
                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
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
            alerta.contentText = "El ISBN y el Titulo son obligatorios."
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
                val sql = "INSERT INTO libros (isbn, titulo, autor, editorial) VALUES (?, ?, ?, ?)"
                val pstmt = conn.prepareStatement(sql)
                pstmt.setString(1, txtIsbn.text)
                pstmt.setString(2, txtTitulo.text)
                pstmt.setString(3, txtAutor.text)
                pstmt.setString(4, txtEditorial.text)
                pstmt.executeUpdate()

                // Crear ejemplares automáticos
                val generatedKeys = pstmt.generatedKeys
                if (generatedKeys.next()) {
                    val nuevoId = generatedKeys.getInt(1)
                    generarEjemplaresParaLibro(conn, nuevoId)
                }

                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun generarEjemplaresParaLibro(conn: java.sql.Connection, idLibro: Int) {
        try {
            val sqlInsert = "INSERT INTO ejemplares (libro_id, estado) VALUES (?, 'DISPONIBLE')"
            val ps = conn.prepareStatement(sqlInsert)
            // Creamos 3 copias automáticas
            for (i in 1..3) {
                ps.setInt(1, idLibro)
                ps.executeUpdate()
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
                val sql = "UPDATE libros SET titulo=?, autor=?, editorial=? WHERE id=?"
                val pstmt = conn.prepareStatement(sql)
                pstmt.setString(1, txtTitulo.text)
                pstmt.setString(2, txtAutor.text)
                pstmt.setString(3, txtEditorial.text)
                pstmt.setInt(4, libroEditar!!.id)
                pstmt.executeUpdate()
                conn.close()
            } catch (e: Exception) {
                println(e.message)
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