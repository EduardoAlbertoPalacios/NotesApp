# Tareas 005 — Deshacer en el editor

> **Estado:** completada · **Fecha:** 2026-10-29 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/undo` desde `main`, actualizado con `origin`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/undo` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Modelos del historial.** Crear `EditField`, `EditKind`, `UndoEdit`, `UndoSnapshot`, `OpenStep` y `UndoHistory` (con `canUndo`), según la sección 4.1 del plan.
  - **Cubre:** RF-26
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T03. Clasificar cambios.** Crear el `object NoteUndo` con `classify(old, new)`:
  - un carácter insertado es `TYPING`, y cierra el paso si es espacio o `\n`;
  - un carácter borrado es `DELETING`;
  - cualquier otro cambio es `STANDALONE`, incluido el texto igual con tareas distintas.

  La diferencia se calcula con prefijo y sufijo comunes, dentro de `NoteUndo` (D5).
  - **Cubre:** RF-5 a RF-7, RF-15, RF-16, CL-3 a CL-6, CL-14
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T04. Registrar, cerrar y deshacer.** Agregar a `NoteUndo`:
  - `record(history, before, edit)`, con las reglas de unión, paso nuevo, `STANDALONE` cerrado, cierre tras espacio o `\n`, y el límite `MAX_STEPS = 100`;
  - `close(history)`;
  - `undo(history)`.
  - **Cubre:** RF-7 a RF-10, RF-13 a RF-20, RF-24, RF-25, RF-29, CL-1, CL-16
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T05. Caso de uso `RestoreNoteUseCase`.** Guarda la nota conservando su `updatedAt`. Como `SaveNoteUseCase`, rechaza la nota vacía con `NoteError.EmptyNote` y devuelve la nota con su id.
  - **Cubre:** RF-34
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T06. Pruebas de dominio** (`test-writer`). Crear `NoteUndoTest` y `RestoreNoteUseCaseTest` con los casos de la sección 6.1 del plan.
  - **Cubre:** RF-5 a RF-10, RF-13 a RF-20, RF-24, RF-25, RF-29, RF-34, CL-1, CL-3 a CL-6, CL-9, CL-14, CL-16
  - **Hecho cuando:** `./gradlew :domain:test` pasa con todos los casos de 6.1.

## Fase 2 — Título como `TextFieldValue` (`:presentation`)

- [x] **T07. Título con cursor.** Cambiar `NoteEditorUiState.title` y `TitleChanged` a `TextFieldValue`. Adaptar:
  - el ViewModel, en la carga, los borradores y `hasUnsavedChanges()`, usando `title.text`;
  - `EditorTextField` y el título de `NoteEditorScreen`;
  - `NoteColorSheet`;
  - las previews.

  Sin cambio de comportamiento.
  - **Cubre:** D8 (base de RF-19 y RF-26)
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T08. Adaptar las pruebas existentes** (`test-writer`). `NoteEditorViewModelTest` y `NoteEditorScreenTest` pasan a usar `TitleChanged(TextFieldValue)` y el título como `TextFieldValue`, sin casos nuevos.
  - **Cubre:** —
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 3 — Estado del editor (`:presentation`)

- [x] **T09. Estado e intent.** Agregar `canUndo` y `focusRequest: FocusRequest?` a `NoteEditorUiState`, con `FocusRequest(field, id)`, y `UndoClicked` a `NoteEditorIntent`. Si el `when` del ViewModel deja de compilar, añadir una rama provisional hasta T11.
  - **Cubre:** RF-2, RF-21, RF-26
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T10. Registro en el ViewModel.**
  - Guardar `history` y registrar cada paso con la foto anterior:
    - `TitleChanged` y `ContentChanged` con texto distinto, ya aplicadas las reglas de tareas;
    - formato con selección, botón de tareas si cambian, y `TaskToggled`, en el campo `CONTENT`.
  - Cerrar el paso con `TitleChanged` o `ContentChanged` que solo mueven la selección.
  - Pausa: `undoPauseJob` con `delay(UNDO_PAUSE_MS = 1000)` que cierra el paso.
  - No registrar formato sin selección, color, categoría, fijar ni imágenes.
  - `canUndo` refleja el historial.
  - **Cubre:** RF-3, RF-5 a RF-20, CL-7, CL-8
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas existentes de `:presentation` pasan.

- [x] **T11. Deshacer en el ViewModel.** `UndoClicked`:
  1. llama a `NoteUndo.undo`;
  2. pone título, contenido, tramos y tareas de la foto, con la selección en su campo;
  3. calcula los formatos activos en el cursor;
  4. emite un `focusRequest` con `id` nuevo;
  5. guarda con `edit {}`.

  No toca color, categoría, fijación ni imágenes.
  - **Cubre:** RF-21 a RF-28, RF-32, RF-33, CL-10, CL-12
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas existentes de `:presentation` pasan.

- [x] **T12. Fecha original.**
  - Guardar `openedNote` al cargar; es nula en una nota nueva.
  - `lastEditWasUndo` se pone a `true` al deshacer y a `false` con cualquier otra edición o `saveNow`.
  - En `save()`, si `lastEditWasUndo` y el estado coincide con `openedNote` en todo (también color, categoría, fijación e imágenes), se usa `RestoreNoteUseCase` y la fecha visible vuelve a la original.
  - **Cubre:** RF-34, CL-11, CL-15
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas existentes de `:presentation` pasan.

- [x] **T13. Pruebas del ViewModel** (`test-writer`). Ampliar `NoteEditorViewModelTest` con los casos de la sección 6.2 del plan, con tiempo virtual para la pausa.
  - **Cubre:** RF-2, RF-3, RF-8 a RF-12, RF-17, RF-19, RF-21 a RF-28, RF-32 a RF-34, CL-2, CL-5 a CL-8, CL-10 a CL-12, CL-15
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` pasa con todos los casos de 6.2.

## Fase 4 — Interfaz del editor (`:presentation`)

- [x] **T14. Botón «Deshacer».** `ToolbarButton(ic_undo, note_editor_toolbar_undo)` con `onClick` solo si `state.canUndo`, sin depender del foco, y con el `testTag` nuevo `TOOLBAR_UNDO`.
  - **Cubre:** RF-1, RF-2, RF-4, RNF-1, RNF-3
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y el botón funciona en el emulador.

- [x] **T15. Foco al deshacer.** Un `FocusRequester` para el contenido, como el del título, y un `LaunchedEffect(state.focusRequest)` que pide el foco al campo indicado.
  - **Cubre:** RF-26, CL-7
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y, en el emulador, deshacer un cambio del título con el cursor en el contenido lleva el cursor al título.

- [x] **T16. Pruebas de UI** (`test-writer`). Ampliar `NoteEditorScreenTest` con los casos de la sección 6.3 del plan.
  - **Cubre:** RF-1, RF-2, RF-4, RF-21, RF-26, RNF-3
  - **Hecho cuando:**
    - `./gradlew :presentation:connectedDebugAndroidTest` pasa;
    - las pruebas nuevas pasan tres veces seguidas.

## Fase 5 — Documentación

- [x] **T17. Excepción en `CLAUDE.md`.** Añadir `NoteUndo` a la lista de los `object` de reglas puras que el ViewModel usa directamente (D6).
  - **Cubre:** principio 3
  - **Hecho cuando:** `CLAUDE.md` nombra `NoteFormatting`, `NoteChecklist` y `NoteUndo`.

## Fase 6 — Verificación y cierre

- [x] **T18. Suite completa.** `./gradlew test`, `connectedDebugAndroidTest`, `assembleDebug` y `lint`.
  - **Cubre:** todos
  - **Hecho cuando:** todo pasa. El único error de `lint` permitido es el que ya existía en `NoteEditorRoute.kt`.
  - **Resultado (2026-10-30):**
    - `./gradlew test`: 451 pruebas, 0 fallos (`:domain` 198, `:data` 73, `:presentation` 179, `:app` 1).
    - `connectedDebugAndroidTest`: 103 pruebas, 0 fallos (`NoteEditorScreenTest` 50, `NoteListScreenTest` 27, `NoteDaoTest` 17, `MigrationTest` 8, `ExampleInstrumentedTest` 1).
    - `assembleDebug`: compila.
    - `lint` (con `--continue`, todos los módulos): un solo error, el preexistente `LocalContextGetResourceValueCall` en `NoteEditorRoute.kt:50`.

- [x] **T19. Revisión de arquitectura** (`architecture-reviewer`).
  - **Cubre:** principios 1, 3 y 6
  - **Hecho cuando:** no quedan hallazgos sin resolver o sin tu decisión.
  - **Resultado:** sin violaciones de arquitectura. Hallazgos y decisiones del usuario, todos aplicados:
    1. **(media)** La petición de foco se quedaba en el estado y, al girar la pantalla, el foco volvía a saltar al campo del último deshacer. Nuevo intent `FocusRequestHandled(id)`: la pantalla lo emite tras mover el foco y el ViewModel pone `focusRequest = null`; los `id` salen de un contador del ViewModel. Plan actualizado (4.2 y D9).
    2. **(menor)** Faltaban pruebas de CL-3, CL-4 y CL-13: añadidas al ViewModel por `test-writer`, junto con las de `FocusRequestHandled` (ViewModel y pantalla, incluida la recreación con `StateRestorationTester`).
    3. **(menor)** El plan no recogía `restoredTo` ni `recordStandalone`: añadidos en 4.2.
    4. **(menor)** Casos límite de la spec desordenados: reordenados de CL-1 a CL-16.

    Tras los cambios: `:presentation:testDebugUnitTest` 185 pruebas y `:presentation:connectedDebugAndroidTest` 79 pruebas, sin fallos.

- [x] **T20. Verificación manual en el emulador.** Los pasos de la sección 6.4 del plan, incluidos RNF-2 con una nota de 10 000 caracteres y el riesgo del teclado (sección 9).
  - **Cubre:** RF-1 a RF-34, RNF-1 a RNF-3
  - **Hecho cuando:** cada paso se comprueba y su resultado queda anotado aquí.
  - **Resultado (2026-10-30, emulador Medium_Phone_API_36.0):**
    - **Escribir y deshacer palabra a palabra [RF-13 a RF-25]:** « hoy mismo sin falta» al final de «Ideas sueltas» se deshace en cinco toques: «falta», «sin », «mismo », «hoy » y « ». Al final el botón queda deshabilitado.
    - **Fecha original [RF-34]:** tras deshacerlo todo, «Ideas sueltas» sigue en «Ayer» y en el mismo sitio de Mis notas; «Una buena idea» vuelve a «27 Oct» y «La compra» a «Hoy, 12:21 PM».
    - **Negrita [RF-8, RF-22]:** negrita sobre «Hacer» y deshacer: vuelve a texto normal y la fecha a «27 Oct».
    - **Pegar [RF-7, CL-3]:** pegar «Hacer» sobre el «.» seleccionado y deshacer: un solo paso devuelve «… bien.».
    - **Tareas [RF-9, RF-10, CL-4]:** en «La compra», Intro tras «Café» y escribir «Sal», marcar «Tomates» y deshacer tres veces: se desmarca «Tomates», desaparece «Sal» (queda la tarea vacía) y desaparece la tarea nueva.
    - **Girar [RF-30]:** con cambios en el título y el contenido, girar y volver: «Deshacer» sigue habilitado y deshace el contenido y luego el título. También se deshizo un paso en horizontal.
    - **Foco tras girar (hallazgo 1 de T19):** deshacer el contenido, tocar el título y girar dos veces: el contenido no recupera el foco.
    - **Foco al deshacer [RF-26]:** comprobado en T15 (título y contenido).
    - **Salir y reabrir [RF-3, RF-31]:** al salir del editor sin pulsar «Deshacer» y volver a abrir la nota, el botón aparece deshabilitado.
    - **Respuesta [RNF-2]:** nota de 10 000 caracteres (1 000 palabras); cada toque deshace una palabra. Con `dumpsys gfxinfo` durante cinco deshacer: percentil 90 de 24 ms y percentil 99 de 46 ms por fotograma, por debajo de 100 ms. La nota de prueba se eliminó.
    - **Riesgo del teclado (sección 9): sin verificar con teclado en pantalla.** El emulador tiene teclado físico y Gboard solo muestra su barra flotante, también con `show_ime_with_hard_keyboard`. Con `adb shell input text` (eventos de tecla) la escritura se agrupa por palabra como se espera. Queda pendiente comprobarlo en un dispositivo con teclado en pantalla.
    - En el emulador, la barra flotante de Gboard tapa las casillas de la izquierda y un toque en ella pidió permiso de micrófono; es del sistema, no de la app.

- [x] **T21. Cierre.** Actualizar `.memory/MEMORY.md` y marcar spec, plan y tareas como implementados o completados, reflejando en el plan cualquier cambio hecho al implementar.
  - **Cubre:** principio 2
  - **Hecho cuando:** los tres documentos tienen su estado final y la memoria refleja el deshacer.
