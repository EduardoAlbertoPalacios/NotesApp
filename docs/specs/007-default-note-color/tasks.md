# Tareas 007 — Color predeterminado de las notas nuevas

> **Estado:** implementada; pendiente T21 (PR) · **Fecha:** 2026-11-03 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/default-note-color` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/007-default-note-color/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/default-note-color` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Valor por defecto y repositorio.** Añadir `NoteColor.DEFAULT = AQUA`. Añadir `observeDefaultNoteColor` y `setDefaultNoteColor` a `SettingsRepository`, según la sección 4.1 del plan.
  - **Cubre:** RF-23, RF-24
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T03. Casos de uso.** Crear `GetDefaultNoteColorUseCase`, que convierte `null` en `NoteColor.DEFAULT`, y `SetDefaultNoteColorUseCase`.
  - **Cubre:** RF-21, RF-23, RF-24
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T04. Pruebas de dominio** (`test-writer`). Crear `GetDefaultNoteColorUseCaseTest` y `SetDefaultNoteColorUseCaseTest`. Ampliar el `FakeSettingsRepository` de `:domain`.
  - **Cubre:** RF-21, RF-23, RF-24, propagación del error
  - **Hecho cuando:** `./gradlew :domain:test` pasa.

## Fase 2 — Datos (`:data`)

- [x] **T05. Mappers.** Añadir `NoteColor.toStorageValue()` y `String?.toNoteColorOrNull()` a `SettingsMappers.kt`, con los ocho valores estables de la sección 4.2 del plan.
  - **Cubre:** RF-22, RF-24
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T06. Repositorio.** Implementar los dos métodos en `SettingsRepositoryImpl` con su clave y su elección en memoria, atrapando solo `IOException`.
  - **Cubre:** RF-21, RF-22, RF-24, RF-25
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T07. Pruebas de datos** (`test-writer`). Ampliar `SettingsRepositoryImplTest` y las pruebas de los mappers: los ocho valores, valor desconocido, e `IOException` al leer y al escribir.
  - **Cubre:** RF-21, RF-22, RF-24, RF-25
  - **Hecho cuando:** `./gradlew :data:test` pasa.

## Fase 3 — Hoja compartida (`:presentation`)

- [x] **T08. Extraer la hoja.** Mover el contenido de `NoteColorSheet` a `notes/common/NoteColorPickerSheet.kt`, con los parámetros de la sección 4.3 del plan y sus previews. Crear `ColorPickerTestTags` y apuntar a ellas `NoteEditorTestTags.COLOR_*`. Dejar `NoteColorSheet` como envoltura del editor.
  - **Cubre:** D1 del plan; base de RF-4 a RF-11 y RF-27 a RF-29
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan sin cambiar la lógica de `NoteEditorScreenTest`.

## Fase 4 — Ajustes (`:presentation`)

- [x] **T09. Estado, intents y efecto.** Añadir a `SettingsUiState` `defaultNoteColor` y `defaultColorPicker`. Añadir los intents y `ShowDefaultColorSaveError` de la sección 4.3 del plan.
  - **Cubre:** RF-3, RF-6, RF-7
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T10. `SettingsViewModel`.** Observar el color. Abrir el selector con el color vigente, cambiar la opción, descartar y aplicar. Guardar solo si el color cambió y emitir el efecto si falla.
  - **Cubre:** RF-3, RF-6 a RF-8, RF-12 a RF-16, RF-21, RF-25, RF-26
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T11. Fila, hoja y aviso.** Sustituir la fila atenuada por `DefaultColorRow`. Mostrar `NoteColorPickerSheet` en `SettingsScreen` con «Nota nueva» en la vista previa. Mostrar el aviso en `SettingsRoute`. Añadir los dos textos nuevos y eliminar `settings_default_color_value`. Confirmar en Figma el estilo del valor de la fila.
  - **Cubre:** RF-1 a RF-5, RF-9 a RF-11, RF-26 a RF-29, requisitos de aspecto, accesibilidad e idioma
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y el estilo del valor coincide con `4:1794` y `40:1211`.

- [x] **T12. Pruebas de Ajustes** (`test-writer`). Ampliar el `FakeSettingsRepository` de `:presentation`, `SettingsViewModelTest` y `SettingsScreenTest`. La fila deja de tratarse como atenuada en `SettingsScreenTest`.
  - **Cubre:** RF-1 a RF-16, RF-25 a RF-29; aplicar el mismo color y hoja abierta al recrear la pantalla
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 5 — Editor (`:presentation`)

- [x] **T13. Color inicial de la nota nueva.** Inyectar `GetDefaultNoteColorUseCase` en `NoteEditorViewModel`. En una nota nueva, poner el estado de carga, leer el primer valor y asignarlo a `note` y al estado.
  - **Cubre:** RF-17, RF-18, RF-19, RF-20
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila y Hilt arma el grafo.

- [x] **T14. Pruebas del editor** (`test-writer`). Ampliar `NoteEditorViewModelTest`: nota nueva con cada caso de carga, con «Sin color», con otro color aplicado y abandonada; una nota guardada no lee el predeterminado.
  - **Cubre:** RF-17 a RF-20
  - **Hecho cuando:** `./gradlew :presentation:test` pasa.

## Fase 6 — Documentación y cierre

- [x] **T15. Actualizar specs anteriores.** En la spec 003, hacer que RF-22 y su línea de «Fuera de alcance» remitan a la 007. En la spec 006, sacar esta fila de RF-9 y de «Fuera de alcance», y hacer que RF-11 remita a la 007.
  - **Cubre:** principio 2
  - **Hecho cuando:** ninguna spec contradice a la 007.

- [x] **T16. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest`.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T17. Demo manual en el emulador.** En ambos temas: aplicar un color y comprobar la fila y una nota nueva; cerrar sin aplicar por las cuatro vías; usar «Sin color»; cerrar la app del todo y reabrirla; comprobar que las notas guardadas conservan su color. Comprobar también el indicador de carga al crear una nota y que el teclado se abre (riesgos del plan).
  - **Cubre:** RF-17, RF-20, RF-22, casos límite, criterios de finalización
  - **Hecho cuando:** todo se comporta según la spec y los riesgos quedan resueltos o anotados.

- [x] **T18. Comparar con Figma** (tú). Revisar la hoja y la fila frente a `4:1710`, `40:1115`, `4:1794` y `40:1211`, salvo los textos adaptados.
  - **Cubre:** requisito de aspecto, criterios de finalización
  - **Hecho cuando:** das el visto bueno o las diferencias quedan resueltas.

- [x] **T19. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T20. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja la spec 007.

- [ ] **T21. Commit y PR** (con tu confirmación). Marcar spec, plan y tareas como implementados. Hacer el commit con Conventional Commits y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
