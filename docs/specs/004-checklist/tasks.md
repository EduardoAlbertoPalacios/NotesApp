# Tareas 004 — Lista de tareas en las notas

> **Estado:** completada · **Fecha:** 2026-10-26 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/checklist` desde `main`, actualizado con `origin`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/checklist` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Modelo de tareas.** Crear `ChecklistItem(line, isDone)` y agregar `Note.contentTasks: List<ChecklistItem> = emptyList()`.
  - **Cubre:** RF-24
  - **Hecho cuando:** `./gradlew assembleDebug` compila y las pruebas existentes de `:domain` pasan.

- [x] **T03. Convertir y marcar líneas.** Crear el `object NoteChecklist` con `isTask`, `linesIn`, `toggleLines` y `setDone`. Devuelve listas normalizadas: ordenadas, sin duplicados y sin líneas fuera del contenido.
  - **Cubre:** RF-3 a RF-8, RF-16, RF-17, RF-20, CL-1 a CL-4
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T04. Interpretar ediciones.** Agregar `onTextChanged(old, new, cursor, tasks): ChecklistEdit`, según la sección 4.1 del plan:
  - Intro en una tarea vacía anula el salto y quita la tarea.
  - Borrar al inicio de una tarea anula el borrado y quita la tarea.
  - Insertar saltos en una tarea crea tareas pendientes.
  - Borrar varias líneas a la vez las quita de la lista.
  - Cualquier edición desplaza los índices de las tareas de más abajo.
  - **Cubre:** RF-10 a RF-14, CL-5, CL-6, CL-11
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T05. Leer marcadores antiguos.** Agregar `fromLegacyMarkers(content): LegacyConversion`. Quita «☐»/«☑» y los espacios que los siguen al inicio de cada línea. Devuelve el texto nuevo, las tareas y los rangos quitados.
  - **Cubre:** RF-25 a RF-27, CL-8
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T06. Pruebas de dominio** (`test-writer`). Crear `NoteChecklistTest` con los casos de la sección 6.1 del plan. Ampliar `SaveNoteUseCaseTest` (una nota con tareas se guarda con fecha nueva) y `GetNotesUseCaseTest` (la búsqueda encuentra el texto de una tarea).
  - **Cubre:** RF-3 a RF-14, RF-16, RF-17, RF-20, RF-23, RF-25 a RF-27, RF-32, CL-1 a CL-6, CL-8, CL-11
  - **Hecho cuando:** `./gradlew :domain:test` pasa con todos los casos de 6.1.

## Fase 2 — Datos (`:data`)

- [x] **T07. Codificación de tareas.** Crear `ChecklistCodec.kt` (`internal`) con `List<ChecklistItem>.encode()` y `String.decodeTasks(content)`, en el formato `línea,estado;…`. Al decodificar se descartan las entradas mal formadas y las líneas que no existen.
  - **Cubre:** RF-24
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T08. Columna y mappers.** Agregar `content_tasks` a `NoteEntity` (`defaultValue = ""`) e incluir las tareas en `toDomain()` y `toEntity()`.
  - **Cubre:** RF-22, RF-24
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila. Es normal que la base aún no cambie de versión; eso se hace en T09.

- [x] **T09. Migración v5→v6** (skill `room-migration`). `MIGRATION_5_6` manual, según la sección 4.2 del plan:
  1. Agrega la columna.
  2. Convierte los «☐»/«☑» con `fromLegacyMarkers`.
  3. Recoloca los tramos de formato con `NoteFormatting.onTextChanged`.
  4. Conserva `updated_at`.

  `NotesDatabase` pasa a `version = 6` y se exporta `6.json`.
  - **Cubre:** RF-25 a RF-27, RNF-1
  - **Hecho cuando:**
    - `./gradlew assembleDebug` compila;
    - `data/schemas/.../6.json` existe con la columna `content_tasks`;
    - no aparece `fallbackToDestructiveMigration`.

- [x] **T10. Semilla.** «La compra» se crea sin «☐»/«☑» y con sus cuatro tareas reales (la tercera, hecha).
  - **Cubre:** RF-28
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila y ninguna nota semilla contiene «☐» o «☑».

- [x] **T11. Pruebas de datos** (`test-writer`). Casos de las secciones 6.2 y 6.3 del plan.
  - **Cubre:** RF-24 a RF-27, RNF-1
  - **Hecho cuando:**
    - `./gradlew :data:testDebugUnitTest` pasa con `ChecklistCodecTest` y las ampliaciones de `NoteMappersTest` y `NoteRepositoryImplTest`;
    - `./gradlew :data:connectedDebugAndroidTest` pasa, y `MigrationTest` prueba de v5 a v6:
      - una nota con «☐»/«☑» y formato queda sin marcadores, con sus tareas, con los tramos sobre las mismas palabras y con la misma fecha;
      - una nota sin marcadores no cambia.
    - Las pruebas que abren la base con Room esperan la versión 6, y `NoteDaoTest` guarda y lee una nota con tareas.

## Fase 3 — Estado del editor (`:presentation`)

- [x] **T12. Estado e intents.** Agregar `contentTasks` a `NoteEditorUiState`, y `ChecklistButtonClicked` y `TaskToggled(line)` a `NoteEditorIntent`. Si el `when` del ViewModel deja de compilar, añadir una rama provisional hasta T13.
  - **Cubre:** RF-3, RF-16
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T13. ViewModel.**
  - `ContentChanged` aplica primero `NoteChecklist.onTextChanged`: si la regla anuló la tecla, sustituye texto y selección. Después aplica `NoteFormatting.onTextChanged` con el texto final.
  - `ChecklistButtonClicked` llama a `toggleLines` y marca cambios pendientes.
  - `TaskToggled` llama a `setDone` y guarda en el momento.
  - `contentTasks` entra en `hasUnsavedChanges()`, en los dos borradores y en la carga.
  - **Cubre:** RF-3 a RF-14, RF-16, RF-17, RF-20 a RF-24
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas existentes de `:presentation` pasan.

- [x] **T14. Pruebas del ViewModel** (`test-writer`). Ampliar `NoteEditorViewModelTest` con los casos de la sección 6.4 del plan.
  - **Cubre:** RF-3 a RF-6, RF-10 a RF-14, RF-16, RF-17, RF-20 a RF-24
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` pasa, con casos para:
    - el botón con cursor y con selección;
    - marcar y desmarcar sin tocar los tramos;
    - Intro en una tarea vacía y borrar al inicio, con los tramos sin desplazar;
    - escribir y pegar en una tarea;
    - cargar una nota con tareas.

## Fase 4 — Interfaz del editor (`:presentation`)

- [x] **T15. Tokens y strings.**
  - Agregar a `Sizes` `taskCheckbox` (20 dp), `taskCheckboxBorder` (1,5 dp), `taskCheckIcon` (14 dp) y `taskGap` (12 dp). Al implementar se añadieron `taskRow` (36 dp, T17), y `cardTaskCheckbox` (13 dp) y `cardTaskGap` (6 dp) para la tarjeta (T19), con `Radii.cardTaskCheckbox` (3 dp).
  - Agregar a `Radii` `taskCheckbox` (5 dp).
  - Agregar los strings `note_editor_task_done` («Hecha») y `note_editor_task_pending` («Pendiente»).
  - **Cubre:** RF-34, RNF-2, RNF-5
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T16. Transformación con tareas.** `buildFormattedContent` y `FormattedContentTransformation` reciben también las tareas:
  - `ParagraphStyle` con sangría en cada línea que es tarea, cubriendo la línea entera con su `\n`;
  - tachado y color secundario solo visuales en las hechas.

  La correspondencia de posiciones sigue siendo `OffsetMapping.Identity`.
  - **Cubre:** RF-19, RF-21, CL-7
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas existentes de `FormattedContentTest` pasan.

- [x] **T17. Casillas en el editor.** Guardar el `TextLayoutResult` del campo de contenido y dibujar encima una `TaskCheckbox` por tarea, en la primera línea visual de esa línea.
  - **Aspecto:** pendiente con borde; hecha con relleno `primary` e `ic_check`.
  - **Interacción:** zona táctil fija de 44 dp (`iconAction`) colocada con `offset` y `toggleable`, que emite `TaskToggled(línea)`. La accesibilidad va en un único nodo con `clearAndSetSemantics` (ver plan 4.3).
  - **Accesibilidad:** `contentDescription` con el texto de la tarea y `stateDescription` «Hecha» o «Pendiente».
  - **`testTag`:** `TASK_CHECKBOX + línea`.
  - **Cubre:** RF-15 a RF-18, RF-33, RF-34, RNF-2 a RNF-4
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y hay una preview del editor con tareas pendientes y hechas.

- [x] **T18. Botón de la barra.** Habilitar «Lista de tareas», deshabilitado sin foco en el contenido, que emita `ChecklistButtonClicked`, con el `testTag` `TOOLBAR_CHECKLIST`.
  - **Cubre:** RF-1, RF-2
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y el botón funciona en el emulador.

## Fase 5 — Mis notas (`:presentation`)

- [x] **T19. Casillas en la tarjeta.**
  - `NoteListViewModel` construye el contenido con las tareas: una marca en línea al inicio de cada tarea y el tachado en las hechas.
  - `NoteCard` pasa al `Text` el mapa `inlineContent` con las dos casillas, sin interacción.
  - **Cubre:** RF-29 a RF-31, CL-9
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y la preview del listado incluye una nota con tareas.

- [x] **T20. Revisar las previews** (tú). Comparar las previews de T17 y T19 con Figma `4:1683`, también sobre una nota con color.
  - **Cubre:** RNF-2
  - **Hecho cuando:** confirmas que se ven bien o las diferencias quedan corregidas.
  - **Resultado:** dada por revisada por el usuario (2026-10-27). Comprobado además en el emulador: editor con «La compra» sobre Rosa y tarjetas con tareas sobre Cielo y Rosa.

- [x] **T21. Pruebas de presentación** (`test-writer`). Ampliar `FormattedContentTest`, `NoteListViewModelTest`, `NoteEditorScreenTest` y `NoteListScreenTest` con los casos de las secciones 6.4 y 6.5 del plan.
  - **Cubre:** RF-1, RF-2, RF-15 a RF-17, RF-19, RF-21, RF-29 a RF-31, RF-33, RF-34, CL-7, RNF-3, RNF-4
  - **Hecho cuando:**
    - `./gradlew :presentation:testDebugUnitTest` pasa;
    - `./gradlew :presentation:connectedDebugAndroidTest` pasa;
    - las pruebas nuevas pasan tres veces seguidas.

## Fase 6 — Verificación y cierre

- [x] **T22. Suite completa.** `./gradlew test`, `connectedDebugAndroidTest`, `assembleDebug` y `lint`.
  - **Cubre:** todos
  - **Hecho cuando:** todo pasa. El único error de `lint` permitido es el que ya existía en `NoteEditorRoute.kt`.
  - **Resultado (2026-10-27):**
    - `./gradlew test`: 393 pruebas, 0 fallos (`:domain` 167, `:data` 73, `:presentation` 152, `:app` 1).
    - `connectedDebugAndroidTest`: 94 pruebas, 0 fallos (`NoteEditorScreenTest` 42, `NoteListScreenTest` 26, `NoteDaoTest` 17, `MigrationTest` 8, `ExampleInstrumentedTest` 1).
    - `assembleDebug`: compila.
    - `lint`: un solo error, el preexistente `LocalContextGetResourceValueCall` en `NoteEditorRoute.kt:50`.

- [x] **T23. Revisión de arquitectura** (`architecture-reviewer`).
  - **Cubre:** principios 1, 3 y 6
  - **Hecho cuando:** no quedan hallazgos sin resolver o sin tu decisión.
  - **Resultado:** sin hallazgos graves. Decisiones del usuario:
    - D6: excepción en `CLAUDE.md` para los `object` de reglas puras de `:domain`.
    - Plan y tareas actualizados (zona táctil, tokens, riesgos, casillas de la tarjeta) y quitado el «cl» suelto del título.
    - Accesibilidad: la casilla de la tarjeta anuncia «Hecha»/«Pendiente» desde `strings.xml` y la tarea vacía se anuncia como «Tarea vacía».
    - KDoc suelto de `EditorTextField` recolocado.
    - No aplicado: el token para el ícono de la casilla de la tarjeta (sigue la fracción `0.75f`).

- [x] **T24. Verificación manual en el emulador.** Los pasos de la sección 6.6 del plan, incluida la migración con «La compra» de `main` y la comprobación de que las casillas seguidas no se pisan los toques (riesgo de la sección 9).
  - **Cubre:** RF-3 a RF-31, RNF-1 a RNF-3
  - **Hecho cuando:** cada paso se comprueba y su resultado queda anotado aquí.
  - **Resultado (2026-10-27, emulador Medium_Phone_API_36.0):**
    - **Migración [RF-25 a RF-27, RNF-1]:** `main` instalado desde cero (base v5, «La compra» con «☐»/«☑»); se puso «Leche» en negrita (tramo `14,19,B`). Tras `adb install -r` de la rama: base v6, texto sin marcadores, tareas `0,0;1,0;2,1;3,0`, negrita recolocada en `8,13` (sigue sobre «Leche»), `updated_at` igual y las 6 notas conservadas.
    - **Casillas seguidas (riesgo de la sección 9):** tocar el centro de cada una de las cuatro marca la tarea correcta; un toque 10 px por encima y otro 10 px por debajo de la frontera entre dos filas marcan la de arriba y la de abajo. No se pisan.
    - **Convertir con selección [RF-5, RF-6]:** tres líneas seleccionadas pasan a tareas y vuelven a texto con el botón.
    - **Intro y fin de lista [RF-10, RF-11]:** comprobado en T18 (nota «Compras»).
    - **Pegar [RF-14]:** pegar «uno / dos / tres» en una tarea nueva crea tres tareas pendientes.
    - **Borrar al inicio [RF-12, RF-13]:** el primer borrado quita la tarea y conserva el texto; el segundo une con la línea anterior.
    - **Reabrir [RF-24]:** al salir y volver a abrir, las tareas siguen en sus líneas y con su estado.
    - **Tarea larga:** la casilla queda en la primera línea en pantalla y el resto del texto se alinea con el texto, no con la casilla.
    - **Tarjetas y color [RF-29, RF-30, RNF-2]:** comprobado en T19 sobre Cielo y Rosa.
    - **Limitación conocida (CL-3, CL-4):** con el contenido vacío, al convertir la primera línea en tarea el cursor queda bajo la casilla hasta escribir el primer carácter; al escribir, el texto se coloca bien.
    - En el emulador, Ctrl+C abre la hoja de compartir de Android y la barra flotante del teclado tapa la parte izquierda; son del sistema, no de la app.

- [x] **T25. Cierre.** Actualizar `.memory/MEMORY.md` y marcar spec, plan y tareas como implementados o completados, reflejando en el plan cualquier cambio hecho al implementar.
  - **Cubre:** principio 2
  - **Hecho cuando:** los tres documentos tienen su estado final y la memoria refleja la lista de tareas.
