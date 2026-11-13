# Plan 011 — Exportar notas

> **Estado:** implementado · **Fecha:** 2026-11-12 · **Spec:** [spec.md](spec.md) (aprobada) · **Diseño:** fila «Exportar notas» de Ajustes (Figma, spec 006); la pantalla de selección no tiene diseño y usa los componentes y el tema existentes

Plan técnico para implementar la spec 011. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

Una pantalla nueva, **Exportar notas**, lista las notas con casillas. Al tocar Exportar:

1. Se generan los PDF y el .zip en una carpeta temporal de la app, con un indicador de progreso.
2. Se abre el selector de documentos del sistema (`CreateDocument`) con el nombre propuesto.
3. Se copia el .zip al destino elegido y se vuelve a Ajustes con el aviso.

Los PDF se dibujan con `android.graphics.pdf.PdfDocument` y el .zip se escribe con `java.util.zip`, ambos del sistema. **No hace falta ninguna dependencia nueva.**

Las reglas puras (nombres de archivo y del .zip) viven en `:domain`. El dibujo de los PDF, el .zip y la escritura en el destino viven en `:data`, detrás de una interfaz de dominio.

No hay cambios en el esquema de Room ni en `:common`.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas: `PdfDocument`, `StaticLayout`, `ZipOutputStream`, `ExifInterface` del framework y `ActivityResultContracts.CreateDocument`, que ya viene con `activity-compose`. |
| 2. La spec manda | Cada parte cita sus RF. El mismo commit actualiza las specs 006 y 010 (sección 4.6) y ajusta la 011 por D1 y D3. |
| 3. Lógica fuera de la UI | Nombres, orden y qué se exporta se deciden en `:domain`. La pantalla solo pinta la selección y emite intents. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, de datos (instrumentadas, abriendo los PDF generados), del ViewModel y de UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | Exportar solo lee las notas [RF-24]. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:domain` | Paquete `notes/export`: `NoteExportNaming`, `NotesExporter`, `PreparedExport`, casos de uso. | RF-14, RF-15, RF-27 a RF-31 |
| `:data` | `PdfNotesExporter` (PDF, .zip, copia al destino), su binding en Hilt y `res/values/strings.xml` (D2). | RF-15, RF-23, RF-26, RF-32 a RF-45 |
| `:presentation` | Pantalla `notes/export` (MVI completo), ruta de navegación, fila de Ajustes habilitada y aviso al volver. | RF-1 a RF-13, RF-16 a RF-22, RF-25, RF-46, RF-47 |
| `:app` | Ninguno. | — |
| Documentación | Specs 006, 010 y 011, y `.memory/MEMORY.md`. | — |

## 4. Diseño por capa

### 4.1 `:domain` (paquete `notes.export`)

- **`NoteExportNaming`** (`object`, reglas puras):
  - `archiveName(epochMillis, timeZone)` → «Notas AAAA-MM-DD.zip» [RF-14].
  - `pdfNames(titles: List<String>, untitled: String)` → un nombre por nota, en el mismo orden [RF-27 a RF-31]:
    - título recortado, o `untitled` si está vacío;
    - `/ \ : * ? " < > |` → «_»;
    - como máximo 100 caracteres (sin partir un emoji: se cuenta por puntos de código);
    - repetidos sin distinguir mayúsculas → « (2)», « (3)», … en el orden de la lista;
    - se añade «.pdf».
- **`NotesExporter`** (interfaz que implementa `:data`):
  - `suspend fun prepare(notes: List<Note>): AppResult<PreparedExport, ExportError>`: genera el .zip temporal.
  - `suspend fun save(export: PreparedExport, destination: String): AppResult<Unit, ExportError>`: lo copia a la URI elegida; si falla, borra el destino [RF-23].
  - `suspend fun discard(export: PreparedExport)`: borra el temporal.
  - `suspend fun discardDestination(destination: String)`: borra un destino vacío (ver 4.3, muerte del proceso).
- **`PreparedExport(id: String, noteCount: Int, missingImageCount: Int)`**: `id` identifica el temporal; no es una ruta de Android.
- **`ExportError`**: `WriteFailed`, `NothingToExport`.
- **Casos de uso:** `PrepareNotesExportUseCase(notes)`, `SaveNotesExportUseCase(export, destination)`, `DiscardNotesExportUseCase(export)`, `DiscardExportDestinationUseCase(destination)` y `GetExportArchiveNameUseCase()`, que usa `TimeProvider`.
- La lista de notas sale de `GetNotesUseCase()` sin categoría ni búsqueda, que ya da el orden de Mis notas con «Todas» [RF-4]. La exportación recibe las notas que ya cargó la pantalla, así que no relee la base.

### 4.2 `:data` (paquete `export`)

**`PdfNotesExporter`** (`internal`, `@IoDispatcher`):

- **Temporal:** `cacheDir/exports/`. Cada `prepare` vacía la carpeta antes de empezar, así que nunca quedan restos de una exportación anterior.
- **.zip:** `ZipOutputStream` con entradas UTF-8, una por nota. Cada PDF se escribe directamente en su entrada, una nota cada vez, para no tener todo en memoria.
- **Página:** A4 a 72 ppp, 595 × 842 puntos, con márgenes de 48 puntos [RF-43]. Fondo blanco y texto negro, sin mirar el tema ni el color [RF-42].
- **Texto:** un `StaticLayout` con un `SpannableStringBuilder`:
  - título en negrita y más grande, o nada si no hay título [RF-32, RF-33];
  - categoría si existe, y la fecha «12 nov 2026, 10:30» (`d MMM yyyy` + hora corta del idioma, como `NoteDateFormatter`) [RF-34, RF-35];
  - contenido con `StyleSpan(BOLD)`, `UnderlineSpan` y `StrikethroughSpan` según sus `FormatSpan` [RF-36];
  - cada línea de tarea empieza por «☐ » o «☑ » [RF-37].
- **Paginado:** se recorren las líneas del layout y se abre una página nueva cuando la siguiente no cabe [RF-41].
- **Imágenes:** después del texto, en el orden de la nota [RF-38]:
  - se leen con `BitmapFactory` y `inSampleSize` hasta unos 1240 px de ancho, una cada vez, y se respeta la rotación EXIF con el `ExifInterface` del framework;
  - se escalan al ancho útil sin deformarse [RF-39]; si son más altas que el área útil, se reducen hasta que quepan [RF-40];
  - si no caben en lo que queda de página, van a la siguiente;
  - si una no existe o no se puede leer, se omite y se cuenta en `missingImageCount` [RF-44].
- **Copia al destino:** `contentResolver.openOutputStream(uri, "wt")`. Si hay un error de E/S o la corrutina se cancela, se borra el destino con `DocumentsContract.deleteDocument` [RF-23].
- **Excepciones:** solo `IOException` y `SecurityException`, que se convierten en `ExportError.WriteFailed`. Nunca `Exception` ni `CancellationException`.
- **Textos (D2):** «Sin título», «Trabajo» y «Personal» salen de `data/src/main/res/values/strings.xml`.

`RepositoryModule` añade `@Binds NotesExporter`.

### 4.3 `:presentation` (paquete `notes.export`)

| Clase | Contenido |
|---|---|
| `ExportNotesUiState` | `notes: List<ExportNoteItem(id, title)>`, `selectedIds: Set<Long>`, `isLoading`, `isExporting`; derivados `selectedCount`, `allSelected`, `canExport` [RF-3 a RF-12, RF-18, RF-19] |
| `ExportNotesIntent` | `NoteToggled(id)`, `SelectAllClicked`, `ClearSelectionClicked`, `ExportClicked`, `DestinationPicked(uri: String?)`, `BackClicked` |
| `ExportNotesEffect` | `OpenDocumentPicker(suggestedName)`, `Finished(exportedCount, missingImageCount)`, `ShowMessage(@StringRes)`, `NavigateBack` |
| `ExportNotesViewModel` | Observa `GetNotesUseCase`; al cambiar la lista quita de la selección las notas que ya no existen. `ExportClicked` → `isExporting`, prepara y emite `OpenDocumentPicker`. `DestinationPicked(null)` → descarta el temporal y vuelve a la selección [RF-16, RF-17]. Con URI → guarda y emite `Finished` o el error [RF-20 a RF-22] |
| `ExportNotesRoute` | `rememberLauncherForActivityResult(CreateDocument("application/zip"))`, efectos y `BackHandler` |
| `ExportNotesScreen` | Sin estado; ver abajo |

**La pantalla**, con los componentes de Ajustes:
- barra superior con Volver y el título «Exportar notas»;
- fila con «N seleccionadas» y el botón de texto «Seleccionar todas» o «Quitar selección» [RF-8 a RF-11];
- `LazyColumn` con `key = id`, filas con `Checkbox` y el título o «Sin título», y `toggleable` con su estado para el lector de pantalla [RF-5, RF-7, RF-46];
- botón «Exportar» abajo, deshabilitado sin selección [RF-12];
- mientras se exporta, el botón muestra un `CircularProgressIndicator` y las filas quedan deshabilitadas [RF-18, RF-19];
- sin notas, el aviso «No hay notas para exportar» [RF-3].

**Giro y muerte del proceso** [RF-25, RF-26]:
- el trabajo corre en `viewModelScope`, así que sobrevive al giro, y el launcher de `CreateDocument` conserva su resultado;
- si el usuario vuelve atrás o el ViewModel se destruye, la corrutina se cancela y se borran el temporal y el destino si ya existía;
- si el proceso muere con el selector abierto, el ViewModel nuevo recibe una URI sin exportación preparada: borra ese destino vacío (`DiscardExportDestinationUseCase`) y muestra el aviso de error.

**Volver a Ajustes con el aviso** [RF-20, RF-21, RF-45]:
- `Finished` guarda el resultado en el `SavedStateHandle` de la entrada anterior (Ajustes) y navega atrás;
- `SettingsRoute` lo lee una vez, muestra «N notas exportadas» (recurso `plurals`, D3) y, si faltaron imágenes, «Algunas imágenes no se pudieron incluir».

**Ajustes:**
- la fila Exportar notas pasa de `DisabledRow` a una fila con acción, igual que Vista de notas [RF-1, RF-2];
- se añaden `SettingsIntent.ExportRowClicked` y `SettingsEffect.NavigateToExport`;
- `NotesNavHost` añade la ruta `settings/export`.

**Textos nuevos:** título de la pantalla, «N seleccionadas», «Seleccionar todas», «Quitar selección», «Exportar», «Sin título», «No hay notas para exportar», «N notas exportadas» (`plurals`), «No se pudieron exportar las notas» y «Algunas imágenes no se pudieron incluir».

### 4.4 `:app`

Sin cambios. `CreateDocument` no necesita permisos ni un `FileProvider`.

### 4.5 Flujo

```
Exportar ─▶ prepare (cache/exports/…zip, progreso) ─▶ CreateDocument(«Notas 2026-11-12.zip»)
                                                        ├─ cancelado ─▶ discard ─▶ misma selección
                                                        └─ URI ─▶ save (copia; error ⇒ borrar destino)
                                                                   ├─ ok ─▶ Ajustes + «N notas exportadas»
                                                                   └─ error ─▶ «No se pudieron exportar las notas»
```

### 4.6 Documentación

- **Spec 006:** la RF-9 deja de incluir Exportar notas, y la línea de «Fuera de alcance» remite a la 011.
- **Spec 010:** la línea «Exportar todas las notas» de «Fuera de alcance» remite a la 011.
- **Spec 011:** por D1, RF-13 dirá «generará los PDF y después abrirá el selector», y RF-18 y RF-19 hablarán de la exportación completa. Por D3, RF-20 admitirá «1 nota exportada».
- **`.memory/MEMORY.md`:** estado y decisiones.

## 5. Decisiones técnicas

- **D1. Generar antes de abrir el selector** **Aprobado** el 2026-11-12. El selector crea el archivo de destino, vacío, en cuanto el usuario confirma. Si generamos después y la app se cierra a mitad, ese archivo vacío o a medias se queda en Descargas o Drive, y eso incumple RF-26. Generando antes, el destino solo existe durante la copia, que dura poco, y un selector abierto tras la muerte del proceso se limpia (sección 4.3). **Coste:** el selector tarda en abrirse lo que tarden los PDF, y RF-13 cambia de orden. **Alternativa:** abrir el selector primero, como dice hoy RF-13, y aceptar que si se cierra la app al generar puede quedar un archivo vacío.
- **D2. Textos del PDF en `:data`** **Aprobado** el 2026-11-12. «Sin título», «Trabajo» y «Personal» hacen falta al dibujar el PDF, que vive en `:data`. Se repiten en un `strings.xml` propio de `:data`, porque `:data` no puede leer los de `:presentation`. **Alternativa:** que la `Route` pase los textos al ViewModel con el intent `ExportClicked`. Evita la copia, pero mezcla textos de UI en los datos de la exportación.
- **D3. Plural del aviso** **Aprobado** el 2026-11-12. Con una nota, «1 nota exportada» en lugar de «1 notas exportadas», con un recurso `plurals`.
- **D4. Todo en una carpeta temporal y un solo .zip.** Se exporta a `cacheDir`, que el sistema puede limpiar y que no se copia en las copias de seguridad. Si la pantalla se destruye con el selector abierto sin pasar por Volver, el .zip temporal se queda ahí hasta el siguiente `prepare`, que vacía la carpeta: no se limpia en `onCleared` porque allí ya no hay corrutinas.
- **D5. La lista es la de Mis notas.** Se reutiliza `GetNotesUseCase`, en vez de un caso de uso nuevo con el mismo orden.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:domain` | `NoteExportNamingTest` | RF-14, RF-27 a RF-31: sin título, caracteres no válidos, 100 caracteres con emoji, repetidos con y sin mayúsculas, «///», tildes |
| `:domain` | Pruebas de los casos de uso con `FakeNotesExporter` | Delegan bien y propagan errores |
| `:data` (instrumentada) | `PdfNotesExporterTest` | Entradas del .zip y sus nombres; páginas A4 y cuántas hay, abriendo cada PDF con `PdfRenderer`; texto largo en varias páginas; imagen alta en una página; imagen que falta contada; copia a un destino; error que borra el destino; `discard` |
| `:presentation` (unitarias) | `ExportNotesViewModelTest` | Selección, todas y ninguna, nota que desaparece, `canExport`, progreso, selector cancelado, éxito, error, URI sin exportación preparada, no modifica notas |
| `:presentation` (unitarias) | `SettingsViewModelTest` | `ExportRowClicked` → `NavigateToExport` |
| `:presentation` (UI) | `ExportNotesScreenTest`, parametrizada en ambos temas | RF-3 a RF-12, RF-18, RF-19, RF-46, RF-47 |
| `:presentation` (UI) | `SettingsScreenTest` | Fila habilitada y que emite el intent [RF-1] |
| Manual | Emulador | Exportar a Descargas y abrir los PDF; títulos repetidos; sin título; formato, tareas e imágenes; cancelar el selector; girar durante la exportación; ambos temas |

Las casillas «☐»/«☑» dentro del PDF se comprueban en la demo, porque `PdfRenderer` solo da la imagen de la página.

## 7. Orden de implementación

1. Crear la rama `feat/export-notes`.
2. `:domain`: `NoteExportNaming`, interfaz, modelos y casos de uso, con pruebas.
3. `:data`: `PdfNotesExporter`, textos y binding, con pruebas instrumentadas.
4. `:presentation`: pantalla de exportación, navegación, fila de Ajustes y aviso, con pruebas.
5. Actualizar las specs 006, 010 y 011.
6. `./gradlew test`, pruebas instrumentadas, demo manual, comparación de la fila de Ajustes con Figma y revisión con `architecture-reviewer`.
7. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1, RF-2 | Fila de Ajustes, `SettingsViewModel`, navegación |
| RF-3 a RF-12, RF-46, RF-47 | `ExportNotesScreen`, `ExportNotesUiState`, `ExportNotesViewModel` |
| RF-13 a RF-22, RF-25 | `ExportNotesViewModel`, `ExportNotesRoute`, `SettingsRoute` |
| RF-23, RF-26 | `PdfNotesExporter.save`, limpieza en el ViewModel, D1 |
| RF-24 | Solo se leen las notas |
| RF-27 a RF-31 | `NoteExportNaming` |
| RF-32 a RF-45 | `PdfNotesExporter` |

## 9. Riesgos

- **Glifos «☐»/«☑» en el PDF:** dependen de las fuentes del sistema. Si alguna versión de Android no los dibuja, se dibujan las casillas con el `Canvas`. Se comprueba en la demo.
- **Memoria:** con imágenes grandes, se decodifica una sola cada vez, reducida con `inSampleSize`.
- **Tiempo:** la spec no fija un umbral. Con muchas notas con imágenes, el selector tardará en abrirse (D1), pero el progreso está visible.
- **Muerte del proceso durante la copia:** queda una ventana corta en la que el destino puede quedar a medias. La spec la acepta implícitamente al no pedir que la exportación sobreviva al cierre, pero no se elimina del todo.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
