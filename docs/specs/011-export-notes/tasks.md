# Tareas 011 — Exportar notas

> **Estado:** implementadas · **Fecha:** 2026-11-12 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/export-notes` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/011-export-notes/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/export-notes` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Reglas de nombres.** Crear `NoteExportNaming` con `archiveName` y `pdfNames`, según la sección 4.1 del plan.
  - **Cubre:** RF-14, RF-27 a RF-31
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T03. Contrato y casos de uso.** Crear `NotesExporter`, `PreparedExport`, `ExportError` y los casos de uso `PrepareNotesExportUseCase`, `SaveNotesExportUseCase`, `DiscardNotesExportUseCase`, `DiscardExportDestinationUseCase` y `GetExportArchiveNameUseCase`.
  - **Cubre:** RF-14, RF-15, RF-23
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T04. Pruebas de dominio** (`test-writer`). `NoteExportNamingTest`: sin título, caracteres no válidos, «///», 100 caracteres con emoji, tildes, repetidos con y sin mayúsculas, fecha del .zip. Pruebas de los casos de uso con `FakeNotesExporter`.
  - **Cubre:** RF-14, RF-27 a RF-31
  - **Hecho cuando:** `./gradlew :domain:test` pasa.

## Fase 2 — Datos (`:data`)

- [x] **T05. Textos de `:data`.** Crear `data/src/main/res/values/strings.xml` con «Sin título», «Trabajo» y «Personal» (D2).
  - **Cubre:** RF-28, RF-34
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T06. Dibujo del PDF.** En `PdfNotesExporter`, dibujar cada nota en páginas A4: encabezado, categoría, fecha «12 nov 2026, 10:30», contenido con formato, tareas con «☐ »/«☑ », paginado e imágenes escaladas con rotación EXIF.
  - **Cubre:** RF-32 a RF-44
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T07. .zip, copia y limpieza.** Completar `prepare` (temporal en `cacheDir/exports`, una entrada por nota), `save` (copia; si falla o se cancela, borra el destino), `discard` y `discardDestination`. Añadir el `@Binds` en `RepositoryModule`.
  - **Cubre:** RF-15, RF-23, RF-26
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T08. Pruebas de datos** (`test-writer`). `PdfNotesExporterTest`, instrumentada:
  - nombres de las entradas del .zip;
  - tamaño A4 y número de páginas, abriendo los PDF con `PdfRenderer`;
  - texto largo en varias páginas e imagen alta en una sola;
  - imagen que falta contada;
  - copia a un destino, error que borra el destino, y `discard`.
  - **Cubre:** RF-15, RF-23, RF-40, RF-41, RF-43, RF-44
  - **Hecho cuando:** `./gradlew :data:test` y `./gradlew :data:connectedDebugAndroidTest` pasan.

## Fase 3 — Pantalla de exportación (`:presentation`)

- [x] **T09. Estado, intent, efecto y ViewModel.** Crear `ExportNotesUiState`, `ExportNotesIntent`, `ExportNotesEffect` y `ExportNotesViewModel`:
  - selección, que se ajusta cuando cambia la lista;
  - preparar y después abrir el selector (D1);
  - selector cancelado, éxito y error;
  - URI sin exportación preparada;
  - cancelación al salir.
  - **Cubre:** RF-4, RF-6 a RF-26
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T10. Pantalla y ruta.** Crear `ExportNotesScreen` con los componentes de Ajustes, con su `testTag`, etiquetas accesibles y previews en ambos temas. Crear `ExportNotesRoute` con `CreateDocument("application/zip")` y `BackHandler`. Añadir los textos nuevos, con «N notas exportadas» como `plurals` (D3).
  - **Cubre:** RF-3 a RF-13, RF-18, RF-19, RF-46, RF-47
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las previews se ven en ambos temas.

- [x] **T11. Ajustes y navegación.** Habilitar la fila Exportar notas, con `ExportRowClicked` y `NavigateToExport`. Añadir la ruta `settings/export` a `NotesNavHost`. Devolver el resultado por el `SavedStateHandle` de Ajustes y mostrar allí los avisos.
  - **Cubre:** RF-1, RF-2, RF-20, RF-21, RF-45
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

- [x] **T12. Pruebas de presentación** (`test-writer`).
  - `ExportNotesViewModelTest`: selección, todas y ninguna, nota que desaparece, `canExport`, progreso, selector cancelado, éxito, error, URI sin exportación preparada, que no modifica notas.
  - `SettingsViewModelTest`: la fila emite `NavigateToExport`.
  - `ExportNotesScreenTest`, en ambos temas.
  - Ampliar `SettingsScreenTest` con la fila habilitada.
  - **Cubre:** RF-1 a RF-26, RF-46, RF-47
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 4 — Documentación y cierre

- [x] **T13. Actualizar las specs.**
  - Spec 006: RF-9 y «Fuera de alcance».
  - Spec 010: la línea «Exportar todas las notas».
  - Spec 011: RF-13, RF-18 y RF-19 según D1, y RF-20 con el plural según D3.
  - **Cubre:** principio 2
  - **Hecho cuando:** ninguna spec contradice a la 011 ni al código.

- [x] **T14. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest`.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T15. Demo manual en el emulador.** En ambos temas:
  - exportar varias notas con formato, tareas, categoría e imágenes a Descargas, y abrir los PDF;
  - títulos repetidos y una nota sin título;
  - cancelar el selector;
  - girar la pantalla durante la exportación;
  - comprobar que las casillas «☐»/«☑» se ven en el PDF.
  - **Cubre:** RF-13 a RF-26, RF-32 a RF-45, riesgos del plan
  - **Hecho cuando:** todo se comporta según la spec y revisas conmigo la pantalla de selección.

- [x] **T16. Comparar con Figma.** Comparar la fila Exportar notas de Ajustes, ya habilitada, con su diseño.
  - **Cubre:** requisito de aspecto
  - **Hecho cuando:** no quedan diferencias o están resueltas contigo. Resuelta contigo: la fila reutiliza el componente de las demás filas activas de Ajustes y se dio por buena con la captura de la demo.

- [x] **T17. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T18. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja la spec 011.

- [x] **T19. Commit y PR** (con tu confirmación). Marcar spec, plan y tareas como implementados. Hacer el commit con Conventional Commits y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
