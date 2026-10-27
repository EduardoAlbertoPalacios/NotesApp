# Plan 004 — Lista de tareas en las notas

> **Estado:** implementado · **Fecha:** 2026-10-26 · **Spec:** [spec.md](spec.md) (implementada) · **Diseño:** Figma `4:1683` («Tareas», dentro del editor `4:1656`)

Plan técnico para implementar la spec 004. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

El contenido **sigue siendo texto plano**, como en la spec 002. Al lado se guarda qué **líneas** son tareas y si están hechas: una lista de `ChecklistItem(line, isDone)`, donde `line` es el índice de la línea (separada por `\n`) dentro del contenido. Las reglas viven en `:domain`, en un `object` puro `NoteChecklist`, como `NoteFormatting`:
- convertir líneas;
- desplazar índices al editar;
- interpretar Intro, borrar y pegar;
- leer los «☐»/«☑» antiguos.

En el editor, el mismo `BasicTextField` del contenido deja hueco a la izquierda de cada tarea con un `ParagraphStyle` (sangría) dentro de la `VisualTransformation` que ya pinta el formato. Las casillas se dibujan encima, en la posición de cada línea, gracias a `onTextLayout`. La cuadrícula usa `InlineTextContent`.

Room pasa de la versión 5 a la 6 con una **migración manual**. Agrega la columna de tareas y convierte los «☐»/«☑» de las notas existentes, ajustando también los tramos de formato. No se agregan dependencias.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas. Codificación propia de las tareas, como la de los tramos. Casillas dibujadas con Compose. |
| 2. La spec manda | Cada parte cita sus RF. Si algo se aparta de la spec, se actualizan ambas en el mismo commit. |
| 3. Lógica fuera de la UI | Convertir líneas, Intro, borrar, pegar, desplazar índices y leer «☐»/«☑» están en `NoteChecklist` (`:domain`). La pantalla solo dibuja casillas y emite intents. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, codificación, mapper, migración, DAO, ViewModel y UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | Migración v5→v6 probada con notas reales con «☐»/«☑» y formato. Esquema 6 exportado. Nada de `fallbackToDestructiveMigration`. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:common` | Ninguno. | — |
| `:domain` | `ChecklistItem(line, isDone)`, `Note.contentTasks` y `NoteChecklist` (funciones puras). | RF-3 a RF-14, RF-16, RF-17, RF-24, RF-25 a RF-27 |
| `:data` | Columna `content_tasks`, codificación de tareas, mappers, migración manual v5→v6 y esquema `6.json`. La semilla «La compra» pasa a tener tareas reales. | RF-24, RF-25 a RF-28, RNF-1 |
| `:presentation` | Editor: estado, intents, ViewModel, transformación con sangría y tachado, casillas superpuestas y botón de la barra. Lista: casillas en la tarjeta. Strings y tokens. | RF-1 a RF-23, RF-29 a RF-34 |
| `:app` | Ninguno. | — |

La búsqueda (`NoteTextMatcher`) no cambia: el contenido sigue en texto plano y las tareas no añaden caracteres [RF-32].

## 4. Diseño por capa

### 4.1 `:domain`

**Modelos:**
- `data class ChecklistItem(val line: Int, val isDone: Boolean)`: la línea `line` (desde 0) del contenido es una tarea.
- `Note.contentTasks: List<ChecklistItem> = emptyList()`: las notas sin tareas y las anteriores a esta funcionalidad quedan vacías.

**`NoteChecklist`** (`object` público, sin estado). Devuelve listas normalizadas: ordenadas por línea, sin duplicados y sin líneas fuera del contenido.

| Función | Qué hace | RF |
|---|---|---|
| `isTask(tasks, line)` | Indica si la línea es tarea. | RF-4 |
| `linesIn(content, start, end)` | Devuelve las líneas que abarca una selección o el cursor. | RF-3 a RF-6 |
| `toggleLines(content, tasks, start, end)` | Si alguna línea abarcada no es tarea, todas pasan a tareas pendientes; si todas lo son, todas pasan a texto. | RF-3 a RF-8, CL-1 a CL-4 |
| `setDone(tasks, line, isDone)` | Marca o desmarca una tarea. | RF-16, RF-17, RF-20 |
| `onTextChanged(old, new, cursor, tasks): ChecklistEdit` | Interpreta un cambio de texto y desplaza los índices (ver abajo). | RF-10 a RF-14, CL-5, CL-6, CL-11 |
| `fromLegacyMarkers(content): LegacyConversion` | Quita «☐»/«☑» y los espacios que siguen al inicio de cada línea. Devuelve el texto nuevo, las tareas y los rangos quitados. | RF-25, RF-26, RF-27, CL-8 |

**`onTextChanged`.** Para hallar el rango editado, supone primero que lo escrito termina en `cursor` (la posición del cursor después del cambio), como al teclear, borrar o pegar. Si eso no cuadra con los textos, usa el mismo cálculo de prefijo y sufijo comunes que `NoteFormatting.onTextChanged`. El cursor hace falta porque el texto solo no basta: un Intro en la primera de dos líneas vacías da el mismo texto que en la segunda. Devuelve `ChecklistEdit(text, tasks, cursor?)`: `text` y `cursor` solo cambian cuando una regla anula la tecla pulsada.
- **Intro en una tarea vacía** [RF-11, CL-5]: el cambio es la inserción de un único `\n` en una línea que es tarea y está vacía. Se descarta el `\n`, la línea deja de ser tarea y se devuelve el texto anterior con el cursor donde estaba.
- **Borrar al inicio de una tarea** [RF-12]: el cambio es el borrado de un único `\n` justo antes de una línea que es tarea, con el cursor al inicio de esa línea. Se descarta el borrado y la línea deja de ser tarea. El siguiente borrado ya no es al inicio de una tarea, así que une las líneas normalmente [RF-13].
- **Insertar texto con `k` saltos de línea** (Intro en una tarea con texto, o pegar) [RF-10, RF-14, CL-11]: si la línea donde empieza la edición es tarea, las `k` líneas nuevas pasan a ser tareas pendientes. La línea original conserva su estado. Las tareas de más abajo bajan `k` líneas.
- **Borrar texto que abarca `k` saltos de línea:** las líneas absorbidas desaparecen de la lista. La línea resultante conserva el estado de la línea donde empezó el borrado, salvo que el borrado empiece al inicio de una línea (líneas enteras): entonces la resultante es la última absorbida y conserva su estado. Las tareas de más abajo suben `k` líneas.

### 4.2 `:data`

- **`NoteEntity`:** `@ColumnInfo(name = "content_tasks", defaultValue = "") val contentTasks: String = ""` [RF-24].
- **Codificación propia** (`ChecklistCodec.kt`, `internal`, en el paquete de mappers, como `FormatSpanCodec`): `línea,estado` separados por `;`, con estado `0` (pendiente) o `1` (hecha); por ejemplo, `0,0;2,1`. Al decodificar se descartan las entradas mal formadas y las líneas que no existen en el contenido.
- **Mappers:** `toDomain()` y `toEntity()` incluyen las tareas [RF-24].
- **`MIGRATION_5_6`** (manual, en `local/migration/Migrations.kt`, con la skill `room-migration`) [RF-25 a RF-27, RNF-1]:
  1. `ALTER TABLE notes ADD COLUMN content_tasks TEXT NOT NULL DEFAULT ''`.
  2. Recorre las notas cuyo `content` contiene «☐» o «☑». Para cada una, llama a `NoteChecklist.fromLegacyMarkers`.
  3. Ajusta los tramos de formato quitando los rangos eliminados, de abajo arriba, con `NoteFormatting.onTextChanged`.
  4. Escribe `content`, `content_spans` y `content_tasks` con un `UPDATE` por nota. `updated_at` no cambia: no es una edición del usuario.

  `AutoMigration` no sirve, porque además de la columna cambia datos.
- **`NotesDatabase`:** `version = 6`. Se exporta `6.json`.
- **`SeedNotesCallback`:** «La compra» se crea con el texto sin «☐»/«☑» y con `content_tasks = "0,0;1,0;2,1;3,0"` [RF-28].

### 4.3 `:presentation` — Editor

**`NoteEditorUiState`:** campo nuevo `contentTasks: List<ChecklistItem>`.

**`NoteEditorIntent`**, intents nuevos:
- `ChecklistButtonClicked` [RF-3 a RF-6].
- `TaskToggled(line: Int)` [RF-16, RF-17].

**`NoteEditorViewModel`**:
- **`ContentChanged`** con cambio de texto:
  1. Primero `NoteChecklist.onTextChanged`. Si la regla anuló la tecla, el `TextFieldValue` se sustituye por el texto y la selección que devuelve.
  2. Después, `NoteFormatting.onTextChanged` con el texto final, para que los tramos no se desplacen por una tecla anulada [RF-9, RF-10 a RF-14].
  3. La nota queda con cambios pendientes, lo que dispara el autoguardado.
- **`ChecklistButtonClicked`:** `toggleLines` sobre la selección actual. Marca cambios pendientes, lo que guarda la nota y actualiza su fecha [RF-3 a RF-8, RF-22, RF-23].
- **`TaskToggled`:** `setDone` y guarda en el momento, igual que fijar la nota [RF-16, RF-17, RF-20, RF-22, RF-23].
- **Cambios pendientes y borradores:** `hasUnsavedChanges()` y los dos borradores (guardar y agregar imágenes) incluyen `contentTasks`. La carga los lee de la nota [RF-24].
- Igual que `NoteFormatting` en la spec 002, el ViewModel usa `NoteChecklist` directamente: son funciones puras de dominio, sin repositorio. Ver D6 (recogido como excepción en `CLAUDE.md`).

**`NoteEditorScreen`**:
- **Botón «Lista de tareas»:** se habilita y emite `ChecklistButtonClicked`, con el `testTag` nuevo `TOOLBAR_CHECKLIST`. Se deshabilita sin foco en el contenido, igual que «Aa» [RF-1, RF-2].
- **Transformación del contenido:** `FormattedContentTransformation` pasa a recibir también las tareas. `buildFormattedContent` añade:
  - un `ParagraphStyle(textIndent = TextIndent(firstLine, restLine))` en cada línea que es tarea, para dejar hueco a la casilla (casilla de 20 dp más separación de 12 dp); las líneas largas de una tarea quedan alineadas con su texto;
  - en las tareas hechas, `TextDecoration.LineThrough` y el color secundario (`onSurfaceVariant`) como estilo solo visual [RF-19, RF-21, CL-7].

  La correspondencia de posiciones sigue siendo `OffsetMapping.Identity`, porque no se insertan caracteres.

  **Ajuste al implementar (T17):** en Compose 1.10 un párrafo que termina en `\n` dibuja una línea vacía de más. Por eso, si la nota tiene tareas:
  - cada línea es su propio párrafo y los `\n` se cambian en la transformación por un espacio de ancho cero, que mide lo mismo y no rompe la correspondencia de posiciones;
  - si el contenido termina en `\n`, ese último se conserva, para que exista la línea vacía final;
  - los párrafos no recortan su altura de línea (`LineHeightStyle.Trim.None`);
  - las tareas usan la altura de fila del diseño, `taskRow` (36 dp: la casilla de 20 dp más 16 dp de separación).
- **Casillas:** el campo guarda el `TextLayoutResult` (`onTextLayout`). Encima, una capa dibuja una `TaskCheckbox` por tarea, en la posición vertical de la primera línea visual de esa línea (`getLineTop` y `getLineBottom` del offset de inicio de línea) [RF-15].
  - **Aspecto** [RNF-2]:
    - Casilla de 20 dp con radio de 5 dp.
    - Pendiente: borde de 1,5 dp en `onSurfaceVariant`.
    - Hecha: relleno `primary` con `ic_check` de 14 dp en `onPrimary`.

    Sobre una nota con color, `NoteSurfaceTheme` ya da `Slate` y `Ink`, con contraste mayor de 4,5:1.
  - **Interacción** [RF-33, RF-34, RNF-3]: zona táctil fija de 44 dp (`iconAction`), colocada con `Modifier.offset` para que los toques lleguen a la casilla, y `toggleable`. La accesibilidad va en un único nodo con `clearAndSetSemantics`: rol de casilla, `contentDescription` con el texto de la tarea, `stateDescription` «Hecha» o «Pendiente», estado y acción. Con un `semantics` aparte, la etiqueta quedaba en un nodo hijo sin acción.
  - **`testTag`:** `TASK_CHECKBOX + línea`.
- **Strings nuevos:** `note_editor_task_done` («Hecha») y `note_editor_task_pending` («Pendiente»). La etiqueta del botón (`note_editor_toolbar_checklist`) ya existe.
- **Tokens nuevos en `Sizes`:** `taskCheckbox` (20 dp), `taskCheckboxBorder` (1,5 dp), `taskCheckIcon` (14 dp), `taskGap` (12 dp) y `taskRow` (36 dp, alto de cada fila de tarea); para la tarjeta, `cardTaskCheckbox` (13 dp) y `cardTaskGap` (6 dp). En `Radii`: `taskCheckbox` (5 dp) y `cardTaskCheckbox` (3 dp).
- **Strings nuevos:** además de «Hecha» y «Pendiente», `note_editor_task_empty` («Tarea vacía»), la etiqueta accesible de la casilla de una tarea sin texto.

### 4.4 `:presentation` — Mis notas

- **`NoteListViewModel`:** construye el contenido con `buildFormattedContent(content, spans, tasks)`. Para la tarjeta, al inicio de cada tarea añade una marca de contenido en línea (`appendInlineContent`) con el id de una casilla pendiente o hecha, y tacha el texto de las hechas [RF-29, RF-30].
- **`NoteCard`:** pasa al `Text` un mapa `inlineContent` con las dos casillas en pequeño (`cardTaskCheckbox`, 13 dp, más `cardTaskGap`, 6 dp; el diseño no tiene tareas en la tarjeta), sin interacción. Cada casilla anuncia «Hecha» o «Pendiente» desde `strings.xml`; el texto alternativo de la marca en línea es un espacio de ancho cero, porque el ViewModel no puede leer `strings.xml` y Compose no admite uno vacío. Al tocar la tarjeta se abre la nota, como hasta ahora [RF-29 a RF-31, CL-9].
- **`NoteItemUi.content`** sigue siendo `AnnotatedString`; las casillas son parte de él.

## 5. Decisiones técnicas

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D1 | **Tareas por índice de línea, aparte del texto** | Igual que el formato (spec 002): el contenido sigue en texto plano, así que la búsqueda, el resumen y el formato no cambian [RF-32]. | **Marcadores en el texto** (un carácter privado al inicio de la línea): aparecerían en la búsqueda y al copiar, y descuadran las posiciones del formato. |
| D2 | **Columna `content_tasks` con codificación propia** | Una sola escritura por guardado y sin dependencias (principio 1), como `content_spans`. | **Tabla `note_tasks`**: borrar y reinsertar en cada tecla. **JSON**: dependencia nueva. |
| D3 | **Sangría con `ParagraphStyle` y casillas superpuestas** | `BasicTextField` no admite contenido en línea. La sangría deja el hueco sin insertar caracteres, así que las posiciones siguen siendo idénticas y el formato de la spec 002 no se toca. | **Insertar espacios con la `VisualTransformation`**: obliga a un `OffsetMapping` propio y a mover el cursor por encima de ellos. **Un campo por línea** (`LazyColumn`): rompe la selección de varias líneas que piden RF-5 y RF-6. |
| D4 | **Intro, borrar y pegar interpretados al cambiar el texto** | El campo solo informa del texto nuevo. Comparando con el anterior se detectan los tres casos y se prueban en Kotlin puro. | **Interceptar teclas** (`onKeyEvent`): no funciona con todos los teclados de software y no cubre pegar. |
| D5 | **Migración manual v5→v6 en Kotlin** | Convertir «☐»/«☑» cambia el texto y obliga a recolocar los tramos de formato; SQL no puede hacerlo. Reutiliza las funciones de `:domain`, que ya están probadas. | **Convertir al leer cada nota**: la conversión se repetiría siempre y una nota guardada mezclaría los dos formatos. |
| D6 | **`NoteChecklist` usado directamente por el ViewModel** | Mantiene el mismo criterio que `NoteFormatting` en la spec 002: son funciones puras de dominio, sin repositorio ni efectos. | **Un caso de uso por función**: clases que solo delegan. Decidido en T23: se recoge como excepción en `CLAUDE.md`. |
| D7 | **Tachado de hecha solo visual** | Desmarcar devuelve el texto tal como era, con su formato [RF-21, CL-7]. | **Añadir un tramo `STRIKETHROUGH`**: desmarcar borraría un tachado que el usuario hubiera puesto a propósito. |

## 6. Estrategia de pruebas

Todas las escribe el agente **`test-writer`**. Se ejecutan con `./gradlew test` y con `connectedDebugAndroidTest`.

### 6.1 Unitarias en `:domain`

**`NoteChecklistTest`:**
- `toggleLines` con el cursor en una línea normal, en una tarea y en una línea vacía, y con el contenido vacío [RF-3, RF-4, CL-3, CL-4].
- `toggleLines` con una selección mixta y con una selección solo de tareas, incluidas hechas y pendientes [RF-5, RF-6, RF-8, CL-1, CL-2].
- `onTextChanged`:
  - Intro al final y en medio de una tarea [RF-10];
  - Intro en una tarea vacía, también en mitad de una lista [RF-11, CL-5];
  - borrar al inicio de una tarea y luego otra vez [RF-12, RF-13];
  - borrar el texto de una tarea sin borrar la casilla [CL-6];
  - pegar varias líneas en una tarea y en una línea normal [RF-14, CL-11];
  - escribir o borrar en líneas anteriores desplaza los índices;
  - borrar varias líneas a la vez.
- `setDone` marca y desmarca sin mover la tarea [RF-16, RF-17, RF-20].
- `fromLegacyMarkers`: «☐» y «☑» al inicio, con uno o varios espacios, sin espacios, en mitad de la línea, en notas sin marcadores y en la primera y la última línea [RF-25 a RF-27, CL-8].

**Pruebas que se amplían:**
- `SaveNoteUseCaseTest`: una nota con tareas se guarda con fecha nueva [RF-23].
- `GetNotesUseCaseTest`: la búsqueda encuentra el texto de una tarea [RF-32].

### 6.2 Unitarias en `:data`
- **`ChecklistCodecTest`:** ida y vuelta, cadena vacía, entradas mal formadas y líneas fuera del contenido.
- **`NoteMappersTest` y `NoteRepositoryImplTest`, que se amplían:** las tareas viajan entre la entidad y el dominio [RF-24].

### 6.3 Instrumentadas en `:data`
- **`MigrationTest`, que se amplía:** de v5 a v6, una nota con «☐»/«☑» y con tramos de formato posteriores queda:
  - con el texto sin marcadores;
  - con las tareas correctas;
  - con los tramos recolocados sobre las mismas palabras;
  - con la misma fecha.

  Una nota sin marcadores no cambia y el número de notas se conserva [RF-25 a RF-27, RNF-1]. Las pruebas que abren la base con Room esperan la versión 6.
- **`NoteDaoTest`:** una nota con tareas se guarda y se lee igual [RF-24].

### 6.4 Unitarias en `:presentation`

**`NoteEditorViewModelTest`, que se amplía:**
- `ChecklistButtonClicked` con cursor y con selección guarda y actualiza la fecha [RF-3 a RF-6, RF-22, RF-23].
- `TaskToggled` marca y desmarca, guarda y no toca los tramos [RF-16, RF-17, RF-20, RF-21].
- Intro en una tarea vacía y borrar al inicio: el estado muestra el texto anterior, la tarea quitada y los tramos sin desplazar [RF-11, RF-12].
- Escribir y pegar en una tarea crea tareas [RF-10, RF-14].
- Cargar una nota con tareas [RF-24].

**`FormattedContentTest`, que se amplía:** la sangría en las tareas, el tachado visual en las hechas, la combinación con negrita [CL-7] y las marcas en línea para la tarjeta.

**`NoteListViewModelTest`, que se amplía:** una nota con tareas llega con sus casillas en el contenido [RF-29, RF-30].

### 6.5 UI en `:presentation`

**`NoteEditorScreenTest`, que se amplía:**
- El botón está habilitado con el foco en el contenido, deshabilitado en el título, y emite el intent [RF-1, RF-2].
- Cada tarea muestra una casilla con su etiqueta y su estado «Hecha» o «Pendiente» [RF-15, RF-33, RF-34].
- Tocar la casilla emite `TaskToggled(línea)` [RF-16, RF-17].

**`NoteListScreenTest`, que se amplía:** una tarjeta con tareas muestra su texto, y tocarla abre la nota [RF-29 a RF-31].

### 6.6 Verificación manual en el emulador
- Convertir una línea y varias en tareas y volver a texto [RF-3 a RF-9].
- Escribir una lista con Intro, salir de ella con Intro en una tarea vacía y borrar al inicio de una tarea [RF-10 a RF-13].
- Pegar varias líneas en una tarea [RF-14].
- Marcar y desmarcar, cerrar y reabrir la nota [RF-16 a RF-24].
- Tareas en la tarjeta y sobre una nota con color [RF-29, RF-30, RNF-2].
- Una tarea larga que ocupa varias líneas en pantalla: la casilla queda en su primera línea y el texto alineado.
- Migración: instalar `main` (con «La compra» de caracteres), actualizar encima con `adb install -r` y comprobar las tareas reales [RF-25 a RF-27].

## 7. Orden de implementación

1. **`:domain`:** `ChecklistItem`, `Note.contentTasks` y `NoteChecklist`, con sus pruebas.
2. **`:data`:** codificación, mappers, columna, migración v5→v6 con la skill `room-migration` y semilla, con sus pruebas y la de migración.
3. **`:presentation`, estado del editor:** `UiState`, intents y ViewModel, con sus pruebas.
4. **`:presentation`, interfaz del editor:** transformación con sangría y tachado, casillas, botón, strings y tokens, con sus pruebas de UI.
5. **`:presentation`, Mis notas:** casillas en la tarjeta, con sus pruebas.
6. **Verificación:**
   - `./gradlew test`, `connectedDebugAndroidTest`, `assembleDebug` y `lint`.
   - Revisión con `architecture-reviewer`.
   - Pruebas manuales de 6.6.
7. **Cierre:** actualizar `.memory/MEMORY.md` y marcar spec y plan como implementados.

## 8. Trazabilidad de requisitos

| RF / RNF | Dónde se implementa | Dónde se prueba |
|---|---|---|
| RF-1, RF-2 Botón | 4.3 barra | 6.5 |
| RF-3 a RF-9 Convertir líneas | 4.1 `toggleLines` · 4.3 ViewModel | 6.1, 6.4, 6.6 |
| RF-10, RF-11 Intro | 4.1 `onTextChanged` · 4.3 ViewModel | 6.1, 6.4, 6.6 |
| RF-12, RF-13 Borrar al inicio | 4.1 `onTextChanged` | 6.1, 6.4, 6.6 |
| RF-14 Pegar | 4.1 `onTextChanged` | 6.1, 6.4, 6.6 |
| RF-15 Casilla | 4.3 casillas superpuestas | 6.5 |
| RF-16 a RF-20 Marcar | 4.1 `setDone` · 4.3 `TaskToggled` | 6.1, 6.4, 6.5 |
| RF-19, RF-21 Tachado visual | 4.3 transformación · D7 | 6.4 |
| RF-22 a RF-24 Guardado | 4.2 columna y mappers · 4.3 ViewModel | 6.1 a 6.4 |
| RF-25 a RF-27 Notas existentes | 4.1 `fromLegacyMarkers` · 4.2 migración | 6.1, 6.3, 6.6 |
| RF-28 Semilla | 4.2 `SeedNotesCallback` | 6.6 |
| RF-29 a RF-31 Tarjeta | 4.4 | 6.4, 6.5, 6.6 |
| RF-32 Búsqueda | D1: contenido plano | 6.1 |
| RF-33, RF-34 Accesibilidad | 4.3 `toggleable` y estados | 6.5 |
| RNF-1 Datos | D5 · migración | 6.3, 6.6 |
| RNF-2 Aspecto y contraste | 4.3 casillas · `NoteSurfaceTheme` | 6.6 |
| RNF-3 Tamaño táctil | 4.3 zona táctil fija de 44 dp (`iconAction`) | 6.5 |
| RNF-4 No solo color | 4.3 marca y tachado | 6.5 |
| RNF-5 Idioma | 4.3 strings | 6.5 |

## 9. Riesgos

- **Casillas que se solapan.** Con 44 dp de zona táctil y filas de tarea de 36 dp (`taskRow`), las zonas de dos tareas seguidas se solapan 8 dp. Compose da el toque a la zona más cercana, así que debería funcionar, pero se comprueba a mano (6.6).
- **`ParagraphStyle` con selección.** Los estilos de párrafo deben cubrir la línea entera, incluido su `\n`. Se prueba en `FormattedContentTest` para que no se recorten al editar.
