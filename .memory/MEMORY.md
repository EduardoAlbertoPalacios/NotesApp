# MEMORY.md — App de Notas

Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte.

## Estado actual
- Existen los 5 módulos (`:app`, `:presentation`, `:domain`, `:data`, `:common`) con Hilt, Room (v4, esquemas en `data/schemas`) y Navigation Compose.
- Pantalla **Mis notas** (`presentation/notes/list`) implementada desde Figma (`JyeMmyDAEO1Y52QnwKwgGH`, nodo `4:1583`).
- **Editor** (`presentation/notes/editor`, Figma `4:1656`): crear, editar, fijar y eliminar; contenido con negrita, subrayado y tachado.
- **Categorías** Trabajo / Personal: se eligen en el editor (sección "Categoría", Figma `14:4`) y filtran el listado.
- **Imágenes**: varias por nota, desde el botón Imagen de la barra de edición (PR #1, en `main`).
- Constitución del proyecto en `docs/constitution.md`; specs en `docs/specs/NNN-nombre/spec.md`.
- **Búsqueda** (spec 001, PR #2, en `main`). TalkBack se dio por bueno sin probar.
- **Formato de texto** (spec 002, rama `feat/text-formatting`): implementado y verificado en emulador; falta el commit.
- Pendiente en Figma: selector de color (`4:1710`), Ajustes (`4:1794`).

## Decisiones (y por qué)
- Clean Architecture + MVI con 5 módulos: separar responsabilidades y probar cada capa.
- `Note` incluye `color` (`NoteColor`) e `isPinned`: el diseño los muestra; aprobado como parte de v1.
- Búsqueda en memoria en `:domain` (`NoteTextMatcher` + `GetNotesUseCase(category, query)` → `FilteredNotes`): ignora tildes y mayúsculas pero «ñ» ≠ «n» (FTS/LIKE no lo permiten). Con texto ignora la categoría. El texto vive en `SavedStateHandle`. Filtros Todas/Trabajo/Personal funcionales (filtran en `GetNotesUseCase`).
- Categoría **opcional** (`Note.category` nula = sin categoría; tocar la elegida la quita). Notas previas a v2 quedan sin categoría. Una nota nueva hereda el filtro activo del listado (arg `category` del editor).
- Room con `AutoMigration`: v1→v2 `category`, v2→v3 `note_images`, v3→v4 `content_spans`. `room-testing` aprobado para `MigrationTest`.
- Orden fijo en `GetNotesUseCase`: fijadas primero, luego última edición desc. El ícono de orden no hace nada.
- Tema **solo oscuro** (el diseño no define claro); sin dynamic color. Tokens en `NotesTheme` (spacing, sizes, radii, noteColors).
- Fuente Inter en `res/font`; íconos Lucide de Figma como VectorDrawable en `presentation/res/drawable`.
- Datos semilla (6 notas del diseño) con `SeedNotesCallback` al crear la base.
- Fechas relativas con `NoteDateFormatter` (java.util, minSdk 24 sin desugaring); `TimeProvider` en `:common` fija la hora en pruebas.
- Navegación con rutas string (sin plugin de serialization para no sumar dependencias). Editor: `notes/editor?noteId=` (0 = nueva).
- Editor: autoguardado 500 ms tras dejar de escribir y al salir; nota nueva vacía no se crea (`SaveNoteUseCase` → `EmptyNote`). Vaciar una nota existente conserva su última versión.
- Editor: barra inferior, etiqueta de categoría y Compartir ocultos hasta tener esas funciones. Nota nueva = color AQUA.
- Eliminar: menú ⋮ + diálogo de confirmación (sin deshacer).
- Imágenes: selector de fotos del sistema (sin permisos); se copian a `filesDir/note_images` porque el acceso a la original es temporal. Tabla `note_images` (FK con CASCADE). Borrar nota/imagen borra también el archivo. Agregar imagen actualiza la fecha de edición.
- Barra de edición del Figma visible completa; funcionan «Aa» (formato) y "Agregar imagen", el resto deshabilitado.
- Formato: el contenido sigue en texto plano y el formato va aparte como tramos `FormatSpan` en la columna `content_spans` (codificación propia `start,end,F;…`, sin dependencias). Reglas en `NoteFormatting` (`:domain`); el editor pinta con `VisualTransformation`. Los formatos activados sin escribir no se guardan ni cambian la fecha.
- Coil 3.3.0: última versión compilada con Kotlin 2.2 (3.5+ exige Kotlin 2.4). Subir Kotlin antes de actualizar Coil.

## Aprendizajes y errores a evitar
- No hay `java` en PATH: usar `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- AGP 9 trae Kotlin integrado: los módulos Android no aplican `kotlin-android`; los JVM puros usan `kotlin-jvm`.
- Pruebas JVM que crean `SQLiteException` necesitan `unitTests.isReturnDefaultValues = true` (`:data`).
- Pruebas de UI de campos de texto: usar un estado que se actualice con los intents; con estado fijo el campo reenvía el valor viejo al perder el foco.
- Scripts de reemplazo: verificar siempre que el texto se encontró (un reemplazo silencioso dejó sin guardar la categoría).
- Pruebas: las escribe el agente `test-writer` (regla en `working-method.md`).
- `LazyVerticalStaggeredGrid` reparte por columna más corta: el orden visual difiere del mock de Figma (columnas fijas).
- Muerte de proceso en emulador: `am kill` (repetido hasta que libere el proceso) o «No conservar actividades»; un `kill` por señal descarta la tarea y no sirve.
- `connectedDebugAndroidTest` desinstala la app del emulador al terminar: sus datos vuelven a las notas semilla.
- Lint: `NoteEditorRoute.kt:45` (`LocalContextGetResourceValueCall`) es un error preexistente que hace fallar `./gradlew lint`.

## Próximos pasos
- Corregir el error de lint preexistente del editor.
- Selector de color de nota (Figma `4:1710`) y pantalla de Ajustes (`4:1794`).
- Prueba de flujo completo con `HiltTestRunner` + `@TestInstallIn` (aún no existe).
