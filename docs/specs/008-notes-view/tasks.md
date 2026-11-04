# Tareas 008 — Vista de notas: cuadrícula o lista

> **Estado:** completada (PR #9) · **Fecha:** 2026-11-04 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/notes-view` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/008-notes-view/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/notes-view` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Modelo y repositorio.** Crear `NotesView` con `DEFAULT = GRID`. Añadir `observeNotesView` y `setNotesView` a `SettingsRepository`, según la sección 4.1 del plan.
  - **Cubre:** RF-38, RF-39
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T03. Casos de uso.** Crear `GetNotesViewUseCase`, que convierte `null` en `NotesView.DEFAULT`, y `SetNotesViewUseCase`.
  - **Cubre:** RF-36, RF-38, RF-39
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T04. Pruebas de dominio** (`test-writer`). Crear `GetNotesViewUseCaseTest` y `SetNotesViewUseCaseTest`. Ampliar el `FakeSettingsRepository` de `:domain`.
  - **Cubre:** RF-36, RF-38, RF-39, propagación del error
  - **Hecho cuando:** `./gradlew :domain:test` pasa.

## Fase 2 — Datos (`:data`)

- [x] **T05. Mappers y repositorio.** Añadir `NotesView.toStorageValue()` y `String?.toNotesViewOrNull()`, con los valores `"grid"` y `"list"`. Implementar los dos métodos en `SettingsRepositoryImpl` con su clave y su elección en memoria, atrapando solo `IOException`.
  - **Cubre:** RF-36, RF-37, RF-39, RF-40
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T06. Pruebas de datos** (`test-writer`). Ampliar `SettingsRepositoryImplTest` y las pruebas de los mappers: valores estables, valor desconocido, e `IOException` al leer y al escribir.
  - **Cubre:** RF-36, RF-37, RF-39, RF-40
  - **Hecho cuando:** `./gradlew :data:test` pasa.

## Fase 3 — Ajustes (`:presentation`)

- [x] **T07. Extraer la hoja de opciones.** Mover el contenido de `ThemeSheet` a un `SettingsOptionSheet<T>` genérico, con su título, sus opciones y sus etiquetas de prueba. Dejar `ThemeSheet` como envoltura.
  - **Cubre:** D2 del plan; base de RF-9 a RF-13 y RF-43
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan sin cambiar la lógica de las pruebas del tema, salvo adaptar los fakes de `SettingsRepository`.

- [x] **T08. Estado, intents y ViewModel.** Añadir `notesView`, `isNotesViewSheetVisible`, los intents y `ShowNotesViewSaveError`. `SettingsViewModel` observa la vista y la guarda solo si cambió.
  - **Cubre:** RF-8, RF-11, RF-13, RF-36, RF-40, RF-41
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T09. Fila, hoja y aviso.** Sustituir la fila atenuada por `NotesViewRow`. Crear `NotesViewSheet` y `NotesView.labelRes()`. Mostrar el aviso en `SettingsRoute`. Añadir los textos nuevos y eliminar `settings_notes_view_value`.
  - **Cubre:** RF-7 a RF-13, RF-41, RF-43, requisitos de aspecto, accesibilidad e idioma
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T10. Pruebas de Ajustes** (`test-writer`). Ampliar `SettingsViewModelTest`, `SettingsScreenTest` y `SettingsRouteTest`. La fila deja de tratarse como atenuada.
  - **Cubre:** RF-7 a RF-13, RF-36, RF-40, RF-41, RF-43; hoja abierta al recrear la pantalla
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 4 — Texto con tareas seguidas (`:presentation`)

- [x] **T11. Probar el corte de línea.** Añadir `flowTasks` a `buildCardContent` y `wrapTasks`, según la sección 4.3 del plan. Comprobar en el emulador que una tarea baja entera si no cabe. El primer enfoque falló; el usuario eligió medir y decidir los cortes.
  - **Cubre:** RF-20 a RF-23; riesgo principal del plan
  - **Hecho cuando:** RF-21 se cumple en el emulador, con texto sin casilla separada ni palabras cortadas.

- [x] **T12. Pruebas del texto** (`test-writer`). Ampliar `FormattedContentTest`: tareas consecutivas y separadas, texto entre tareas, saltos conservados, formato y tachado en su sitio. Sin `flowTasks` no cambia nada.
  - **Cubre:** RF-20 a RF-23
  - **Hecho cuando:** `./gradlew :presentation:test` pasa.

## Fase 5 — Mis notas (`:presentation`)

- [x] **T13. Estado y ViewModel.** Añadir `notesView`, `NoteItemUi.category` y `NoteItemUi.listContent`, `NotesViewSelected` y `ShowNotesViewSaveError`. `NoteListViewModel` observa la vista, la guarda solo si cambió y rellena los campos nuevos.
  - **Cubre:** RF-3, RF-4, RF-32 a RF-37, RF-40, RF-41
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila y Hilt arma el grafo.

- [x] **T14. Selector y columnas.** Crear `NotesViewSelector` desde el frame, con `ic_layout_grid` e `ic_list`. Hacer variable el número de columnas. Mostrar el estado de carga mientras `notesView` sea nula.
  - **Cubre:** RF-1, RF-2, RF-5, RF-6, RF-17, RF-31, RF-37, RF-42, RF-43
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las previews en ambos temas se ven según `62:311` y `55:880`.

- [x] **T15. Tarjeta de lista.** Adaptar `NoteCard` a la vista, con `CategoryTag`, cuatro líneas y la fecha a la derecha. Mover `NoteCategory.labelRes()` a `notes/common`. Medir el contraste de la etiqueta sobre cada color de nota.
  - **Cubre:** RF-16, RF-18, RF-19, RF-24 a RF-30, requisito de contraste
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y el contraste es de al menos 4,5:1 en todos los colores.

- [x] **T16. Aviso en Mis notas.** Añadir `SnackbarHost` a la pantalla y atender el efecto en `NoteListRoute`, sin tapar «Nueva nota».
  - **Cubre:** RF-41
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T17. Pruebas de Mis notas** (`test-writer`). Ampliar `NoteListViewModelTest` y `NoteListScreenTest`.
  - **Cubre:** RF-1 a RF-6, RF-16 a RF-19, RF-24 a RF-34, RF-37, RF-40 a RF-43
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 6 — Documentación y cierre

- [x] **T18. Actualizar la spec 006.** Sacar esta fila de RF-9 y de «Fuera de alcance», y hacer que RF-12 remita a la 008.
  - **Cubre:** principio 2
  - **Hecho cuando:** la spec 006 no contradice a la 008.

- [x] **T19. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest`.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T20. Demo manual en el emulador.** En ambos temas: cambiar de vista desde Mis notas y desde Ajustes; ver en lista notas con tareas, imagen, formato, sin categoría y fijadas; cambiar con filtro y con búsqueda activos; cambiar sin notas; cerrar la app del todo y reabrirla.
  - **Cubre:** RF-14, RF-15, RF-21, RF-37, casos límite, criterios de finalización
  - **Hecho cuando:** todo se comporta según la spec.

- [x] **T21. Comparar con Figma** (tú). Revisar el selector y la tarjeta de lista frente a `62:311` y `55:880`, y la fila frente a `4:1794` y `40:1211`, salvo la marca del selector.
  - **Cubre:** requisito de aspecto, criterios de finalización
  - **Hecho cuando:** das el visto bueno o las diferencias quedan resueltas.

- [x] **T22. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T23. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas. Corregir que la spec 007 ya está en `main`.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja las specs 007 y 008.

- [x] **T24. Commit y PR** (con tu confirmación). Marcar spec, plan y tareas como implementados. Hacer el commit con Conventional Commits y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
