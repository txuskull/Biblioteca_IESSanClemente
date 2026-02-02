package controller

import database.GestorBaseDatos
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.chart.BarChart
import javafx.scene.chart.PieChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.stage.Stage

class DashboardController {

    // botones del menu lateral para moverme entre pantallas
    @FXML private lateinit var btnCerrarSesion: Button
    @FXML private lateinit var btnCatalogo: Button
    @FXML private lateinit var btnUsuarios: Button
    @FXML private lateinit var btnPrestamos: Button
    @FXML private lateinit var btnSanciones: Button

    // etiquetas para los numeros grandes del dashboard (las tarjetas de colores)
    @FXML private lateinit var lblTotalTitulos: Label
    @FXML private lateinit var lblTotalEjemplares: Label
    @FXML private lateinit var lblPrestamosTotales: Label
    @FXML private lateinit var lblUsuarios: Label
    @FXML private lateinit var lblSancionesActivas: Label
    @FXML private lateinit var lblLibroTop: Label

    // graficas para ver los datos visualmente
    @FXML private lateinit var pieChartDisponibilidad: PieChart
    @FXML private lateinit var barChartTop5: BarChart<String, Number>

    @FXML
    fun initialize() {
        // al arrancar el dashboard cargo todos los datos de la base de datos
        cargarEstadisticas()
        cargarGraficaDisponibilidad()
        cargarGraficaTop5()
    }

    // funcion para volver al login y desconectar
    @FXML
    fun handleCerrarSesion() {
        val stage = btnCerrarSesion.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource("/fxml/login.fxml"))
        val root = loader.load<Any>()

        // vuelvo a poner la ventana en tamaño pequeño para el login
        stage.scene = Scene(root as javafx.scene.Parent, 900.0, 600.0)
        stage.title = "Biblioteca IES San Clemente - Acceso Seguro"
    }

    // botones de navegacion del menu lateral
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

    // funcion auxiliar para no repetir codigo al cambiar de pantalla
    private fun navegarA(fxml: String, titulo: String) {
        val stage = btnCatalogo.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource(fxml))
        val root = loader.load<Any>()

        // maximizo la ventana para aprovechar la pantalla completa
        stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
        stage.isMaximized = true
        stage.title = titulo
    }

    // aqui hago todas las consultas sql para rellenar los contadores
    private fun cargarEstadisticas() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                // cuento cuantos libros unicos hay en catalogo
                var rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM libros")
                if (rs.next()) lblTotalTitulos.text = rs.getInt("total").toString()
                // rs.next() avanza el cursor a la siguiente fila (la primera en este caso) y devuelve true si existe

                // cuento cuantos libros fisicos tengo en total
                rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM ejemplares")
                if (rs.next()) lblTotalEjemplares.text = rs.getInt("total").toString()

                // cuento cuantos prestamos se han hecho en la historia
                rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM prestamos")
                if (rs.next()) lblPrestamosTotales.text = rs.getInt("total").toString()

                // cuento los usuarios registrados (alumnos + profes + conserjes)
                rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM usuarios")
                if (rs.next()) lblUsuarios.text = rs.getInt("total").toString()

                // cuento cuanta gente esta castigada ahora mismo
                rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM sanciones WHERE estado = 'ACTIVA'")
                if (rs.next()) lblSancionesActivas.text = rs.getInt("total").toString()

                // busco cual es el libro que mas veces se ha prestado
                val sqlTop = """
                    SELECT l.titulo, COUNT(p.id) as total
                    FROM prestamos p
                    JOIN ejemplares e ON p.ejemplar_id = e.id
                    JOIN libros l ON e.libro_id = l.id
                    GROUP BY l.id, l.titulo
                    ORDER BY total DESC
                    LIMIT 1
                """
                rs = conn.createStatement().executeQuery(sqlTop)
                if (rs.next()) {
                    val titulo = rs.getString("titulo")
                    // si el titulo es muy largo lo corto para que no se salga de la tarjeta
                    val tituloCorto = if (titulo.length > 25)
                        titulo.substring(0, 22) + "..."
                    else
                        titulo
                    lblLibroTop.text = tituloCorto
                } else {
                    lblLibroTop.text = "Sin datos"
                }

                conn.close()
            } catch (e: Exception) {
                println("Error cargando estadísticas: ${e.message}")
            }
        }
    }

    // cargo el grafico de tarta para ver cuantos libros hay libres vs prestados
    private fun cargarGraficaDisponibilidad() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                val sql = """
                SELECT estado, COUNT(*) as cantidad 
                FROM ejemplares 
                GROUP BY estado
            """
                val rs = conn.createStatement().executeQuery(sql)

                var disponibles = 0
                var prestados = 0

                while (rs.next()) {
                    val estado = rs.getString("estado")
                    val cantidad = rs.getInt("cantidad")

                    when (estado) {
                        "DISPONIBLE" -> disponibles = cantidad
                        "PRESTADO" -> prestados = cantidad
                    }
                }

                conn.close()

                val dataDisponibles = PieChart.Data("Disponibles ($disponibles)", disponibles.toDouble())
                val dataPrestados = PieChart.Data("Prestados ($prestados)", prestados.toDouble())

                pieChartDisponibilidad.data.addAll(dataDisponibles, dataPrestados)
                pieChartDisponibilidad.setLegendVisible(true)

            } catch (e: Exception) {
                println("Error cargando gráfica de disponibilidad: ${e.message}")
            }
        }
    }

    // cargo el grafico de barras con el top 5 de libros
    private fun cargarGraficaTop5() {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        if (conn != null) {
            try {
                val sql = """
                SELECT l.titulo, COUNT(p.id) as total_prestamos
                FROM prestamos p
                JOIN ejemplares e ON p.ejemplar_id = e.id
                JOIN libros l ON e.libro_id = l.id
                GROUP BY l.id, l.titulo
                ORDER BY total_prestamos DESC
                LIMIT 5
            """
                val rs = conn.createStatement().executeQuery(sql)

                val series = XYChart.Series<String, Number>()
                series.name = "Préstamos"

                while (rs.next()) {
                    val titulo = rs.getString("titulo")
                    val total = rs.getInt("total_prestamos")

                    // corto el titulo para que quepa bien debajo de la barra
                    val tituloCorto = if (titulo.length > 20)
                        titulo.substring(0, 17) + "..."
                    else
                        titulo

                    series.data.add(XYChart.Data(tituloCorto, total))
                }

                conn.close()

                barChartTop5.data.add(series)
                barChartTop5.setLegendVisible(false)

                // aplico los estilos css manualmente para que las barras sean del color corporativo
                barChartTop5.applyCss()
                barChartTop5.layout()

                // uso runLater para asegurarme de que la grafica ya se pinto antes de cambiar el color
                javafx.application.Platform.runLater {
                    val allBars = barChartTop5.lookupAll(".chart-bar")
                    allBars.forEach { bar ->
                        // color azul oscuro #26547C
                        bar.style = "-fx-bar-fill: rgba(38, 84, 124, 1);"
                    }
                }

            } catch (e: Exception) {
                println("Error cargando gráfica top 5: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}