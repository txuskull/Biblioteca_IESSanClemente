package view

import database.GestorBaseDatos
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class Main : Application() {
    override fun start(stage: Stage) {
        // INICIALIZAR BASE DE DATOS
        val gestor = GestorBaseDatos()
        gestor.iniciarSistema()

        //gestor.generarEjemplaresFaltantes()

        // SALTAR LOGIN - IR DIRECTO AL DASHBOARD
        val loader = FXMLLoader(javaClass.getResource("/fxml/dashboard.fxml"))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 1200.0, 700.0)
        stage.isMaximized = true
        stage.title = "Panel de Gestión - Biblioteca IES San Clemente"
        stage.show()
    }
}

fun main() {
    Application.launch(Main::class.java)
}

//package view
//
//import database.GestorBaseDatos
//import javafx.application.Application
//import javafx.fxml.FXMLLoader
//import javafx.scene.Scene
//import javafx.stage.Stage
//
//class Main : Application() {
//    override fun start(stage: Stage) {
//        // INICIALIZAR BASE DE DATOS
//        val gestor = GestorBaseDatos()
//        gestor.iniciarSistema()
//        gestor.generarEjemplaresFaltantes()
//
//        // DEBUG: Verificar que encuentra el archivo
//        val fxmlUrl = javaClass.getResource("/fxml/login.fxml")
//        println("DEBUG: FXML URL = $fxmlUrl")
//
//        if (fxmlUrl == null) {
//            println("❌ ERROR: No se encuentra el archivo login.fxml en /fxml/")
//            println("Verifica que el archivo esté en: src/main/resources/fxml/login.fxml")
//            return
//        }
//
//        val loader = FXMLLoader(fxmlUrl)
//        val root = loader.load<Any>()
//
//        stage.scene = Scene(root as javafx.scene.Parent, 900.0, 600.0)
//        stage.title = "Biblioteca IES San Clemente - Acceso Seguro"
//        stage.show()
//    }
//}
//
//fun main() {
//    Application.launch(Main::class.java)
//}
