# Tareas 003 — Color de la nota

> **Estado:** completada · **Fecha:** 2026-10-21 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/note-color` desde `main`, actualizado con `origin`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/note-color` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Ocho colores.** `NoteColor` pasa a `NONE, AQUA, ROSE, VANILLA, LAVENDER, MINT, PEACH, SAND`, en el orden de la paleta. Ajustar los `when` exhaustivos que dejen de compilar, por ahora solo para que compilen; los colores reales llegan en T07.
  - **Cubre:** RF-3, RF-22
  - **Hecho cuando:** `./gradlew assembleDebug` compila y `./gradlew :domain:test` pasa.

- [x] **T03. Pruebas de dominio** (`test-writer`). Ampliar `SaveNoteUseCaseTest`.
  - **Cubre:** RF-16, RF-17
  - **Hecho cuando:** `./gradlew :domain:test` pasa con casos para:
    - una nota `NONE` se guarda con su color y una fecha nueva;
    - una nota con un color nuevo (`VANILLA`, `SAND`) se guarda igual.

## Fase 2 — Datos (`:data`)

- [x] **T04. Migración v4→v5** (skill `room-migration`).
  - `NotesDatabase` pasa a `version = 5`.
  - `MIGRATION_4_5` es una `Migration` manual con `UPDATE notes SET color = 'VANILLA' WHERE color = 'SAND'`, registrada con `addMigrations` en `DatabaseModule`.
  - Se exporta `5.json`.
  - **Cubre:** RF-29
  - **Hecho cuando:**
    - `./gradlew assembleDebug` compila;
    - `data/schemas/.../5.json` existe;
    - no aparece `fallbackToDestructiveMigration`.

- [x] **T05. Semilla.** En `SeedNotesCallback`, la nota amarilla pasa de `SAND` a `VANILLA`.
  - **Cubre:** RF-29
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila y ninguna nota semilla usa `SAND`.

- [x] **T06. Pruebas de datos** (`test-writer`).
  - **Cubre:** RF-21, RF-29
  - **Hecho cuando:**
    - `./gradlew :data:testDebugUnitTest` pasa, y `NoteMappersTest` cubre los ocho colores de ida y vuelta y un valor desconocido que cae en `AQUA`;
    - `./gradlew :data:connectedDebugAndroidTest` pasa, y `MigrationTest` comprueba que de v4 a v5 una nota `SAND` pasa a `VANILLA`, las de otros colores no cambian, el número de notas y los demás campos se conservan;
    - `NoteDaoTest` guarda y lee una nota `NONE`.

## Fase 3 — Tema (`:presentation`)

- [x] **T07. Colores del tema.**
  - En `Color.kt`, agregar `Vanilla = #F4E6A4`, cambiar `Sand` a `#E2D5C3` y agregar `Swatch = #262628` y `SwatchBorder = #323234`.
  - `Slate` pasa a `#3B4C50` (D5).
  - `NoteColors` gana `vanilla` y `none` (`Graphite`).
  - `toContainerColor()` cubre los ocho valores.
  - Agregar `toContentColor()` y `toMetadataColor()`.
  - **Cubre:** RF-23, RF-25, RF-26, RNF Contraste
  - **Hecho cuando:**
    - `./gradlew :presentation:compileDebugKotlin` compila;
    - el contraste de `Ink` y `Slate` sobre los siete colores, y de `Snow` y `Ash` sobre `Graphite`, es de al menos 4,5:1, calculado y anotado en esta tarea al marcarla.
  - **Contraste calculado** (WCAG 2.1):

    | Texto | Cielo | Rosa | Vainilla | Lavanda | Menta | Melocotón | Arena |
    |---|---|---|---|---|---|---|---|
    | `Ink` `#152326` | 11,11 | 8,29 | 12,84 | 8,87 | 11,64 | 10,25 | 11,18 |
    | `Slate` `#3B4C50` | 6,19 | 4,61 | 7,15 | 4,94 | 6,48 | 5,70 | 6,23 |

    Sobre «Sin color» (`Graphite` `#1C1C1E`): `Snow` 15,89 y `Ash` 5,49.
  - **Nota:** `Theme.kt` usaba `Sand` como `secondary`; pasa a `Vanilla` para conservar el amarillo.

- [x] **T08. Nombres de los colores.** Agregar `NoteColor.labelRes` y los ocho strings `note_color_*`.
  - **Cubre:** RF-3, RF-10, RF-30
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y no hay nombres de color escritos en el código.

- [x] **T09. `NoteSurfaceTheme`.** Un `MaterialTheme` anidado que, para un color distinto de `NONE`, sustituye en el esquema:
  - `background` por el color de la nota;
  - `onBackground` y `onSurface` por `Ink`;
  - `onSurfaceVariant` por `Slate`;
  - `primary` por `Ink`;
  - `onPrimary` por el color de la nota.

  Con `NONE` deja el esquema igual.
  - **Cubre:** RF-24, RF-25, RF-27
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

## Fase 4 — Estado del editor (`:presentation`)

- [x] **T10. Estado e intents.** Agregar `pickerColor: NoteColor?` a `NoteEditorUiState`, y `ColorButtonClicked`, `PickerColorSelected(color)`, `ColorApplied` y `ColorPickerDismissed` a `NoteEditorIntent`.
  - **Cubre:** RF-4 a RF-7, RF-11
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T11. ViewModel.** Manejar los cuatro intents:
  - `ColorButtonClicked` abre la hoja con el color actual.
  - `PickerColorSelected` solo cambia `pickerColor`.
  - `ColorApplied` asigna el color, cierra la hoja y, si el color cambió, guarda en el momento.
  - `ColorPickerDismissed` cierra la hoja sin aplicar.

  Incluir `color` en `hasUnsavedChanges()` y en los dos borradores (guardar y agregar imágenes).
  - **Cubre:** RF-2, RF-4 a RF-6, RF-11 a RF-21
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas existentes de `:presentation` pasan.

- [x] **T12. Pruebas del ViewModel** (`test-writer`). Ampliar `NoteEditorViewModelTest` con los casos de la sección 6.4 del plan.
  - **Cubre:** RF-2, RF-4, RF-5, RF-11 a RF-21
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` pasa, con casos para:
    - abrir la hoja con el color actual;
    - elegir una opción sin guardar;
    - aplicar un color distinto: guarda y cambia la fecha;
    - aplicar el mismo color: no guarda;
    - descartar y volver a abrir;
    - nota nueva vacía: no se guarda hasta escribir, y luego se guarda con el color;
    - cargar una nota con su color.

## Fase 5 — Interfaz (`:presentation`)

- [x] **T13. Ícono y strings de la hoja.**
  - Agregar `ic_slash` (Lucide `slash`, VectorDrawable como los demás).
  - Agregar `note_color_sheet_description`, `note_color_preview_untitled`, `note_color_preview_caption`, `note_color_apply` y `note_color_close`.
  - Agregar a `Sizes` y `NotesTypography` las medidas y estilos nuevos de la hoja.
  - **Cubre:** RF-2, RF-3, RF-9, RF-10, RF-32, RNF Idioma
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T14. Hoja del selector.** Crear `NoteColorSheet` (`internal`, en su propio archivo) con `ModalBottomSheet`, según la sección 4.4 del plan:
  - asa, cabecera con X;
  - paleta de 2×4 con marca de selección;
  - vista previa con título o «Sin título» y el texto del color;
  - botón «Aplicar color»;
  - `selectable(role = RadioButton)` y etiquetas accesibles;
  - los `testTag` nuevos.

  La hoja se muestra cuando `pickerColor` no es nulo.
  - **Cubre:** RF-2 a RF-10, RF-12 a RF-14, RF-30 a RF-32
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y existen previews del contenido de la hoja (una `ModalBottomSheet` no se dibuja en las previews estáticas).

- [x] **T15. Botón de la barra.** Habilitar «Color de la nota» y que emita `ColorButtonClicked`.
  - **Cubre:** RF-1
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T16. Fondo del editor.** Envolver la cabecera y la zona desplazable en `NoteSurfaceTheme(state.color)`. La barra inferior, la hoja, el diálogo y el menú de formato quedan fuera.
  - **Cubre:** RF-24, RF-25, RF-27, RF-28
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y hay previews del editor en Rosa y en «Sin color».

- [x] **T17. Barra de estado.** Mientras el color no sea `NONE`, los íconos de la barra de estado son oscuros. Se restauran al salir del editor con un `DisposableEffect`, en `NoteEditorRoute`; `:app` no cambia.
  - **Cubre:** RF-25
  - **Hecho cuando:** `./gradlew assembleDebug` compila.

- [x] **T18. Tarjeta de Mis notas.** `NoteCard` usa `toContainerColor()`, `toContentColor()` y `toMetadataColor()`.
  - **Cubre:** RF-23, RF-26
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y la preview del listado incluye una nota `NONE`.

- [x] **T19. Revisar las previews** (tú). Comparar las previews de T14, T16 y T18 con Figma `4:1710`.
  - **Cubre:** RNF Aspecto
  - **Hecho cuando:** confirmas que se ven bien o las diferencias quedan corregidas.
  - **Resultado:** aprobadas. Además se decidió:
    - El pin fijado usa el ícono relleno `ic_pin_filled`, porque sobre un color el de acento no distinguía el estado.
    - Con «Sin color», el círculo del botón de color muestra `ic_slash`.

- [x] **T20. Pruebas de UI** (`test-writer`). Ampliar `NoteEditorScreenTest` y `NoteListScreenTest` con los casos de la sección 6.5 del plan, y `NoteListViewModelTest` con una nota `NONE`.
  - **Cubre:** RF-1 a RF-5, RF-8 a RF-10, RF-12, RF-14, RF-26, RF-30 a RF-32
  - **Hecho cuando:**
    - `./gradlew :presentation:testDebugUnitTest` pasa;
    - `./gradlew :presentation:connectedDebugAndroidTest` pasa;
    - las pruebas nuevas pasan tres veces seguidas.

## Fase 6 — Verificación y cierre

- [x] **T21. Suite completa.** `./gradlew test`, `connectedDebugAndroidTest`, `assembleDebug` y `lint`.
  - **Cubre:** todos
  - **Hecho cuando:** todo pasa. El único error de `lint` permitido es el que ya existía en `NoteEditorRoute.kt`.
  - **Resultado (2026-10-22):**
    - `./gradlew test`: 292 pruebas, 0 fallos (`:domain` 121, `:data` 52, `:presentation` 118, `:app` 1).
    - `connectedDebugAndroidTest`: 83 pruebas, 0 fallos (`NoteDaoTest` 15, `MigrationTest` 7, `NoteEditorScreenTest` 36, `NoteListScreenTest` 24, `ExampleInstrumentedTest` 1).
    - `assembleDebug`: compila.
    - `lint`: un solo error, `LocalContextGetResourceValueCall`, el que ya existía en `NoteEditorRoute.kt`. Ahora está en la línea 50 (antes 45) porque se agregaron imports.

- [x] **T22. Revisión de arquitectura** (`architecture-reviewer`).
  - **Cubre:** principios 1, 3 y 6
  - **Hecho cuando:** no quedan hallazgos sin resolver o sin tu decisión.
  - **Resultado:** sin hallazgos de severidad alta. Se aplicaron los tres:
    1. Plan y tareas actualizados con lo que cambió al implementar (principio 2).
    2. `NoteEditorUiState.isNoteVisible`, usada por la Route y la Screen, con su prueba `NoteEditorUiStateTest`.
    3. Imports de `NoteEditorRoute.kt` ordenados.

- [x] **T23. Verificación manual en el emulador.** Los pasos de la sección 6.6 del plan. La migración se prueba instalando sobre la versión actual, con notas amarillas reales.
  - **Cubre:** RF-14, RF-19, RF-23 a RF-29, RNF Aspecto
  - **Hecho cuando:** cada paso se comprueba y su resultado queda anotado aquí.
  - **Resultado (2026-10-22, emulador Medium_Phone_API_36.0):**
    - **Migración [RF-29]:** se instaló la versión de `main` desde cero (base v4, «Ideas sueltas» en `SAND`) y encima la de la rama. La base quedó en v5 con «Ideas sueltas» en `VANILLA`, sigue amarilla, las 6 notas conservan su color y la hoja la muestra como «Vainilla».
    - **Cerrar sin aplicar [RF-14, RF-15]:** con la X, con atrás, tocando fuera y deslizando: tras elegir Rosa, al reabrir se ve Vainilla y la base no cambia.
    - **Giro con la hoja abierta:** sigue abierta con la opción elegida. **Bug encontrado y corregido:** en horizontal la hoja no cabía y «Aplicar color» quedaba fuera de pantalla; el contenido de la hoja ahora se desplaza. Tras el arreglo se aplicó Lavanda en horizontal y `NoteEditorScreenTest` (36) y `NoteListScreenTest` (24) siguen pasando.
    - **Aplicar el mismo color [RF-18]:** cierra la hoja y `updated_at` no cambia.
    - **Aplicar otro color [RF-12, RF-16, RF-17, RF-24, RF-25, RF-28]:** Menta y Lavanda se guardan con la hora actual; fondo, textos oscuros e íconos de la barra de estado cambian sin salir de la nota.
    - **«Sin color» [RF-26, RF-27]:** tarjeta gris con texto claro y editor negro (comprobado en T18).
    - **Nota nueva vacía [RF-19]:** se aplicó Melocotón y se salió sin escribir; no se creó ninguna nota (6 antes y después).
    - **Tarjetas [RF-23] y aspecto de la hoja frente a Figma `4:1710`:** comprobado en T15, T18 y T19.

- [x] **T24. Cierre.** Actualizar `.memory/MEMORY.md` y marcar spec, plan y tareas como implementados o completados.
  - **Cubre:** principio 2
  - **Hecho cuando:** los tres documentos tienen su estado final y la memoria ya no lista el selector de color como pendiente.
