# MEMORY.md — App de Notas

Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte.

## Estado actual
- Existen los 5 módulos (`:app`, `:presentation`, `:domain`, `:data`, `:common`) con Hilt, Room (v6, esquemas en `data/schemas`) y Navigation Compose. Constitución en `docs/constitution.md`; specs en `docs/specs/NNN-nombre/`.
- Pantalla **Mis notas** (`presentation/notes/list`) implementada desde Figma (`JyeMmyDAEO1Y52QnwKwgGH`, nodo `4:1583`).
- **Editor** (`presentation/notes/editor`, Figma `4:1656`): crear, editar, fijar y eliminar; contenido con negrita, subrayado y tachado.
- **Categorías** Trabajo / Personal: se eligen en el editor (sección "Categoría", Figma `14:4`) y filtran el listado.
- **Imágenes**: varias por nota, desde el botón Imagen de la barra de edición (PR #1, en `main`).
- **Búsqueda** (spec 001, PR #2), **formato de texto** (002, PR #3), **color de la nota** (003, PR #4) y **lista de tareas** (004, PR #5), en `main`.
- **Deshacer** (spec 005, rama `feat/undo`): implementado y verificado en emulador; sin commit ni PR todavía.

## Decisiones (y por qué)
- Clean Architecture + MVI con 5 módulos: separar responsabilidades y probar cada capa.
- `Note` incluye `color` (`NoteColor`) e `isPinned`: el diseño los muestra; aprobado como parte de v1.
- Búsqueda en memoria en `:domain` (`NoteTextMatcher` + `GetNotesUseCase(category, query)` → `FilteredNotes`): ignora tildes y mayúsculas pero «ñ» ≠ «n» (FTS/LIKE no lo permiten). Con texto ignora la categoría. El texto vive en `SavedStateHandle`. Filtros Todas/Trabajo/Personal funcionales (filtran en `GetNotesUseCase`).
- Categoría **opcional** (`Note.category` nula = sin categoría; tocar la elegida la quita). Notas previas a v2 quedan sin categoría. Una nota nueva hereda el filtro activo del listado (arg `category` del editor).
- Room con `AutoMigration`: v1→v2 `category`, v2→v3 `note_images`, v3→v4 `content_spans`. Manuales en `local/migration/Migrations.kt`: v4→v5 `SAND`→`VANILLA`; v5→v6 `content_tasks` y conversión de «☐»/«☑» en tareas recolocando el formato. `room-testing` aprobado para `MigrationTest`.
- Tema **solo oscuro** (el diseño no define claro); sin dynamic color. Tokens en `NotesTheme` (spacing, sizes, radii, noteColors). Navegación con rutas string (sin plugin de serialization). Editor: `notes/editor?noteId=` (0 = nueva).
- Editor: autoguardado 500 ms tras dejar de escribir y al salir; nota nueva vacía no se crea (`SaveNoteUseCase` → `EmptyNote`). Vaciar una nota existente conserva su última versión.
- Editor: Compartir oculto hasta tener esa función. Nota nueva = color AQUA.
- Color: 8 valores de `NoteColor` en el orden de la paleta (`NONE` = «Sin color»). El amarillo guardado como `SAND` era «Vainilla»: se migró a `VANILLA` y `SAND` es ahora el beige «Arena». El selector (`NoteColorSheet`) guarda la opción en `pickerColor` hasta «Aplicar color»; aplicar el mismo color no guarda. El editor se pinta con `NoteSurfaceTheme` (tema anidado) y `AppColorsTheme` restaura el oscuro en barra, menús y botón de imagen. `Slate` oscurecido a `#3B4C50` para 4,5:1.
- Imágenes: selector de fotos del sistema (sin permisos); se copian a `filesDir/note_images` porque el acceso a la original es temporal. Tabla `note_images` (FK con CASCADE). Borrar nota/imagen borra también el archivo. Agregar imagen actualiza la fecha de edición.
- Barra de edición del Figma completa y funcional: «Aa», lista de tareas, imagen, deshacer y color.
- Tareas: líneas del contenido (texto plano) marcadas aparte como `ChecklistItem(line, isDone)` en `content_tasks` (`línea,estado;…`). Reglas en `NoteChecklist` (`:domain`); `onTextChanged` recibe el cursor para ubicar Intro/borrar/pegar. Editor: un solo campo; las casillas se dibujan encima con el `TextLayoutResult` y el texto deja hueco con `ParagraphStyle`. Tarjeta: casillas con `InlineTextContent`.
- ViewModels: excepción en `CLAUDE.md` para usar directamente los `object` de reglas puras de `:domain` (`NoteFormatting`, `NoteChecklist`, `NoteUndo`).
- Deshacer: fotos completas de la nota (`UndoSnapshot`) en un historial del ViewModel (máx. 100, se pierde al salir); reglas en `NoteUndo`. Un carácter escrito/borrado se agrupa; espacio, `\n`, 1 s de pausa o mover el cursor cierran el paso; lo demás es paso propio. Título como `TextFieldValue` para restaurar el cursor. Deshacer hasta el estado de apertura recupera la fecha con `RestoreNoteUseCase`. Foco por estado (`FocusRequest` + `FocusRequestHandled`) para que no se repita al girar.
- Formato: el contenido sigue en texto plano y el formato va aparte como tramos `FormatSpan` en la columna `content_spans` (codificación propia `start,end,F;…`, sin dependencias). Reglas en `NoteFormatting` (`:domain`); el editor pinta con `VisualTransformation`. Los formatos activados sin escribir no se guardan ni cambian la fecha.
- Coil 3.3.0: última versión compilada con Kotlin 2.2 (3.5+ exige Kotlin 2.4). Subir Kotlin antes de actualizar Coil.

## Aprendizajes y errores a evitar
- No hay `java` en PATH: usar `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`. AGP 9 trae Kotlin integrado: los módulos Android no aplican `kotlin-android`; los JVM puros usan `kotlin-jvm`.
- Pruebas JVM que crean `SQLiteException` necesitan `unitTests.isReturnDefaultValues = true` (`:data`).
- Pruebas de UI de campos de texto: usar un estado que se actualice con los intents; con estado fijo el campo reenvía el valor viejo al perder el foco.
- Scripts de reemplazo: verificar siempre que el texto se encontró (un reemplazo silencioso dejó sin guardar la categoría).
- `LazyVerticalStaggeredGrid` reparte por columna más corta: el orden visual difiere del mock de Figma (columnas fijas).
- `connectedDebugAndroidTest` desinstala la app del emulador al terminar: sus datos vuelven a las notas semilla.
- Probar migraciones de verdad: compilar `main` en un `git worktree` temporal, instalarlo desde cero y luego `adb install -r` la rama; leer la base con `run-as ... cat` y `sqlite3` local (el emulador no trae `sqlite3`).
- Lint: `NoteEditorRoute.kt:50` (`LocalContextGetResourceValueCall`) es un error preexistente que hace fallar `./gradlew lint`.
- Compose 1.10: un párrafo que termina en `\n` dibuja una línea vacía de más y cada párrafo recorta su altura (`Trim`). Para la sangría por línea: `\n`→`\u200B` en la transformación y `LineHeightStyle.Trim.None`.
- Semántica: un `semantics {}` junto a `toggleable` deja la etiqueta en un nodo aparte; usar `clearAndSetSemantics`. `appendInlineContent` no admite `alternateText` vacío.
- Al cambiar una `@Entity`, sube la versión de la base antes de compilar: si no, KSP reescribe el JSON del esquema publicado.
- Emulador: Atrás con el teclado cerrado sale del editor (y descarta el historial de deshacer). El emulador tiene teclado físico: Gboard solo muestra su barra flotante (tapa la izquierda), así que la escritura con teclado en pantalla no se puede probar ahí.

## Próximos pasos
- Corregir el error de lint preexistente del editor.
- Spec 005: commit y PR; comprobar en un móvil con teclado en pantalla que escribir se agrupa por palabra (confirmar una sugerencia es paso propio).
- Nota vaciada (a mano o deshaciendo) tras autoguardarse: `leave()` mira la última versión guardada y no la borra; vuelve a Mis notas con su texto anterior. Preexistente; candidato a spec.
- Limitación conocida: con el contenido vacío, al crear la primera tarea el cursor queda bajo la casilla hasta escribir.
- Pantalla de Ajustes (`4:1794`).
- Prueba de flujo completo con `HiltTestRunner` + `@TestInstallIn` (aún no existe).
