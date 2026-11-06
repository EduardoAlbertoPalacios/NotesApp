# Plan 008 — Vista de notas: cuadrícula o lista

> **Estado:** implementado · **Fecha:** 2026-11-04 · **Spec:** [spec.md](spec.md) (implementada) · **Diseño:** Figma Mis notas en lista `62:311` y `55:880`; Ajustes `4:1794` y `40:1211`

Plan técnico para implementar la spec 008. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

La vista se guarda igual que el tema y el color predeterminado: con DataStore Preferences, detrás de `SettingsRepository`. Si falla el guardado, el repositorio mantiene la elección en memoria durante la sesión.

Mis notas conserva su único contenedor desplazable: la vista solo cambia el número de columnas (una o dos) y la tarjeta que se dibuja. El selector es un elemento más de la cabecera. Mientras la vista no se ha leído, la zona de notas muestra el estado de carga, de modo que nunca aparecen primero en la otra vista.

Las tareas seguidas se resuelven al construir el texto de la tarjeta: el contenido sigue siendo un único texto con casillas en línea, como hoy.

No hay cambios en el esquema de Room, dependencias nuevas ni cambios en `:app` o `:common`.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas. |
| 2. La spec manda | Cada parte cita sus RF. El mismo commit actualiza la spec 006 (sección 4.4). |
| 3. Lógica fuera de la UI | El valor por defecto vive en `:domain`. Los composables solo pintan estado y emiten intents. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, repositorio, mappers, ViewModels y UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | El esquema no cambia y cambiar de vista no toca las notas [RF-35]. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:domain` | `NotesView`; dos métodos en `SettingsRepository`; `GetNotesViewUseCase` y `SetNotesViewUseCase`. | RF-36, RF-38, RF-39, RF-40 |
| `:data` | Lectura, escritura y mappers en el paquete `settings`. | RF-36, RF-37, RF-39, RF-40 |
| `:presentation` | Selector y tarjeta de lista en Mis notas; fila y hoja en Ajustes; texto con tareas seguidas; íconos y textos. | RF-1 a RF-35, RF-37, RF-41 a RF-43 |
| Documentación | Spec 006 y `.memory/MEMORY.md`. | — |

## 4. Diseño por capa

### 4.1 `:domain` (paquete `settings`)

- `enum class NotesView { GRID, LIST }`, con `DEFAULT = GRID` en su `companion object` [RF-38, RF-39].
- `SettingsRepository` añade:
  - `fun observeNotesView(): Flow<NotesView?>`: `null` significa que no hay vista guardada o que no se puede leer.
  - `suspend fun setNotesView(view: NotesView): AppResult<Unit, SettingsError>`.
- `GetNotesViewUseCase`: convierte `null` en `NotesView.DEFAULT` y devuelve `Flow<NotesView>` [RF-38, RF-39].
- `SetNotesViewUseCase`: delega en el repositorio [RF-36].

### 4.2 `:data` (paquete `settings`)

`SettingsRepositoryImpl` repite el patrón de los otros dos ajustes, con su clave `notes_view` y su propio `MutableStateFlow`:

- **Lectura:** combina lo guardado con lo elegido en la sesión; lo elegido tiene prioridad [RF-37, RF-40].
- **Escritura:** pone la elección en memoria y guarda con `dataStore.edit`. Si lanza `IOException`, responde `AppResult.Error(SettingsError.Storage)` [RF-36, RF-40].
- **Lectura fallida o valor desconocido:** da `null` [RF-39].
- **Mappers:** `NotesView.toStorageValue()` y `String?.toNotesViewOrNull()`, con los valores estables `"grid"` y `"list"`.

La copia de seguridad ya excluye el archivo de preferencias entero.

### 4.3 `:presentation`

**Mis notas: estado (`notes/list/`)**

| Clase | Cambios |
|---|---|
| `NoteListUiState` | `notesView: NotesView? = null`, nula hasta la primera lectura |
| `NoteItemUi` | `category: NoteCategory? = null` y `listContent: AnnotatedString = content`, con sus valores por defecto para no tocar previews ni pruebas |
| `NoteListIntent` | `NotesViewSelected(view)` |
| `NoteListEffect` | `ShowNotesViewSaveError` [RF-41] |
| `NoteListViewModel` | Observa `GetNotesViewUseCase`. Al elegir, si la vista es distinta, llama a `SetNotesViewUseCase`, y un error emite el efecto [RF-3, RF-4, RF-36]. `toUi()` rellena los campos nuevos |
| `NoteListRoute` | Muestra el aviso en un `Snackbar`, sobre el botón «Nueva nota» [RF-41] |

La vista no cambia filtro ni texto buscado, porque son estados independientes [RF-32 a RF-34].

**Mis notas: pantalla**

- `LazyVerticalStaggeredGrid` recibe `Fixed(1)` en lista y `Fixed(2)` en cuadrícula, con las mismas claves por id y los mismos elementos de cabecera [RF-17, RF-31].
- `isLoading` sigue activo hasta conocer notas y vista, así que no se muestran en la otra vista [RF-37].
- `NotesViewSelector` va tras `Filters`. Sigue el frame: contenedor de 364 × 60 dp y dos opciones de 176 × 52 dp, con ícono de 18 dp y nombre. Usa `selectableGroup` y `selectable` con `Role.RadioButton`, y no depende de `isSearching` ni de que haya notas [RF-1, RF-2, RF-5, RF-6, RF-42, RF-43]. Con `notesView` nula no marca ninguna opción.
- `NoteCard` recibe la vista. En cuadrícula queda igual que hoy [RF-16]. En lista usa `listContent`, cuatro líneas como máximo y una fila inferior con `CategoryTag`, si la nota tiene categoría, y la fecha a la derecha [RF-18, RF-19, RF-24 a RF-26]. Imagen, ícono de fijada, colores y clic se comparten [RF-27 a RF-30].
- `NoteCategory.labelRes()` pasa de privada en el editor a `notes/common`, para usarla también en la etiqueta. `NotesView.labelRes()` se añade en el mismo sitio.
- Tokens nuevos en `Dimens`; colores y estilos exactos se toman de los frames al implementar. Íconos nuevos desde Figma: `ic_layout_grid` e `ic_list`.

**Texto con tareas seguidas (`notes/common/FormattedContent.kt`)**

**Ajuste al implementar:** el primer enfoque (espacios sin corte y `WORD JOINER`) falló en el emulador, porque Compose permite cortar entre una casilla en línea y su texto. El usuario eligió medir y decidir los cortes.

- `buildCardContent` añade `flowTasks: Boolean = false`. Con `true`, entre dos tareas consecutivas pone una marca (`TASK_BREAK`) en lugar del salto de línea [RF-20]. Los demás saltos se conservan [RF-22], y el formato y el tachado no cambian [RF-23, RF-29].
- `wrapTasks(maxWidth, separatorWidth, measure)`, función pura, resuelve cada marca: la tarea sigue en la línea si cabe entera tras el separador y, si no, empieza en la siguiente [RF-20, RF-21]. Una tarea más ancha que la línea empieza en una nueva y se reparte por palabras.
- `ListCardContent` obtiene el ancho con `BoxWithConstraints`, mide con `rememberTextMeasurer` y guarda el resultado con `remember` hasta que cambien el contenido o el ancho.

Sin tareas, `listContent` es el mismo objeto que `content`, y no se calcula nada de más.

**Ajustes (`settings/`)**

| Clase | Cambios |
|---|---|
| `SettingsUiState` | `notesView: NotesView = NotesView.DEFAULT`, `isNotesViewSheetVisible: Boolean = false` |
| `SettingsIntent` | `NotesViewRowClicked`, `NotesViewSelected(view)`, `NotesViewSheetDismissed` |
| `SettingsEffect` | `ShowNotesViewSaveError` [RF-41] |
| `SettingsViewModel` | Mismo patrón que el tema [RF-8, RF-11, RF-13, RF-36] |
| `SettingsScreen` | `NotesViewRow`, activa, con el valor en gris y regular, como en los frames [RF-7, RF-8] |
| `SettingsOptionSheet<T>` | Contenido genérico de `ThemeSheet` extraído (D2). `ThemeSheet` y `NotesViewSheet` quedan como envolturas [RF-9 a RF-13, RF-43] |

- Las dos pantallas observan el mismo repositorio, así que se mantienen sincronizadas sin más código [RF-14, RF-15].
- Textos nuevos: `notes_view_grid` («Cuadrícula»), `notes_view_list` («Lista») y `notes_view_save_error`. Se elimina `settings_notes_view_value`.

### 4.4 Documentación

- **Spec 006:** RF-9, RF-12 y la línea de «Fuera de alcance» dejan de incluir esta fila y remiten a la spec 008.
- **`.memory/MEMORY.md`:** estado y decisiones.

## 5. Decisiones técnicas

- **D1. Un solo contenedor con columnas variables.** Mantiene cabecera, claves, relleno y estados vacíos en un único sitio, con un cambio de una línea, y sigue usando el id como clave estable. Dos contenedores duplicarían toda la cabecera y perderían la posición de desplazamiento al cambiar.
- **D2. Extraer la hoja de opciones.** Copiar `ThemeSheet` duplicaría unas 100 líneas que deben verse igual. Es un cambio sobre código existente, necesario para esta tarea, y lo comprueban las pruebas actuales del tema.
- **D3. Tareas seguidas en un único texto.** Un `FlowRow` de casillas no se combina bien con el límite de cuatro líneas ni con el texto normal que puede haber entre tareas. Los cortes se calculan midiendo, porque el texto podría separar una casilla de su tarea.
- **D4. Vista nula hasta leerla.** Cumple RF-37 sin bloquear el dibujo de la cabecera. **Ajuste al implementar:** `isLoading` no termina hasta conocer notas y vista, porque el ViewModel las combina; así las pantallas de prueba con estado fijo no quedan en carga. `NotesView.DEFAULT` solo se aplica en el caso de uso, y la pantalla lo usa como columnas mientras la vista es nula.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:domain` | `GetNotesViewUseCaseTest`, `SetNotesViewUseCaseTest` | RF-36, RF-38, RF-39, propagación del error |
| `:data` | `SettingsRepositoryImplTest`, mappers | RF-36, RF-37, RF-39, RF-40; valor desconocido e `IOException` al leer y al escribir |
| `:presentation` (unitarias) | `NoteListViewModelTest` | RF-3, RF-4, RF-32 a RF-34, RF-37, RF-40, RF-41; vista nula hasta leer; elegir la misma vista |
| `:presentation` (unitarias) | `SettingsViewModelTest` | RF-8, RF-11, RF-13, RF-36, RF-40, RF-41; hoja abierta al recrear la pantalla |
| `:presentation` (unitarias) | `FormattedContentTest` | RF-20 a RF-23: tareas consecutivas y separadas, texto entre tareas, saltos conservados, formato y tachado en su sitio; sin `flowTasks` no cambia nada |
| `:presentation` (UI) | `NoteListScreenTest` | RF-1 a RF-6, RF-16 a RF-19, RF-24 a RF-30, RF-37, RF-42, RF-43, en ambos temas |
| `:presentation` (UI) | `SettingsScreenTest`, `SettingsRouteTest` | RF-7 a RF-13, RF-41, RF-43; las pruebas del tema siguen en verde tras extraer la hoja |
| Manual | Emulador | Demo de los criterios de finalización, RF-14, RF-15, RF-21 con tareas reales y comparación con Figma |

`SettingsScreenTest` deja de tratar esta fila como atenuada, y los fakes de `SettingsRepository` añaden los métodos nuevos.

## 7. Orden de implementación

1. Crear la rama `feat/notes-view`.
2. `:domain`: modelo, repositorio y casos de uso, con pruebas.
3. `:data`: mappers y repositorio, con pruebas.
4. Extraer `SettingsOptionSheet` y comprobar que las pruebas del tema siguen en verde.
5. Ajustes: estado, ViewModel, fila, hoja y textos, con pruebas.
6. Texto con tareas seguidas, con pruebas.
7. Mis notas: estado y ViewModel; selector, columnas y tarjeta de lista frente a Figma, con pruebas.
8. Actualizar la spec 006.
9. `./gradlew test`, pruebas instrumentadas, demo manual en ambos temas, comparación con Figma y revisión con `architecture-reviewer`.
10. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1 a RF-6 | `NotesViewSelector`, `NoteListViewModel` |
| RF-7 a RF-13 | `NotesViewRow`, `SettingsOptionSheet`, `SettingsViewModel` |
| RF-14, RF-15 | Repositorio observado por ambas pantallas |
| RF-16 a RF-19 | `NoteListScreen`, `NoteCard` |
| RF-20 a RF-23 | `buildCardContent` |
| RF-24 a RF-30 | `NoteCard`, `CategoryTag` |
| RF-31 a RF-35 | `NoteListViewModel`, sin relación con filtro, búsqueda ni notas |
| RF-36 a RF-39 | `SettingsRepositoryImpl`, mappers, casos de uso, `NoteListScreen` |
| RF-40, RF-41 | `SettingsRepositoryImpl`, ViewModels, rutas |
| RF-42, RF-43 | `NotesViewSelector`, `SettingsOptionSheet` |
| Requisitos no funcionales | Secciones 4.2 y 4.3, `strings.xml` |
| Casos límite | Sección 6 |

## 9. Riesgos

- **Corte de línea de las tareas (RF-21):** resuelto midiendo cada tarea. Queda por comprobar que la medida coincide con lo que dibuja el texto, en la demo y con distintos tamaños de letra.
- **Contraste de la etiqueta:** se mide el color del frame contra cada color de nota. Si alguno no llega a 4,5:1, se oscurece solo el texto de la etiqueta.
- **Aviso sobre el botón flotante:** Mis notas aún no tiene `Snackbar`, y debe colocarse sin tapar «Nueva nota».
- **Cambio visual brusco:** al pasar de una a dos columnas la cuadrícula se recoloca sin animación; la spec no pide ninguna.
- **Aviso al salir:** si el usuario sale justo tras un fallo al guardar, puede no ver el aviso. La vista sigue vigente.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
