# Tareas 006 — Pantalla de Ajustes y tema claro/oscuro

> **Estado:** completada salvo T17 y T28 en API 30, pendientes por decisión del usuario · **Fecha:** 2026-11-02 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/settings-theme` desde `main`, actualizado con `origin`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/settings-theme` y su base es el último commit de `origin/main`.

- [x] **T02. Agregar DataStore.** Añadir `androidx.datastore:datastore-preferences` 1.3.0 a `libs.versions.toml` y a `:data`. Si no compila con Kotlin 2.2.10, usar la 1.2.1 y anotarlo en el plan (D1). Añadir DataStore a `CLAUDE.md`.
  - **Cubre:** principio 1
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

## Fase 1 — Dominio (`:domain`)

- [x] **T03. Modelos y repositorio.** Crear en el paquete `settings` `ThemeMode` (con `DEFAULT = DARK`), `SettingsError` y `SettingsRepository`, según la sección 4.1 del plan.
  - **Cubre:** RF-24
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T04. Casos de uso.** Crear `GetThemeModeUseCase`, que convierte `null` en el tema por defecto, y `SetThemeModeUseCase`.
  - **Cubre:** RF-22, RF-24, RF-25
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T05. Pruebas de dominio** (`test-writer`). Crear `GetThemeModeUseCaseTest` y `SetThemeModeUseCaseTest`.
  - **Cubre:** RF-22, RF-24, RF-25
  - **Hecho cuando:** `./gradlew :domain:test` pasa.

## Fase 2 — Datos (`:data`)

- [x] **T06. Repositorio y mappers.** Crear `SettingsRepositoryImpl` con `chosenThemeMode`, atrapando solo `IOException`, y los mappers `toStorageValue` y `toThemeModeOrNull`, según la sección 4.2 del plan.
  - **Cubre:** RF-22, RF-23, RF-25, RF-38
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T07. Módulos Hilt y copias de seguridad.** Crear `PreferencesModule` con una única instancia de DataStore, su `ReplaceFileCorruptionHandler` y su `CoroutineScope`. Añadir el `@Binds` a `RepositoryModule`. Excluir `datastore/settings.preferences_pb` en `backup_rules.xml` y `data_extraction_rules.xml`.
  - **Cubre:** RF-25, RNF-4
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila y Hilt arma el grafo.

- [x] **T08. Pruebas de datos** (`test-writer`). Crear `SettingsRepositoryImplTest` con un `DataStore` fake, las pruebas de los mappers y una prueba instrumentada con un archivo temporal.
  - **Cubre:** RF-22, RF-23, RF-25, RF-38
  - **Hecho cuando:** `./gradlew :data:test` y `./gradlew :data:connectedDebugAndroidTest` pasan.

## Fase 3 — Tema (`:presentation`)

- [x] **T09. Paleta clara y acento.** Agregar a `theme/` la paleta de la sección 4.3 del plan, `NotesAppTheme(darkTheme)`, `NotesTheme.accent` y el token del borde de la muestra elegida.
  - **Cubre:** RF-27 a RF-32, RNF-2
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T10. Usos de acento y hojas.** Pasar a `accent.content` el botón activo de la barra del editor, la etiqueta del color elegido y los indicadores de carga. Pasar `NoteColorSheet` a `surfaceContainerHigh` y a su nuevo token de borde. Adaptar `NoteSurfaceTheme` y `AppColorsTheme`.
  - **Cubre:** RF-29, RF-33 a RF-35
  - **Hecho cuando:** las pruebas existentes de `:presentation` pasan sin cambios de comportamiento en oscuro.

- [x] **T11. Previews en claro.** Añadir previews en claro de Mis notas, el editor con y sin color, y el selector de color.
  - **Cubre:** RF-27, RF-29, RF-32 a RF-35
  - **Hecho cuando:** las previews compilan.

- [x] **T12. Revisar las previews** (tú). Comparar las previews de T11 con Figma `40:956`, `40:1037` y `40:1115`.
  - **Cubre:** RF-27, RF-29, RF-32 a RF-35, RNF-1
  - **Hecho cuando:** das el visto bueno o las diferencias quedan resueltas.

- [x] **T13. Pruebas del tema** (`test-writer`). Ampliar `NoteListScreenTest` y `NoteEditorScreenTest` con los colores de nota y el acento en claro.
  - **Cubre:** RF-32 a RF-35
  - **Hecho cuando:** `./gradlew :presentation:connectedDebugAndroidTest` pasa.

## Fase 4 — Aplicar el tema (`:presentation` y `:app`)

- [x] **T14. `AppThemeViewModel`.** Exponer `StateFlow<ThemeMode?>`, con `null` hasta la primera lectura.
  - **Cubre:** RF-23 a RF-25
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T15. `MainActivity`.** Retener el primer dibujo mientras el tema sea `null`. Con el tema leído, aplicar `NotesAppTheme`, las barras del sistema y el fondo de la ventana. Fijar `setApplicationNightMode` en `onStop`, en API 31+. Añadir `lifecycle-runtime-compose` a `:app`.
  - **Cubre:** RF-26 a RF-31, RF-36, RF-37, RF-40
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

- [x] **T16. Temas XML y manifiesto.** Crear los recursos `window_background`, los temas de `values` y `values-v31`, y añadir `android:configChanges="uiMode"`.
  - **Cubre:** RF-19, RF-26, RF-40, CL-4 a CL-6
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

- [ ] **T17. Comprobar la ventana inicial.** Con el valor por defecto cambiado temporalmente a Claro, abrir la app en frío en API 36 y API 30. Revertir el cambio al terminar. Si no se cumple RF-40, parar y revisar la spec contigo.
  - **Cubre:** RF-40 (riesgo de la sección 9 del plan)
  - **Hecho cuando:** el resultado en cada API queda anotado aquí.
  - **Resultado (2026-11-02):** **API 36:** con Claro guardado y el teléfono en oscuro, la ventana inicial sale clara. **API 30: pendiente por decisión del usuario;** no hay imagen de sistema instalada. Hasta entonces, RF-40 no está comprobado antes de API 31.

- [x] **T18. Pruebas de `AppThemeViewModel`** (`test-writer`).
  - **Cubre:** RF-23 a RF-25
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` pasa.

## Fase 5 — Pantalla de Ajustes (`:presentation`)

- [x] **T19. Íconos y textos.** Descargar desde Figma los nueve íconos nuevos y convertirlos a vectores. Agregar los textos a `strings.xml`.
  - **Cubre:** RF-4 a RF-8, RF-11, RF-12, RF-14, RNF-5
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y no queda ninguna URL temporal de Figma.

- [x] **T20. Estado y ViewModel.** Crear `SettingsUiState`, `SettingsIntent`, `SettingsEffect` y `SettingsViewModel`.
  - **Cubre:** RF-2, RF-16 a RF-22, RF-39, CL-2, CL-3
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T21. Pantalla.** Crear `SettingsScreen` y `SettingsRow`, con sus tokens, `testTag` y previews en ambos temas. Las filas deshabilitadas no son clicables y se anuncian como deshabilitadas.
  - **Cubre:** RF-4 a RF-16, RNF-1 a RNF-3, CL-10
  - **Hecho cuando:** las previews compilan.

- [x] **T22. Hoja de tema.** Crear `ThemeSheet` con el estilo de `NoteColorSheet`.
  - **Cubre:** RF-17 a RF-21, RNF-1, RNF-3, CL-2
  - **Hecho cuando:** la preview compila.

- [x] **T23. Ruta y navegación.** Crear `SettingsRoute` con el `Snackbar` de RF-39. Añadir la ruta `settings`, conectar `onNavigateToSettings` y pasar `BuildConfig.VERSION_NAME` desde `:app`.
  - **Cubre:** RF-1 a RF-3, RF-8, RF-39, CL-9
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila y Ajustes se abre desde Mis notas.

- [x] **T24. Revisar Ajustes** (tú). Comparar las previews con Figma `4:1794` y `40:1211`, y dar el visto bueno a la hoja de tema.
  - **Cubre:** RNF-1
  - **Hecho cuando:** das el visto bueno o las diferencias quedan resueltas.

- [x] **T25. Pruebas de Ajustes** (`test-writer`). Crear `SettingsViewModelTest` y `SettingsScreenTest`.
  - **Cubre:** RF-2, RF-4 a RF-21, RF-39, RNF-3, CL-2, CL-3, CL-9, CL-10
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 6 — Verificación y cierre

- [x] **T26. Verificación completa.** Ejecutar `./gradlew test`, `./gradlew connectedDebugAndroidTest`, `./gradlew assembleDebug` y `./gradlew lint --continue`.
  - **Cubre:** principio 4
  - **Hecho cuando:** las pruebas pasan, compila y lint solo muestra el error previo de `NoteEditorRoute.kt`.
  - **Resultado (2026-11-02, Medium_Phone_API_36.0):** `test`: `:domain` 204, `:data` 91, `:presentation` 204 y `:app` 1, sin fallos. Instrumentadas: `:presentation` 144, `:data` 28 y `:app` 1, con 0 fallos. `assembleDebug` compila. `lint --continue`: un solo error, el previo de `NoteEditorRoute.kt:50`.

- [x] **T27. Revisión de arquitectura** (`architecture-reviewer`).
  - **Cubre:** principios 1, 3 y 6
  - **Hecho cuando:** no quedan hallazgos sin resolver o sin tu decisión.
  - **Resultado:** sin violaciones graves. Corregidos: el aviso retrasaba la navegación de volver, el velo de la barra de navegación antes de API 26, `SheetHandle` movido a `presentation/common`, comentarios y documentación. Decisiones del usuario: `:app` puede usar `ThemeMode` de `:domain` y se añade al diagrama de `CLAUDE.md`; el pie sigue diciendo «Notas», como el diseño, y unificarlo con `app_name` («NotesApp») queda aparte.

- [ ] **T28. Verificación manual en el emulador.** Las comprobaciones de los criterios de finalización de la spec, en API 36 y API 30, incluidos los menús y diálogos en claro, instalar sobre la versión anterior y el teléfono en el modo contrario al de la app.
  - **Cubre:** RF-1 a RF-3, RF-23, RF-26 a RF-37, RF-40, RNF-1, CL-1, CL-4 a CL-8
  - **Hecho cuando:** cada paso se comprueba y su resultado queda anotado aquí.
  - **Resultado parcial (2026-11-02, API 36):** nueva instalación sobre `main` conserva las notas [CL-1]; flecha y Atrás vuelven a Mis notas [RF-2, RF-3]; tocar Papelera no hace nada [RF-10]; la hoja sobrevive al giro [CL-3]; el filtro sigue activo tras cambiar de tema [CL-5]; con el teléfono en el modo contrario se mantiene el tema [CL-6]; con la carpeta sin escritura aparece el aviso y el tema se mantiene hasta cerrar la app [RF-38, RF-39]; menú y diálogo legibles en claro [RF-30]; barras correctas en ocho cambios de tema medidos [RF-36, RF-37]. **Pendiente por decisión del usuario:** repetir en API 30.

- [x] **T29. Cierre.** Actualizar `.memory/MEMORY.md` y marcar spec, plan y tareas como implementados o completados, reflejando en el plan cualquier cambio hecho al implementar. Anotar como candidato a spec la posible copia de las notas en la nube frente al aviso «Tus notas se guardan en este dispositivo».
  - **Cubre:** principio 2
  - **Hecho cuando:** los tres documentos tienen su estado final y la memoria refleja la pantalla de Ajustes.
