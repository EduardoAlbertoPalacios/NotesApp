# Plan 005 — Deshacer en el editor

> **Estado:** implementado · **Fecha:** 2026-10-29 · **Spec:** [spec.md](spec.md) (implementada) · **Diseño:** Figma `4:1656` (barra del editor, botón «Deshacer»)

Plan técnico para implementar la spec 005. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

El historial guarda **fotos completas** del texto de la nota: título, contenido, formato, tareas y dónde estaba el cursor. Cada paso guarda la foto de antes de empezar. Deshacer saca la última y la vuelve a poner en el editor.

Las reglas son funciones puras en `:domain`, en un `object` sin estado, `NoteUndo`, como `NoteFormatting` y `NoteChecklist`. Esas reglas deciden:
- qué tipo de cambio es cada edición;
- cuándo se une al paso abierto y cuándo empieza uno nuevo;
- el límite de 100 pasos.

El historial vive en el `NoteEditorViewModel`: sobrevive al giro de pantalla y desaparece al salir del editor, sin guardarse en la base. La pausa de 1 segundo la marca un `delay` del ViewModel, como el autoguardado, así que `:domain` no maneja tiempo.

Para RF-34 hace falta guardar una nota **sin cambiar su fecha**. Lo hace un caso de uso nuevo, `RestoreNoteUseCase`, porque `SaveNoteUseCase` siempre pone la fecha actual.

No hay cambios en `:data` ni en el esquema de Room, ni dependencias nuevas.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas. |
| 2. La spec manda | Cada parte cita sus RF. Si algo se aparta de la spec, se actualizan ambas en el mismo commit. |
| 3. Lógica fuera de la UI | Clasificar cambios, agrupar pasos, el límite y deshacer están en `NoteUndo` (`:domain`). La pantalla solo pinta el botón, emite `UndoClicked` y pone el foco donde dice el estado. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, caso de uso, ViewModel y UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | El esquema no cambia. Deshacer guarda como cualquier edición. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español. No hay textos nuevos: `note_editor_toolbar_undo` ya existe. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:common` | Ninguno. `TimeProvider` ya existe. | — |
| `:domain` | `NoteUndo` con sus modelos (`UndoHistory`, `OpenStep`, `UndoSnapshot`, `UndoEdit`, `EditField`, `EditKind`) y `RestoreNoteUseCase`. | RF-5 a RF-25, RF-29, RF-34 |
| `:data` | Ninguno. | — |
| `:presentation` | Editor: título como `TextFieldValue`, estado, intent, ViewModel, foco al deshacer y botón de la barra. | RF-1 a RF-4, RF-11, RF-12, RF-17 a RF-19, RF-21 a RF-28, RF-30 a RF-34 |
| `:app` | Ninguno. | — |
| Documentación | `CLAUDE.md`: se añade `NoteUndo` a la lista de la excepción de objetos de reglas puras (ver D6). | — |

## 4. Diseño por capa

### 4.1 `:domain`

**Modelos** (paquete `notes`):
- `enum class EditField { TITLE, CONTENT }`.
- `data class UndoSnapshot(title, content, contentSpans, contentTasks, field: EditField, selectionStart: Int, selectionEnd: Int)`: la nota y el cursor antes de un paso. `field` es el campo que cambia el paso, y la selección es la que tenía ese campo antes del paso [RF-26].
- `enum class EditKind { TYPING, DELETING, STANDALONE }`: escribir, borrar, o un paso propio.
- `data class UndoEdit(kind: EditKind, closesStep: Boolean)`: un cambio ya clasificado; `closesStep` cierra el paso tras unirse a él (espacio o `\n`).
- `data class UndoHistory(val steps: List<UndoSnapshot> = emptyList(), val open: OpenStep? = null)`, con `canUndo` = `steps.isNotEmpty()`. `OpenStep(kind, field)` es el paso de escritura o borrado que todavía admite más caracteres.

**`NoteUndo`** (`object` público, sin estado; devuelve historiales nuevos):

| Función | Qué hace | RF |
|---|---|---|
| `classify(old, new): UndoEdit` | Clasifica un cambio de texto. Un carácter insertado y nada borrado es `TYPING`, y cierra el paso si es espacio o `\n`. Un carácter borrado y nada insertado es `DELETING`. Cualquier otro cambio es `STANDALONE`: pegar, cortar, sustituir una selección, borrar una selección, autocorrección del teclado, o texto igual con tareas distintas (Intro en tarea vacía, borrar al inicio de una tarea). | RF-5 a RF-7, RF-13 a RF-16, CL-3 a CL-6, CL-14 |
| `record(history, before, edit): UndoHistory` | `TYPING` o `DELETING` con un paso abierto del mismo tipo y campo se une a él sin guardar nada. Si no, guarda `before` como paso nuevo. `STANDALONE` siempre es un paso nuevo y queda cerrado. Un `TYPING` que cierra (espacio o `\n`) deja el paso cerrado después de unirse. Si hay más de 100 pasos, quita el más antiguo. | RF-7 a RF-10, RF-13 a RF-16, RF-18, RF-20, RF-29 |
| `close(history): UndoHistory` | Cierra el paso abierto, por pausa o por mover el cursor. | RF-17, RF-19 |
| `undo(history): Pair<UndoHistory, UndoSnapshot>?` | Saca el último paso y lo devuelve; cierra el paso abierto. Nulo si no hay pasos. | RF-21 a RF-25 |

`MAX_STEPS = 100`.

La diferencia entre textos se calcula con prefijo y sufijo comunes, como en `NoteFormatting.onTextChanged`. Se escribe en `NoteUndo` sin tocar los otros objetos (D5).

**`RestoreNoteUseCase`** [RF-34]: guarda la nota tal cual, **conservando su `updatedAt`**. Como `SaveNoteUseCase`, rechaza la nota vacía con `NoteError.EmptyNote` y devuelve la nota con su id.

### 4.2 `:presentation` — Editor

**`NoteEditorUiState`**:
- `title` pasa de `String` a `TextFieldValue`, como `content`, para conocer y restaurar el cursor del título [RF-19, RF-26]. Quien lee el texto usa `title.text`, por ejemplo `NoteColorSheet` y los borradores.
- `canUndo: Boolean` [RF-2, RF-4].
- `focusRequest: FocusRequest?`, con `FocusRequest(field: EditField, id: Long)`: pide el foco en un campo tras deshacer. El `id` sale de un contador del ViewModel que crece en cada deshacer, para que dos peticiones seguidas al mismo campo vuelvan a dispararse [RF-26]. Vuelve a `null` cuando la pantalla la atiende (`FocusRequestHandled`).

**`NoteEditorIntent`**:
- `TitleChanged(title: TextFieldValue)`, antes `String`.
- `UndoClicked` (nuevo) [RF-21].
- `FocusRequestHandled(id)` (nuevo): la pantalla ya movió el foco; si `id` es el de la petición actual, el ViewModel pone `focusRequest = null`. **Ajuste de la revisión (T19):** sin él, la petición se quedaba en el estado y, al girar la pantalla, el foco volvía a saltar al campo del último deshacer.

**`NoteEditorViewModel`** guarda `private var history = UndoHistory()` y `private var openedNote: Note?`, la nota tal como se cargó; es nula en una nota nueva.
- **Registro** (una función `recordStep(before, edit)` que actualiza `history` y `canUndo`):
  - `TitleChanged` y `ContentChanged` con texto distinto: se clasifica el cambio final, ya aplicadas las reglas de tareas, y se registra con la foto anterior [RF-5, RF-6, RF-13 a RF-16].
  - `TitleChanged` y `ContentChanged` con el mismo texto y otra selección: `NoteUndo.close` [RF-19].
  - Formato con selección, botón «Lista de tareas» si cambian las tareas, y `TaskToggled`: `STANDALONE` en el campo `CONTENT`, con la selección del contenido, mediante el ayudante `recordStandalone(before)` [RF-8 a RF-10, CL-7].
  - Formato sin selección, color, categoría, fijar e imágenes: no registran nada [RF-11, RF-12].
  - **Pausa:** cada `TYPING` o `DELETING` reinicia `undoPauseJob`, un `delay(UNDO_PAUSE_MS = 1000)` que llama a `NoteUndo.close` [RF-17].
- **`UndoClicked`** [RF-21 a RF-28]:
  1. `NoteUndo.undo`.
  2. Pone en el estado título, contenido, tramos y tareas de la foto, con la selección en el campo de la foto. El otro campo se queda como está (`restoredTo`); si su texto fuera distinto, su cursor iría al final.
  3. Los formatos activos salen de `NoteFormatting.activeFormats` en el cursor restaurado [RF-27].
  4. Emite un `focusRequest` nuevo, con el siguiente `id` del contador.
  5. No toca color, categoría, fijación ni imágenes [RF-28].
  6. Guarda con `edit {}`, que dispara el autoguardado [RF-32, RF-33].
- **Fecha original** [RF-34, CL-11, CL-15]: deshacer marca `lastEditWasUndo = true`; cualquier otra edición o `saveNow` la vuelve a `false`. En `save()`, si `lastEditWasUndo` y el estado coincide con `openedNote` en título, contenido, tramos, tareas, fijación, categoría, color e imágenes, se llama a `restoreNote(openedNote)` en lugar de `saveNote`. La fecha visible vuelve a la de `openedNote`.
- **Girar y salir** [RF-30, RF-31]: el ViewModel sobrevive al giro con su historial. Al salir, la entrada de navegación se destruye con su ViewModel, y al reabrir la nota empieza vacío [RF-3].

**`NoteEditorScreen`**:
- **Botón «Deshacer»:** `ToolbarButton(ic_undo, note_editor_toolbar_undo)` con `onClick` solo si `state.canUndo`, de modo que sin pasos sale deshabilitado, y sin depender del foco. Lleva el `testTag` nuevo `TOOLBAR_UNDO` [RF-1, RF-2, RF-4, RNF-1, RNF-3].
- **Título:** `EditorTextField` recibe `TextFieldValue`.
- **Foco:** el contenido usa un `FocusRequester`, como el título. Un `LaunchedEffect(state.focusRequest)` pide el foco al campo indicado y después emite `FocusRequestHandled(id)`, para que la petición no se repita al recrear la pantalla [RF-26].

## 5. Decisiones técnicas

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D1 | **Fotos completas en lugar de diferencias** | Deshacer es poner una foto: no hay que invertir reglas de tareas ni de formato. Con 100 pasos y 10 000 caracteres son unos 2 MB en el peor caso, y deshacer es una asignación [RNF-2]. | **Guardar cada cambio y su inverso**: habría que invertir cada regla de `NoteChecklist` y `NoteFormatting`, con más casos y más riesgo de restaurar mal. |
| D2 | **Historial en el ViewModel, no en `SavedStateHandle`** | Cumple RF-30 (giro) y RF-31 (salir). La muerte del proceso está fuera de alcance, y 2 MB no caben en el `Bundle`. | **`SavedStateHandle`**: límite de tamaño del `Bundle` y nada que lo pida en la spec. |
| D3 | **Pausa con `delay` en el ViewModel** | Igual que el autoguardado; se prueba con tiempo virtual y `:domain` sigue sin reloj. | **Marcas de tiempo en `NoteUndo`**: obligan a inyectar un reloj en cada edición. |
| D4 | **Clasificar por la diferencia de texto** | El campo solo informa del texto nuevo. Un carácter es escribir o borrar, y lo demás es un paso propio; cubre pegar, cortar, autocorrección y teclado por deslizamiento sin saber qué tecla se pulsó. | **Eventos de teclado** (`onKeyEvent`): no llegan con todos los teclados de software y no ven pegar. |
| D5 | **Diferencia de texto propia en `NoteUndo`** | Cumple «haz solo lo que se pidió»: no se tocan `NoteFormatting` ni `NoteChecklist`. Se anota al final como posible mejora compartirla. | **Extraer un helper común**: refactoriza código ajeno a la tarea. |
| D6 | **`NoteUndo` usado directamente por el ViewModel** | Mismo criterio que `NoteFormatting` y `NoteChecklist`: `object` sin estado con funciones puras. El historial es un valor inmutable que guarda el ViewModel. Se añade a la lista de la excepción en `CLAUDE.md`. | **Un caso de uso por función**: clases que solo delegan. |
| D7 | **`RestoreNoteUseCase` para la fecha original** | Una clase por acción. `SaveNoteUseCase` sigue igual: guardar pone la fecha actual. | **Parámetro `keepDate` en `SaveNoteUseCase`**: mezcla dos acciones en una. |
| D8 | **Título como `TextFieldValue`** | RF-19 y RF-26 necesitan el cursor del título. | **Seguir con `String`**: no se podría restaurar ni detectar el cursor en el título. |
| D9 | **Foco por estado con `id`, marcado como atendido** | La pantalla sigue sin estado, se prueba con un `UiState` fijo y la petición se repite aunque sea al mismo campo. Al atenderla, la pantalla emite `FocusRequestHandled` y el estado vuelve a `null`, así que girar la pantalla no repite el foco. | **Efecto `FocusField`**: los `FocusRequester` viven en la pantalla, no en la `Route`. |

## 6. Estrategia de pruebas

Todas las escribe el agente **`test-writer`**. Se ejecutan con `./gradlew test` y con `connectedDebugAndroidTest`.

### 6.1 Unitarias en `:domain`

**`NoteUndoTest`:**
- `classify`:
  - un carácter escrito, un espacio y un `\n` (estos dos cierran);
  - un carácter borrado;
  - pegar, sustituir una selección y borrar una selección;
  - texto igual con tareas distintas [RF-5 a RF-7, RF-15, RF-16, CL-3 a CL-6, CL-14].
- `record`:
  - «Hola mundo» da dos pasos [CL-1];
  - escribir y borrar seguidos dan dos pasos [RF-18];
  - un cambio en otro campo empieza paso [CL-9];
  - un `STANDALONE` cierra el paso abierto [RF-20];
  - con 101 pasos se descarta el primero [RF-29, CL-16].
- `close` corta el paso [RF-17, RF-19].
- `undo` devuelve los pasos del más reciente al más antiguo y es nulo sin pasos [RF-24, RF-25].

**`RestoreNoteUseCaseTest`:** conserva `updatedAt`, rechaza la nota vacía y devuelve el id [RF-34].

### 6.2 Unitarias en `:presentation`

**`NoteEditorViewModelTest`, que se amplía** (y se adapta a `TitleChanged(TextFieldValue)`):
- `canUndo` es falso al abrir una nota nueva y una existente [RF-2, RF-3].
- Escribir, pausar 1 segundo con tiempo virtual y seguir escribiendo da dos pasos [RF-17, CL-2].
- Mover el cursor cierra el paso [RF-19].
- Formato con selección, botón de tareas y `TaskToggled` se deshacen [RF-8 a RF-10, RF-22, RF-23].
- Formato sin selección, color, categoría y fijar no se registran [RF-11, RF-12, CL-8].
- Deshacer:
  - restaura el título y el contenido con su selección y su campo [RF-21, RF-26, CL-7];
  - restaura los formatos activos [RF-27, CL-12];
  - deja el color [RF-28];
  - guarda con fecha nueva [RF-32, RF-33].
- Deshacer hasta el estado de apertura restaura la fecha original; con el color cambiado, no [RF-34, CL-11, CL-15].
- Deshacer todo en una nota nueva deja `canUndo` falso [CL-10].
- Intro en tarea vacía y borrar al inicio de una tarea se deshacen [CL-5, CL-6].

### 6.3 UI en `:presentation`

**`NoteEditorScreenTest`, que se amplía:**
- El botón está deshabilitado con `canUndo` falso y habilitado con `canUndo` verdadero, con el foco en el título y en el contenido [RF-1, RF-2, RF-4].
- Tocarlo emite `UndoClicked` [RF-21].
- Su etiqueta accesible es «Deshacer» [RNF-3].
- Un `focusRequest` al contenido o al título deja el foco en ese campo [RF-26].

### 6.4 Verificación manual en el emulador
Los casos de los criterios de finalización de la spec:
- escribir varias palabras y deshacerlas una a una;
- pegar y deshacer;
- negrita en una selección y deshacer;
- tareas con Intro, marcar una y deshacer;
- girar y seguir deshaciendo;
- salir y reabrir con el botón deshabilitado;
- deshacer hasta el estado de apertura y ver la fecha original en Mis notas.

Además, se comprueba **RNF-2** con una nota de 10 000 caracteres.

## 7. Orden de implementación

1. **`:domain`:** modelos, `NoteUndo` y `RestoreNoteUseCase`, con sus pruebas.
2. **`:presentation`, título como `TextFieldValue`:** estado, intent, ViewModel y pantalla, con las pruebas existentes adaptadas. Sin cambio de comportamiento.
3. **`:presentation`, estado del editor:** historial, `UndoClicked`, pausa, foco y fecha original en el ViewModel, con sus pruebas.
4. **`:presentation`, interfaz:** botón «Deshacer» y foco, con sus pruebas de UI.
5. **Documentación:** excepción de `CLAUDE.md` (D6).
6. **Verificación:**
   - `./gradlew test`, `connectedDebugAndroidTest`, `assembleDebug` y `lint`;
   - revisión con `architecture-reviewer`;
   - pruebas manuales de 6.4.
7. **Cierre:** actualizar `.memory/MEMORY.md` y marcar spec y plan como implementados.

## 8. Trazabilidad de requisitos

| RF / RNF | Dónde se implementa | Dónde se prueba |
|---|---|---|
| RF-1, RF-2, RF-4 Botón | 4.2 barra y `canUndo` | 6.2, 6.3 |
| RF-3 Historial vacío al abrir | 4.2 ViewModel | 6.2 |
| RF-5 a RF-12 Qué entra | 4.1 `classify` y `record` · 4.2 registro | 6.1, 6.2 |
| RF-13 a RF-20 Agrupación | 4.1 `record` y `close` · 4.2 pausa y cursor | 6.1, 6.2 |
| RF-21 a RF-25 Deshacer | 4.1 `undo` · 4.2 `UndoClicked` | 6.1, 6.2, 6.3 |
| RF-26 Cursor y campo | 4.1 `UndoSnapshot` · 4.2 `focusRequest` · D8, D9 | 6.2, 6.3 |
| RF-27 Formatos activos | 4.2 `UndoClicked` | 6.2 |
| RF-28 Resto de la nota | 4.2 `UndoClicked` | 6.2 |
| RF-29 Límite de 100 | 4.1 `MAX_STEPS` | 6.1 |
| RF-30, RF-31 Duración | 4.2 · D2 | 6.4 |
| RF-32, RF-33 Guardado | 4.2 `edit {}` | 6.2 |
| RF-34 Fecha original | 4.1 `RestoreNoteUseCase` · 4.2 `save()` · D7 | 6.1, 6.2, 6.4 |
| RNF-1 Aspecto | 4.2 `ToolbarButton` | 6.4 |
| RNF-2 Respuesta | D1 | 6.4 |
| RNF-3 Accesibilidad | 4.2 `ToolbarButton` | 6.3 |
| RNF-4 Sin persistencia | D2 | 6.2 |
| RNF-5 Idioma | Sin textos nuevos | — |

## 9. Riesgos

- **Teclados con texto en composición.** Algunos teclados envían la palabra entera al confirmar una sugerencia, y eso es un paso propio (D4). Se comprueba a mano con el teclado del emulador que escribir letra a letra se agrupa por palabra.
- **Cambios del campo tras deshacer.** Al poner un `TextFieldValue` nuevo, el campo puede devolver un `onValueChange` con el mismo texto. Solo cierra el paso si la selección cambió, y no registra nada, pero se vigila en las pruebas de ViewModel.
- **Fecha original y autoguardado.** Si el usuario edita dentro de los 500 ms del autoguardado tras deshacer, `lastEditWasUndo` vuelve a `false` y se guarda con fecha nueva, que es lo correcto. Se cubre con tiempo virtual en 6.2.
