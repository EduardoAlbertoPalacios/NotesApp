# Plan 003 — Color de la nota

> **Estado:** implementado · **Fecha:** 2026-10-21 · **Spec:** [spec.md](spec.md) (implementada) · **Diseño:** Figma `4:1748` («Selector de color»)

Plan técnico para implementar la spec 003. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

`NoteColor` pasa de seis a ocho valores: se agregan `NONE` («Sin color») y `VANILLA` («Vainilla»), y `SAND` pasa a ser el beige «Arena». El amarillo que hoy se guarda como `SAND` es el «Vainilla» del diseño (`#F4E6A4`), así que una **migración manual v4→v5** reescribe `SAND` → `VANILLA` en las notas existentes para que no cambien de aspecto [RF-29].

En el editor, el botón «Color de la nota» abre una `ModalBottomSheet` de Material 3. La opción elegida en la hoja vive en el estado de la UI hasta pulsar «Aplicar color», que asigna el color y guarda como cualquier otro cambio. El fondo del editor y sus textos toman el color de la nota con un `MaterialTheme` anidado solo sobre la zona de la nota.

No se agregan dependencias.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas: `ModalBottomSheet` ya está en Material 3. El ícono nuevo es un VectorDrawable. |
| 2. La spec manda | Cada parte cita sus RF. Si algo se aparta de la spec, se actualizan ambas en el mismo commit. |
| 3. Lógica fuera de la UI | Qué se guarda y cuándo lo decide el ViewModel con `SaveNoteUseCase`. Los composables solo pintan el `UiState` y emiten intents. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, mapper, migración, ViewModel y UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | Migración v4→v5 probada que conserva las notas y su color visible. Esquema 5 exportado. Nada de `fallbackToDestructiveMigration`. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:common` | Ninguno. | — |
| `:domain` | `NoteColor` con ocho valores, en el orden de la paleta. | RF-3, RF-22 |
| `:data` | Migración manual v4→v5 (`SAND` → `VANILLA`), esquema `5.json`, semilla con `VANILLA`. | RF-21, RF-29 |
| `:presentation` | Tema (colores nuevos y esquema de la nota), hoja del selector, estado e intents del editor, fondo del editor, barra de estado, tarjeta «Sin color», strings e íconos. | RF-1 a RF-28, RF-30 a RF-32 |
| `:app` | Ninguno: el editor cambia los íconos de la barra de estado desde `NoteEditorRoute` (ver 4.4). | — |

## 4. Diseño por capa

### 4.1 `:domain`

```kotlin
enum class NoteColor { NONE, AQUA, ROSE, VANILLA, LAVENDER, MINT, PEACH, SAND }
```

- El orden del enum es el de la paleta del diseño, así que la hoja recorre `NoteColor.entries` [RF-3].
- `AQUA` = Cielo y `SAND` = Arena (`#E2D5C3`, nuevo tono). `VANILLA` = Vainilla (`#F4E6A4`, el amarillo actual).
- `SaveNoteUseCase` no cambia: ya rechaza las notas vacías con `NoteError.EmptyNote` (cubre RF-19) y fija la fecha de edición al guardar (cubre RF-17).

### 4.2 `:data`

- **`NotesDatabase`:** `version = 5`. Se exporta `5.json`, igual que `4.json` salvo la versión.
- **`MIGRATION_4_5`:** es una `Migration` manual, porque cambia datos y no esquema; `AutoMigration` no puede hacerlo. Ejecuta `UPDATE notes SET color = 'VANILLA' WHERE color = 'SAND'` y se registra con `addMigrations` en `DatabaseModule`. Se implementa con la skill `room-migration` [RF-29].
- **Mappers:** sin cambios. El color se guarda con `name` y un valor desconocido sigue cayendo en `AQUA` [RF-21].
- **`SeedNotesCallback`:** la nota semilla amarilla pasa de `SAND` a `VANILLA`, para que una instalación nueva se vea igual que hasta ahora.

### 4.3 `:presentation` — Tema

- **`Color.kt`:** se agregan `Vanilla = #F4E6A4` y `Sand = #E2D5C3`, junto con `Swatch = #262628` (fondo de la muestra «Sin color») y `SwatchBorder = #323234` (borde de las muestras).
- **`NoteColors`:** gana `vanilla` y `none`. `none` vale `Graphite`, el gris de las superficies, con el texto claro de la app [RF-26].
- **`NoteColor.toContainerColor()`:** cubre los ocho valores. Se agregan `NoteColor.toContentColor()` y `toMetadataColor()`, que devuelven `Ink` y `Slate` sobre un color, o `Snow` y `Ash` sobre `NONE` [RF-25, RF-26].
- **`NoteColor.labelRes`:** el nombre visible de cada color en `strings.xml` [RF-3, RF-10, RF-30].
- **`NoteSurfaceTheme(color) { … }`** (en `notes/common`, porque usa `NoteColor`): un `MaterialTheme` anidado que copia el esquema actual y sustituye [RF-24, RF-25, RF-27]:
  - `background` por el color de la nota;
  - `onBackground`, `onSurface`, `primary` y `secondary` por `Ink` (`secondary` para que la etiqueta «Personal» no quede amarilla con texto del color de la nota);
  - `onSurfaceVariant` por `Slate`;
  - `onPrimary` por el color de la nota;
  - `surfaceContainer` y `outlineVariant` por un tinte translúcido de `Ink` (6 % y 16 %), para que las tarjetas de categoría sin elegir sigan legibles.

  También fija `LocalContentColor` en `Ink`, porque `MaterialTheme` no lo cambia. Con `NONE` no cambia nada, así que el editor sigue negro.
- **`AppColorsTheme { … }`:** dentro de un `NoteSurfaceTheme`, restaura el tema oscuro de la app para lo que no va sobre el color de la nota: la barra inferior (con el menú de formato), el menú «Más opciones» y el botón de quitar imagen.
- **Contraste:** `Ink` supera 8:1 sobre todos los colores. `Slate` (`#405357`) da 4,16:1 sobre Rosa y 4,45:1 sobre Lavanda, por debajo del 4,5:1 del RNF. Se oscurece a **`#3B4C50`**, que da 4,61:1 y 4,94:1 respectivamente; afecta también a la fecha de las tarjetas. Ver D5.

### 4.4 `:presentation` — Editor

**`NoteEditorUiState`**, campos nuevos:
- `pickerColor: NoteColor?`: la opción elegida en la hoja. Vale `null` con la hoja cerrada, así que la hoja se muestra cuando no es nulo [RF-4 a RF-7, RF-11].
- `isNoteVisible` (calculada): la nota cargó sin error. Mientras no lo es, el estado aún tiene el Cielo por defecto, así que ni el fondo ni la barra de estado toman el color.

**`NoteEditorIntent`**, intents nuevos:
- `ColorButtonClicked`: abre la hoja con `pickerColor = color` [RF-2, RF-4, RF-15].
- `PickerColorSelected(color)`: cambia `pickerColor` sin tocar `color` ni guardar [RF-5, RF-6, RF-11].
- `ColorApplied`: asigna `color = pickerColor` y cierra la hoja. Si el color cambió, guarda en el momento, igual que fijar la nota [RF-12, RF-13, RF-16, RF-28].
- `ColorPickerDismissed`: cierra la hoja descartando `pickerColor` [RF-14].

**`NoteEditorViewModel`**:
- `hasUnsavedChanges()` y los dos borradores (guardar y agregar imágenes) incluyen `color`. Así, aplicar el mismo color no deja cambios pendientes y no guarda ni cambia la fecha [RF-18].
- Con una nota nueva vacía, `SaveNoteUseCase` devuelve `EmptyNote`. El color se queda en el estado y viaja en el siguiente guardado, cuando el usuario escribe [RF-19, RF-20].
- La carga ya copia `note.color` al estado [RF-21].
- El estado vive en el ViewModel, así que la hoja abierta y la opción elegida sobreviven al giro de pantalla (caso límite de la spec).

**`NoteEditorScreen`**:
- **Botón de la barra:** el círculo de color pasa a ser un `IconButton` que emite `ColorButtonClicked`, con el `testTag` nuevo `TOOLBAR_COLOR` [RF-1]. Con «Sin color» muestra la muestra oscura del selector con `ic_slash` (12 dp), para no confundirse con la barra.
- **Fondo de color:** toda la pantalla se envuelve en `NoteSurfaceTheme`, con el color de la nota solo si `isNoteVisible`. La barra inferior, el menú «Más opciones» y el botón de quitar imagen usan `AppColorsTheme`; la hoja y el diálogo de borrar quedan fuera [RF-24, RF-25, RF-27, RF-28].
- **Pin:** con la nota fijada se usa el ícono relleno `ic_pin_filled`, porque sobre un color el de acento (`Ink`) no distingue el estado.
- **Barra de estado (en `NoteEditorRoute`, no en la pantalla, para que `NoteEditorScreen` siga sin estado y sus previews no toquen la ventana):** mientras `isNoteVisible` y el color no sea `NONE`, sus íconos pasan a oscuros con `WindowCompat.getInsetsController(...).isAppearanceLightStatusBars`. Se restauran al salir del editor con un `DisposableEffect` [RF-25].
- **Hoja (`NoteColorSheet`, `internal` en su propio archivo `NoteColorSheet.kt`, porque `NoteEditorScreen.kt` ya era largo):** su contenido se desplaza verticalmente, porque en horizontal no cabe entero y «Aplicar color» quedaría fuera de pantalla. `ModalBottomSheet` con `containerColor = surfaceContainer`, radio superior de 28 dp y el asa del diseño (36×4 dp, `onSurfaceVariant`). Se cierra con `onDismissRequest`, que cubre deslizar, tocar fuera y atrás, y con la X [RF-2, RF-14, RF-32].
  - **Cabecera:** título de 22 sp SemiBold, descripción de 13 sp y botón X de 44 dp, que usa el `ic_close` existente.
  - **Paleta:** dos filas de cuatro opciones, cada una de 76 dp de ancho. La muestra es un círculo de 50 dp con borde de 1 dp `SwatchBorder`; la elegida lleva un borde de 2 dp `Snow` y un `ic_check` de 22 dp en `Ink`. El nombre va en 11 sp, en `onSurfaceVariant`, o en SemiBold `primary` si está elegida. «Sin color» usa una muestra `Swatch` con el ícono nuevo `ic_slash` de 20 dp [RF-3, RF-5, RF-6].
  - **Accesibilidad:** cada opción es `selectable(role = RadioButton)` con la etiqueta del color, y el lector anuncia «Seleccionado» [RF-30, RF-31].
  - **Vista previa:** tarjeta de radio 16 dp y relleno 16 dp, pintada con `pickerColor` y sus colores de texto. Muestra el título de 16 sp SemiBold en una línea con elipsis, o «Sin título» si está vacío. Debajo, «Así se verá en tus notas · %1$s» en 12 sp [RF-7 a RF-10].
  - **«Aplicar color»:** botón de 50 dp de alto, a todo el ancho, con radio 28 dp, `primary` / `onPrimary` y 15 sp SemiBold [RF-12].
- **Medidas y tipos nuevos:** van como tokens en `Sizes` y `NotesTypography` del tema, no escritos a mano.
- **Previews:** una `ModalBottomSheet` no se dibuja en las previews estáticas, así que se previsualiza su contenido (`NoteColorSheetContent`) con Cielo y con «Sin color».
- **`testTag` nuevos:** `TOOLBAR_COLOR`, `COLOR_SHEET`, `COLOR_OPTION + color.name`, `COLOR_PREVIEW`, `COLOR_APPLY` y `COLOR_CLOSE`.
- **Strings nuevos:**
  - Uno por color: `note_color_none`, `note_color_aqua`, `note_color_rose`, `note_color_vanilla`, `note_color_lavender`, `note_color_mint`, `note_color_peach` y `note_color_sand`.
  - De la hoja: `note_color_sheet_description`, `note_color_preview_untitled`, `note_color_preview_caption` (con `%1$s`), `note_color_apply` y `note_color_close`.
  - El título de la hoja reutiliza `note_editor_toolbar_color`.

### 4.5 `:presentation` — Mis notas

- **`NoteCard`:** usa `toContainerColor()`, `toContentColor()` y `toMetadataColor()` en lugar de `noteColors.content` y `noteColors.metadata` fijos, así que «Sin color» se ve gris con texto claro [RF-23, RF-26].

## 5. Decisiones técnicas

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D1 | **Migración v4→v5 `SAND` → `VANILLA`** | El enum queda con los nombres del diseño y las notas existentes conservan su amarillo [RF-29]. Es una línea de SQL y se prueba con `MigrationTestHelper`. | **Mantener `SAND` como amarillo y llamar `BEIGE` a Arena:** evita la migración, pero deja el código con nombres que no coinciden con los que ve el usuario, una trampa para el futuro. |
| D2 | **`NONE` como valor del enum** | «Sin color» es una opción más de la paleta y se guarda igual que las demás. Sin cambios de esquema. | **`color` nulo:** obliga a cambiar la columna a anulable, una migración más compleja, y a tratar `null` en todas las capas. |
| D3 | **`pickerColor` en el `UiState`, no en el composable** | El descarte (RF-14, RF-15) y el giro de pantalla quedan cubiertos y se prueban en el ViewModel. | **`remember` en la hoja:** se pierde al girar y el comportamiento de descarte no se podría probar sin UI. |
| D4 | **`MaterialTheme` anidado (`NoteSurfaceTheme`)** | Los composables del editor ya leen `colorScheme`; basta con cambiar el esquema en la zona de la nota, sin tocar cada `Text` ni cada `Icon`. | **Pasar colores por parámetro a cada composable:** muchos cambios y fácil olvidar alguno. |
| D5 | **Oscurecer `Slate` a `#3B4C50`** | Cumple el contraste de 4,5:1 del RNF sobre los ocho colores. La diferencia con el diseño es apenas visible. | **Dejar `#405357`:** incumple el RNF sobre Rosa y Lavanda. **Usar `Ink` también para los textos secundarios:** se pierde la jerarquía del diseño. |
| D6 | **`ModalBottomSheet` de Material 3** | Trae el gesto de deslizar, el fondo atenuado, el cierre con atrás y la accesibilidad. | **Una hoja propia:** más código y más riesgo, sin ganancia. |
| D7 | **`Theme.secondary` pasa de `Sand` a `Vanilla`** | `Sand` pasa a ser el beige «Arena»; con `Vanilla` el `secondary` de la app conserva su amarillo. | — |

## 6. Estrategia de pruebas

Todas las escribe el agente **`test-writer`**. Se ejecutan con `./gradlew test` y con `connectedDebugAndroidTest`.

### 6.1 Unitarias en `:domain`
- **`SaveNoteUseCaseTest`, que se amplía:** una nota con `NONE` o con un color nuevo se guarda con su color y su fecha nueva [RF-16, RF-17].

### 6.2 Unitarias en `:data`
- **`NoteMappersTest`, que se amplía:** los ocho colores van y vuelven entre la entidad y el dominio; un valor desconocido cae en `AQUA` [RF-21].

### 6.3 Instrumentadas en `:data`
- **`MigrationTest`, que se amplía:** de v4 a v5 una nota `SAND` pasa a `VANILLA`, las demás conservan su color, y el resto de los campos y el número de notas no cambian [RF-29].
- **`NoteDaoTest`:** una nota `NONE` se guarda y se lee igual [RF-21].

### 6.4 Unitarias en `:presentation`

**`NoteEditorViewModelTest`, que se amplía:**
- `ColorButtonClicked` abre la hoja con el color actual [RF-2, RF-4].
- `PickerColorSelected` cambia la opción sin cambiar el color ni guardar [RF-5, RF-11].
- `ColorApplied` con un color distinto asigna el color, cierra la hoja, guarda y actualiza la fecha [RF-12, RF-13, RF-16, RF-17].
- `ColorApplied` con el mismo color cierra la hoja sin guardar [RF-18].
- `ColorPickerDismissed` conserva el color, y al volver a abrir la hoja se ve el color actual [RF-14, RF-15].
- En una nota nueva vacía, aplicar un color no la guarda; al escribir se guarda con ese color [RF-19, RF-20].
- Al cargar una nota, el estado lleva su color [RF-21].

**`NoteListViewModelTest`, que se amplía:** las notas `NONE` llegan a la UI con su color.

### 6.5 UI en `:presentation`

**`NoteEditorScreenTest`, que se amplía:**
- El botón «Color de la nota» está habilitado y emite `ColorButtonClicked` [RF-1].
- Con `pickerColor` no nulo se ve la hoja con su título, su descripción y ocho opciones en orden [RF-2, RF-3].
- La opción elegida está marcada como seleccionada y las demás no [RF-4, RF-5, RF-31].
- Tocar una opción emite `PickerColorSelected` [RF-5].
- La vista previa muestra el título o «Sin título», y el texto con el nombre del color [RF-8 a RF-10].
- «Aplicar color» emite `ColorApplied`, y la X emite `ColorPickerDismissed` [RF-12, RF-14].
- Las opciones y la X tienen etiqueta accesible [RF-30, RF-32].

**`NoteListScreenTest`, que se amplía:** una tarjeta `NONE` muestra su título y su contenido [RF-26].

### 6.6 Verificación manual en el emulador
- Abrir la hoja, elegir y aplicar un color; comprobar el fondo del editor, los textos oscuros, la barra de estado y la tarjeta [RF-23 a RF-25, RF-28].
- Cerrar sin aplicar con la X, deslizando, tocando fuera y con atrás [RF-14].
- Girar la pantalla con la hoja abierta.
- Aplicar «Sin color» y ver el editor negro y la tarjeta gris [RF-26, RF-27].
- En una nota nueva vacía, aplicar un color y salir; no debe aparecer ninguna nota [RF-19].
- Instalar sobre la versión actual: las notas amarillas siguen amarillas y se muestran como «Vainilla» en la hoja [RF-29].
- Comparar la hoja con la captura de Figma `4:1710`.

## 7. Orden de implementación

1. **`:domain`:** `NoteColor` con ocho valores, con sus pruebas.
2. **`:data`:** migración v4→v5 con la skill `room-migration`, `5.json`, semilla y pruebas de migración y mappers.
3. **`:presentation`, tema:** colores nuevos, `Slate` ajustado, extensiones de color y `NoteSurfaceTheme`.
4. **`:presentation`, estado del editor:** `UiState`, intents y ViewModel, con sus pruebas.
5. **`:presentation`, interfaz:**
   - Hoja, botón y fondo del editor, con sus pruebas de UI.
   - Barra de estado.
   - Tarjeta «Sin color».
   - Strings e ícono `ic_slash`, el `slash` de Lucide que usa el diseño.
6. **Verificación:**
   - `./gradlew test`, `connectedDebugAndroidTest`, `assembleDebug` y `lint`.
   - Revisión con `architecture-reviewer`.
   - Pruebas manuales de 6.6.
7. **Cierre:** actualizar `.memory/MEMORY.md` y marcar spec y plan como implementados.

## 8. Trazabilidad de requisitos

| RF / RNF | Dónde se implementa | Dónde se prueba |
|---|---|---|
| RF-1 Habilitar botón | 4.4 barra | 6.5 |
| RF-2 a RF-4 Abrir la hoja | 4.4 `ColorButtonClicked` · hoja | 6.4, 6.5 |
| RF-5, RF-6 Elegir opción | 4.4 `PickerColorSelected` | 6.4, 6.5 |
| RF-7 a RF-10 Vista previa | 4.4 hoja | 6.5 |
| RF-11 Sin cambios hasta aplicar | D3 · `pickerColor` | 6.4 |
| RF-12, RF-13 Aplicar | 4.4 `ColorApplied` | 6.4, 6.5 |
| RF-14, RF-15 Descartar | 4.4 `ColorPickerDismissed` · `onDismissRequest` | 6.4, 6.5, 6.6 |
| RF-16 a RF-18 Guardado y fecha | 4.4 `hasUnsavedChanges` · `SaveNoteUseCase` | 6.1, 6.4 |
| RF-19, RF-20 Nota nueva vacía | 4.1 `EmptyNote` · 4.4 borrador | 6.4, 6.6 |
| RF-21 Reabrir | 4.2 mappers · 4.4 carga | 6.2, 6.3, 6.4 |
| RF-22 Notas nuevas en Cielo | 4.4 `AQUA` por defecto, sin cambios | 6.4 |
| RF-23 Tarjeta | 4.5 `NoteCard` | 6.5, 6.6 |
| RF-24, RF-25, RF-28 Fondo del editor | 4.3 `NoteSurfaceTheme` · 4.4 barra de estado | 6.6 |
| RF-26, RF-27 «Sin color» | 4.3 `none` · 4.5 | 6.5, 6.6 |
| RF-29 Notas existentes | D1 · migración v4→v5 | 6.3, 6.6 |
| RF-30 a RF-32 Accesibilidad | 4.4 `selectable` y etiquetas | 6.5 |
| RNF Aspecto | 4.4 medidas del diseño | 6.6 |
| RNF Contraste | D5 · `Slate` ajustado | 4.3 (cálculo) |
| RNF Idioma | 4.4 strings | 6.5 |
