package controller

import database.GestorBaseDatos
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.stage.Stage

class DashboardController {

    @FXML private lateinit var btnCerrarSesion: Button
    @FXML private lateinit var btnCatalogo: Button
    @FXML private lateinit var btnUsuarios: Button
    @FXML private lateinit var btnPrestamos: Button
    @FXML private lateinit var btnSanciones: Button

    @FXML private lateinit var lblTotalLibros: Label
    @FXML private lateinit var lblPrestamosTotales: Label
    @FXML private lateinit var lblUsuarios: Label
    @FXML private lateinit var lblSancionesActivas: Label

    @FXML
    fun initialize() {
        cargarEstadisticas()
    }

    @FXML
    fun handleCerrarSesion() {
        val stage = btnCerrarSesion.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource("/fxml/login.fxml"))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 900.0, 600.0)
        stage.title = "Biblioteca IES San Clemente - Acceso Seguro"
    }

    @FXML
    fun handleCatalogo() {
        navegarA("/fxml/catalogo.fxml", "Catálogo de Libros")
    }

    @FXML
    fun handleUsuarios() {
        navegarA("/fxml/usuarios.fxml", "Gestión de Usuarios")
    }

    @FXML
    fun handlePrestamos() {
        navegarA("/fxml/prestamos.fxml", "Gestión de Préstamos")
    }

    @FXML
    fun handleSanciones() {
        navegarA("/fxml/sanciones.fxml", "Registro de Sanciones")
    }

    private fun navegarA(fxml: String, titulo: String) {
        val stage = btnCatalogo.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource(fxml))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
        stage.isMaximized = true
        stage.title = titulo
    }

    private fun cargarEstadisticas() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                // Total de libros
                var rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM libros")
                if (rs.next()) lblTotalLibros.text = rs.getInt("total").toString()

                // Préstamos totales
                rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM prestamos")
                if (rs.next()) lblPrestamosTotales.text = rs.getInt("total").toString()

                // Usuarios
                rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM usuarios")
                if (rs.next()) lblUsuarios.text = rs.getInt("total").toString()

                // Sanciones activas
                rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM sanciones WHERE estado = 'ACTIVA'")
                if (rs.next()) lblSancionesActivas.text = rs.getInt("total").toString()

                conn.close()
            } catch (e: Exception) {
                println("Error cargando estadísticas: ${e.message}")
            }
        }
    }
}