# Tareas 009 — Guardado automático activable

> **Estado:** implementada; pendiente T22 (PR) · **Fecha:** 2026-11-06 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/auto-save-setting` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/009-auto-save-setting/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/auto-save-setting` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Ajuste.** Añadir `observeAutoSaveEnabled` y `setAutoSaveEnabled` a `SettingsRepository`. Crear `GetAutoSaveEnabledUseCase`, que convierte `null` en `true`, y `SetAutoSaveEnabledUseCase`.
  - **Cubre:** RF-4 a RF-6
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T03. Imágenes pendientes.** Añadir `NoteRepository.getImagePaths()` e `ImageStorage.listPaths()`. Crear `ImportNoteImagesUseCase`, `SaveNoteChangesUseCase`, `DiscardImageCopiesUseCase` y `CleanUpImageFilesUseCase`, según la sección 4.1 del plan.
  - **Cubre:** RF-11, RF-12, RF-16, RF-17, RF-24
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T04. Pruebas de dominio** (`test-writer`). Probar los seis casos de uso nuevos y ampliar los fakes de `SettingsRepository`, `NoteRepository` e `ImageStorage`.
  - **Cubre:** RF-4 a RF-6, RF-16, RF-17, RF-24; nota vacía, fallos parciales y limpieza que respeta las imágenes usadas
  - **Hecho cuando:** `./gradlew :domain:test` pasa.

## Fase 2 — Datos (`:data`)

- [x] **T05. Ajuste y rutas.** Implementar el ajuste en `SettingsRepositoryImpl` con la clave `auto_save_enabled` y su elección en memoria. Añadir la consulta de rutas a `NoteDao` y `NoteRepositoryImpl`, y `listPaths()` a `ImageStorageImpl`. El esquema de Room no cambia.
  - **Cubre:** RF-4 a RF-7
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila y el esquema exportado no cambia.

- [x] **T06. Pruebas de datos** (`test-writer`). Ampliar `SettingsRepositoryImplTest`, los mappers y `NoteRepositoryImplTest`. Añadir pruebas instrumentadas de la consulta de rutas y del listado de archivos.
  - **Cubre:** RF-4 a RF-7
  - **Hecho cuando:** `./gradlew :data:test` y `./gradlew :data:connectedDebugAndroidTest` pasan.

## Fase 3 — Ajustes (`:presentation`)

- [x] **T07. Estado, ViewModel y fila.** Añadir `isAutoSaveEnabled`, `AutoSaveToggled` y `ShowAutoSaveSaveError`. Sustituir la fila atenuada por `AutoSaveRow` con `toggleable` y `SwitchOn` por `SettingsSwitch(checked)`. Mostrar el aviso en `SettingsRoute`. Eliminar `settings_auto_save_on`.
  - **Cubre:** RF-1 a RF-3, RF-7, RF-8, RF-38
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las previews muestran el interruptor apagado en ambos temas.

- [x] **T08. Pruebas de Ajustes** (`test-writer`). Ampliar `SettingsViewModelTest`, `SettingsScreenTest` y `SettingsRouteTest`. La fila deja de tratarse como atenuada.
  - **Cubre:** RF-1 a RF-3, RF-7, RF-8, RF-38
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 4 — Editor: modo manual sin imágenes (`:presentation`)

- [x] **T09. Modo del editor.** Inyectar `GetAutoSaveEnabledUseCase`, leerlo al abrir junto con la nota o el color predeterminado, y añadir `isManualSave`. Con el ajuste encendido, nada cambia.
  - **Cubre:** RF-9, RF-35
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila y las pruebas existentes del editor pasan sin cambiar lo que comprueban.

- [x] **T10. Cambios pendientes y guardar.** En modo manual, `edit` y `saveNow` no guardan. Recalcular `hasUnsavedChanges` y `SaveStatus.Unsaved` tras cada cambio. Añadir `SaveClicked` con su manejo de errores.
  - **Cubre:** RF-11, RF-12, RF-14 a RF-19, RF-28, RF-29
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T11. Salir y diálogo.** Añadir `isLeaveDialogVisible` y los intents del diálogo. Adaptar `leave()` y `delete()` al modo manual. Crear `LeaveNoteDialog` y el botón Guardar en la barra superior. Añadir los textos nuevos.
  - **Cubre:** RF-10, RF-13, RF-20 a RF-27, RF-30, RF-36, RF-37
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las previews muestran botón, estado y diálogo en ambos temas.

- [x] **T12. Pruebas del modo manual** (`test-writer`). Ampliar `NoteEditorViewModelTest` con ambos modos en cada intent que guarda, y `NoteEditorScreenTest` con el botón, el estado y el diálogo.
  - **Cubre:** RF-9 a RF-30, RF-35 a RF-37
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 5 — Editor: imágenes y borrador (`:presentation`)

- [x] **T13. Imágenes pendientes.** En modo manual, usar `ImportNoteImagesUseCase` al agregar, ocultar al quitar, enviar ambas listas a `SaveNoteChangesUseCase`, descartar copias al salir o eliminar, y llamar a `CleanUpImageFilesUseCase` al terminar la sesión.
  - **Cubre:** RF-12, RF-16, RF-24, D3 del plan
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T14. Borrador.** Crear `EditorDraft` y `EditorDraftCodec`. Guardarlo en el `SavedStateHandle` en cada cambio del modo manual, aplicarlo al recrear el ViewModel y borrarlo al guardar o descartar. Medir el tamaño con una nota de 100 000 caracteres.
  - **Cubre:** RF-31 a RF-34
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y el tamaño medido queda anotado en el plan.

- [x] **T15. Pruebas de imágenes y borrador** (`test-writer`). Ampliar `NoteEditorViewModelTest` con imágenes pendientes, descarte, limpieza y recuperación del borrador. Probar `EditorDraftCodec` ida y vuelta.
  - **Cubre:** RF-12, RF-16, RF-24, RF-31 a RF-34
  - **Hecho cuando:** `./gradlew :presentation:test` pasa.

## Fase 6 — Documentación y cierre

- [x] **T16. Actualizar la spec 006.** Sacar esta fila de RF-9 y de «Fuera de alcance», y hacer que RF-13 y CL-10 remitan a la 009.
  - **Cubre:** principio 2
  - **Hecho cuando:** la spec 006 no contradice a la 009.

- [x] **T17. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest`.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T18. Demo manual en el emulador.** En ambos temas: con el ajuste encendido, el editor guarda igual que antes; apagado, editar texto, color, tareas e imágenes y guardar; probar Guardar, Descartar y Cancelar del diálogo; forzar el cierre del proceso en segundo plano y volver; cerrar desde recientes; cerrar la app del todo y reabrirla.
  - **Cubre:** RF-9, RF-20 a RF-24, RF-31 a RF-34, casos límite, criterios de finalización
  - **Hecho cuando:** todo se comporta según la spec.

- [x] **T19. Revisar botón, estado y diálogo** (tú). Dar el visto bueno al diseño propuesto, que no tiene frame.
  - **Cubre:** requisito de aspecto, criterios de finalización
  - **Hecho cuando:** das el visto bueno o las diferencias quedan resueltas.

- [x] **T20. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T21. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas. Corregir que la spec 008 ya está en `main`.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja las specs 008 y 009.

- [ ] **T22. Commit y PR** (con tu confirmación). Marcar spec, plan y tareas como implementados. Hacer el commit con Conventional Commits y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
