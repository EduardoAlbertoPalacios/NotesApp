# Tareas 013 — Ayuda y comentarios

> **Estado:** implementadas · **Fecha:** 2026-11-18 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

**Paralelismo posible:** una vez terminada T02, T03 (pruebas de dominio) puede correr en paralelo con T04 (`:data`) y con T06 (extraer componentes). Una vez terminada T07, T08 y las pruebas de T09 se pueden repartir entre agentes.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/help-feedback` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/013-help-feedback/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/help-feedback` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Modelo, reglas y caso de uso.** Crear en el paquete `help`:
  - `Faq(id, question, answer)`;
  - la interfaz `FaqRepository` con `suspend fun getFaqs(): List<Faq>`;
  - `FaqSearch.filter(faqs, query)`, que reutiliza `NoteTextMatcher.words` y `normalize` (D2);
  - `SearchFaqsUseCase(query)`;
  - un `FakeFaqRepository` en `src/test`.
  - **Cubre:** RF-7, RF-17 a RF-19
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` y `./gradlew :domain:compileTestKotlin` compilan.

- [x] **T03. Pruebas de dominio** (`test-writer`).
  - **`FaqSearchTest`:**
    - vacío y solo espacios;
    - coincidencia en la pregunta y solo en la respuesta;
    - palabras repartidas entre pregunta y respuesta;
    - mayúsculas, tildes y «ñ» ≠ «n»;
    - sin resultados;
    - orden conservado.
  - **`SearchFaqsUseCaseTest`:** lee del repositorio y filtra.
  - **Cubre:** RF-17 a RF-20, CL-1, CL-2
  - **Hecho cuando:** `./gradlew :domain:test` pasa.

## Fase 2 — Datos (`:data`)

- [x] **T04. Repositorio de preguntas.**
  - Añadir a `data/src/main/res/values/strings.xml` las 12 preguntas y respuestas de la spec, tal cual.
  - Crear `FaqRepositoryImpl` (`internal`, `@ApplicationContext`, `@IoDispatcher`) y su `@Binds` en `RepositoryModule`.
  - **Cubre:** RF-7
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T05. Prueba de datos** (`test-writer`). `FaqRepositoryImplTest` instrumentada: 12 preguntas en orden, ids de 1 a 12, sin textos vacíos, y la primera coincide con la del diseño.
  - **Cubre:** RF-7
  - **Hecho cuando:** `./gradlew :data:connectedDebugAndroidTest` pasa en el emulador.

## Fase 3 — Presentación (`:presentation`)

- [x] **T06. Componentes comunes (D3, D4).**
  - Extraer `SearchField` de Mis notas a `presentation/common`, con el texto de ayuda, los callbacks y las `testTag` como parámetros.
  - Extraer `SettingsGroup`, `SettingsRow`, `DisabledRow`, `Chevron` y la barra superior de Ajustes a `presentation/common/SettingsComponents.kt` como `internal`.
  - No cambiar el aspecto, el comportamiento ni las `testTag`.
  - **Cubre:** D3, D4
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas de UI de Mis notas y de Ajustes siguen pasando sin cambios.

- [x] **T07. Pantalla Ayuda.**
  - **MVI:** `HelpUiState`, `HelpIntent`, `HelpEffect`, `HelpViewModel` (con `query` y `expandedIds` en el `SavedStateHandle`, D5), `HelpRoute` y `HelpScreen`.
  - **Contenido según los nodos `102:68` y `102:2`:**
    - barra superior;
    - cabecera;
    - buscador;
    - grupo de preguntas con `key = id`, chevron girado y `toggleable` con estado accesible;
    - aviso sin resultados;
    - grupo «Valora la app» con la fila deshabilitada y el aviso atenuado.
  - **Recursos:** el icono de estrella descargado del diseño y los textos nuevos en `strings.xml`.
  - **Calidad:** `testTag`, etiquetas accesibles y previews en ambos temas (con preguntas abiertas y sin resultados).
  - **Cubre:** RF-4 a RF-30
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las previews se ven como el diseño en ambos temas.

- [x] **T08. Ajustes y navegación.**
  - Fila «Ayuda y comentarios» habilitada, con `HelpRowClicked` y `NavigateToHelp`.
  - Ruta `settings/help` en `NotesNavHost`.
  - **Cubre:** RF-1 a RF-3
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

- [x] **T09. Pruebas de presentación** (`test-writer`).
  - **`HelpViewModelTest`:**
    - todas cerradas al empezar;
    - abrir y cerrar varias;
    - filtrar y sin resultados;
    - limpiar;
    - abiertas conservadas al filtrar;
    - restauración desde el `SavedStateHandle`;
    - volver.
  - **`SettingsViewModelTest`:** `HelpRowClicked` → `NavigateToHelp`.
  - **`HelpScreenTest`,** en ambos temas: cabecera, lista, abrir y cerrar con su estado accesible, sin resultados, «Limpiar búsqueda», y la fila de Google Play deshabilitada y sin acción.
  - **Pruebas actualizadas:** `SettingsScreenTest` con la fila de Ayuda habilitada.
  - **Cubre:** RF-1 a RF-30
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan en el emulador.

## Fase 4 — Documentación y cierre

- [x] **T10. Actualizar la spec 006.** RF-9 («Ayuda y comentarios funciona desde la spec 013») y su línea de «Fuera de alcance».
  - **Cubre:** principio 2
  - **Hecho cuando:** ninguna spec contradice a la 013 ni al código.

- [x] **T11. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest`, en el emulador y nunca en el teléfono real.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T12. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T13. Demo manual en el emulador.** En ambos temas:
  - entrar desde Ajustes y volver con la flecha y con Atrás;
  - buscar con y sin tildes, una palabra que solo está en una respuesta, y una sin resultados;
  - limpiar la búsqueda;
  - abrir varias preguntas, girar y comprobar que siguen abiertas;
  - salir y volver, y comprobar que todo está cerrado;
  - tocar la fila de Google Play;
  - letra del sistema al 200 %.
  - **Cubre:** RF-1 a RF-30
  - **Hecho cuando:** todo se comporta según la spec y revisas conmigo la pantalla frente al diseño.

- [x] **T14. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja la spec 013.

- [ ] **T15. Commit y PR** (con tu confirmación).
  - Marcar spec, plan y tareas como implementados.
  - Hacer el commit con Conventional Commits.
  - Subir la rama y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
