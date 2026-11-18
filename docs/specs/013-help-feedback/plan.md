# Plan 013 — Ayuda y comentarios

> **Estado:** implementado · **Fecha:** 2026-11-18 · **Spec:** [spec.md](spec.md) (aprobada) · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`, nodos `102:68` (Oscuro) y `102:2` (Claro)

Plan técnico para implementar la spec 013. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

Una pantalla nueva, **Ayuda y comentarios**, a la que se entra desde la fila de Ajustes, que deja de estar atenuada [RF-1 a RF-3].

- Las 12 preguntas y respuestas son textos de recursos. Las sirve un repositorio de `:data`, como los textos propios del PDF de exportar (spec 011, D2) [RF-7].
- La búsqueda es una regla pura de `:domain` que reutiliza la normalización de la búsqueda de notas, así que «las mismas reglas que la spec 001» se cumplen por construcción [RF-17 a RF-19].
- La pantalla solo guarda dos cosas: el texto buscado y las preguntas abiertas. Ambas viven en el `SavedStateHandle` de su entrada de navegación: sobreviven al giro y se pierden al salir [RF-29, RF-30].
- La fila «Calificar en Google Play» y su aviso se pintan deshabilitados, sin intent asociado [RF-25 a RF-28].

**Sin dependencias nuevas, sin cambios de esquema.** Un icono nuevo (estrella) sale del diseño.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Solo Compose, Hilt y recursos de Android, que ya están. |
| 2. La spec manda | Cada parte cita sus RF. El mismo commit actualiza la spec 006 (sección 4.5). |
| 3. Lógica fuera de la UI | Qué preguntas coinciden se decide en `:domain`. La pantalla pinta el estado y emite intents. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, del repositorio, del ViewModel y de UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | No toca Room ni DataStore. |
| 6. Idioma | Identificadores en inglés; preguntas, respuestas y textos de la pantalla en español en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:domain` | Paquete `help`: `Faq`, `FaqRepository`, `FaqSearch` y `SearchFaqsUseCase`. `NoteTextMatcher` sin cambios de comportamiento. | RF-7, RF-17 a RF-19 |
| `:data` | Paquete `help`: `FaqRepositoryImpl` con las 12 preguntas en `data/res/values/strings.xml`, y su `@Binds`. | RF-7 |
| `:presentation` | Paquete `help` (MVI completo), fila de Ajustes habilitada, ruta `settings/help`, buscador extraído a `common` (D3). | RF-1 a RF-30 |
| `:app` | Ninguno. | — |
| Documentación | Spec 006 y `.memory/MEMORY.md`. | — |

## 4. Diseño por capa

### 4.1 `:domain` (paquete `help`)

- **`Faq(id: Int, question: String, answer: String)`.** El `id` es la posición fija (1 a 12) y sirve para recordar qué preguntas están abiertas, aunque cambie el idioma o el texto.
- **`FaqRepository`**: `suspend fun getFaqs(): List<Faq>`, en el orden de la tabla de la spec [RF-7]. No es un `Flow` porque el contenido no cambia mientras la app está abierta.
- **`FaqSearch`** (`object`, reglas puras): `filter(faqs, query): List<Faq>`.
  - Usa `NoteTextMatcher.words(query)` y `NoteTextMatcher.normalize`, que ya son `internal` del mismo módulo: minúsculas, sin tildes, «ñ» ≠ «n», espacios exteriores ignorados [RF-18, CL-2].
  - Coincide si **cada palabra** buscada aparece en la pregunta o en la respuesta, como en la búsqueda de notas, donde las palabras pueden repartirse entre título y contenido [RF-17, CL-1].
  - Sin palabras (vacío o solo espacios) devuelve todas, en su orden [RF-19].
- **`SearchFaqsUseCase(query): List<Faq>`**: lee el repositorio y aplica `FaqSearch.filter`.

`NoteTextMatcher` no se renombra ni se mueve: solo se reutiliza (D2).

### 4.2 `:data` (paquete `help`)

- **`FaqRepositoryImpl`** (`internal`): con `@ApplicationContext` lee pares de textos `help_faq_1_question` / `help_faq_1_answer` … `_12_` de `data/src/main/res/values/strings.xml`, en el `@IoDispatcher` [RF-7].
- La lista de ids de recurso vive en el propio repositorio, en orden. Lee los textos en cada llamada, así que un cambio de idioma del sistema se refleja al volver a entrar.
- **`RepositoryModule`**: `@Binds FaqRepository`.

### 4.3 `:presentation`

**Ayuda** (paquete `help`, MVI completo):

| Clase | Contenido |
|---|---|
| `HelpUiState` | `query: String`, `faqs: List<FaqUi(id, question, answer, isExpanded)>`, `isLoading`; derivado `showNoResults` (texto no vacío y lista vacía) |
| `HelpIntent` | `QueryChanged(text)`, `ClearQueryClicked`, `FaqClicked(id)`, `BackClicked` |
| `HelpEffect` | `NavigateBack` |
| `HelpViewModel` | Guarda `query` y `expandedIds: Set<Int>` en el `SavedStateHandle`. Combina ambos y llama a `SearchFaqsUseCase` en cada cambio del texto. `FaqClicked` añade o quita el id del conjunto, sin depender del filtro [RF-11 a RF-14, RF-23] |
| `HelpRoute` / `HelpScreen` | `BackHandler` y pantalla sin estado |

**La pantalla** (según los nodos `102:68` y `102:2`):
- **Barra superior:** la de Ajustes, con Volver y el título [RF-4].
- **Contenido:** una `LazyColumn`. Su `rememberLazyListState` es guardable, así que la posición sobrevive al giro [RF-29].
  - Cabecera «¿En qué podemos ayudarte?» y su texto [RF-5].
  - Buscador común (D3) con «Buscar una pregunta» y «Limpiar búsqueda» [RF-6, RF-21, RF-22].
  - Grupo «Preguntas frecuentes»: tarjeta con las preguntas filtradas, `key = id` y separadores [RF-7]. Cada fila es `toggleable` con `Role.Button`: la etiqueta es la pregunta y el estado se anuncia como abierta o cerrada [RF-16]. Muestra el chevron derecho cerrada y el mismo icono girado hacia arriba abierta [RF-15]. La respuesta aparece debajo con `AnimatedVisibility` [RF-12, RF-13].
  - Sin resultados: «No hay preguntas que coincidan con «texto»» en el lugar de la tarjeta [RF-20].
  - Grupo «Valora la app»: la fila «Calificar en Google Play» con el icono de estrella, deshabilitada con la misma opacidad y semántica `disabled()` que las filas deshabilitadas de Ajustes, y el aviso atenuado debajo [RF-8, RF-9, RF-24 a RF-28].
- **Tema:** solo tokens de `NotesTheme` y `MaterialTheme` [RF-10].

**Componentes de Ajustes reutilizados (D4):** `SettingsGroup`, `SettingsRow`, `DisabledRow`, `Chevron` y la barra superior son hoy `private` en `SettingsScreen.kt`. Pasan a `presentation/common/SettingsComponents.kt` como `internal`, sin cambiar su aspecto ni su comportamiento.

**Ajustes** [RF-1, RF-2]:
- La fila Ayuda pasa de `DisabledRow` a una fila con acción, con la misma descripción y el chevron.
- `SettingsIntent.HelpRowClicked` → `SettingsEffect.NavigateToHelp`. `NotesNavHost` añade la ruta `settings/help`.

**Textos nuevos en `presentation`:** el título, la cabecera y su texto, «Buscar una pregunta», «Limpiar búsqueda», «Preguntas frecuentes», el aviso sin resultados, «Valora la app», «Calificar en Google Play», su descripción, el aviso de Google Play, «Volver» y los estados «Abierta» / «Cerrada» para la accesibilidad.

### 4.4 Flujo

```
Ajustes ─ Ayuda y comentarios ─▶ Ayuda ─┬─ escribir ─▶ SearchFaqsUseCase ─▶ lista filtrada / sin resultados
                                         ├─ tocar pregunta ─▶ abrir / cerrar (varias a la vez)
                                         ├─ Calificar en Google Play ─▶ nada (deshabilitada)
                                         └─ Volver / Atrás ─▶ Ajustes (se olvidan búsqueda y preguntas abiertas)
```

### 4.5 Documentación

- **Spec 006:** RF-9 deja de citar Ayuda y comentarios como atenuada («funciona desde la spec 013»); la línea de «Fuera de alcance» remite a la 013.
- **`.memory/MEMORY.md`:** estado y decisiones.

## 5. Decisiones técnicas

- **D1. Las preguntas viven en `:data` como recursos.** **Aprobado** el 2026-11-18. La constitución pide los textos en `strings.xml`, y `:domain` no puede leer recursos de Android. Un repositorio en `:data` los convierte en `Faq`, así el ViewModel solo depende de un caso de uso y la búsqueda se prueba en `:domain` con un fake. Es el mismo enfoque que los textos del PDF (spec 011, D2). **Alternativa:** dejarlas en `presentation/strings.xml` y que la `Route` resuelva los textos y se los pase al ViewModel con un intent. Evita el repositorio, pero mete la carga de datos en la UI.
- **D2. Reutilizar `NoteTextMatcher`.** **Aprobado** el 2026-11-18. `FaqSearch` llama a sus funciones `words` y `normalize`, ya `internal` en `:domain`. Garantiza las mismas reglas que la spec 001 sin duplicar la expresión regular ni tocar la búsqueda de notas. **Alternativa:** extraer un `TextMatcher` genérico y renombrar; es un refactor de código ajeno a esta spec.
- **D3. El buscador pasa a `presentation/common`.** **Aprobado** el 2026-11-18. El `SearchField` de Mis notas es `private` y ya tiene el aspecto del diseño (fondo, icono, «Limpiar búsqueda»). Se extrae como `SearchField(query, placeholder, onQueryChange, onClear, modifier, enabled, testTags)` y Mis notas lo usa sin cambiar su aspecto ni su comportamiento. **Alternativa:** copiarlo en `help`, lo que duplica unas 70 líneas.
- **D4. Componentes de filas de Ajustes a `common`.** **Aprobado** el 2026-11-18. El diseño usa los mismos grupos y filas que Ajustes. Se extraen sin cambios. **Alternativa:** reconstruirlos en `help` con los mismos tokens, a riesgo de que diverjan.
- **D5. Estado en el `SavedStateHandle`.** **Aprobado** el 2026-11-18. Texto y conjunto de ids abiertos sobreviven al giro y a la muerte del proceso. Como el ViewModel pertenece a la entrada de navegación, salir y volver crea uno nuevo con todo cerrado y el campo vacío [RF-29, RF-30].

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:domain` | `FaqSearchTest` | Vacío y solo espacios, coincidencia en pregunta, solo en respuesta, palabras repartidas, mayúsculas y tildes, «ñ» ≠ «n», sin resultados, orden conservado [RF-17 a RF-20] |
| `:domain` | `SearchFaqsUseCaseTest` con repositorio fake | Lee y filtra |
| `:data` (instrumentada) | `FaqRepositoryImplTest` | 12 preguntas en orden, ids 1 a 12, sin textos vacíos |
| `:presentation` (unitarias) | `HelpViewModelTest` | Todas cerradas al empezar, abrir y cerrar varias, filtrar, sin resultados, limpiar, abiertas conservadas al filtrar, restauración desde `SavedStateHandle`, volver |
| `:presentation` (unitarias) | `SettingsViewModelTest` | `HelpRowClicked` → `NavigateToHelp` |
| `:presentation` (UI) | `HelpScreenTest`, parametrizada en ambos temas | Cabecera, lista, abrir/cerrar y su estado accesible, sin resultados, «Limpiar búsqueda», fila de Google Play deshabilitada y sin acción |
| `:presentation` (UI) | `SettingsScreenTest`, `NoteListScreenTest` | Fila de Ayuda habilitada; el buscador de Mis notas sigue igual tras extraerlo |
| Manual | Emulador | Los flujos de la sección 4.4, giro con preguntas abiertas y búsqueda, letra al 200 %, en ambos temas |

## 7. Orden de implementación

1. Crear la rama `feat/help-feedback` desde `main`.
2. `:domain`: modelo, repositorio, `FaqSearch` y caso de uso, con pruebas.
3. `:data`: textos, repositorio y binding, con prueba.
4. `:presentation`:
   1. extraer el buscador y los componentes de Ajustes (D3, D4);
   2. la pantalla Ayuda;
   3. la fila de Ajustes y la navegación;
   4. las pruebas.
5. Actualizar la spec 006.
6. Ejecutar `./gradlew test` y las pruebas instrumentadas en el emulador, hacer la demo manual y pasar `architecture-reviewer`.
7. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1 a RF-3 | Fila de Ajustes, `SettingsViewModel`, `NotesNavHost`, `HelpRoute` |
| RF-4 a RF-10 | `HelpScreen`, componentes comunes |
| RF-7 | `FaqRepositoryImpl`, `data/res/values/strings.xml` |
| RF-11 a RF-16 | `HelpViewModel` (`expandedIds`), fila de pregunta en `HelpScreen` |
| RF-17 a RF-24 | `FaqSearch`, `SearchFaqsUseCase`, `HelpViewModel`, buscador común |
| RF-25 a RF-28 | `DisabledRow` en `HelpScreen` |
| RF-29, RF-30 | `SavedStateHandle` del `HelpViewModel`, `rememberLazyListState` |

## 9. Riesgos

- **Respuestas que dejan de ser ciertas** cuando cambie una función: el texto está en recursos y la spec 013 lo recoge; se actualiza con la spec que cambie esa función (CL-6).
- **Extraer componentes** (D3, D4) toca Mis notas y Ajustes: las pruebas de UI existentes de ambas pantallas deben seguir pasando sin cambios.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
