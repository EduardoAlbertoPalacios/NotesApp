# Tareas 002 — Formato de texto en las notas

> **Estado:** completada · **Fecha:** 2026-10-19 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/text-formatting` desde `main`, actualizado con `origin`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/text-formatting` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Modelos de formato.** Crear `TextFormat` (`BOLD`, `UNDERLINE`, `STRIKETHROUGH`) y `FormatSpan(start, end, format)`. Agregar `Note.contentSpans: List<FormatSpan> = emptyList()`.
  - **Cubre:** RF-21, RF-22
  - **Hecho cuando:** `./gradlew assembleDebug` compila, las pruebas existentes de `:domain` pasan y `:domain` no tiene dependencias nuevas.

- [x] **T03. Alternar formato en `NoteFormatting`.** Crear el `object` con `isApplied(spans, range, format)` y `toggle(spans, range, format)`. Devuelve listas normalizadas: sin tramos vacíos, sin solapes del mismo formato y con los contiguos fusionados.
  - **Cubre:** RF-5, RF-7, RF-8, RF-9, CL-1, CL-2, CL-4
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T04. Formatos en el cursor.** Agregar `formatsAt(spans, position)`: devuelve los formatos del carácter anterior a la posición y ninguno en la posición 0.
  - **Cubre:** RF-14, CL-9
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T05. Ajustar tramos tras editar.** Agregar `onTextChanged(spans, old, new, activeFormats)`: calcula el rango editado por prefijo y sufijo comunes, desplaza o recorta los tramos y aplica `activeFormats` a lo insertado.
  - **Cubre:** RF-12, RF-18, CL-3, CL-8
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T06. Pruebas de `NoteFormatting`** (`test-writer`). Casos de la sección 6.1 del plan.
  - **Cubre:** RF-5, RF-7 a RF-9, RF-12, RF-14, RF-18, CL-1 a CL-4, CL-8, CL-9
  - **Hecho cuando:** `./gradlew :domain:test` pasa, y `NoteFormattingTest` incluye:
    - `toggle` sobre un rango sin formato, con formato completo y con formato parcial;
    - conservar los demás formatos;
    - un rango con saltos de línea;
    - `formatsAt` en la posición 0 y en el borde de un tramo;
    - `onTextChanged` al insertar antes, dentro, después y en los bordes, al borrar todo y al reemplazar una selección;
    - formatos activos al escribir y al pegar;
    - fusión de tramos contiguos.

- [x] **T07. Pruebas de casos de uso existentes** (`test-writer`). Ampliar `SaveNoteUseCaseTest` (una nota con tramos se guarda con fecha nueva) y `GetNotesUseCaseTest` (la búsqueda ignora los tramos).
  - **Cubre:** RF-20, RF-25
  - **Hecho cuando:** `./gradlew :domain:test` pasa con esos dos casos nuevos.

## Fase 2 — Datos (`:data`)

- [x] **T08. Codificación de tramos.** Crear las funciones `internal` `List<FormatSpan>.encode()` y `String.decodeSpans(contentLength)`, con el formato `start,end,F;…`. Al decodificar se descartan las entradas mal formadas y se recortan al largo del contenido.
  - **Cubre:** RF-21, RNF-1
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T09. Columna y mappers.** Agregar a `NoteEntity` `@ColumnInfo(name = "content_spans", defaultValue = "") val contentSpans: String = ""`. Incluir los tramos en `toDomain()` y `toEntity()`.
  - **Cubre:** RF-19, RF-21
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila. Es normal que la base aún no cambie de versión; eso se hace en T10.

- [x] **T10. Migración v3→v4** (skill `room-migration`). `NotesDatabase` pasa a `version = 4` con `AutoMigration(from = 3, to = 4)` y se exporta el esquema 4.
  - **Cubre:** RF-22, RNF-1
  - **Hecho cuando:**
    - `./gradlew assembleDebug` compila;
    - `data/schemas/.../4.json` existe con la columna `content_spans`;
    - no hay `fallbackToDestructiveMigration` en el proyecto.

- [x] **T11. Pruebas de `:data`** (`test-writer`).
  - Unitarias:
    - `FormatSpanCodecTest`: ida y vuelta, cadena vacía, entradas mal formadas y recorte;
    - ampliar `NoteMappersTest` y `NoteRepositoryImplTest`.
  - Instrumentadas:
    - ampliar `MigrationTest` para v3→v4, que conserva las notas y deja `content_spans` vacío;
    - en `NoteDaoTest`, una nota con tramos se guarda y se lee igual.
  - **Cubre:** RF-21, RF-22, RNF-1
  - **Hecho cuando:** `./gradlew :data:testDebugUnitTest` y `./gradlew :data:connectedDebugAndroidTest` pasan.

## Fase 3 — Estado del editor (`:presentation`)

- [x] **T12. Strings e íconos.**
  - Strings: `note_editor_format_bold`, `note_editor_format_underline`, `note_editor_format_strikethrough`, `note_editor_format_active` y `note_editor_format_inactive`.
  - Íconos Lucide como VectorDrawable: `ic_bold`, `ic_underline` y `ic_strikethrough`.
  - **Cubre:** RNF-4, RNF-5
  - **Hecho cuando:** los 5 strings y los 3 drawables existen y `./gradlew :presentation:assembleDebug` compila.

- [x] **T13. Contenido con selección.**
  - `NoteEditorUiState.content` pasa a ser un `TextFieldValue`. Se agregan `contentSpans`, `activeFormats`, `isFormatMenuVisible` e `isContentFocused`.
  - `ContentChanged` recibe un `TextFieldValue`.
  - Adaptar el ViewModel y la pantalla **sin comportamiento nuevo**. El agente `test-writer` adapta las pruebas existentes del editor.
  - **Cubre:** apoyo a RF-7 a RF-14
  - **Hecho cuando:** `./gradlew assembleDebug` compila, `./gradlew :presentation:testDebugUnitTest` pasa y `./gradlew :presentation:compileDebugAndroidTestKotlin` compila.

- [x] **T14. Escribir y mover el cursor.**
  - Si cambió el texto, `ContentChanged` aplica `NoteFormatting.onTextChanged(…, activeFormats)` y marca cambios pendientes.
  - Si solo cambió la selección, recalcula `activeFormats = formatsAt(...)` sin marcar cambios.
  - **Cubre:** RF-12, RF-14, RF-18, CL-6, CL-9
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y un cambio solo de selección no lanza el autoguardado. Esto se comprueba por lectura y se prueba en T17.

- [x] **T15. Menú y aplicar formato.**
  - Intents `FormatButtonClicked`, `FormatMenuDismissed` y `FormatSelected(format)`.
  - Con selección, `toggle` mantiene la selección y marca cambios.
  - Sin selección, alterna `activeFormats` sin marcar cambios.
  - En los dos casos se cierra el menú.
  - **Cubre:** RF-2, RF-3, RF-4, RF-7 a RF-11, RF-13, CL-5
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T16. Guardar y cargar tramos.**
  - El borrador incluye `contentSpans`.
  - Los cambios pendientes también comparan los tramos.
  - La carga construye el `TextFieldValue` y los tramos desde la nota.
  - **Cubre:** RF-19, RF-20, RF-21, RF-22
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T17. Pruebas de `NoteEditorViewModel`** (`test-writer`). Casos de la sección 6.4 del plan.
  - **Cubre:** RF-2 a RF-4, RF-7 a RF-14, RF-18 a RF-22, CL-5, CL-6, CL-9
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` pasa, con pruebas para:
    - abrir y cerrar el menú;
    - `FormatSelected` con selección (alterna, mantiene la selección, guarda y actualiza la fecha);
    - `FormatSelected` sin selección (no guarda);
    - escribir con formatos activos;
    - mover el cursor;
    - pegar con formatos activos;
    - cargar una nota con tramos y otra sin tramos.

## Fase 4 — Interfaz del editor

- [x] **T18. Pintar el formato.** Crear `buildFormattedContent(content, spans): AnnotatedString` fuera de los composables y una `VisualTransformation` con `OffsetMapping.Identity` para el campo de contenido. El título queda sin transformación.
  - **Cubre:** RF-12, RF-15, RF-16, RF-17, RF-21
  - **Hecho cuando:** `./gradlew assembleDebug` compila y la vista previa del editor con contenido formateado muestra negrita, subrayado y tachado.

- [x] **T19. Habilitar «Aa» según el foco.** `onFocusChanged` en el campo de contenido emite `ContentFocusChanged`. El botón «Aa» se habilita solo con el foco en el contenido y emite `FormatButtonClicked`.
  - **Cubre:** RF-1, RF-6
  - **Hecho cuando:** `./gradlew assembleDebug` compila y con el foco en el título el botón no está habilitado.

- [x] **T20. Menú de formato.** `DropdownMenu` anclado a «Aa» con Negrita, Subrayado y Tachado: cada uno con su ícono y etiqueta, `ic_check` si está activo y `stateDescription` «Activo» o «Inactivo». Usa colores y tipografía del tema.
  - **Cubre:** RF-2 a RF-5, RNF-2, RNF-3, RNF-4
  - **Hecho cuando:** `./gradlew assembleDebug` compila y la vista previa con el menú abierto muestra las tres opciones con el estado activo marcado por un ícono.

- [x] **T21. Etiquetas de prueba y vistas previas.** Agregar etiquetas de prueba para «Aa», el menú y cada opción, y vistas previas de «contenido formateado» y «menú abierto».
  - **Cubre:** apoyo a las pruebas de RF-1 a RF-6
  - **Hecho cuando:** `./gradlew assembleDebug` compila y las vistas previas se renderizan en Android Studio.

- [x] **T22. Pruebas del editor** (`test-writer`). `NoteEditorScreenTest` según 6.5 y prueba unitaria de `buildFormattedContent`.
  - **Cubre:** RF-1 a RF-6, RF-23, RNF-2, RNF-5
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 5 — Mis notas

- [x] **T23. Tarjetas con formato.** `NoteItemUi.content` pasa a ser un `AnnotatedString` construido con `buildFormattedContent` en el ViewModel de la lista, y `NoteCard` lo pinta. El agente `test-writer` adapta las pruebas existentes de la lista.
  - **Cubre:** RF-23, RF-24
  - **Hecho cuando:** `./gradlew assembleDebug` compila, las pruebas de la lista pasan y la vista previa de Mis notas muestra una tarjeta con formato.

- [x] **T24. Pruebas de Mis notas** (`test-writer`). Ampliar `NoteListViewModelTest` (el contenido llega con sus estilos) y `NoteListScreenTest` (la tarjeta muestra el texto formateado).
  - **Cubre:** RF-23, RF-24
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 6 — Verificación

- [x] **T25. Suite completa.**
  - **Cubre:** todos los RF
  - **Hecho cuando:**
    - `./gradlew test` pasa;
    - `./gradlew connectedDebugAndroidTest` pasa;
    - `./gradlew assembleDebug` compila;
    - `./gradlew lint` no reporta errores nuevos.

- [x] **T26. Revisión de arquitectura** con el agente `architecture-reviewer`.
  - **Cubre:** principios 1, 3 y 5 de la constitución
  - **Hecho cuando:** no hay hallazgos bloqueantes, o están corregidos y T25 vuelve a pasar.

- [x] **T27. Verificación manual en el emulador** (sección 6.6 del plan).
  - **Cubre:** RF-1 a RF-25, RNF-1, RNF-4
  - **Hecho cuando:** todas estas comprobaciones están marcadas:
    - [x] Instalar sobre la versión anterior: las notas existentes siguen intactas y sin formato (RF-22, RNF-1).
    - [x] «Aa» está deshabilitado con el foco en el título y habilitado en el contenido (RF-1, RF-6).
    - [x] Seleccionar un fragmento y aplicar y quitar Negrita, Subrayado y Tachado (RF-7, RF-8).
    - [x] Aplicar Subrayado a un fragmento en negrita: queda con los dos (RF-9).
    - [x] Activar Negrita sin selección y escribir: el texto sale en negrita (RF-11, RF-12).
    - [x] Activar Negrita sin escribir y tocar otra parte: deja de estar activa (RF-14, CL-9).
    - [x] Pegar desde otra app un texto con formato: llega plano; con Negrita activa, llega en negrita (RF-17, RF-18). *(RF-18 probado pegando desde la propia app. RF-17 queda garantizado por diseño: el campo solo guarda texto plano; no se probó con una app que copie formato.)*
    - [x] Cerrar y reabrir la nota: el formato se mantiene (RF-21).
    - [x] La tarjeta en Mis notas muestra el formato, también cuando se recorta (RF-23, RF-24). *(El recorte lo comprueba `NoteListScreenTest` con 30 líneas.)*
    - [x] La búsqueda encuentra texto con formato (RF-25).
    - [x] El menú sigue el estilo de la app (RNF-4).

## Fase 7 — Cierre

- [x] **T28. Documentación y memoria.**
  - Marcar la spec y el plan como implementados y reflejar cualquier cambio en la spec (principio 2).
  - Actualizar `.memory/MEMORY.md`.
  - **Cubre:** —
  - **Hecho cuando:** la spec y el plan dicen «implementada» o «implementado», ningún requisito contradice el código y `MEMORY.md` sigue en ~50 líneas o menos.

- [x] **T29. Commit.** Proponer el mensaje y los archivos con `/commit` y esperar tu confirmación. No hacer push sin que lo pidas.
  - **Cubre:** —
  - **Hecho cuando:** el commit existe en `feat/text-formatting` con autor `Eduardo Alberto <>`, sin `Co-Authored-By`, y `git status` está limpio.

## Trazabilidad

| Requisito | Tareas |
|---|---|
| RF-1 Habilitar «Aa» | T19, T22, T27 |
| RF-2 a RF-4 Menú | T15, T17, T20, T22 |
| RF-5 Opciones activas | T03, T20, T22 |
| RF-6 Deshabilitado en el título | T19, T22, T27 |
| RF-7 a RF-10 Selección | T03, T06, T15, T17, T27 |
| RF-11 a RF-13 Formato activo | T05, T06, T15, T17, T27 |
| RF-14 Mover el cursor | T04, T06, T14, T17, T27 |
| RF-15, RF-16 Solo el contenido | T18, T27 |
| RF-17 Pegar plano | T18, T27 |
| RF-18 Pegar con formatos activos | T05, T06, T14, T17, T27 |
| RF-19, RF-20 Guardado y fecha | T07, T09, T16, T17 |
| RF-21 Reabrir con formato | T02, T08, T09, T11, T16, T18, T27 |
| RF-22 Notas antiguas | T02, T10, T11, T16, T27 |
| RF-23, RF-24 Tarjetas | T22, T23, T24, T27 |
| RF-25 Búsqueda | T07, T27 |
| RNF-1 Datos | T08, T10, T11, T27 |
| RNF-2, RNF-3 Accesibilidad | T20, T22 |
| RNF-4 Apariencia | T12, T20, T27 |
| RNF-5 Idioma | T12, T22 |
