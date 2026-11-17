# Plan 012 — Papelera de notas eliminadas

> **Estado:** implementado · **Fecha:** 2026-11-17 · **Spec:** [spec.md](spec.md) (aprobada) · **Diseño:** sin diseño en Figma; la papelera usa los componentes y el tema existentes

Plan técnico para implementar la spec 012. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

Una nota eliminada deja de borrarse: se marca con la **fecha en que entró en la papelera**, en una columna nueva de la tabla `notes`. Con esa marca:

- Mis notas, la búsqueda, los filtros, el contador y Exportar dejan de verla, porque todos leen de la misma consulta, que pasa a excluir las notas de la papelera [RF-7].
- Restaurar o deshacer solo quita la marca, así que la nota vuelve exactamente como estaba, con su fecha de edición [RF-4, RF-22].
- Las imágenes de una nota en la papelera siguen asociadas a ella, así que la limpieza de imágenes huérfanas no las toca. Se borran solo al eliminarla definitivamente [RF-26, RF-32].

El plazo (7, 30, 90 días o «Nunca») se guarda en DataStore, como los demás ajustes. Las reglas puras (si una nota ha vencido, cuántos días le quedan) viven en `:domain`.

Hay una pantalla nueva, **Papelera**, a la que se entra desde Ajustes. El editor deja de mostrar el diálogo de eliminar y Mis notas muestra el aviso con «Deshacer».

**Sin dependencias nuevas.** El esquema de Room pasa de la versión 6 a la 7.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Solo Room, DataStore y Compose, que ya están. |
| 2. La spec manda | Cada parte cita sus RF. El mismo commit actualiza las specs 006, 009 y 011 (sección 4.6). |
| 3. Lógica fuera de la UI | Vencimiento, días restantes, notas afectadas por un plazo más corto y borrado automático se deciden en `:domain`. La pantalla solo pinta el estado. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, de DAO, de migración, de ajustes, de ViewModels y de UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | Versión 7 con una columna nueva nula, esquema exportado y prueba de migración: las notas existentes quedan fuera de la papelera. Nada se borra sin confirmación, salvo el borrado automático por plazo. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:domain` | Paquete `notes/trash`: `TrashedNote`, `TrashRetention`, `TrashRules` y casos de uso. Métodos nuevos en `NoteRepository` y `SettingsRepository`. | RF-1, RF-4, RF-6, RF-7, RF-14, RF-16, RF-22, RF-37 a RF-50 |
| `:data` | Columna `trashed_at`, migración 6 → 7, consultas del DAO, repositorio y clave de DataStore del plazo. | RF-6, RF-7, RF-26, RF-32, RF-47 |
| `:presentation` | Editor sin diálogo, aviso con «Deshacer» en Mis notas, pantalla `notes/trash`, fila de Ajustes, navegación y `AppStartupViewModel`. | RF-1 a RF-5, RF-8 a RF-46, RF-49 |
| `:app` | `MainActivity` crea `AppStartupViewModel`. | RF-49 |
| Documentación | Specs 006, 009 y 011, y `.memory/MEMORY.md`. | — |

## 4. Diseño por capa

### 4.1 `:domain`

**Modelos** (paquete `notes.trash`):
- **`TrashEntry(trashed, daysLeft)`**: una nota tal como la muestra la papelera.
- **`TrashedNote(note: Note, trashedAt: Long)`**: una nota de la papelera. `Note` no cambia, así que el resto de la app no tiene que saber que existe la papelera.
- **`TrashRetention`** (`enum`): `DAYS_7`, `DAYS_30`, `DAYS_90` y `NEVER`, cada uno con `days: Int?`. `DEFAULT = DAYS_30` [RF-37, RF-38].
- **`TrashRules`** (`object`, reglas puras):
  - `isExpired(trashedAt, retention, now)`: si lleva al menos `days × 24 h` en la papelera; con `NEVER`, nunca [RF-48, RF-50].
  - `daysLeft(trashedAt, retention, now): Int?`: los días enteros que faltan para vencer, entre 0 y `days`. Nulo con `NEVER` [RF-16]. El tope en `days` cubre el caso límite del reloj atrasado.
  - `expiredBy(notes, retention, now)`: las notas que vencerían con un plazo dado [RF-42].
  - `isShorter(new, current)`: `NEVER` cuenta como el plazo más largo [RF-40 a RF-42].

**`NoteRepository`**, métodos nuevos:
- `moveToTrash(id, trashedAt)` [RF-1, RF-6];
- `restoreFromTrash(id)`: quita la marca sin tocar `updatedAt` [RF-4, RF-22];
- `observeTrash(): Flow<AppResult<List<TrashedNote>, NoteError>>`;
- `deleteNotes(ids): AppResult<Unit, NoteError>`: borra varias en una sola transacción [RF-32, RF-43].

`observeNotes()` mantiene su firma, pero deja de devolver las notas de la papelera [RF-7].

**`SettingsRepository`**: `observeTrashRetention(): Flow<TrashRetention?>` y `setTrashRetention(retention)`, con el mismo contrato que los demás ajustes [RF-46, RF-47].

**Casos de uso** (paquete `notes.trash`):

| Caso de uso | Qué hace | RF |
|---|---|---|
| `MoveNoteToTrashUseCase(id)` | `moveToTrash(id, timeProvider.now())` | RF-1, RF-6 |
| `RestoreFromTrashUseCase(id)` | `restoreFromTrash(id)`; lo usan Deshacer y Restaurar | RF-4, RF-22 |
| `GetTrashUseCase()` | Combina `observeTrash` con el plazo. Quita las vencidas, ordena por `trashedAt` descendente y devuelve cada nota con sus `daysLeft` | RF-14, RF-16, RF-48 |
| `DeleteTrashedNotesUseCase(notes)` | Borra esas notas en una transacción y, solo si fue bien, sus imágenes; lo comparten los tres siguientes | RF-26, RF-32, RF-43, RF-49 |
| `EmptyTrashUseCase()` | Borra todas las notas de la papelera en una transacción y después sus imágenes | RF-32, RF-35 |
| `PurgeExpiredTrashUseCase()` | Borra las vencidas con el plazo vigente, y sus imágenes | RF-49, RF-50 |
| `GetTrashRetentionUseCase()` | Plazo vigente, o `DEFAULT` | RF-37, RF-38, RF-47 |
| `ChangeTrashRetentionUseCase(retention, confirmed)` | Si el plazo es más corto, hay notas que vencerían y no está confirmado, devuelve `NeedsConfirmation(count)` sin cambiar nada. Si no, guarda el plazo y, solo si se guardó, purga las vencidas. Devuelve `Failed` si no se pudo guardar, o si no se pudo leer la papelera al acortar el plazo, sin borrar nada | RF-40 a RF-46 |

**Borrado definitivo de una nota:** se reutiliza `DeleteNoteUseCase`, que ya borra la nota y sus imágenes [RF-26]. El editor lo sigue usando para descartar una nota que quedó vacía, y en ese caso no pasa por la papelera.

### 4.2 `:data`

- **`NoteEntity`:** la columna `trashed_at INTEGER` es nula si la nota no está en la papelera. Base en la versión 7 con `AutoMigration(from = 6, to = 7)`, porque solo se añade una columna nula, como en las versiones 2 y 4. Se exporta el esquema `7.json` [RF-6, RNF datos].
- **`NoteDao`:**
  - `observeAll()` añade `WHERE trashed_at IS NULL` [RF-7];
  - nuevas `observeTrashed()` (`trashed_at IS NOT NULL`), `setTrashedAt(id, trashedAt: Long?)` y `deleteByIds(ids)`;
  - **los guardados no tocan `trashed_at` (D1):** `update` pasa a `@Update(entity = NoteEntity::class)` con una clase parcial sin esa columna. Así, si un autoguardado del editor llega después de mover la nota a la papelera, no la saca de ella.
- **`getImagePaths()`** sigue leyendo todas las imágenes, incluidas las de la papelera. Por eso `CleanUpImageFilesUseCase` no las borra.
- **`SettingsRepositoryImpl`:** clave `trash_retention` (`stringPreferencesKey`) y mapper `TrashRetention` ↔ nombre, como `NotesView`.

### 4.3 `:presentation`

**Editor** [RF-1, RF-2, RF-5, RF-8]:
- Se quitan `isDeleteDialogVisible`, `DeleteDismissed`, `DeleteConfirmed` y `DeleteNoteDialog`. `DeleteClicked` llama directamente a `MoveNoteToTrashUseCase`.
- La espera del autoguardado y de la importación de imágenes, y el descarte de los cambios pendientes, siguen como hoy. Por eso a la papelera va la última versión guardada [RF-8].
- Si va bien, emite `NoteEditorEffect.MovedToTrash(id)`. La ruta guarda el id en el `SavedStateHandle` de Mis notas y vuelve atrás, con el mismo patrón que Exportar → Ajustes.
- Si falla, muestra el aviso que ya existe [RF-5].
- Una nota nueva que nunca se guardó se descarta como hoy, sin aviso.
- Se quitan los textos del diálogo de eliminar, que ya no se usan.

**Mis notas** [RF-3, RF-4]:
- `NoteListRoute` recibe `trashedNoteId: Long?` y `onTrashResultShown`, y envía `NoteListIntent.NoteMovedToTrash(id)`.
- El ViewModel emite `ShowMovedToTrash(id)`. La ruta cierra el aviso anterior y muestra «Nota movida a la papelera» con la acción «Deshacer» y duración corta. Si se pulsa la acción, envía `UndoMoveToTrashClicked(id)`, que llama a `RestoreFromTrashUseCase`. Si falla, muestra «No se pudo restaurar la nota».

**Arranque** (paquete `startup`) [RF-49, D4]:
- `AppStartupViewModel` (`@HiltViewModel`) llama una vez a `PurgeExpiredTrashUseCase` en su `init`, en `viewModelScope`.
- No expone estado ni efectos. Sobrevive al giro, así que la purga se hace una vez por cada apertura de la app, entre por la pantalla que entre.

**Papelera** (paquete `notes.trash`, MVI completo):

| Clase | Contenido |
|---|---|
| `TrashUiState` | `isLoading`, `loadFailed`, `notes: List<TrashItemUi(id, title, excerpt, color, daysLeft)>`, `retention`, `actionsNoteId`, `pendingDeleteId`, `isEmptyConfirmVisible`, `isRetentionSheetVisible`, `pendingRetention: PendingRetention(retention, count)?`; derivados `isEmpty` y `canEmpty` |
| `TrashIntent` | `NoteClicked(id)`, `ActionsDismissed`, `RestoreClicked`, `DeleteForeverClicked`, `DeleteForeverConfirmed`, `DeleteForeverDismissed`, `EmptyClicked`, `EmptyConfirmed`, `EmptyDismissed`, `RetentionClicked`, `RetentionSelected(retention)`, `RetentionConfirmed`, `RetentionDismissed`, `BackClicked` |
| `TrashEffect` | `ShowMessage(@StringRes)`, `NavigateBack` |
| `TrashViewModel` | Observa `GetTrashUseCase` y `GetTrashRetentionUseCase`. Cada acción llama a su caso de uso y convierte el error en su aviso |
| `TrashRoute` / `TrashScreen` | `BackHandler`, aviso con `SnackbarHost` y pantalla sin estado |

Las hojas y los diálogos se controlan desde el `UiState`, así que sobreviven al giro (caso límite).

**La pantalla:**
- **Barra superior:** como la de Ajustes, con Volver, el título «Papelera» y un botón de icono «Vaciar automáticamente» que siempre está visible [RF-13, RF-36].
- **Cabecera:** «N notas» y el botón de texto «Vaciar papelera», como la cabecera de Exportar. El botón solo aparece si hay notas [RF-29, RF-30].
- **Lista:** `LazyColumn` con `key = id` y tarjetas `TrashNoteCard` [RF-14 a RF-17]:
  - el color de la nota, con `NoteColorExt` y `NoteSurfaceTheme`;
  - el título o «Sin título»;
  - un extracto de tres líneas;
  - «Se eliminará en N días», «mañana» u «hoy»;
  - sin el indicador de fijada.
- **Hoja de acciones:** «Restaurar» y «Eliminar definitivamente» al tocar una nota [RF-20].
- **Hoja del plazo:** `SettingsOptionSheet`, que pasa de `settings` a un paquete común `presentation/common`, con los cuatro plazos [RF-39].
- **Diálogos:** eliminar definitivamente, vaciar y acortar el plazo, con el estilo de `DeleteNoteDialog` extraído a un `ConfirmDialog` común [RF-25, RF-31, RF-42].
- **Avisos de lista:** «La papelera está vacía» y «No se pudo cargar la papelera», con el componente de mensaje de Exportar [RF-18, RF-19].

**Ajustes** [RF-9 a RF-12]:
- La fila Papelera pasa de `DisabledRow` a una fila con acción.
- Su descripción es «N notas» (`plurals`) o «Vacía». La da `SettingsViewModel` observando `GetTrashUseCase`.
- Se añaden `SettingsIntent.TrashRowClicked` y `SettingsEffect.NavigateToTrash`, y `NotesNavHost` añade la ruta `settings/trash`.

**Textos nuevos** (los `plurals` con `one`, `many` y `other`, como en la 011):
- Avisos: «Nota movida a la papelera», «Deshacer», «Nota restaurada», «No se pudo restaurar la nota», «No se pudo eliminar la nota», «No se pudo vaciar la papelera», «No se pudo guardar el plazo».
- Pantalla: «Papelera», «Vaciar papelera», «Vaciar automáticamente», «La papelera está vacía», «No se pudo cargar la papelera», «Sin título», «Restaurar», «Eliminar definitivamente».
- Días restantes: «Se eliminará en N días», «Se eliminará mañana», «Se eliminará hoy».
- Plazos: «7 días», «30 días», «90 días», «Nunca».
- Diálogos: los títulos, los mensajes con plural y los botones «Eliminar», «Vaciar» y «Cancelar».
- Fila de Ajustes: «N notas» y «Vacía».

### 4.4 `:app`

`MainActivity` obtiene `AppStartupViewModel` con `by viewModels()` y lo usa en `onCreate` para crearlo, como ya hace con `AppThemeViewModel`. `:app` solo conoce el ViewModel, no el caso de uso, así que se respeta la regla de `CLAUDE.md` [RF-49].

### 4.5 Flujos

```
Editor ─ Eliminar ─▶ moveToTrash ─▶ Mis notas + «Nota movida a la papelera · Deshacer»
                                        └─ Deshacer ─▶ restoreFromTrash

Ajustes ─ Papelera ─▶ Papelera ─ nota ─▶ hoja ─┬─ Restaurar ─▶ restoreFromTrash + «Nota restaurada»
                                                └─ Eliminar definitivamente ─▶ diálogo ─▶ DeleteNoteUseCase
                       ├─ Vaciar papelera ─▶ diálogo ─▶ EmptyTrashUseCase
                       └─ Vaciar automáticamente ─▶ hoja ─▶ ChangeTrashRetentionUseCase
                                                             ├─ Applied
                                                             └─ NeedsConfirmation(n) ─▶ diálogo ─▶ confirmed = true

Abrir la app (MainActivity) ─▶ AppStartupViewModel ─▶ PurgeExpiredTrashUseCase
```

### 4.6 Documentación

- **Spec 006:** la RF-9 deja de incluir Papelera; las RF-14 y RF-15 pasan a «sustituida por la spec 012»; la línea de «Fuera de alcance» remite a la 012.
- **Spec 009:** el caso límite «Eliminar una nota con cambios sin guardar» remite a la RF-8 de la 012.
- **Spec 011:** la línea de «Fuera de alcance» sobre Papelera remite a la 012.
- **`.memory/MEMORY.md`:** estado y decisiones.

## 5. Decisiones técnicas

- **D1. Los guardados no tocan la marca de papelera.** **Aprobado** el 2026-11-17. `@Update` con una clase parcial sin `trashed_at`. Sin esto, un autoguardado que llegue tarde reescribiría la fila con `trashed_at = NULL` y sacaría la nota de la papelera. **Alternativa:** añadir `trashedAt` a `Note`, que obligaría a propagarlo por toda la app.
- **D2. Una columna, no una tabla aparte.** **Aprobado** el 2026-11-17. Mover a la papelera es una actualización de una fila. Las imágenes no se mueven, y restaurar conserva el id, la fecha y todo lo demás. **Alternativa:** una tabla `trashed_notes` con copia de los datos. Duplica el esquema y complica restaurar.
- **D3. Días contados en bloques de 24 h desde la entrada.** **Aprobado** el 2026-11-17. No se usan días de calendario. Es simple, no depende de la zona horaria y coincide con «lleva más de N días» (RF-42). **Consecuencia:** una nota que entra a las 23:00 con el plazo de 7 días vence a las 23:00 del séptimo día.
- **D4. El borrado automático se hace al abrir la app, con `AppStartupViewModel`.** **Aprobado** el 2026-11-17. Lo crea `MainActivity`, así que se ejecuta una vez por apertura, entre por la pantalla que entre, incluso si el proceso se restaura en el editor o en Ajustes. Mis notas no se ocupa de la papelera. No se usa WorkManager: sería una dependencia nueva y la spec deja fuera el borrado con la app cerrada. Mientras tanto, `GetTrashUseCase` ya no muestra las notas vencidas [RF-48]. **Alternativa descartada:** purgar en el `init` del ViewModel de Mis notas, que mezcla responsabilidades y se retrasa si el proceso se restaura en otra pantalla.
- **D5. Tarjeta propia en la papelera.** **Aprobado** el 2026-11-17. `NoteCard` es privada de `NoteListScreen` y muestra la fecha de edición y el indicador de fijada, que aquí no aplican. `TrashNoteCard` reutiliza los colores y la forma del tema, pero no extrae ni cambia la tarjeta de Mis notas.
- **D6. Componentes comunes extraídos.** **Aprobado** el 2026-11-17. `SettingsOptionSheet` y el diálogo de confirmación del editor pasan a `presentation/common` para reutilizarlos tal cual. Es el único cambio en código ajeno a la papelera, y no cambia su comportamiento.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:domain` | `TrashRulesTest` | Vencimiento y días restantes en los bordes de 24 h, `NEVER`, reloj atrasado, `expiredBy`, `isShorter` [RF-16, RF-42, RF-45, RF-48] |
| `:domain` | Pruebas de los casos de uso con repositorios fake | Mover con la hora, restaurar, `GetTrashUseCase` (orden, vencidas ocultas, días), vaciar, purgar, cambiar el plazo (confirmación, error al guardar sin borrar nada) |
| `:data` (instrumentada) | `NoteDaoTest` | `observeAll` sin papelera; `observeTrashed`; mover y restaurar conservan `updated_at`; un `update` no saca de la papelera (D1); `deleteByIds` |
| `:data` (instrumentada) | `MigrationTest` 6 → 7 | Las notas existentes conservan sus datos y quedan fuera de la papelera |
| `:data` | `SettingsRepositoryImpl` | Plazo guardado, leído y por defecto |
| `:presentation` (unitarias) | `TrashViewModelTest` | Carga, error, hoja de acciones, restaurar, eliminar con confirmación y cancelada, vaciar, plazo aplicado o con confirmación, errores |
| `:presentation` (unitarias) | `NoteEditorViewModelTest` | Eliminar sin diálogo, `MovedToTrash`, error, cambios pendientes descartados |
| `:presentation` (unitarias) | `NoteListViewModelTest` | Aviso, Deshacer, error al deshacer |
| `:presentation` (unitarias) | `AppStartupViewModelTest` | Purga una sola vez al crearse |
| `:presentation` (unitarias) | `SettingsViewModelTest` | Número de notas en la fila y navegación |
| `:presentation` (UI) | `TrashScreenTest`, parametrizada en ambos temas | Lista, días, vacía, error, hoja, diálogos, «Vaciar papelera» oculto, botón del plazo |
| `:presentation` (UI) | `SettingsScreenTest`, `NoteEditorScreenTest` | Fila habilitada con su descripción; el editor ya no muestra el diálogo |
| Manual | Emulador | Los flujos de la sección 4.5, el giro con hojas y diálogos abiertos, y una nota vencida (cambiando la hora del emulador) borrada al abrir la app, en ambos temas |

## 7. Orden de implementación

1. Crear la rama `feat/trash`.
2. `:domain`: modelos, reglas, métodos de repositorio y casos de uso, con pruebas.
3. `:data`: columna, migración, DAO, repositorio y ajuste, con pruebas.
4. `:presentation`:
   1. extraer los componentes comunes (D6);
   2. el editor y Mis notas;
   3. la pantalla Papelera;
   4. Ajustes, la navegación y `AppStartupViewModel` con su uso en `MainActivity`;
   5. las pruebas.
5. Actualizar las specs 006, 009 y 011.
6. Ejecutar `./gradlew test` y las pruebas instrumentadas en el emulador, hacer la demo manual y pasar `architecture-reviewer`.
7. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1, RF-2, RF-5, RF-6, RF-8 | `NoteEditorViewModel`, `MoveNoteToTrashUseCase`, `NoteDao.setTrashedAt` |
| RF-3, RF-4 | `NoteListViewModel`, `NoteListRoute`, `RestoreFromTrashUseCase` |
| RF-7 | `NoteDao.observeAll` |
| RF-9 a RF-13 | Fila de Ajustes, `SettingsViewModel`, navegación, `TrashRoute` |
| RF-14 a RF-21 | `GetTrashUseCase`, `TrashRules.daysLeft`, `TrashScreen` |
| RF-22 a RF-28 | `TrashViewModel`, `RestoreFromTrashUseCase`, `DeleteNoteUseCase` |
| RF-29 a RF-35 | `TrashViewModel`, `EmptyTrashUseCase`, `NoteDao.deleteByIds` |
| RF-36 a RF-47 | `TrashRetention`, `ChangeTrashRetentionUseCase`, `SettingsRepository`, hoja del plazo |
| RF-48 a RF-50 | `TrashRules`, `GetTrashUseCase`, `PurgeExpiredTrashUseCase` en `AppStartupViewModel` |

## 9. Riesgos

- **El aviso con «Deshacer» se pierde al girar**, porque el `SnackbarHostState` vive en la ruta. La nota no se pierde: sigue en la papelera.
- **Los días no se actualizan mientras la papelera está abierta:** se calculan al cargar y en cada cambio de la lista. Si se deja abierta toda la noche, un «hoy» podría haber vencido ya. Se borrará la siguiente vez que se abra la app.
- **Vaciar con muchas imágenes:** la transacción borra las filas y después los archivos uno a uno. Si la app se cierra a mitad, quedan archivos huérfanos, que `CleanUpImageFilesUseCase` borra la próxima vez que se cierre el editor.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
