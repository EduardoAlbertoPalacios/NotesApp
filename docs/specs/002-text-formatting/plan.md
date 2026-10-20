# Plan 002 — Formato de texto en las notas

> **Estado:** implementado · **Fecha:** 2026-10-19 · **Spec:** [spec.md](spec.md) (implementada)

Plan técnico para implementar la spec 002. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

El contenido de la nota **sigue siendo texto plano**. Al lado se guarda una lista de **tramos de formato**, cada uno con inicio, fin y formato (negrita, subrayado o tachado). Las reglas para aplicar, quitar, desplazar y consultar tramos viven en `:domain` como funciones puras.

En el editor, el campo de contenido trabaja con texto y selección (`TextFieldValue`) y pinta los tramos con una `VisualTransformation`. La cuadrícula convierte los tramos en un `AnnotatedString`.

Room pasa de la versión 3 a la 4 con una `AutoMigration` que agrega la columna de tramos con valor vacío por defecto. No se agregan dependencias.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas. Los tramos se guardan con un formato de texto propio, sin bibliotecas de serialización. Los íconos son VectorDrawable, como los demás. |
| 2. La spec manda | Cada parte cita sus RF. Si algo se aparta de la spec, se actualizan ambas en el mismo commit. |
| 3. Lógica fuera de la UI | Alternar, desplazar tras una edición, formatos activos y fusión de tramos están en `:domain`. El ViewModel orquesta y los composables solo pintan. |
| 4. Sin pruebas no hay commit | Pruebas unitarias de dominio, mapper, repositorio y ViewModel; prueba de migración; pruebas de UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | Migración v3→v4 automática con valor por defecto vacío, esquema 4 exportado y prueba de migración que conserva las notas existentes (RNF-1). Nada de `fallbackToDestructiveMigration`. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en español en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:common` | Ninguno. | — |
| `:domain` | Se agregan `TextFormat` (enum), `FormatSpan(start, end, format)` y `NoteFormatting` (funciones puras sobre tramos); `Note` gana `contentSpans`. | RF-7 a RF-14, RF-18, RF-21 |
| `:data` | `NoteEntity` gana la columna `content_spans`. Hay un codificador de tramos, se ajustan los mappers y la base pasa a v4 con `AutoMigration(3, 4)` y el esquema `4.json`. | RF-19, RF-21, RF-22, RNF-1 |
| `:presentation` | Editor: estado, intents, ViewModel, menú de formato, `VisualTransformation` y el campo de contenido con selección. Lista: `NoteItemUi` con el contenido como `AnnotatedString`. Strings e íconos nuevos. | RF-1 a RF-6, RF-15 a RF-18, RF-20, RF-23, RF-24 |
| `:app` | Ninguno. | — |

La búsqueda (`NoteTextMatcher`) no cambia: el contenido sigue siendo texto plano, así que el formato no afecta [RF-25].

## 4. Diseño por capa

### 4.1 `:domain`

**Modelos** [RF-7 a RF-14]:
- `enum class TextFormat { BOLD, UNDERLINE, STRIKETHROUGH }`.
- `data class FormatSpan(val start: Int, val end: Int, val format: TextFormat)`: rango semiabierto `[start, end)` sobre el contenido.
- `Note.contentSpans: List<FormatSpan> = emptyList()`. Las notas antiguas quedan sin tramos [RF-22].

**`NoteFormatting`** (`object` público, sin estado). Todas sus funciones devuelven listas normalizadas, sin tramos vacíos ni solapes del mismo formato, y con los contiguos del mismo formato fusionados.

| Función | Qué hace | RF |
|---|---|---|
| `isApplied(spans, start, end, format)` | Indica si todo el rango tiene ese formato. | RF-5, RF-7, RF-8 |
| `toggle(spans, start, end, format)` | Si el rango no lo tiene completo, lo aplica a todo el rango; si lo tiene completo, lo quita. No toca los demás formatos. | RF-7, RF-8, RF-9, CL-1, CL-2, CL-4 |
| `formatsAt(spans, position)` | Devuelve los formatos del carácter anterior a la posición; en la posición 0, ninguno. | RF-14, CL-9 |
| `onTextChanged(spans, old, new, activeFormats)` | Calcula el rango editado (prefijo y sufijo comunes). Desplaza o recorta los tramos y aplica `activeFormats` al texto insertado, ya sea escrito o pegado. | RF-12, RF-18, CL-3, CL-8 |

### 4.2 `:data`

- **`NoteEntity`:** `@ColumnInfo(name = "content_spans", defaultValue = "") val contentSpans: String = ""` [RF-19, RF-22].
- **Codificación propia:** `start,end,F` separados por `;`, con `F` ∈ {`B`, `U`, `S`}; por ejemplo, `0,5,B;3,9,U`. Las funciones `List<FormatSpan>.encode()` y `String.decodeSpans(contentLength)` son `internal`, en el paquete de mappers. Al decodificar se descartan las entradas mal formadas y se recortan al largo del contenido, para tolerar datos corruptos.
- **Mappers:** `toDomain()` y `toEntity()` incluyen los tramos [RF-21].
- **`NotesDatabase`:** pasa a `version = 4` y agrega `AutoMigration(from = 3, to = 4)`. Se exporta `data/schemas/.../4.json`. Se implementa con la skill `room-migration` [RNF-1].
- **Repositorio y DAO:** sin cambios de interfaz. `update` e `insert` ya escriben la entidad completa.

### 4.3 `:presentation` — Editor

**`NoteEditorUiState`**:
- `content` pasa de `String` a `TextFieldValue`, para tener la selección.
- Campos nuevos:
  - `contentSpans: List<FormatSpan>`.
  - `activeFormats: Set<TextFormat>`.
  - `isFormatMenuVisible: Boolean`.
  - `isContentFocused: Boolean`.

**`NoteEditorIntent`**, intents nuevos y cambiados:
- `ContentChanged(value: TextFieldValue)`, que sustituye a la versión con `String`.
- `FormatButtonClicked`.
- `FormatMenuDismissed`.
- `FormatSelected(format)`.
- `ContentFocusChanged(focused)`.

**`NoteEditorViewModel`**:
- **`ContentChanged`:**
  - Si cambió el texto, llama a `NoteFormatting.onTextChanged(…, activeFormats)` y marca la nota con cambios pendientes, lo que dispara el autoguardado [RF-12, RF-18, RF-19].
  - Si solo cambió la selección, recalcula `activeFormats = formatsAt(...)` y no marca cambios [RF-14, CL-6, CL-9].
- **`FormatSelected`:**
  - Con selección, llama a `toggle(...)`, mantiene la selección y marca la nota con cambios pendientes (autoguardado y fecha) [RF-7 a RF-10, RF-19, RF-20].
  - Sin selección, alterna el formato en `activeFormats` y no marca cambios [RF-11, RF-13, CL-5, CL-6].
  - En los dos casos cierra el menú [RF-3].
- **`FormatButtonClicked` / `FormatMenuDismissed`:** abren y cierran el menú [RF-2, RF-4].
- **Guardado:** el borrador incluye `contentSpans`. El cálculo de cambios pendientes compara también los tramos. La fecha de edición la sigue fijando `SaveNoteUseCase` [RF-19, RF-20].
- **Carga:** construye el `TextFieldValue` y los tramos desde la nota guardada [RF-21].

**`NoteEditorScreen`**:
- **Campo de contenido:**
  - `BasicTextField(value: TextFieldValue)` con `onFocusChanged` y una `VisualTransformation` que pinta los tramos con `SpanStyle`: negrita con `FontWeight.Bold`, subrayado y tachado con `TextDecoration`. Usa `OffsetMapping.Identity`, porque el texto no cambia [RF-12, RF-21].
  - Lo pegado entra como texto plano, porque el campo solo maneja texto; el formato de origen se descarta [RF-17, CL-7].
- **Título:** sigue siendo un campo de texto plano, sin transformación [RF-15, RF-16].
- **Barra de edición:**
  - El botón «Aa» se habilita [RF-1].
  - Se deshabilita cuando `isContentFocused` es falso [RF-6].
  - Abre un `DropdownMenu` de Material 3 anclado al botón [RF-2].
- **Menú:**
  - Tres `DropdownMenuItem` (Negrita, Subrayado, Tachado), cada uno con su ícono y su etiqueta.
  - El estado activo se marca con un ícono de verificación (`ic_check`, que ya existe) y con `stateDescription` «Activo» o «Inactivo», no solo con color [RF-5, RNF-2, RNF-3].
  - Usa colores y tipografía del tema [RNF-4].
- **Íconos nuevos:** `ic_bold`, `ic_underline` y `ic_strikethrough`, en Lucide como VectorDrawable, igual que los demás.
- **Strings nuevos:** `note_editor_format_bold`, `note_editor_format_underline`, `note_editor_format_strikethrough`, `note_editor_format_active` y `note_editor_format_inactive` [RNF-5].

### 4.4 `:presentation` — Mis notas

- **`NoteItemUi.content`:** pasa a ser un `AnnotatedString`. Lo construye una función de mapeo de presentación, `buildFormattedContent(content, spans)`, fuera de los composables. La reutiliza la `VisualTransformation` del editor [RF-23].
- **`NoteCard`:** pinta el `AnnotatedString`. Al recortar con `maxLines`, se conservan los estilos del fragmento visible [RF-24].

## 5. Decisiones técnicas

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D1 | **Texto plano + tramos aparte** | El contenido sigue siendo texto: la búsqueda, el resumen y las notas antiguas no cambian (RF-22, RF-25). Pegar llega plano de forma natural (RF-17). | **Marcadores en el texto** (`**negrita**`, HTML): ensucian el contenido, rompen la búsqueda y chocan con textos que ya tienen asteriscos o `<`. |
| D2 | **Una columna `content_spans` con codificación propia** | Una sola escritura por guardado y una migración trivial (columna con valor por defecto). Sin dependencias (principio 1). | **Tabla `note_spans`** con una fila por tramo: obliga a borrar y reinsertar en cada tecla. **JSON con kotlinx.serialization**: dependencia nueva. |
| D3 | **Lógica de tramos en `:domain`** (`NoteFormatting`) | Son reglas de la spec (alternar, completo o parcial, formatos al mover el cursor, pegar con formatos activos) y se prueban en Kotlin puro (principio 3). | **Ponerla en el ViewModel o en el composable**: lógica de negocio en la presentación y difícil de probar. |
| D4 | **`VisualTransformation` sobre `BasicTextField(TextFieldValue)`** | Pinta estilos sin tocar el texto, con correspondencia de posiciones idéntica. `TextFieldValue` da la selección y el cursor que piden RF-7 a RF-14. | **Editar un `AnnotatedString` directamente** en el campo: Compose descarta los estilos al editar. **`TextFieldState` con `OutputTransformation`**: no permite agregar estilos. |
| D5 | **Formatos activos solo en el estado de la UI** | Activar un formato sin escribir no es una edición: no se guarda ni cambia la fecha (CL-6). | **Guardarlos con la nota**: cambiaría la fecha sin que el usuario editara nada. |
| D6 | **`DropdownMenu` de Material 3** | Ya forma parte del stack, respeta el tema (RNF-4) y trae accesibilidad (RNF-2). | **Un popup propio**: más código y más riesgo de accesibilidad, sin ganancia. |
| D7 | **`AutoMigration(3, 4)`** | Agregar una columna con valor por defecto es lo que AutoMigration resuelve sin código; ya se usó en v1→v2 y v2→v3. | **`Migration` manual con SQL**: más código para el mismo resultado. |

## 6. Estrategia de pruebas

Todas las escribe el agente **`test-writer`**. Se ejecutan con `./gradlew test` y con `connectedDebugAndroidTest`.

### 6.1 Unitarias en `:domain`

**`NoteFormattingTest`**:
- `toggle` aplica a un rango sin formato, quita de uno con formato completo y aplica a todo un rango con formato parcial [RF-7, RF-8, CL-1].
- `toggle` conserva los demás formatos [RF-9, CL-4].
- Una selección que cruza saltos de línea queda formateada entera [CL-2].
- `formatsAt` toma el carácter anterior, devuelve vacío en la posición 0 y combina formatos [RF-14, CL-9].
- `onTextChanged` cubre:
  - insertar antes, dentro y después de un tramo, y en sus bordes;
  - borrar dentro de un tramo, un tramo entero y todo el texto [CL-3];
  - reemplazar una selección;
  - aplicar los formatos activos a lo escrito o pegado [RF-12, RF-18, CL-8].
- Normalización: fusión de tramos contiguos y ningún tramo vacío.

**Pruebas que se amplían:**
- `SaveNoteUseCaseTest`: una nota con tramos se guarda con fecha nueva [RF-20].
- `GetNotesUseCaseTest`: la búsqueda ignora los tramos [RF-25].

### 6.2 Unitarias en `:data`

- **`FormatSpanCodecTest`:** ida y vuelta de la codificación, cadena vacía, entradas mal formadas descartadas y tramos recortados al largo del contenido [RF-21, RNF-1].
- **`NoteMappersTest` y `NoteRepositoryImplTest`, que se amplían:** los tramos viajan entre la entidad y el dominio.

### 6.3 Instrumentadas en `:data`

- **`MigrationTest`, que se amplía:** de v3 a v4 se conservan las notas existentes y su `content_spans` queda vacío [RF-22, RNF-1].
- **`NoteDaoTest`:** una nota con tramos se guarda y se lee igual [RF-21].

### 6.4 Unitarias en `:presentation`

**`NoteEditorViewModelTest`, que se amplía:**
- El menú se abre y se cierra con `FormatButtonClicked`, `FormatMenuDismissed` y `FormatSelected` [RF-2 a RF-4].
- `FormatSelected` con selección alterna el formato, mantiene la selección, guarda y actualiza la fecha [RF-7 a RF-10, RF-19, RF-20].
- `FormatSelected` sin selección activa o desactiva el formato sin guardar [RF-11, RF-13, CL-6].
- Escribir con formatos activos aplica tramos [RF-12].
- Mover el cursor recalcula `activeFormats` [RF-14, CL-9].
- Pegar con formatos activos [RF-18].
- Cargar una nota con tramos y una nota antigua sin tramos [RF-21, RF-22].

**`buildFormattedContent`:** produce un `AnnotatedString` con un `SpanStyle` por tramo [RF-23].

### 6.5 UI en `:presentation`

**`NoteEditorScreenTest`, que se amplía:**
- El botón «Aa» está habilitado y abre el menú con tres opciones [RF-1, RF-2].
- Elegir una opción emite `FormatSelected` [RF-3].
- Las opciones activas tienen `stateDescription` «Activo» [RF-5, RNF-2].
- Con el foco en el título, el botón está deshabilitado [RF-6].

**`NoteListScreenTest`, que se amplía:** una tarjeta con contenido formateado muestra su texto [RF-23].

### 6.6 Verificación manual en el emulador

- Aplicar y quitar cada formato a una selección.
- Activar un formato y escribir con él.
- Mover el cursor y ver los formatos activos.
- Pegar desde otra app, con y sin formatos activos [RF-17, RF-18].
- Cerrar y reabrir la nota [RF-21].
- Ver el formato en la tarjeta [RF-23, RF-24].
- Instalar sobre la versión anterior y comprobar que las notas siguen intactas [RF-22, RNF-1].

## 7. Orden de implementación

1. **`:domain`:** `TextFormat`, `FormatSpan`, `Note.contentSpans` y `NoteFormatting`, con sus pruebas.
2. **`:data`:** codificación, mappers, columna nueva y migración v3→v4 con la skill `room-migration`, con sus pruebas y prueba de migración.
3. **`:presentation`, estado del editor:** `UiState`, `Intent` y `ViewModel`, con sus pruebas.
4. **`:presentation`, interfaz del editor:** `VisualTransformation`, campo con `TextFieldValue`, botón «Aa», menú, íconos y strings, con sus pruebas de UI.
5. **`:presentation`, Mis notas:** `AnnotatedString` en la tarjeta, con sus pruebas.
6. **Verificación:**
   - `./gradlew test`, `connectedDebugAndroidTest`, `assembleDebug` y `lint`.
   - Revisión con `architecture-reviewer`.
   - Pruebas manuales de 6.6.
7. **Cierre:** actualizar `.memory/MEMORY.md` y marcar spec y plan como implementados.

## 8. Trazabilidad de requisitos

| RF / RNF | Dónde se implementa | Dónde se prueba |
|---|---|---|
| RF-1 Habilitar «Aa» | 4.3 barra | 6.5 |
| RF-2 Abrir menú | 4.3 ViewModel y `DropdownMenu` | 6.4, 6.5 |
| RF-3 Cerrar al elegir | 4.3 `FormatSelected` | 6.4, 6.5 |
| RF-4 Cerrar al tocar fuera | 4.3 `FormatMenuDismissed` | 6.4 |
| RF-5 Opciones activas | 4.1 `isApplied` / `activeFormats` · 4.3 menú | 6.1, 6.5 |
| RF-6 Deshabilitado en el título | 4.3 `isContentFocused` | 6.5 |
| RF-7 a RF-10 Selección | 4.1 `toggle` · 4.3 ViewModel | 6.1, 6.4 |
| RF-11 a RF-13 Formato activo | 4.1 `onTextChanged` · 4.3 `activeFormats` | 6.1, 6.4 |
| RF-14 Mover el cursor | 4.1 `formatsAt` · 4.3 ViewModel | 6.1, 6.4 |
| RF-15, RF-16 Solo el contenido | 4.3 título sin transformación | 6.5, 6.6 |
| RF-17 Pegar plano | D1 · 4.3 campo de texto | 6.6 |
| RF-18 Pegar con formatos activos | 4.1 `onTextChanged` | 6.1, 6.4, 6.6 |
| RF-19, RF-20 Guardado y fecha | 4.3 autoguardado · `SaveNoteUseCase` | 6.1, 6.4 |
| RF-21 Reabrir con formato | 4.2 columna y mappers · 4.3 carga | 6.2, 6.3, 6.4 |
| RF-22 Notas antiguas | 4.2 migración con valor vacío | 6.3, 6.4, 6.6 |
| RF-23, RF-24 Tarjetas | 4.4 `AnnotatedString` | 6.4, 6.5, 6.6 |
| RF-25 Búsqueda | D1: contenido plano | 6.1 |
| RNF-1 Datos | D2, D7 · migración | 6.3, 6.6 |
| RNF-2, RNF-3 Accesibilidad | 4.3 menú con ícono y `stateDescription` | 6.5 |
| RNF-4 Apariencia | D6 · tema | 6.6 |
| RNF-5 Idioma | 4.3 strings | 6.5 |

## 9. Riesgos

- **Edición con IME** (composición y autocorrección). Algunos teclados reemplazan palabras enteras, lo que puede mover los tramos de forma inesperada. `onTextChanged` trabaja por prefijo y sufijo comunes, y se cubrirá con pruebas de reemplazo. La verificación manual usará el teclado del emulador.
- **Cambio de `content` a `TextFieldValue`** en el editor. Afecta a las pruebas actuales del ViewModel y de la pantalla del editor, que habrá que adaptar.
- **Rendimiento de la `VisualTransformation`** con contenidos largos. Construir el `AnnotatedString` es lineal en el número de tramos. Si se notara lento, se memoriza por texto y tramos.
