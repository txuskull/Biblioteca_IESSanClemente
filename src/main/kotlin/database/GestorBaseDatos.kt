package database

import java.sql.DriverManager

class GestorBaseDatos {

    // la ruta donde se guarda mi base de datos es un archivo local
    private val url = "jdbc:sqlite:biblioteca.db"

    // funcion basica para conectar, la uso en todas las consultas
    fun getConexion(): java.sql.Connection? {
        var conn: java.sql.Connection? = null
        try {
            conn = DriverManager.getConnection(url)
        } catch (e: Exception) {
            println("❌ Error de conexión: " + e.message)
        }
        return conn
    }

    // rellenar la base de datos automaticamente
    // si meto un libro nuevo sin copias, esto le crea 3 ejemplares solos
    fun generarEjemplaresFaltantes() {
        val conn = getConexion()
        if (conn != null) {
            try {
                // 1. busco si hay algun libro que no tenga ningun ejemplar fisico registrado
                val sql = """
                SELECT l.id, l.tipo_publicacion 
                FROM libros l 
                WHERE NOT EXISTS (SELECT 1 FROM ejemplares e WHERE e.libro_id = l.id)
            """
                val rs = conn.createStatement().executeQuery(sql)

                // me guardo la lista de libros vacios para rellenarlos
                val librosParaRellenar = mutableListOf<Pair<Int, String>>()
                while (rs.next()) {
                    librosParaRellenar.add(Pair(rs.getInt("id"), rs.getString("tipo_publicacion")))
                }

                // 2. preparo la sentencia para crear las copias fisicas
                val sqlInsert = """
                INSERT INTO ejemplares (libro_id, codigo_ejemplar, numero_revista, fecha_adquisicion, estado) 
                VALUES (?, ?, ?, ?, 'DISPONIBLE')
            """
                val ps = conn.prepareStatement(sqlInsert)

                var cont = 0
                for ((idLibro, tipo) in librosParaRellenar) {
                    if (tipo == "LIBRO") {
                        // si es un libro normal, creo 3 copias inventadas
                        for (i in 1..3) {
                            ps.setInt(1, idLibro)
                            ps.setString(2, "AUTO-$idLibro-$i") // codigo tipo AUTO-1-1
                            ps.setNull(3, java.sql.Types.INTEGER) // los libros no tienen numero de revista
                            ps.setString(4, java.time.LocalDate.now().toString())
                            ps.addBatch()
                            cont++
                        }
                    } else {
                        // si es una revista, solo creo 1 copia del numero 1
                        ps.setInt(1, idLibro)
                        ps.setString(2, "REV-$idLibro-1")
                        ps.setInt(3, 1) // numero de la revista
                        ps.setString(4, java.time.LocalDate.now().toString())
                        ps.addBatch()
                        cont++
                    }
                }

                // si he creado algo nuevo, ejecuto todos los inserts de golpe
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

    // crear todas las tablas la primera vez
    fun iniciarSistema() {
        // tabla de usuarios con un campo para diferenciar profe, alumno o conserje
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

        // tabla de libros
        val sqlLibros = """
            CREATE TABLE IF NOT EXISTS libros (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                isbn TEXT UNIQUE,
                titulo TEXT,
                tipo_publicacion TEXT,
                
                -- Campos comunes
                temas TEXT,
                editorial TEXT,
                editorial_direccion TEXT,
                editorial_telefono TEXT,
                idioma TEXT DEFAULT 'Español',
                modulos_relacionados TEXT,
                ciclos_relacionados TEXT,
                
                -- Campos solo de LIBROS
                autor TEXT,
                nacionalidad_autor TEXT,
                edicion TEXT,
                fecha_publicacion TEXT,
                
                -- Campos solo de REVISTAS
                periodicidad TEXT
            );
        """

        // aqui guardo cada copia fisica de los libros
        val sqlEjemplares = """
            CREATE TABLE IF NOT EXISTS ejemplares (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                libro_id INTEGER,
                codigo_ejemplar TEXT,
                numero_revista INTEGER DEFAULT NULL,
                fecha_adquisicion TEXT,
                estado TEXT DEFAULT 'DISPONIBLE',
                FOREIGN KEY(libro_id) REFERENCES libros(id) ON DELETE CASCADE
            );
        """

        // registro historico de prestamos
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

        // tabla para sanciones de los alumnos que se retrasan
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

            // ejecuto la creacion de tablas
            stmt.execute(sqlUsers)
            stmt.execute(sqlLibros)
            stmt.execute(sqlEjemplares)
            stmt.execute(sqlPrestamos)
            stmt.execute(sqlSanciones)

            // creo al super admin por defecto para poder entrar
            stmt.execute("""
                INSERT OR IGNORE INTO usuarios (dni, nombre, password, tipo, email) 
                VALUES ('admin', 'Administrador Principal', '1234', 'CONSERJE', 'admin@ies.es');
            """)

            // ==============================================================================
            // DATOS DE PRUEBA MASIVOS (USUARIOS) - Total: 20+
            // ==============================================================================
            stmt.execute("""
                INSERT OR IGNORE INTO usuarios (dni, nombre, tipo, email) VALUES
                -- Profesores del departamento de Informatica
                ('11111111A', 'Ana Garcia Perez', 'PROFESOR', 'ana.garcia@sanclemente.net'),
                ('22222222B', 'Carlos Rodriguez Lopez', 'PROFESOR', 'carlos.rodriguez@sanclemente.net'),
                ('33333333C', 'Maria Martinez Ruiz', 'PROFESOR', 'maria.martinez@sanclemente.net'),
                ('44444444D', 'David Fernandez Gomez', 'PROFESOR', 'david.fernandez@sanclemente.net'),
                ('55555555E', 'Lucia Gonzalez Diaz', 'PROFESOR', 'lucia.gonzalez@sanclemente.net'),

                -- Alumnos de DAM (Desarrollo de Aplicaciones Multiplataforma)
                ('66666666F', 'Pablo Sanchez Martin', 'ESTUDIANTE', 'pablo.sanchez@sanclemente.net'),
                ('77777777G', 'Laura Lopez Perez', 'ESTUDIANTE', 'laura.lopez@sanclemente.net'),
                ('88888888H', 'Javier Martin Garcia', 'ESTUDIANTE', 'javier.martin@sanclemente.net'),
                ('99999999I', 'Sara Gomez Rodriguez', 'ESTUDIANTE', 'sara.gomez@sanclemente.net'),
                ('10101010J', 'Daniel Ruiz Fernandez', 'ESTUDIANTE', 'daniel.ruiz@sanclemente.net'),
                ('12121212K', 'Marta Diaz Gonzalez', 'ESTUDIANTE', 'marta.diaz@sanclemente.net'),
                ('13131313L', 'Alejandro Perez Sanchez', 'ESTUDIANTE', 'alejandro.perez@sanclemente.net'),
                ('24242424W', 'Nerea Castro Varela', 'ESTUDIANTE', 'nerea.castro@sanclemente.net'),
                ('25252525X', 'Hugo Dominguez Blanco', 'ESTUDIANTE', 'hugo.dominguez@sanclemente.net'),
                ('26262626Y', 'Alba Pereira Sanchez', 'ESTUDIANTE', 'alba.pereira@sanclemente.net'),
                ('27272727Z', 'Mario Alvarez Lago', 'ESTUDIANTE', 'mario.alvarez@sanclemente.net'),
                ('28282828A', 'Noa Garcia Seara', 'ESTUDIANTE', 'noa.garcia@sanclemente.net'),
                ('29292929B', 'Izan Romero Iglesias', 'ESTUDIANTE', 'izan.romero@sanclemente.net'),
                ('30303030C', 'Carla Soto Martinez', 'ESTUDIANTE', 'carla.soto@sanclemente.net'),
                ('31313131D', 'Enzo Vidal Pardo', 'ESTUDIANTE', 'enzo.vidal@sanclemente.net'),
                ('32323232E', 'Vera Mendez Lopez', 'ESTUDIANTE', 'vera.mendez@sanclemente.net'),
                ('34343434F', 'Gael Blanco Fernandez', 'ESTUDIANTE', 'gael.blanco@sanclemente.net'),
                
                -- Alumnos de DAW (Desarrollo de Aplicaciones Web)
                ('14141414M', 'Sofia Rodriguez Martin', 'ESTUDIANTE', 'sofia.rodriguez@sanclemente.net'),
                ('15151515N', 'Diego Garcia Lopez', 'ESTUDIANTE', 'diego.garcia@sanclemente.net'),
                ('16161616O', 'Paula Martinez Gomez', 'ESTUDIANTE', 'paula.martinez@sanclemente.net'),
                ('17171717P', 'Adrian Fernandez Ruiz', 'ESTUDIANTE', 'adrian.fernandez@sanclemente.net'),
                
                -- Alumnos de ASIR (Administracion de Sistemas)
                ('18181818Q', 'Claudia Gonzalez Diaz', 'ESTUDIANTE', 'claudia.gonzalez@sanclemente.net'),
                ('19191919R', 'Manuel Sanchez Perez', 'ESTUDIANTE', 'manuel.sanchez@sanclemente.net'),
                ('20202020S', 'Irene Lopez Martin', 'ESTUDIANTE', 'irene.lopez@sanclemente.net'),
                ('21212121T', 'Ruben Martin Garcia', 'ESTUDIANTE', 'ruben.martin@sanclemente.net'),
                
                -- Conserjes adicionales
                ('23232323V', 'Antonio Conserje Tarde', 'CONSERJE', 'antonio.conserje@sanclemente.net');
            """)

            // ==============================================================================
            // DATOS DE PRUEBA MASIVOS (LIBROS DE INFORMATICA) - Total: 30+
            // ==============================================================================
            // He arreglado el punto y coma que habia a mitad de la lista
            stmt.execute("""
                INSERT OR IGNORE INTO libros 
                (isbn, titulo, tipo_publicacion, autor, editorial, idioma, temas, modulos_relacionados, ciclos_relacionados) 
                VALUES
                -- PROGRAMACION Y DESARROLLO
                ('978-0132350884', 'Clean Code', 'LIBRO', 'Robert C. Martin', 'Prentice Hall', 'Inglés', 'Programación, Buenas Prácticas', 'Programación', 'DAM, DAW'),
                ('978-0201633610', 'Design Patterns', 'LIBRO', 'Erich Gamma', 'Addison-Wesley', 'Inglés', 'Patrones, Arquitectura', 'Programación', 'DAM, DAW'),
                ('978-1617293290', 'Kotlin in Action', 'LIBRO', 'Dmitry Jemerov', 'Manning', 'Inglés', 'Kotlin, Android', 'PMDM', 'DAM'),
                ('978-8441542353', 'Java para Novatos', 'LIBRO', 'A. M. Vozmediano', 'Anaya', 'Español', 'Java, Fundamentos', 'Programación', 'DAM, DAW, ASIR'),
                ('978-0596007126', 'Head First Design Patterns', 'LIBRO', 'Eric Freeman', 'OReilly', 'Inglés', 'Patrones, Java', 'Programación', 'DAM'),
                ('978-0134685991', 'Effective Java', 'LIBRO', 'Joshua Bloch', 'Addison-Wesley', 'Inglés', 'Java Avanzado', 'Programación', 'DAM, DAW'),
                ('978-1492078008', 'Kubernetes: Up & Running', 'LIBRO', 'Brendan Burns', 'OReilly', 'Inglés', 'DevOps, Contenedores, Cloud', 'Sistemas', 'ASIR, DAM'),
                ('978-0134494166', 'Clean Architecture', 'LIBRO', 'Robert C. Martin', 'Prentice Hall', 'Inglés', 'Arquitectura, Buenas Prácticas', 'Programación', 'DAM, DAW'),
                ('978-0134757599', 'Refactoring (2nd Edition)', 'LIBRO', 'Martin Fowler', 'Addison-Wesley', 'Inglés', 'Refactorización, Código', 'Programación', 'DAM, DAW'),
                ('978-1617292231', 'Spring in Action', 'LIBRO', 'Craig Walls', 'Manning', 'Inglés', 'Java, Backend, Frameworks', 'Programación', 'DAW, DAM'),
                ('978-1492045529', 'Learning SQL', 'LIBRO', 'Alan Beaulieu', 'OReilly', 'Inglés', 'SQL, Consultas', 'Bases de Datos', 'DAM, DAW, ASIR'),
                ('978-0131103627', 'The C Programming Language', 'LIBRO', 'Brian W. Kernighan', 'Prentice Hall', 'Inglés', 'C, Fundamentos', 'Programación', 'DAM, ASIR'),
                ('978-0262033848', 'Introduction to Algorithms', 'LIBRO', 'Thomas H. Cormen', 'MIT Press', 'Inglés', 'Algoritmos, Estructuras', 'Programación', 'DAM, DAW'),
                ('978-1492077216', 'Building Microservices', 'LIBRO', 'Sam Newman', 'OReilly', 'Inglés', 'Microservicios, Arquitectura', 'Programación', 'DAM, DAW'),
                ('978-0131872486', 'Computer Networking: A Top-Down Approach', 'LIBRO', 'James F. Kurose', 'Pearson', 'Inglés', 'Redes, Protocolos', 'Redes Locales', 'ASIR, DAM'),
                ('978-1492056358', 'Designing Data-Intensive Applications (Spanish)', 'LIBRO', 'Martin Kleppmann', 'OReilly', 'Español', 'Arquitectura, Datos', 'Acceso a Datos', 'DAM'),
                
                -- BASES DE DATOS (Arreglado el punto y coma que cortaba aqui)
                ('978-8441536765', 'Fundamentos de Bases de Datos', 'LIBRO', 'Silberschatz', 'McGraw-Hill', 'Español', 'SQL, Diseño', 'Bases de Datos', 'DAM, DAW, ASIR'),
                ('978-8499640983', 'MySQL Avanzado', 'LIBRO', 'Varios', 'Ra-Ma', 'Español', 'MySQL, Admin', 'Bases de Datos', 'ASIR'),
                ('978-1449373320', 'Designing Data-Intensive Applications', 'LIBRO', 'Martin Kleppmann', 'OReilly', 'Inglés', 'Arquitectura, Datos', 'Acceso a Datos', 'DAM'),
                
                -- SISTEMAS Y REDES
                ('978-8499640884', 'Redes de Computadores', 'LIBRO', 'Tanenbaum', 'Ra-Ma', 'Español', 'Redes, Protocolos', 'Redes Locales', 'ASIR, SMR'),
                ('978-8441539742', 'Administración de Sistemas Linux', 'LIBRO', 'Tom Adelstein', 'Anaya', 'Español', 'Linux, Admin', 'ISO', 'ASIR, DAM'),
                ('978-0131429383', 'Sistemas Operativos Modernos', 'LIBRO', 'Tanenbaum', 'Prentice Hall', 'Español', 'SO, Procesos', 'Sistemas', 'DAM, ASIR'),
                ('978-8426724391', 'Windows Server 2019', 'LIBRO', 'Varios', 'Marcombo', 'Español', 'Windows, Server', 'ISO', 'ASIR'),
                
                -- DESARROLLO WEB
                ('978-1491950296', 'Programming JavaScript Applications', 'LIBRO', 'Eric Elliott', 'OReilly', 'Inglés', 'JS, Web', 'Desarrollo Web', 'DAW'),
                ('978-8441532149', 'HTML5 y CSS3', 'LIBRO', 'Juan Diego Gauchat', 'Marcombo', 'Español', 'Frontend, Web', 'Lenguajes de Marcas', 'DAM, DAW, ASIR'),
                ('978-1491904244', 'You Dont Know JS', 'LIBRO', 'Kyle Simpson', 'OReilly', 'Inglés', 'JavaScript Avanzado', 'Cliente', 'DAW'),
                ('978-1785281373', 'Mastering Python', 'LIBRO', 'Rick van Hattem', 'Packt', 'Inglés', 'Python, Backend', 'HLC', 'DAM, DAW'),

                -- INTERFACES Y UX
                ('978-8441528654', 'No me hagas pensar', 'LIBRO', 'Steve Krug', 'Anaya', 'Español', 'Usabilidad, UX', 'Diseño Interfaces', 'DAM'),
                ('978-0321965516', 'Don t Make Me Think', 'LIBRO', 'Steve Krug', 'New Riders', 'Inglés', 'Usabilidad, Web', 'DI', 'DAM'),
                ('978-1119575945', 'Refactoring UI', 'LIBRO', 'Adam Wathan', 'Indie', 'Inglés', 'Diseño, CSS', 'DI', 'DAM'),

                -- HARDWARE Y SEGURIDAD
                ('978-8499645322', 'Seguridad Informática Hacking Ético', 'LIBRO', 'Varios', 'Ra-Ma', 'Español', 'Seguridad, Hacking', 'Seguridad', 'ASIR, DAM'),
                ('978-8441536772', 'Arquitectura de Computadores', 'LIBRO', 'Patterson', 'Reverte', 'Español', 'Hardware, CPU', 'Fundamentos', 'ASIR'),
                ('978-1593272203', 'Hacking: The Art of Exploitation', 'LIBRO', 'Jon Erickson', 'No Starch Press', 'Inglés', 'C, Hacking', 'Seguridad', 'ASIR'),

                -- EMPRESA E INICIATIVA
                ('978-8416138661', 'El libro negro del emprendedor', 'LIBRO', 'Fernando Trias', 'Planeta', 'Español', 'Emprendimiento', 'EIE', 'Todos'),
                ('978-0670921607', 'Lean Startup', 'LIBRO', 'Eric Ries', 'Crown', 'Inglés', 'Negocio, Agile', 'EIE', 'Todos');
            """)

            // ==============================================================================
            // DATOS DE PRUEBA (REVISTAS TECNICAS)
            // ==============================================================================
            // He arreglado la coma que faltaba
            stmt.execute("""
                INSERT OR IGNORE INTO libros 
                (isbn, titulo, tipo_publicacion, editorial, idioma, periodicidad, temas, modulos_relacionados) 
                VALUES
                ('REV-001', 'Linux Magazine', 'REVISTA', 'Linux New Media', 'Inglés', 'Mensual', 'Sistemas, Open Source', 'Sistemas'),
                ('REV-002', 'Computer Hoy', 'REVISTA', 'Axel Springer', 'Español', 'Quincenal', 'Tecnología General', 'Todos'),
                ('REV-003', 'Wired', 'REVISTA', 'Condé Nast', 'Inglés', 'Mensual', 'Futuro, Tecnología', 'EIE'),
                ('REV-004', 'Personal Computer', 'REVISTA', 'Axel Springer', 'Español', 'Mensual', 'Hardware, Gadgets', 'Fundamentos'),
                ('REV-005', 'Hacker News', 'REVISTA', 'Y Combinator', 'Inglés', 'Semanal', 'Programación, Startups', 'Programación'),
                ('REV-006', 'ACM Queue', 'REVISTA', 'ACM', 'Inglés', 'Mensual', 'Arquitectura, Ingeniería de Software', 'Programación'),
                ('REV-007', 'IEEE Software', 'REVISTA', 'IEEE', 'Inglés', 'Bimestral', 'Software, Gestión, Calidad', 'DI'),
                ('REV-008', 'IEEE Security & Privacy', 'REVISTA', 'IEEE', 'Inglés', 'Bimestral', 'Ciberseguridad, Privacidad', 'Seguridad'),
                ('REV-009', 'Docker Weekly', 'REVISTA', 'Docker', 'Inglés', 'Semanal', 'Contenedores, DevOps', 'Sistemas'),
                ('REV-010', 'DevOps Monthly', 'REVISTA', 'DevOps Institute', 'Inglés', 'Mensual', 'CI/CD, Automatización', 'Sistemas'),
                ('REV-011', 'Android Weekly', 'REVISTA', 'Android Weekly', 'Inglés', 'Semanal', 'Android, Kotlin, Mobile', 'PMDM'),
                ('REV-012', 'Frontend Focus', 'REVISTA', 'Cooperpress', 'Inglés', 'Semanal', 'Frontend, UX, Web', 'Lenguajes de Marcas'),
                ('REV-013', 'DBA Monthly', 'REVISTA', 'TechPress', 'Español', 'Mensual', 'Bases de Datos, SQL, Rendimiento', 'Bases de Datos'),
                ('REV-014', 'Linux Journal (Digital)', 'REVISTA', 'Linux Journal', 'Inglés', 'Mensual', 'Linux, Open Source', 'Sistemas'),
                ('REV-015', 'Cybersecurity Review', 'REVISTA', 'Security Press', 'Español', 'Mensual', 'Seguridad, Amenazas, Buenas prácticas', 'Seguridad');
            """)

            conn.close()
            println("✅ Sistema de Base de Datos inicializado correctamente.")
        } catch (e: Exception) {
            println("Error iniciando sistema: " + e.message)
        }
    }
}