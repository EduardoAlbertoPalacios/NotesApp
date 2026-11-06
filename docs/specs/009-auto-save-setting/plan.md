# Plan 009 — Guardado automático activable

> **Estado:** implementado · **Fecha:** 2026-11-06 · **Spec:** [spec.md](spec.md) (implementada) · **Diseño:** Figma Ajustes `4:1794` y `40:1211`; botón, estado y diálogo sin frame

Plan técnico para implementar la spec 009. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

El ajuste se guarda igual que el tema, el color y la vista: con DataStore Preferences, detrás de `SettingsRepository`.

El editor lee el ajuste al abrir la nota y trabaja en uno de dos modos durante toda la sesión:

- **Automático:** el código actual, sin cambios [RF-9, RF-10].
- **Manual:** ninguna acción escribe en la base. Los cambios viven en el estado del ViewModel hasta pulsar Guardar o elegirlo en el diálogo [RF-11 a RF-30].

Lo delicado del modo manual son las imágenes. El selector de fotos solo da acceso temporal, así que las imágenes agregadas se copian al momento, pero no se asocian a la nota hasta guardar. Las que se quitan solo se ocultan, y su archivo se borra al guardar. Descartar borra las copias pendientes.

Los cambios pendientes sobreviven a la muerte del proceso en el `SavedStateHandle` del editor. Android lo descarta cuando el usuario cierra la app desde recientes, que es justo lo que piden RF-32 y RF-34.

No hay cambios en el esquema de Room, dependencias nuevas ni cambios en `:app` o `:common`.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas. |
| 2. La spec manda | Cada parte cita sus RF. El mismo commit actualiza la spec 006 (sección 4.5). |
| 3. Lógica fuera de la UI | Guardar los cambios con imágenes, descartar copias y limpiar archivos huérfanos son casos de uso de `:domain`. Los composables solo pintan estado y emiten intents. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, repositorio, mappers, ViewModels y UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | El esquema no cambia. Con el ajuste encendido el guardado es el de siempre. Con el ajuste apagado nada se borra hasta guardar [RF-24]. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:domain` | Ajuste en `SettingsRepository` con sus dos casos de uso; casos de uso de imágenes pendientes; dos métodos nuevos para limpiar huérfanas. | RF-4 a RF-7, RF-16, RF-24 |
| `:data` | Ajuste en `SettingsRepositoryImpl` y mappers; listado de rutas en `NoteRepositoryImpl` e `ImageStorageImpl`. | RF-4 a RF-7 |
| `:presentation` | Fila y efecto en Ajustes; modo manual, botón, estado, diálogo y borrador en el editor; textos. | RF-1 a RF-3, RF-8 a RF-38 |
| Documentación | Spec 006 y `.memory/MEMORY.md`. | — |

## 4. Diseño por capa

### 4.1 `:domain`

**Ajuste (paquete `settings`)**

- `SettingsRepository` añade `observeAutoSaveEnabled(): Flow<Boolean?>` y `setAutoSaveEnabled(enabled: Boolean): AppResult<Unit, SettingsError>`.
- `GetAutoSaveEnabledUseCase`: convierte `null` en `true` [RF-5, RF-6].
- `SetAutoSaveEnabledUseCase`: delega en el repositorio [RF-4].

**Imágenes pendientes (paquete `notes`)**

| Caso de uso | Qué hace | RF |
|---|---|---|
| `ImportNoteImagesUseCase(uris)` | Copia las imágenes con `ImageStorage.save` y devuelve las rutas y cuántas fallaron, sin tocar la base | RF-11, RF-12 |
| `SaveNoteChangesUseCase(note, addedPaths, removedImages)` | Guarda la nota con la fecha actual, asocia las rutas nuevas y quita las imágenes eliminadas con `NoteRepository.saveNoteChanges`, en una sola transacción. Solo si va bien, borra los archivos quitados. Si la nota queda vacía, contando sus imágenes, devuelve `EmptyNote` | RF-16, RF-17 |
| `DiscardImageCopiesUseCase(paths)` | Borra las copias pendientes | RF-24 |
| `CleanUpImageFilesUseCase()` | Borra los archivos de imágenes que ninguna nota usa | — |

`AddNoteImagesUseCase` y `RemoveNoteImageUseCase` siguen igual: los usa el modo automático.

`NoteRepository` añade `saveNoteChanges(note, addedPaths, removedImageIds)` y `getImagePaths()`, e `ImageStorage` añade `listPaths()`.

**Ajuste al implementar:** el guardado con imágenes iba a componerse en el caso de uso con los métodos actuales. Si fallaba a medias, una nota nueva quedaba creada sin que el editor lo supiera, y un reintento la duplicaba. Por eso es una sola transacción de Room (`NoteDao.saveChanges`).

### 4.2 `:data`

- **Ajuste:** clave `auto_save_enabled` como booleano, con su propio `MutableStateFlow` de lo elegido en la sesión, igual que los otros tres ajustes [RF-4 a RF-7]. Una lectura fallida da `null`.
- **`NoteRepositoryImpl.saveNoteChanges()`:** llama a `NoteDao.saveChanges`, un método `@Transaction` que inserta o actualiza la nota, borra y añade imágenes. Devuelve la nota leída de nuevo.
- **`NoteRepositoryImpl.getImagePaths()`:** consulta nueva en `NoteDao` sobre `note_images`, sin cambiar el esquema.
- **`ImageStorageImpl.listPaths()`:** lista los archivos de `filesDir/note_images`.

### 4.3 `:presentation`: Ajustes

| Clase | Cambios |
|---|---|
| `SettingsUiState` | `isAutoSaveEnabled: Boolean = true` |
| `SettingsIntent` | `AutoSaveToggled` |
| `SettingsEffect` | `ShowAutoSaveSaveError` [RF-8] |
| `SettingsViewModel` | Observa el ajuste y guarda el valor contrario al tocar [RF-2, RF-3] |
| `SettingsScreen` | `AutoSaveRow` sustituye a la fila atenuada. Usa `toggleable(role = Role.Switch)` [RF-1, RF-38]. `SwitchOn` pasa a `SettingsSwitch(checked)`: apagado, el círculo va a la izquierda sobre `surfaceVariant` |

Se elimina `settings_auto_save_on`, que ya no hace falta, porque el lector anuncia el estado del interruptor.

### 4.4 `:presentation`: editor

**Estado**

| Clase | Cambios |
|---|---|
| `NoteEditorUiState` | `isManualSave: Boolean`, `hasUnsavedChanges: Boolean`, `isLeaveDialogVisible: Boolean`. `SaveStatus` añade `Unsaved` |
| `NoteEditorIntent` | `SaveClicked`, `LeaveSaveClicked`, `LeaveDiscardClicked`, `LeaveDialogDismissed` |

**ViewModel**

- **Apertura:** recibe `GetAutoSaveEnabledUseCase` y lee el primer valor junto con la nota, o con el color predeterminado si es nueva. `isLoading` no termina hasta tener ambos, y el modo no cambia durante la sesión [RF-35].
- **Modo manual:**
  - `edit` y `saveNow` actualizan el estado sin lanzar ningún guardado [RF-11, RF-12].
  - `addImages` llama a `ImportNoteImagesUseCase` y añade las rutas a `pendingImagePaths`.
  - `removeImage` mueve la imagen a `removedImages`, o borra la copia si era pendiente.
  - `state.images` muestra las de la nota menos las quitadas, más las pendientes.
- **Cambios pendientes:** tras cada cambio se recalcula `hasUnsavedChanges`: la comparación actual con `note` más las listas de imágenes. Así, deshacer o volver a dejar algo como estaba deshabilita Guardar [RF-14]. `saveStatus` pasa a `Unsaved` cuando hay cambios [RF-15].
- **Guardar** (`SaveClicked`): llama a `SaveNoteChangesUseCase` dentro de `saveMutex` y vacía las listas. La lógica de `restoreNote` al deshacer hasta la apertura se mantiene. El historial de Deshacer no se toca [RF-16 a RF-19]. Si falla, se muestra el aviso actual y los cambios siguen pendientes [RF-28, RF-29].
- **Salir:**
  - `BackClicked` con cambios pendientes muestra el diálogo [RF-20], y sin cambios vuelve [RF-26].
  - Una nota nueva sin título, contenido ni imágenes nunca tiene cambios pendientes [RF-27].
  - `LeaveSaveClicked` guarda y vuelve solo si va bien [RF-21, RF-22, RF-30].
  - `LeaveDiscardClicked` llama a `DiscardImageCopiesUseCase` y vuelve [RF-23, RF-24].
  - `LeaveDialogDismissed` cierra el diálogo [RF-25].
- **Eliminar:** con cambios pendientes, descarta las copias antes de borrar.
- **Limpieza:** al terminar la sesión, sea guardando, descartando o eliminando, se llama a `CleanUpImageFilesUseCase` (D3).

**Borrador ante la muerte del proceso**

- En modo manual, cada cambio guarda en el `SavedStateHandle` un `EditorDraft`: título, contenido, tramos, tareas, fijación, categoría, color, rutas pendientes e ids quitados.
- `EditorDraftCodec` lo convierte a tipos de `Bundle` (cadenas, listas y arreglos de enteros), porque los modelos de `:domain` no son `Parcelable`.
- Al recrear el ViewModel con borrador, se carga la nota y se aplica el borrador encima. Los cambios quedan pendientes y el estado muestra `Unsaved` [RF-31 a RF-33]. El historial de Deshacer no se recupera, como ya dice la spec 005.
- Al guardar o descartar, el borrador se borra.

**Pantalla**

- **Botón Guardar:** se muestra en la barra superior, junto a Fijar, si `isManualSave` es verdadero. Es un `TextButton` con el estilo de la barra, se deshabilita sin cambios pendientes y anuncia su estado [RF-10, RF-13, RF-14, RF-36, RF-37]. Mide al menos 44 dp.
- **Línea de estado:** añade el texto de `Unsaved` [RF-15].
- **Diálogo de salida:** `LeaveNoteDialog`, con el estilo de `DeleteNoteDialog` y tres botones, sobrevive al giro porque su visibilidad vive en el estado [RF-20].
- **Textos nuevos:**
  - `note_editor_save` («Guardar»).
  - `note_editor_unsaved` («Cambios sin guardar»).
  - `note_editor_leave_title` («¿Guardar los cambios?»), `note_editor_leave_message` («Si sales sin guardar, perderás los cambios.»).
  - `note_editor_leave_save`, `note_editor_leave_discard` y `note_editor_leave_cancel`.
  - `settings_auto_save_error`.

### 4.5 Documentación

- **Spec 006:** RF-9, RF-13, CL-10 y la línea de «Fuera de alcance» remiten a la spec 009.
- **`.memory/MEMORY.md`:** estado y decisiones. También se corrige que la spec 008 ya está en `main`.

## 5. Decisiones técnicas

- **D1. Dos modos dentro del mismo ViewModel.** El modo automático no cambia ninguna ruta de código, así que las pruebas actuales del editor siguen sirviendo para RF-9. El modo manual se decide en los puntos donde hoy se guarda.
- **D2. Borrador en `SavedStateHandle`.** Cumple RF-32 y RF-34 sin tablas nuevas ni migración, porque Android conserva ese estado ante la muerte del proceso y lo descarta al cerrar desde recientes. Un archivo propio necesitaría además detectar ese cierre.
- **D3. Limpieza de huérfanas al terminar la sesión.** Si el usuario cierra desde recientes con imágenes pendientes, sus copias quedan sin nota. Solo hay un editor abierto a la vez, así que al terminar una sesión ningún archivo pendiente debe sobrevivir. Limpiar al abrir la app podría borrar las copias de un borrador que se está recuperando.
- **D4. Copiar las imágenes al agregarlas.** El acceso a la imagen original es temporal, así que esperar a guardar podría perderla.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:domain` | Casos de uso del ajuste | RF-4 a RF-6, propagación del error |
| `:domain` | `ImportNoteImagesUseCase`, `SaveNoteChangesUseCase`, `DiscardImageCopiesUseCase`, `CleanUpImageFilesUseCase` | Copia sin base; guardado con altas y bajas; nota vacía; fallos parciales; borrado de copias; limpieza que respeta las imágenes usadas |
| `:data` | `SettingsRepositoryImplTest`, mappers, `NoteRepositoryImplTest` | RF-4 a RF-7; `getImagePaths` |
| `:data` (instrumentada) | `NoteDao`, `ImageStorageImpl` | Consulta de rutas; listado de archivos |
| `:presentation` (unitarias) | `SettingsViewModelTest` | RF-2, RF-3, RF-7, RF-8 |
| `:presentation` (unitarias) | `NoteEditorViewModelTest` | Modo automático sin cambios (RF-9); RF-11 a RF-35 en modo manual; `EditorDraftCodec` ida y vuelta |
| `:presentation` (UI) | `SettingsScreenTest`, `SettingsRouteTest` | RF-1 a RF-3, RF-8, RF-38 |
| `:presentation` (UI) | `NoteEditorScreenTest` | RF-10, RF-13 a RF-15, RF-20, RF-25, RF-36, RF-37, en ambos temas |
| Manual | Emulador | Criterios de finalización de la spec; RF-32 con «No conservar actividades» o `adb shell am kill` |

## 7. Orden de implementación

1. Crear la rama `feat/auto-save-setting`.
2. `:domain`: ajuste y casos de uso de imágenes, con pruebas.
3. `:data`: ajuste, rutas de imágenes y listado de archivos, con pruebas.
4. Ajustes: estado, ViewModel, fila e interruptor, con pruebas.
5. Editor: lectura del modo; modo manual sin imágenes; botón, estado y diálogo, con pruebas.
6. Editor: imágenes pendientes, limpieza y borrador, con pruebas.
7. Actualizar la spec 006.
8. `./gradlew test`, pruebas instrumentadas, demo manual en ambos temas y revisión con `architecture-reviewer`.
9. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1 a RF-3, RF-38 | `AutoSaveRow`, `SettingsSwitch`, `SettingsViewModel` |
| RF-4 a RF-8 | `SettingsRepositoryImpl`, casos de uso, `SettingsViewModel`, `SettingsRoute` |
| RF-9, RF-10 | Modo automático del editor, sin cambios |
| RF-11 a RF-19 | Modo manual de `NoteEditorViewModel`, `SaveNoteChangesUseCase`, botón Guardar |
| RF-20 a RF-27 | `leave()`, `LeaveNoteDialog`, `DiscardImageCopiesUseCase` |
| RF-28 a RF-30 | Guardado manual con error |
| RF-31 a RF-34 | `EditorDraft` en `SavedStateHandle` |
| RF-35 | Lectura del modo al abrir la nota |
| RF-36, RF-37 | Botón Guardar |

## 9. Riesgos

- **RF-27 con imágenes:** resuelto con el usuario. RF-27 solo aplica a notas sin título, contenido ni imágenes, y la spec ya lo dice.
- **Tamaño del borrador:** medido al implementar. Un borrador de 100 000 caracteres con 500 tramos y 500 tareas ocupa 210 500 bytes en el `Bundle`, alrededor del 20 % del límite de ~1 MB, así que no hace falta omitirlo. Una prueba instrumentada lo vigila.
- **Complejidad del editor:** `NoteEditorViewModel` ya es grande, y el modo manual añade otra rama en cada punto de guardado. Se mitiga con pruebas de ambos modos en cada intent que guarda.
- **Sin diseño:** el botón y el diálogo se construyen con el estilo actual, sin frame. Se revisan contigo en la demo.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
