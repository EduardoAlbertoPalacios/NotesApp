# Tareas 012 — Papelera de notas eliminadas

> **Estado:** implementadas · **Fecha:** 2026-11-17 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

**Paralelismo posible:** una vez terminada T03, T04 (pruebas de dominio) puede correr en paralelo con T05 a T07 (`:data`). Una vez terminada T08, T09 a T12 tocan archivos distintos y se pueden repartir entre agentes.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/trash` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/012-trash/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/trash` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Modelos y reglas.** Crear en `notes/trash`:
  - `TrashedNote`;
  - `TrashRetention`, con `days` y `DEFAULT = DAYS_30`;
  - `TrashRules`, con `isExpired`, `daysLeft`, `expiredBy` e `isShorter` (D3).
  - **Cubre:** RF-16, RF-37, RF-38, RF-42, RF-45, RF-48, RF-50
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T03. Contratos y casos de uso.**
  - **`NoteRepository`:** añadir `moveToTrash`, `restoreFromTrash`, `observeTrash` y `deleteNotes`, y documentar que `observeNotes` excluye la papelera.
  - **`SettingsRepository`:** añadir `observeTrashRetention` y `setTrashRetention`.
  - **Casos de uso:** `MoveNoteToTrashUseCase`, `RestoreFromTrashUseCase`, `GetTrashUseCase`, `EmptyTrashUseCase`, `PurgeExpiredTrashUseCase`, `GetTrashRetentionUseCase`, `DeleteTrashedNotesUseCase` y `ChangeTrashRetentionUseCase`, este con `Applied` / `NeedsConfirmation(count)` / `Failed`.
  - **Fakes de prueba:** actualizar los que ya implementan las interfaces para que compilen.
  - **Cubre:** RF-1, RF-4, RF-6, RF-14, RF-22, RF-32, RF-35, RF-38, RF-40 a RF-47, RF-49, RF-50
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` y `./gradlew :domain:compileTestKotlin` compilan.

- [x] **T04. Pruebas de dominio** (`test-writer`).
  - **`TrashRulesTest`:**
    - bordes de 24 h;
    - `NEVER`;
    - reloj atrasado;
    - `expiredBy`;
    - `isShorter` con `NEVER`.
  - **Pruebas de cada caso de uso con fakes:**
    - mover con la hora del `TimeProvider`;
    - orden de la papelera y vencidas ocultas;
    - vaciar y purgar, que borran también las imágenes;
    - cambiar el plazo: más largo, más corto sin notas afectadas, con confirmación pendiente, confirmado, y error al guardar sin borrar nada.
  - **Cubre:** RF-16, RF-40 a RF-46, RF-48 a RF-50
  - **Hecho cuando:** `./gradlew :domain:test` pasa.

## Fase 2 — Datos (`:data`)

- [x] **T05. Esquema y migración.**
  - `NoteEntity` con `trashed_at` nula.
  - `NotesDatabase` en la versión 7, con `AutoMigration(6, 7)`.
  - Generar y añadir `schemas/…/7.json`.
  - **Cubre:** RF-6, RNF datos
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila y existe `7.json`.

- [x] **T06. DAO y repositorio de notas.**
  - **`observeAll`:** filtrar con `trashed_at IS NULL`.
  - **Consultas nuevas:** `observeTrashed`, `setTrashedAt` y `deleteByIds`.
  - **`update` (D1):** usar una entidad parcial sin `trashed_at`.
  - **`NoteRepositoryImpl`:** implementar los métodos nuevos y añadir el mapper a `TrashedNote`.
  - **Cubre:** RF-1, RF-4, RF-6, RF-7, RF-22, RF-32
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T07. Ajuste del plazo.** En `SettingsRepositoryImpl`, añadir la clave `trash_retention`, el mapper y los métodos `observe`/`set` con el mismo contrato que los demás ajustes.
  - **Cubre:** RF-38, RF-46, RF-47
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T08. Pruebas de datos** (`test-writer`).
  - **`NoteDaoTest`:**
    - `observeAll` sin la papelera;
    - `observeTrashed`;
    - mover y restaurar conservan `updated_at` y las imágenes;
    - un `update` no saca la nota de la papelera;
    - `deleteByIds` borra las notas y sus filas de imágenes.
  - **`MigrationTest`:** de 6 a 7.
  - **Pruebas del repositorio de ajustes:** plazo guardado, leído y por defecto.
  - **Cubre:** RF-6, RF-7, RF-47, D1, RNF datos
  - **Hecho cuando:** `./gradlew :data:test` y `./gradlew :data:connectedDebugAndroidTest` pasan en el emulador.

## Fase 3 — Presentación (`:presentation` y `:app`)

- [x] **T09. Componentes comunes (D6).**
  - Mover `SettingsOptionSheet` a `presentation/common`.
  - Extraer el diálogo de eliminar del editor como `ConfirmDialog` común.
  - No cambiar el aspecto ni el comportamiento.
  - **Cubre:** D6
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas de UI de Ajustes y del editor siguen pasando.

- [x] **T10. Editor y Mis notas.**
  - **Editor:**
    - quitar el diálogo de eliminar y sus textos;
    - `DeleteClicked` → `MoveNoteToTrashUseCase`;
    - si va bien, efecto `MovedToTrash(id)`, que se devuelve a Mis notas por el `SavedStateHandle`;
    - si falla, el aviso actual.
  - **Mis notas:**
    - `NoteMovedToTrash(id)` → efecto → aviso con «Deshacer», que sustituye al anterior;
    - `UndoMoveToTrashClicked(id)` → `RestoreFromTrashUseCase`, y su error.
  - **`NotesNavHost`:** clave del resultado.
  - **Cubre:** RF-1 a RF-5, RF-8
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T11. Pantalla Papelera.**
  - **MVI:** crear `TrashUiState`, `TrashIntent`, `TrashEffect`, `TrashViewModel`, `TrashRoute` y `TrashScreen`.
  - **Contenido:**
    - barra superior con el botón del plazo;
    - cabecera con «Vaciar papelera»;
    - `TrashNoteCard` (D5);
    - hoja de acciones y hoja del plazo;
    - tres diálogos;
    - avisos de lista vacía y de error de carga.
  - **Calidad:** `testTag`, etiquetas accesibles y previews en ambos temas.
  - **Textos:** los nuevos, con sus `plurals`.
  - **Cubre:** RF-13 a RF-46
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las previews se ven en ambos temas.

- [x] **T12. Ajustes, navegación y arranque.**
  - **Fila Papelera:** habilitada, con «N notas» o «Vacía», `TrashRowClicked` y `NavigateToTrash`.
  - **`NotesNavHost`:** ruta `settings/trash`.
  - **`AppStartupViewModel`:** en el paquete `startup`, crearlo y usarlo en `MainActivity` (D4).
  - **Cubre:** RF-9 a RF-12, RF-49
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

- [x] **T13. Pruebas de presentación** (`test-writer`).
  - **`TrashViewModelTest`:**
    - carga y error;
    - hoja de acciones;
    - restaurar;
    - eliminar, confirmado y cancelado;
    - vaciar;
    - plazo aplicado y con confirmación;
    - errores.
  - **`NoteEditorViewModelTest`:** eliminar sin diálogo, `MovedToTrash`, error y cambios pendientes descartados.
  - **`NoteListViewModelTest`:** aviso, Deshacer y error al deshacer.
  - **`SettingsViewModelTest`:** número de notas y navegación.
  - **`AppStartupViewModelTest`:** purga una sola vez.
  - **`TrashScreenTest`:** en ambos temas.
  - **Pruebas actualizadas:** ampliar `SettingsScreenTest` y ajustar `NoteEditorScreenTest`.
  - **Cubre:** RF-1 a RF-46, RF-49
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan en el emulador.

## Fase 4 — Documentación y cierre

- [x] **T14. Actualizar las specs.**
  - **Spec 006:** RF-9, RF-14, RF-15 y «Fuera de alcance».
  - **Spec 009:** caso límite de eliminar con cambios sin guardar.
  - **Spec 011:** línea de «Fuera de alcance».
  - **Cubre:** principio 2
  - **Hecho cuando:** ninguna spec contradice a la 012 ni al código.

- [x] **T15. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest`, en el emulador y nunca en el teléfono real.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T16. Demo manual en el emulador.** En ambos temas:
  - eliminar una nota y deshacer;
  - restaurar, eliminar definitivamente y vaciar, cancelando cada diálogo una vez;
  - cambiar el plazo, incluido acortarlo con notas afectadas;
  - girar con las hojas y los diálogos abiertos;
  - adelantar la hora del emulador y comprobar que la nota vencida desaparece y se borra al abrir la app.
  - **Cubre:** RF-1 a RF-50, riesgos del plan
  - **Hecho cuando:** todo se comporta según la spec y revisas conmigo la pantalla Papelera.

- [x] **T17. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T18. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja la spec 012.

- [x] **T19. Commit y PR** (con tu confirmación).
  - Marcar spec, plan y tareas como implementados.
  - Hacer el commit con Conventional Commits.
  - Subir la rama y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
