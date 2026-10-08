# MEMORY.md — App de Notas

Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte.

## Estado actual
- Existen los 5 módulos (`:app`, `:presentation`, `:domain`, `:data`, `:common`) con Hilt, Room (v2, esquemas en `data/schemas`) y Navigation Compose.
- Pantalla **Mis notas** (`presentation/notes/list`) implementada desde Figma (`JyeMmyDAEO1Y52QnwKwgGH`, nodo `4:1583`).
- **Editor** (`presentation/notes/editor`, Figma `4:1656`): crear, editar (solo texto plano), fijar y eliminar.
- **Categorías** Trabajo / Personal: se eligen en el editor (sección "Categoría", Figma `14:4`) y filtran el listado.
- Pendiente en Figma: selector de color (`4:1710`), Ajustes (`4:1794`).

## Decisiones (y por qué)
- Clean Architecture + MVI con 5 módulos: separar responsabilidades y probar cada capa.
- `Note` incluye `color` (`NoteColor`) e `isPinned`: el diseño los muestra; aprobado como parte de v1.
- Búsqueda **solo visual** todavía. Filtros Todas/Trabajo/Personal funcionales (filtran en `GetNotesUseCase`).
- Categoría **opcional** (`Note.category` nula = sin categoría; tocar la elegida la quita). Notas previas a v2 quedan sin categoría. Una nota nueva hereda el filtro activo del listado (arg `category` del editor).
- Room v1→v2 con `AutoMigration` (columna `category` TEXT nula). `room-testing` aprobado para `MigrationTest`.
- Orden fijo en `GetNotesUseCase`: fijadas primero, luego última edición desc. El ícono de orden no hace nada.
- Tema **solo oscuro** (el diseño no define claro); sin dynamic color. Tokens en `NotesTheme` (spacing, sizes, radii, noteColors).
- Fuente Inter en `res/font`; íconos Lucide de Figma como VectorDrawable en `presentation/res/drawable`.
- Datos semilla (6 notas del diseño) con `SeedNotesCallback` al crear la base.
- Fechas relativas con `NoteDateFormatter` (java.util, minSdk 24 sin desugaring).
- Navegación con rutas string (sin plugin de serialization para no sumar dependencias). Editor: `notes/editor?noteId=` (0 = nueva).
- Editor: autoguardado 500 ms tras dejar de escribir y al salir; nota nueva vacía no se crea (`SaveNoteUseCase` → `EmptyNote`). Vaciar una nota existente conserva su última versión.
- Editor: barra inferior, etiqueta de categoría y Compartir ocultos hasta tener esas funciones. Nota nueva = color AQUA.
- Eliminar: menú ⋮ + diálogo de confirmación (sin deshacer).
- `TimeProvider` en `:common` para fijar la fecha de edición en pruebas.

## Aprendizajes y errores a evitar
- No hay `java` en PATH: usar `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- AGP 9 trae Kotlin integrado: los módulos Android no aplican `kotlin-android`; los JVM puros usan `kotlin-jvm`.
- Pruebas JVM que crean `SQLiteException` necesitan `unitTests.isReturnDefaultValues = true` (`:data`).
- Pruebas de UI de campos de texto: usar un estado que se actualice con los intents; con estado fijo el campo reenvía el valor viejo al perder el foco.
- Pruebas: las escribe el agente `test-writer` (regla en `working-method.md`).
- `LazyVerticalStaggeredGrid` reparte por columna más corta: el orden visual difiere del mock de Figma (columnas fijas).

## Próximos pasos
- Selector de color de nota (Figma `4:1710`) y pantalla de Ajustes (`4:1794`).
- Prueba de flujo completo con `HiltTestRunner` + `@TestInstallIn` (aún no existe).
