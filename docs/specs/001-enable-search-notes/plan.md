# Plan 001 — Buscar en mis notas

> **Estado:** implementado · **Fecha:** 2026-10-13 · **Spec:** [spec.md](spec.md) (implementada)

Plan técnico para implementar la spec 001. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

La búsqueda se resuelve **en memoria, dentro de `:domain`**, sobre la lista de notas que Room ya emite. Se amplía `GetNotesUseCase` para que reciba el texto buscado: aplica la coincidencia flexible y la regla «la búsqueda ignora la categoría». También devuelve el total de notas del usuario. `NoteListViewModel` guarda el texto en `SavedStateHandle` y la pantalla convierte el buscador visual en un campo editable.

No cambia el esquema de la base, no hay migración y no se agregan dependencias.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas. La normalización de texto usa `java.text.Normalizer`, de la biblioteca estándar de la JVM. |
| 2. La spec manda | Cada parte del plan cita sus RF. Si algo se aparta de la spec durante la implementación, la spec se actualiza en el mismo commit. |
| 3. Lógica fuera de la UI | Coincidencia, orden, «ignorar la categoría» y conteos viven en `:domain`. El ViewModel orquesta el estado y los composables solo pintan el `UiState` y emiten intents. |
| 4. Sin pruebas no hay commit | Hay pruebas unitarias de dominio y ViewModel, y pruebas de UI de la pantalla (sección 6), escritas por el agente `test-writer`. `./gradlew test` debe pasar antes de cada commit. |
| 5. Los datos no se pierden | No se toca el esquema de Room: la versión sigue en 3 y no hay migración. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español. Los textos nuevos van en español en `strings.xml`, con plurales. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:common` | Ninguno. Ya existen el qualifier `@DefaultDispatcher` y `AppResult`. | — |
| `:domain` | Se amplía `GetNotesUseCase` y se agregan el modelo `FilteredNotes` y el objeto interno `NoteTextMatcher` (sección 4.1). | RF-1 a RF-5, RF-10 |
| `:data` | Ninguno. `NoteDao.observeAll()` ya emite todas las notas y se actualiza solo al editar o eliminar. | RF-9 (CA-9.2) |
| `:presentation` | Cambian `NoteListUiState`, `NoteListIntent`, `NoteListViewModel` y `NoteListScreen`. Hay strings y plurales nuevos y se reutiliza el ícono `ic_close`. | RF-1, RF-4, RF-6 a RF-11 |
| `:app` | Ninguno. | — |

Dependencias entre módulos: sin cambios (`presentation → domain → common`).

## 4. Diseño por capa

### 4.1 `:domain`

**`NoteTextMatcher`** (`internal object`, sin estado) [RF-2, RF-3]

Es un `object` y no una clase inyectable: `GetNotesUseCase` es público y su constructor no puede recibir un tipo `internal`.

Normaliza el texto de la nota y el buscado antes de compararlos:

1. Pasa todo a minúsculas con `Locale.ROOT` [CA-3.1].
2. Protege la «ñ» y luego descompone con `Normalizer` (NFD), elimina las marcas diacríticas (tildes y diéresis) y recompone [CA-3.2, CA-3.3].
3. Divide el texto buscado en palabras por espacios, descartando las vacías [CA-3.6].

Una nota coincide si **cada** palabra aparece como subcadena del título o del contenido normalizados. Cada palabra puede aparecer en un campo distinto [CA-2.1, CA-3.4, CA-3.5]. La categoría, el color, la fecha y las imágenes no se consideran [CA-2.2, CL-5]. Los signos y emojis se comparan tal cual [CL-2].

**`GetNotesUseCase(category: NoteCategory?, query: String = "")`** [RF-1, RF-4, RF-5, RF-10]

- Si `query` está en blanco, filtra por categoría igual que hoy [CA-1.3, CA-4.4].
- Si `query` tiene texto, ignora la categoría y filtra con `NoteTextMatcher` sobre todas las notas [CA-4.1].
- Mantiene el orden actual en ambos casos: fijadas primero y después por última edición, de la más reciente a la más antigua [CA-5.1].
- Devuelve `Flow<AppResult<FilteredNotes, NoteError>>`, donde `FilteredNotes(notes, totalCount)`:
  - `notes` es la lista filtrada y ordenada.
  - `totalCount` es el número total de notas del usuario, sin filtrar. Distingue «sin notas» de «sin resultados» [CA-7.2, RF-10].
- El filtrado corre en `@DefaultDispatcher` mediante `flowOn`, para no bloquear el hilo principal [RNF-1, RNF-2].

### 4.2 `:presentation`

**`NoteListUiState`**, campos nuevos [RF-1, RF-4, RF-6, RF-7, RF-10]:
- `query: String`: el texto tal como lo escribió el usuario.
- `totalCount: Int`.
- Derivados:
  - `isSearching`: `query` no está en blanco.
  - `isSearchEnabled`: `totalCount > 0`.
  - `showNoResults`: `isSearching` y `notes` vacía, con `totalCount > 0`.
  - `displayQuery`: `query` sin espacios al inicio ni al final, para el mensaje de sin resultados.

**`NoteListIntent`**, intents nuevos:
- `QueryChanged(text)` [RF-1].
- `ClearQueryClicked` [RF-8].

**`NoteListViewModel`**:
- Combina `selectedFilter` con el texto buscado y llama a `getNotes(category, query)` con `flatMapLatest`. Cada tecla cancela el cálculo anterior [CA-1.1, CA-1.2].
- `QueryChanged` actualiza `_state` de forma **síncrona** antes de lanzar el filtrado. Así el campo nunca recibe un valor viejo y no se pierden caracteres [RNF-2].
- Guarda el texto en `SavedStateHandle`. Al recrearse tras la muerte del proceso, lo restaura [CA-9.3]. Si el usuario cierra la app, Android descarta ese estado y vuelve sin búsqueda [CA-9.4]. Al volver del editor, el ViewModel sigue vivo en la pila de navegación y conserva el texto [CA-9.1].
- `FilterSelected` con una búsqueda activa vacía el texto y selecciona el chip [CA-4.3]. Sin búsqueda, se comporta como hoy.
- `ClearQueryClicked` vacía el texto [CA-8.2].
- Si `totalCount` pasa a 0 con una búsqueda activa, vacía el texto [CL-8, CA-10.1].
- `CreateNoteClicked` sigue enviando la categoría de `selectedFilter`, que la búsqueda no modifica [RF-11, CL-9].
- Cuando Room emite tras editar o eliminar una nota, el filtro se vuelve a aplicar solo [CA-9.2].

**`NoteListScreen`**:
- **Buscador** [RF-1, RF-8, RF-10]:
  - El `SearchField` visual se convierte en un `BasicTextField` con la misma apariencia del Figma: píldora, ícono, marcador y tokens del tema.
  - Acción de teclado «Buscar», que oculta el teclado.
  - Botón de limpiar con `ic_close`, visible solo con texto [CA-8.1].
  - Se deshabilita cuando `isSearchEnabled` es falso [CA-10.1, CA-10.2].
- **Chips** [CA-4.2]: con `isSearching` se pintan con el estilo no seleccionado, incluido el chip elegido, y llevan la descripción de estado «inactivo». Siguen pudiéndose tocar [CA-4.3].
- **Encabezado** [RF-6]: con `isSearching` muestra el plural `note_list_search_results` («1 resultado», «3 resultados») en lugar de `note_list_summary`.
- **Sin resultados** [RF-7]: con `showNoResults` muestra `note_list_search_no_results` con el texto buscado, sin espacios sobrantes. El mensaje de lista vacía queda para `totalCount == 0` [CA-7.2, CL-6].
- **Accesibilidad** [RNF-5]:
  - El campo usa el marcador como etiqueta y el botón de limpiar tiene `contentDescription`.
  - El contador y el mensaje de sin resultados usan `liveRegion` educado, para que los lectores de pantalla los anuncien.
- **Etiquetas de prueba nuevas:** `SEARCH_FIELD`, `SEARCH_CLEAR`, `SEARCH_NO_RESULTS` y `SUMMARY`.

**Strings nuevos** en `presentation/src/main/res/values/strings.xml` [RNF-6]:
- Plural `note_list_search_results`.
- `note_list_search_no_results`, con `%1$s`.
- `note_list_search_clear`.
- `note_list_filter_inactive`.

El marcador `note_list_search_placeholder` ya existe.

## 5. Decisiones técnicas

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D1 | Filtrar **en memoria en `:domain`** | Room ya emite todas las notas para la lista. Con 1 000 notas, filtrar en memoria cuesta milisegundos. La regla «ñ distinta de n» con tildes ignoradas es fácil de expresar y probar en Kotlin puro. | **SQL `LIKE`**: SQLite solo ignora mayúsculas en ASCII («Ñ» y «Á» fallan) y no ignora tildes. **FTS4/FTS5 con `unicode61 remove_diacritics`**: convierte «ñ» en «n», lo que va contra CA-3.3. Además exige una tabla nueva y una migración (principio 5) para un volumen que no lo necesita. |
| D2 | Ampliar **`GetNotesUseCase`** con `query` | El orden y la regla «el texto anula la categoría» quedan en un solo lugar y se prueban juntos. El ViewModel sigue con un único flujo de notas. | **Crear `SearchNotesUseCase` aparte**: duplicaría el orden. Además, el ViewModel tendría que decidir qué caso de uso llamar, y esa regla de negocio acabaría en la presentación (principio 3). |
| D3 | Devolver **`FilteredNotes(notes, totalCount)`** | Con un filtro de categoría o con una búsqueda, la lista puede quedar vacía aunque el usuario tenga notas. `totalCount` distingue sin notas, sin resultados y buscador deshabilitado (RF-7, RF-10) con una sola lectura coherente. | **Observar dos flujos** (todas las notas y las filtradas) y combinarlos: dobla las lecturas de Room y puede mostrar estados intermedios inconsistentes. |
| D4 | Guardar el texto en **`SavedStateHandle`** | Sobrevive a la muerte del proceso (CA-9.3) y Android lo descarta cuando el usuario cierra la app (CA-9.4), justo lo que pide la spec. Ya viene con el stack. | **DataStore o una tabla de Room**: es una dependencia nueva (principio 1), recordaría la búsqueda después de cerrar la app (va contra CA-9.4) y la guardaría como historial (RNF-3). |
| D5 | **Sin debounce**; filtrar en `@DefaultDispatcher` con `flatMapLatest` | El filtrado es barato y un debounce consumiría casi todo el margen de 300 ms (RNF-1). `flatMapLatest` descarta los cálculos viejos. | **Debounce de 300 ms**: retrasa cada resultado sin necesidad y deja el tiempo justo en el límite de RNF-1. |
| D6 | **Normalizar en cada búsqueda**, sin caché | Es lo más simple y cabe en el presupuesto de tiempo. | **Guardar el texto normalizado de cada nota**: más rápido, pero agrega estado e invalidación. Queda como contingencia si la búsqueda se nota lenta en la verificación manual. |
| D7 | Los chips **siguen pudiéndose tocar** durante la búsqueda | CA-4.3 exige que tocar un chip limpie la búsqueda. Si se deshabilitaran, no se podría. | **Deshabilitarlos** (`enabled = false`): bloquea CA-4.3 y los lectores de pantalla los anunciarían como no disponibles. |
| D8 | Mantener el **texto crudo** en el estado y normalizar solo en el dominio | El campo muestra exactamente lo que escribió el usuario, y el mensaje de sin resultados lo repite sin espacios sobrantes. | **Normalizar en el ViewModel**: lógica de negocio en la presentación (principio 3). |

## 6. Estrategia de pruebas

Todas las pruebas las escribe el agente **`test-writer`**. Las ejecuta `./gradlew test` y, en el emulador, `connectedDebugAndroidTest`.

### 6.1 Unitarias en `:domain`

**`NoteTextMatcherTest`** [RF-2, RF-3, CL-1 a CL-5, CL-7]:
- Mayúsculas, incluidas «Ñ» y «ñ».
- Tildes: «reunion» encuentra «Reunión».
- Diéresis: «pinguino» encuentra «pingüino».
- «ano» **no** encuentra «año».
- Parte de una palabra: «reun».
- Varias palabras en cualquier orden y repartidas entre título y contenido.
- Espacios al inicio, al final y repetidos.
- Texto con solo espacios.
- Signos y emojis.
- Texto muy largo.
- Nota sin título o sin contenido.
- No coincide por categoría ni con una nota que solo tiene imágenes.

**`GetNotesUseCaseTest`**, que se amplía [RF-1, RF-4, RF-5, RF-10]:
- Sin texto, filtra por categoría como hoy.
- Con texto, ignora la categoría.
- El orden de fijadas y fecha se mantiene en los resultados.
- `totalCount` no depende del filtro ni del texto.
- Propaga los errores de almacenamiento.

**Rendimiento** [RNF-1]: sin prueba con 1 000 notas, por decisión del usuario. RNF-1 se respalda con el diseño (D1, D5, D6) y con la verificación manual de 6.4 sobre las notas existentes.

### 6.2 Unitarias en `:presentation`

**`NoteListViewModelTest`**, que se amplía:
- `QueryChanged` actualiza `query` y los resultados, y `isSearching` cambia [RF-1].
- El conteo de resultados, `showNoResults` y la diferencia frente a `totalCount == 0` [RF-6, RF-7].
- Con búsqueda, los resultados incluyen notas de otras categorías [RF-4].
- `FilterSelected` con búsqueda vacía el texto y cambia el chip [CA-4.3].
- Vaciar el texto vuelve al chip anterior [CA-4.4].
- `ClearQueryClicked` [RF-8].
- El texto se restaura desde `SavedStateHandle` [CA-9.3].
- Una nueva emisión del repositorio tras editar o eliminar recalcula los resultados [CA-9.2].
- Si `totalCount` llega a 0, el texto se vacía y el buscador se deshabilita [RF-10, CL-8].
- `CreateNoteClicked` durante la búsqueda envía la categoría del chip previo [RF-11, CL-9].

### 6.3 UI en `:presentation`, sobre `NoteListScreen`

**`NoteListScreenTest`**, que se amplía:
- Escribir en el buscador emite `QueryChanged`. Usa un estado que se actualiza con los intents, por lo aprendido en el editor [RF-1].
- El botón de limpiar aparece solo con texto y emite `ClearQueryClicked` [RF-8].
- El encabezado muestra «N resultados» [RF-6].
- Aparece el mensaje de sin resultados con el texto buscado, sin el de lista vacía [RF-7].
- Con `totalCount == 0` el buscador está deshabilitado [RF-10].
- Durante la búsqueda, los chips llevan la descripción de estado «inactivo» y tocarlos emite `FilterSelected` [CA-4.2, CA-4.3].

### 6.4 Verificación manual en el emulador

Cubre los criterios de finalización de la spec:
- Buscar y ver el contador.
- Abrir un resultado y volver con la búsqueda conservada [CA-9.1].
- Girar la pantalla.
- Forzar la muerte del proceso con «No conservar actividades» o `adb shell am kill` y comprobar que el texto se restaura [CA-9.3].
- Cerrar la app desde recientes y comprobar que vuelve sin búsqueda [CA-9.4].
- Tocar un chip durante la búsqueda.
- Limpiar la búsqueda.
- Ver el mensaje de sin resultados.
- Con TalkBack, oír el contador y el mensaje [RNF-5].
- Comprobar que los resultados aparecen sin demora perceptible al escribir, con las notas existentes [RNF-1, RNF-2].

**Fuera de este plan:** la prueba de flujo completo con `HiltTestRunner`. Sigue pendiente en el proyecto y no la exige la spec.

## 7. Orden de implementación

1. **`:domain`:** `NoteTextMatcher`, `FilteredNotes` y la ampliación de `GetNotesUseCase`, con sus pruebas (`test-writer`).
2. **`:presentation`, estado:** `UiState`, `Intent` y `ViewModel` con `SavedStateHandle`, con sus pruebas.
3. **`:presentation`, interfaz:** buscador editable, chips inactivos, contador, mensaje de sin resultados, strings y accesibilidad, con sus pruebas de UI.
4. **Verificación:**
   - `./gradlew test`, `connectedDebugAndroidTest` y `assembleDebug`.
   - Revisión con `architecture-reviewer`.
   - Pruebas manuales de 6.4.
5. **Cierre:** actualizar `.memory/MEMORY.md` y marcar la spec como implementada. Si algo cambió durante la implementación, actualizar la spec en el mismo commit (principio 2).

## 8. Trazabilidad de requisitos

| RF / RNF | Dónde se implementa | Dónde se prueba |
|---|---|---|
| RF-1 Búsqueda al escribir | 4.1 caso de uso · 4.2 ViewModel y buscador | 6.1, 6.2, 6.3 |
| RF-2 Campos | 4.1 `NoteTextMatcher` | 6.1 |
| RF-3 Coincidencia flexible | 4.1 `NoteTextMatcher` | 6.1 |
| RF-4 Ignorar la categoría | 4.1 caso de uso · 4.2 ViewModel y chips | 6.1, 6.2, 6.3 |
| RF-5 Orden | 4.1 caso de uso | 6.1 |
| RF-6 Número de resultados | 4.2 encabezado | 6.2, 6.3 |
| RF-7 Sin resultados | 4.1 `totalCount` · 4.2 mensaje | 6.2, 6.3 |
| RF-8 Limpiar | 4.2 intent y botón | 6.2, 6.3 |
| RF-9 Conservar la búsqueda | 4.2 `SavedStateHandle` · Room reactivo | 6.2, 6.4 |
| RF-10 Buscador sin notas | 4.1 `totalCount` · 4.2 campo deshabilitado | 6.2, 6.3 |
| RF-11 Nota nueva durante una búsqueda | 4.2 `CreateNoteClicked` sin cambios | 6.2 |
| RNF-1 Rapidez | D1, D5, D6 · `@DefaultDispatcher` | 6.4 verificación manual, sin medición con 1 000 notas |
| RNF-2 Fluidez | 4.2 actualización síncrona de `query` | 6.3, 6.4 |
| RNF-3 Privacidad | D4: solo `SavedStateHandle`, sin red ni historial | Revisión |
| RNF-4 Sin conexión | D1: todo local | 6.4 |
| RNF-5 Accesibilidad | 4.2 etiquetas, descripción de estado y `liveRegion` | 6.3, 6.4 TalkBack |
| RNF-6 Idioma | 4.2 strings y plurales | Revisión · 6.3 |

## 9. Riesgos

- **Rendimiento sin medir a escala.** RNF-1 no se mide con 1 000 notas. Si la búsqueda se notara lenta con muchas notas o notas muy largas, se aplica D6 (guardar el texto normalizado).
- **Campo de texto y estado asíncrono.** Si el campo perdiera caracteres al escribir rápido, se pasaría a manejar el texto con `TextFieldState` en la pantalla y enviar solo el valor al ViewModel. No implica dependencias nuevas.
- **Cambio de tipo de `GetNotesUseCase`.** Hay que ajustar sus pruebas y las del ViewModel. Hoy es su único consumidor.
