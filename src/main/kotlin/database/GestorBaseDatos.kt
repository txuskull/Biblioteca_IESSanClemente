package database

import java.sql.DriverManager

class GestorBaseDatos {

    private val url = "jdbc:sqlite:biblioteca.db"

    fun getConexion(): java.sql.Connection? {
        var conn: java.sql.Connection? = null
        try {
            conn = DriverManager.getConnection(url)
        } catch (e: Exception) {
            println("❌ Error de conexión: " + e.message)
        }
        return conn
    }

    fun generarEjemplaresFaltantes() {
        val conn = getConexion()
        if (conn != null) {
            try {
                // 1. Buscar libros que NO tengan ejemplares
                val sql = """
                SELECT l.id, l.tipo_publicacion 
                FROM libros l 
                WHERE NOT EXISTS (SELECT 1 FROM ejemplares e WHERE e.libro_id = l.id)
            """
                val rs = conn.createStatement().executeQuery(sql)

                val librosParaRellenar = mutableListOf<Pair<Int, String>>()
                while (rs.next()) {
                    librosParaRellenar.add(Pair(rs.getInt("id"), rs.getString("tipo_publicacion")))
                }

                // 2. Por cada libro sin copias, creamos según su tipo
                val sqlInsert = "INSERT INTO ejemplares (libro_id, codigo_ejemplar, fecha_adquisicion, estado) VALUES (?, ?, ?, 'DISPONIBLE')"
                val ps = conn.prepareStatement(sqlInsert)

                var cont = 0
                for ((idLibro, tipo) in librosParaRellenar) {
                    // LIBROS: 3 ejemplares
                    // REVISTAS: 1 ejemplar
                    val numEjemplares = if (tipo == "LIBRO") 3 else 1

                    for (i in 1..numEjemplares) {
                        ps.setInt(1, idLibro)
                        ps.setString(2, "AUTO-$idLibro-$i")
                        ps.setString(3, java.time.LocalDate.now().toString())
                        ps.addBatch()
                        cont++
                    }
                }

                if (cont > 0) {
                    ps.executeBatch()
                    println("✅ Se han generado automáticamente $cont ejemplares nuevos para el inventario.")
                } else {
                    println("ℹ️ Todos los libros ya tienen ejemplares asignados.")
                }

                conn.close()
            } catch (e: Exception) {
                println("Error generando ejemplares: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ESTA FUNCION SOLO SE LLAMARÁ UNA VEZ AL PRINCIPIO
    fun iniciarSistema() {
        val sqlUsers = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dni TEXT UNIQUE,
                nombre TEXT,
                password TEXT,
                tipo TEXT,
                email TEXT,
                sancionado_hasta TEXT DEFAULT NULL
            );
        """

        // TABLA LIBROS ACTUALIZADA SEGUN PDF (página 2)
        val sqlLibros = """
            CREATE TABLE IF NOT EXISTS libros (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                isbn TEXT UNIQUE,
                titulo TEXT,
                tipo_publicacion TEXT,
                
                -- Campos comunes para LIBROS y REVISTAS
                temas TEXT,
                editorial TEXT,
                editorial_direccion TEXT,
                editorial_telefono TEXT,
                idioma TEXT DEFAULT 'Español',
                modulos_relacionados TEXT,
                ciclos_relacionados TEXT,
                
                -- Campos SOLO para LIBROS
                autor TEXT,
                nacionalidad_autor TEXT,
                edicion TEXT,
                fecha_publicacion TEXT,
                
                -- Campos SOLO para REVISTAS
                periodicidad TEXT
            );
        """

        val sqlEjemplares = """
            CREATE TABLE IF NOT EXISTS ejemplares (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                libro_id INTEGER,
                codigo_ejemplar TEXT,
                fecha_adquisicion TEXT,
                estado TEXT DEFAULT 'DISPONIBLE',
                FOREIGN KEY(libro_id) REFERENCES libros(id) ON DELETE CASCADE
            );
        """

        val sqlPrestamos = """
            CREATE TABLE IF NOT EXISTS prestamos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER,
                ejemplar_id INTEGER,
                fecha_prestamo TEXT,
                fecha_devolucion_prevista TEXT,
                fecha_devolucion_real TEXT DEFAULT NULL,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id),
                FOREIGN KEY(ejemplar_id) REFERENCES ejemplares(id)
            );
        """

        val sqlSanciones = """
            CREATE TABLE IF NOT EXISTS sanciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER,
                motivo TEXT,
                fecha_inicio TEXT,
                fecha_fin TEXT,
                dias_sancion INTEGER,
                estado TEXT DEFAULT 'ACTIVA',
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id)
            );
        """

        try {
            val conn = DriverManager.getConnection(url)
            val stmt = conn.createStatement()
            stmt.execute(sqlUsers)
            stmt.execute(sqlLibros)
            stmt.execute(sqlEjemplares)
            stmt.execute(sqlPrestamos)
            stmt.execute(sqlSanciones)

            // Crear admin
            stmt.execute("""
                INSERT OR IGNORE INTO usuarios (dni, nombre, password, tipo, email) 
                VALUES ('admin', 'Administrador Principal', '1234', 'CONSERJE', 'admin@ies.es');
            """)

            // Insertar usuarios de ejemplo (ESTUDIANTES, PROFESORES, CONSERJE)
            stmt.execute("""
                INSERT OR IGNORE INTO usuarios (dni, nombre, tipo, email) VALUES
                ('12345678A', 'Jesus Lopez Gonzalez', 'ESTUDIANTE', 'jesus.lopez@sanclemente.com'),
                ('11111111C', 'Pedro Sanchez Ruiz', 'ESTUDIANTE', 'pedro.sanchez@sanclemente.com'),
                ('22222222D', 'Ana Martinez Lopez', 'ESTUDIANTE', 'ana.martinez@sanclemente.com'),
                ('33333333E', 'Carlos Rodriguez Vega', 'ESTUDIANTE', 'carlos.rodriguez@sanclemente.com'),
                ('44444444P', 'Lozana Docente', 'PROFESOR', 'lozana.profe@sanclemente.com'),
                ('55555555P', 'Cayetano Docente', 'PROFESOR', 'cayetano.profe@sanclemente.com'),
                ('66666666P', 'Josefa Docente', 'PROFESOR', 'josefa.profe@sanclemente.com'),
                ('77777777X', 'Luis Conserje', 'CONSERJE', 'luis.conserje@sanclemente.com');
            """)

            // Insertar LIBROS de ejemplo (tipo_publicacion = 'LIBRO')
            stmt.execute("""
                INSERT OR IGNORE INTO libros 
                (isbn, titulo, tipo_publicacion, autor, nacionalidad_autor, editorial, idioma, edicion, temas, modulos_relacionados, ciclos_relacionados) 
                VALUES
                ('978-84-123-4567-0', 'Don Quijote de la Mancha', 'LIBRO', 'Miguel de Cervantes', 'Español', 'Alfaguara', 'Español', '1ª Edicion', 'Literatura Clásica', 'Lengua y Literatura', 'Todos'),
                ('978-84-456-7890-1', 'Kotlin for Beginners', 'LIBRO', 'JetBrains Team', 'Internacional', 'OReilly', 'Inglés', '1ª Edicion', 'Programación', 'Programación', 'DAM, DAW'),
                ('978-84-789-0123-2', 'Principios de Diseño de Interfaces', 'LIBRO', 'IES San Clemente', 'Español', 'Autoedicion', 'Español', '1ª Edicion', 'Desarrollo de Interfaces', 'DI', 'DAM'),
                ('978-84-234-5678-3', 'Clean Code', 'LIBRO', 'Robert C. Martin', 'Estadounidense', 'Prentice Hall', 'Inglés', '1ª Edicion', 'Buenas Prácticas', 'Programación', 'DAM, DAW'),
                ('978-84-567-8901-4', 'Cien Años de Soledad', 'LIBRO', 'Gabriel Garcia Marquez', 'Colombiano', 'Sudamericana', 'Español', '1ª Edicion', 'Literatura', 'Lengua', 'Todos');
            """)

            // Insertar REVISTAS de ejemplo (tipo_publicacion = 'REVISTA')
            stmt.execute("""
                INSERT OR IGNORE INTO libros 
                (isbn, titulo, tipo_publicacion, editorial, idioma, periodicidad, temas, modulos_relacionados, ciclos_relacionados) 
                VALUES
                ('REV-001', 'Clean Code Magazine', 'REVISTA', 'Tech Publishers', 'Inglés', 'Mensual', 'Programación', 'Programación', 'DAM, DAW'),
                ('REV-002', 'National Geographic España', 'REVISTA', 'RBA', 'Español', 'Mensual', 'Ciencia y Naturaleza', 'Ciencias', 'Todos'),
                ('REV-003', 'Linux Magazine', 'REVISTA', 'Linux New Media', 'Inglés', 'Mensual', 'Sistemas Operativos', 'SXE', 'ASIR, DAM');
            """)

            conn.close()
            println("✅ Sistema de Base de Datos inicializado correctamente.")
        } catch (e: Exception) {
            println("Error iniciando sistema: " + e.message)
        }
    }
}