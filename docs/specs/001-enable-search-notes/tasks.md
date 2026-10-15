# Tareas 001 — Buscar en mis notas

> **Estado:** pendiente · **Fecha:** 2026-10-14 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/search-notes` desde `main`, actualizado.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/search-notes` y `git log -1` coincide con `main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Normalización de texto en `NoteTextMatcher`.** Crear la clase interna que pasa el texto a minúsculas (`Locale.ROOT`), quita tildes y diéresis con `Normalizer` y conserva la «ñ».
  - **Cubre:** RF-3 (CA-3.1, CA-3.2, CA-3.3)
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila y `:domain` sigue sin dependencias nuevas en su `build.gradle.kts`.

- [x] **T03. Coincidencia por palabras en `NoteTextMatcher`.** Dividir el texto buscado en palabras sin espacios sobrantes. Una nota coincide si cada palabra aparece, como parte de palabra, en su título o en su contenido.
  - **Cubre:** RF-2 (CA-2.1, CA-2.2), RF-3 (CA-3.4, CA-3.5, CA-3.6)
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila y el matcher solo lee el título y el contenido de `Note`.

- [x] **T04. Pruebas de `NoteTextMatcher`** (`test-writer`). Cubrir los casos de la sección 6.1 del plan.
  - **Cubre:** RF-2, RF-3, CL-1 a CL-5, CL-7
  - **Hecho cuando:** `./gradlew :domain:test` pasa, y `NoteTextMatcherTest` incluye al menos «reunion» → «Reunión», «pinguino» → «pingüino», «ano» ↛ «año», «AÑO» → «año», «reun», varias palabras en distinto orden, solo espacios, emojis y una nota que solo tiene imágenes.

- [x] **T05. Modelo `FilteredNotes(notes, totalCount)`.**
  - **Cubre:** RF-7 (CA-7.2), RF-10
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila y la clase es una `data class` de Kotlin puro.

- [x] **T06. Ampliar `GetNotesUseCase(category, query)`.**
  - Sin texto filtra por categoría; con texto ignora la categoría y usa `NoteTextMatcher`.
  - Conserva el orden actual y devuelve `FilteredNotes`.
  - Filtra en `@DefaultDispatcher` con `flowOn`.
  - Adaptar `NoteListViewModel` al nuevo tipo de retorno, **sin comportamiento nuevo**, para que el proyecto siga compilando.
  - **Cubre:** RF-1 (CA-1.3), RF-4 (CA-4.1, CA-4.4), RF-5, RF-10
  - **Hecho cuando:** `./gradlew assembleDebug` compila y `./gradlew :presentation:testDebugUnitTest` sigue pasando con las pruebas actuales ajustadas al nuevo tipo.

- [x] **T07. Pruebas de `GetNotesUseCase`** (`test-writer`). Ampliar `GetNotesUseCaseTest` según la sección 6.1 del plan.
  - **Cubre:** RF-1, RF-4, RF-5, RF-10
  - **Hecho cuando:** `./gradlew :domain:test` pasa, con pruebas para:
    - sin texto, filtra por categoría;
    - con texto, ignora la categoría;
    - fijadas primero en los resultados;
    - `totalCount` es el mismo con y sin filtro o texto;
    - error de almacenamiento propagado.

## Fase 2 — Estado de la pantalla (`:presentation`)

- [x] **T08. Strings y plurales nuevos.** Agregar `note_list_search_results` (plural), `note_list_search_no_results` (con `%1$s`), `note_list_search_clear` y `note_list_filter_inactive`, en español.
  - **Cubre:** RNF-6
  - **Hecho cuando:** los cuatro recursos existen en `strings.xml` de `:presentation`, el plural tiene `one` y `other`, y `./gradlew :presentation:assembleDebug` compila.

- [x] **T09. Campos nuevos en `NoteListUiState`.** Agregar `query` y `totalCount`, y los derivados `isSearching`, `isSearchEnabled` y `showNoResults`.
  - **Cubre:** RF-6, RF-7, RF-10
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y los derivados no contienen reglas de coincidencia (solo comparan `query`, `notes` y `totalCount`).

- [x] **T10. Escribir en el buscador (`QueryChanged`).**
  - El intent actualiza `query` en `_state` de forma síncrona.
  - El ViewModel combina el filtro y el texto con `flatMapLatest` sobre `getNotes(category, query)`, y guarda `totalCount`.
  - **Cubre:** RF-1 (CA-1.1, CA-1.2), RF-4 (CA-4.1), RNF-2
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y no hay `debounce` en el ViewModel.

- [x] **T11. Conservar el texto en `SavedStateHandle`.** Leer el texto inicial del `SavedStateHandle` y escribirlo en cada `QueryChanged`.
  - **Cubre:** RF-9 (CA-9.1, CA-9.3, CA-9.4)
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y el texto no se guarda en ningún otro almacenamiento (RNF-3).

- [x] **T12. Chips y limpiar durante la búsqueda.**
  - `FilterSelected` con búsqueda activa vacía el texto y selecciona el chip.
  - El nuevo intent `ClearQueryClicked` vacía el texto.
  - **Cubre:** RF-4 (CA-4.3, CA-4.4), RF-8 (CA-8.2)
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y `selectedFilter` solo cambia con `FilterSelected`.

- [x] **T13. Vaciar la búsqueda cuando no quedan notas.** Si `totalCount` llega a 0 con una búsqueda activa, el ViewModel vacía el texto.
  - **Cubre:** RF-10 (CA-10.1), CL-8
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T14. Pruebas de `NoteListViewModel`** (`test-writer`). Ampliar `NoteListViewModelTest` con los casos de la sección 6.2 del plan.
  - **Cubre:** RF-1, RF-4, RF-6, RF-7, RF-8, RF-9 (CA-9.2, CA-9.3), RF-10, RF-11, CL-8, CL-9
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` pasa, con pruebas para:
    - `QueryChanged`;
    - resultados de otras categorías;
    - `FilterSelected` y `ClearQueryClicked` con búsqueda;
    - restaurar desde `SavedStateHandle`;
    - recalcular tras una nueva emisión del repositorio;
    - `totalCount` = 0;
    - `CreateNoteClicked` con búsqueda, que envía la categoría del chip previo.

## Fase 3 — Interfaz (`NoteListScreen`)

- [x] **T15. Buscador editable.** Convertir el `SearchField` visual en un `BasicTextField` con la apariencia actual del Figma.
  - Acción de teclado «Buscar», que oculta el teclado.
  - Deshabilitado cuando `isSearchEnabled` es falso.
  - **Cubre:** RF-1, RF-10 (CA-10.1, CA-10.2)
  - **Hecho cuando:** `./gradlew assembleDebug` compila; el campo usa solo tokens del tema y emite `QueryChanged`.

- [x] **T16. Botón de limpiar.** Ícono `ic_close` dentro del buscador, visible solo con texto, que emite `ClearQueryClicked`.
  - **Cubre:** RF-8 (CA-8.1)
  - **Hecho cuando:** `./gradlew assembleDebug` compila y el botón tiene `contentDescription` con `note_list_search_clear`.

- [x] **T17. Chips inactivos durante la búsqueda.** Con `isSearching`, todos los chips usan el estilo no seleccionado y la descripción de estado `note_list_filter_inactive`, pero siguen pudiéndose tocar.
  - **Cubre:** RF-4 (CA-4.2, CA-4.3)
  - **Hecho cuando:** `./gradlew assembleDebug` compila y los chips no usan `enabled = false`.

- [x] **T18. Contador de resultados en el encabezado.** Con `isSearching`, mostrar `note_list_search_results` en lugar de `note_list_summary`.
  - **Cubre:** RF-6 (CA-6.1, CA-6.2)
  - **Hecho cuando:** `./gradlew assembleDebug` compila y la vista previa de búsqueda muestra «N resultados».

- [x] **T19. Mensaje de sin resultados.** Con `showNoResults`, mostrar `note_list_search_no_results` con el texto buscado sin espacios sobrantes. El mensaje de lista vacía queda solo para `totalCount == 0`.
  - **Cubre:** RF-7 (CA-7.1, CA-7.2), CL-6
  - **Hecho cuando:** `./gradlew assembleDebug` compila y las vistas previas «sin resultados» y «sin notas» muestran mensajes distintos.

- [x] **T20. Accesibilidad.** `liveRegion` educado en el contador y en el mensaje de sin resultados; el marcador sirve de etiqueta del campo.
  - **Cubre:** RNF-5
  - **Hecho cuando:** `./gradlew assembleDebug` compila y los nodos del contador y del mensaje tienen `liveRegion` en su `semantics`.

- [x] **T21. Etiquetas de prueba y vistas previas.**
  - Agregar `SEARCH_FIELD`, `SEARCH_CLEAR`, `SEARCH_NO_RESULTS` y `SUMMARY` a `NoteListTestTags`.
  - Agregar las vistas previas «buscando», «sin resultados» y «sin notas».
  - **Cubre:** apoyo a las pruebas de RF-1, RF-6 a RF-8 y RF-10
  - **Hecho cuando:** `./gradlew assembleDebug` compila y las tres vistas previas se renderizan en Android Studio.

- [x] **T22. Pruebas de UI de `NoteListScreen`** (`test-writer`). Ampliar `NoteListScreenTest` con los casos de la sección 6.3 del plan, usando un estado que se actualice con los intents.
  - **Cubre:** RF-1, RF-4 (CA-4.2, CA-4.3), RF-6, RF-7, RF-8, RF-10, RNF-5
  - **Hecho cuando:** `./gradlew :presentation:connectedDebugAndroidTest` pasa en el emulador `Medium_Phone_API_36.0`.

## Fase 4 — Verificación

- [x] **T23. Suite completa.**
  - **Cubre:** todos los RF
  - **Hecho cuando:**
    - `./gradlew test` pasa;
    - `./gradlew connectedDebugAndroidTest` pasa;
    - `./gradlew assembleDebug` compila;
    - `./gradlew lint` no reporta errores nuevos.

- [x] **T24. Revisión de arquitectura.** Ejecutar el agente `architecture-reviewer` sobre los cambios.
  - **Cubre:** principios 1 y 3 de la constitución
  - **Hecho cuando:** el agente no reporta problemas bloqueantes, o los que reporte están corregidos y T23 vuelve a pasar.

- [x] **T25. Verificación manual en el emulador** (sección 6.4 del plan).
  - **Cubre:** RF-1, RF-4, RF-6 a RF-9, RNF-1, RNF-2, RNF-4, RNF-5
  - **Hecho cuando:** todas estas comprobaciones están marcadas:
    - [x] Escribir «reunion» muestra «Reunión de equipo» y el contador «1 resultado», o el que corresponda.
    - [x] Con el chip Trabajo seleccionado, la búsqueda encuentra también notas personales y los chips se ven inactivos.
    - [x] Abrir un resultado y volver conserva el texto (CA-9.1).
    - [x] Girar la pantalla conserva el texto.
    - [x] Con «No conservar actividades» o `adb shell am kill`, el texto se restaura (CA-9.3).
    - [x] Cerrar la app desde recientes y reabrirla deja la búsqueda vacía (CA-9.4).
    - [x] Tocar un chip durante la búsqueda la limpia y aplica esa categoría (CA-4.3).
    - [x] El botón de limpiar vacía la búsqueda (RF-8).
    - [x] Un texto sin coincidencias muestra el mensaje con ese texto (RF-7).
    - [x] Los resultados aparecen sin demora perceptible y no se pierden caracteres al escribir rápido (RNF-1, RNF-2).
    - [x] Funciona en modo avión (RNF-4).
    - [x] Con TalkBack se anuncian el contador y el mensaje de sin resultados (RNF-5). *(Dada por comprobada por el usuario el 2026-10-15; no se verificó con TalkBack en esta sesión.)*

## Fase 5 — Cierre

- [x] **T26. Documentación y memoria.**
  - Marcar la spec y el plan como «implementados».
  - Si algo cambió durante la implementación, reflejarlo en la spec (principio 2).
  - Actualizar `.memory/MEMORY.md`.
  - **Cubre:** —
  - **Hecho cuando:** spec y plan dicen «implementada» o «implementado», ningún requisito contradice el código y `MEMORY.md` sigue en ~50 líneas o menos.

- [ ] **T27. Commit.** Proponer el mensaje y los archivos con `/commit` y esperar tu confirmación. No hacer push sin que lo pidas.
  - **Cubre:** —
  - **Hecho cuando:** el commit existe en `feat/search-notes` con autor `Eduardo Alberto <>`, sin `Co-Authored-By`, y `git status` está limpio.

## Trazabilidad

| Requisito | Tareas |
|---|---|
| RF-1 Búsqueda al escribir | T06, T07, T10, T14, T15, T22, T25 |
| RF-2 Campos | T03, T04 |
| RF-3 Coincidencia flexible | T02, T03, T04 |
| RF-4 Ignorar la categoría | T06, T07, T10, T12, T14, T17, T22, T25 |
| RF-5 Orden | T06, T07 |
| RF-6 Número de resultados | T09, T14, T18, T22, T25 |
| RF-7 Sin resultados | T05, T09, T14, T19, T22, T25 |
| RF-8 Limpiar | T12, T14, T16, T22, T25 |
| RF-9 Conservar la búsqueda | T11, T14, T25 |
| RF-10 Buscador sin notas | T05, T06, T07, T09, T13, T14, T15, T22 |
| RF-11 Nota nueva durante una búsqueda | T14 (sin cambios de código: `CreateNoteClicked` ya usa el chip seleccionado) |
| RNF-1 a RNF-6 | T08 (RNF-6), T10 (RNF-2), T11 (RNF-3), T20 (RNF-5), T25 (RNF-1, RNF-2, RNF-4, RNF-5) |
