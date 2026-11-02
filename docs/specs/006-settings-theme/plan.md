# Plan 006 — Pantalla de Ajustes y tema claro/oscuro

> **Estado:** implementado · **Fecha:** 2026-11-02 · **Spec:** [spec.md](spec.md) (implementada) · **Diseño:** Figma Ajustes `4:1794` y `40:1211`; en claro, Mis notas `40:956`, editor `40:1037` y selector de color `40:1115`

Plan técnico para implementar la spec 006. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

La elección del tema se guarda con DataStore Preferences, una dependencia nueva aprobada para esta spec, detrás de un repositorio de `:domain`. Si falla el guardado, el repositorio mantiene la elección en memoria, así que se sigue aplicando durante la sesión.

Un ViewModel raíz, `AppThemeViewModel`, expone el tema a `MainActivity`, que lo aplica a `NotesAppTheme`, a las barras del sistema y a la ventana inicial. Mientras el tema no se ha leído, la actividad no dibuja nada, para evitar un primer fotograma con el tema equivocado.

Ajustes es una pantalla MVI nueva con un solo elemento activo: la fila Tema, que abre una hoja inferior. Las demás filas son solo visuales.

La paleta clara sale de los frames de Figma. El cambio más delicado es separar los dos usos actuales de `primary`: como relleno sigue siendo Aqua, pero como texto o ícono de acento Aqua es ilegible sobre claro (contraste 1,28:1).

No hay cambios en el esquema de Room.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Única dependencia nueva: `androidx.datastore:datastore-preferences` 1.3.0, aprobada por el usuario, solo en `:data` y con su versión en `libs.versions.toml`. `UiModeManager` es del framework. `lifecycle-runtime-compose` ya está en el catálogo y solo se añade a `:app`. |
| 2. La spec manda | Cada parte cita sus RF. Si algo se aparta de la spec, se actualizan ambas en el mismo commit. |
| 3. Lógica fuera de la UI | El tema por defecto vive en `:domain`. Los composables solo pintan `SettingsUiState` y emiten intents. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, repositorio, ViewModels y UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | El esquema no cambia y cambiar de tema no toca las notas [RNF-4]. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:common` | Ninguno. `AppResult` ya existe. | — |
| `:domain` | Paquete `settings`: `ThemeMode`, `SettingsError`, `SettingsRepository`, `GetThemeModeUseCase` y `SetThemeModeUseCase`. | RF-22, RF-24, RF-25, RF-38 |
| `:data` | Paquete `settings`: `SettingsRepositoryImpl` y mappers. Módulo Hilt `PreferencesModule` y nuevo `@Binds`. | RF-22, RF-23, RF-25, RF-38 |
| `:presentation` | Paquete `settings`; `AppThemeViewModel`; paleta clara y tokens; ruta de navegación; ajuste de colores de acento; íconos y textos. | RF-1 a RF-21, RF-27 a RF-35, RF-39 |
| `:app` | `MainActivity`, manifiesto, temas XML y `BuildConfig`. | RF-8, RF-26, RF-36, RF-37, RF-40 |
| Documentación | `CLAUDE.md`: se añade DataStore a la fila de persistencia del stack y a la lista de `:data`. | — |

## 4. Diseño por capa

### 4.1 `:domain` (paquete `settings`)

- `enum class ThemeMode { LIGHT, DARK }`, con `DEFAULT = DARK` en su `companion object` [RF-24].
- `sealed interface SettingsError { data object Storage : SettingsError }`.
- `SettingsRepository`:
  - `fun observeThemeMode(): Flow<ThemeMode?>`: `null` significa que no hay tema guardado o que no se puede leer.
  - `suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError>`.
- `GetThemeModeUseCase`: convierte `null` en `ThemeMode.DEFAULT` y devuelve `Flow<ThemeMode>` [RF-24, RF-25].
- `SetThemeModeUseCase`: delega en el repositorio [RF-22].

### 4.2 `:data` (paquete `settings`)

`SettingsRepositoryImpl` es `internal` y `@Singleton`, y recibe `DataStore<Preferences>`.

- **Lectura:** combina `dataStore.data`, mapeado a `ThemeMode?`, con un `MutableStateFlow<ThemeMode?>` llamado `chosenThemeMode`. Si este último tiene valor, tiene prioridad. **Ajuste al implementar:** antes solo guardaba lo no guardado, y tras una lectura fallida un guardado correcto no llegaba a quien ya observaba.
- **Escritura:** pone la elección en `chosenThemeMode` y guarda con `dataStore.edit`. Si lanza `IOException`, responde `AppResult.Error(SettingsError.Storage)`; el tema sigue aplicado hasta cerrar la app [RF-38].
- **Lectura fallida:** en `data`, un operador `catch` atrapa solo `IOException` y emite `null` [RF-25]. Los demás errores y `CancellationException` no se atrapan.
- **Archivo corrupto:** un `ReplaceFileCorruptionHandler` lo sustituye por preferencias vacías, que dan `null` [RF-25]. La siguiente elección se guarda con normalidad.
- **Valor no válido:** un texto desconocido da `null` [RF-25].
- **Mappers:** `ThemeMode.toStorageValue()` y `String?.toThemeModeOrNull()`, con los valores estables `"light"` y `"dark"`, independientes del nombre del enum.

`PreferencesModule` (`@Provides`, `@Singleton`) crea la única instancia de DataStore con `PreferenceDataStoreFactory`, el archivo `settings` y un `CoroutineScope` propio con `@IoDispatcher` y `SupervisorJob`. `RepositoryModule` añade el `@Binds`. Para cumplir RNF-4, `datastore/settings.preferences_pb` se excluye de las copias de seguridad en `backup_rules.xml` y `data_extraction_rules.xml`.

### 4.3 `:presentation`

**Tema (`theme/`)**

- `NotesAppTheme(darkTheme: Boolean = true, content)`: el valor por defecto evita tocar las previews y pruebas existentes.
- Paleta clara, tomada de Figma:

| Rol | Oscuro (actual) | Claro |
|---|---|---|
| `background`, `surface` | `Black` | `#FAFBFC` |
| `onBackground`, `onSurface` | `Snow` | `#202629` |
| `surfaceContainer`, `surfaceVariant` | `Graphite` | `#EEF1F3` |
| `surfaceContainerHigh` (nuevo, hojas) | `Graphite` | `#FFFFFF` |
| `onSurfaceVariant` | `Ash` | `#626C71` |
| `outlineVariant` | `Iron` | `#DCE2E5` |
| `primary` / `onPrimary` | `Aqua` / `Ink` | Sin cambios |
| `secondary` | `Vanilla` | Sin cambios |
| `tertiary` | `Mint` | `Aqua`, por el chip «Todas» de `40:956` |
| `NoteColors.none`, `swatch` | `Graphite`, `Swatch` | `#EEF1F3` |
| `NoteColors.swatchBorder` | `SwatchBorder` | `#DCE2E5` |
| Ocho colores de nota, `content`, `metadata` | Actuales | Sin cambios [RF-32] |

- Contraste medido en claro: `#202629` ≥ 13,5:1, `#626C71` ≥ 4,74:1 y `#17646D` ≥ 6,01:1 sobre `#FAFBFC`, `#EEF1F3` y `#FFFFFF` [RNF-2].
- **Acento de contenido (D3):** nuevo `NotesTheme.accent.content`: `Aqua` en oscuro y `#17646D` en claro. Lo usan el botón activo de la barra del editor, la etiqueta del color elegido, los indicadores de carga, los cursores, los botones del diálogo y los íconos de Ajustes.
- `NoteColorSheet`: el borde de la muestra elegida pasa a un token propio (`Snow` en oscuro, `#17646D` en claro) y el contenedor pasa a `surfaceContainerHigh` [RF-29].
- `NoteSurfaceTheme`: `accent.content` toma `onContainer` y `AppColorsTheme` lo restaura; se actualizan los comentarios que hablan de «tema oscuro» [RF-33]. `NoteEditorRoute` solo fuerza íconos oscuros en la barra de estado sobre una nota con color, porque antes los ponía claros con «Sin color», lo que los ocultaría en tema Claro [RF-36].
- Otros tokens en `Dimens` y `Type`: fila de 68 dp, íconos de 21 dp, tarjeta de privacidad, estilos de fila. `DisabledAlpha = 0.38f` vive en `Color.kt`, como constante.

**Tema de la app:** `AppThemeViewModel` (`@HiltViewModel`) expone `StateFlow<ThemeMode?>`, que vale `null` hasta la primera lectura.

**Pantalla (`settings/`)**

| Clase | Contenido |
|---|---|
| `SettingsUiState` | `themeMode: ThemeMode`, `isThemeSheetVisible: Boolean` |
| `SettingsIntent` | `BackClicked`, `ThemeRowClicked`, `ThemeSelected(mode)`, `ThemeSheetDismissed` |
| `SettingsEffect` | `NavigateBack`, `ShowThemeSaveError` |
| `SettingsViewModel` | Observa `GetThemeModeUseCase`. Al elegir, si el tema es distinto, llama a `SetThemeModeUseCase`, y un error emite `ShowThemeSaveError`. La hoja se cierra aparte con `ThemeSheetDismissed`, tras su animación, para que el tema se aplique sin esperarla [RF-19 a RF-22, RF-39, CL-2] |
| `SettingsRoute` | Recolecta el estado, atiende los efectos y muestra el aviso en un `Snackbar` |
| `SettingsScreen` | Composable sin estado; recibe `state`, `appVersion` y `onIntent` |
| `ThemeSheet` | `ModalBottomSheet` con `selectableGroup` y dos opciones, con el estilo y el cierre animado de `NoteColorSheet` [RF-17, RF-18, RF-21, RNF-1] |

- La visibilidad de la hoja vive en el ViewModel, así que sobrevive al giro [CL-3].
- `SettingsRow` tiene dos variantes. La activa es clicable con `Role.Button`. La deshabilitada usa `alpha(disabledAlpha)`, no tiene `clickable` y se anuncia con `semantics { disabled() }` [RF-9, RF-10, RNF-3].
- El interruptor es un dibujo propio de 42 × 26 dp, como el del diseño, porque el `Switch` de Material no admite ese tamaño y aquí es solo visual [RF-13, CL-10].
- La fila Papelera usa el texto «Próximamente» y no muestra valor [RF-14, RF-15].
- El valor de la fila Tema sigue cada frame: gris y regular en oscuro; `#17646D` y seminegrita en claro.
- El contenido va en una `Column` con `verticalScroll`, porque no cabe en pantallas bajas. Los elementos llevan `testTag` en `SettingsTestTags`.
- Íconos nuevos desde Figma: escudo, luna, paleta, cuadrícula, guardar, exportar, papelera, ayuda y flecha derecha. La flecha de volver reutiliza `ic_arrow_left`.

**Navegación:** `NotesNavHost(appVersion: String, modifier)` añade la ruta `settings` y conecta `onNavigateToSettings`, sustituyendo el `{}` actual. Volver usa `popBackStack()`; el botón Atrás del sistema funciona sin código adicional [RF-1 a RF-3].

### 4.4 `:app`

**`MainActivity`**

- Obtiene `AppThemeViewModel` con `by viewModels()`.
- Mientras el tema sea `null`, retiene el primer dibujo con un `OnPreDrawListener` del contenido [RF-40].
- En `onStop`, en API 31+, llama a `UiModeManager.setApplicationNightMode` para que el sistema pinte con ese tema la siguiente ventana inicial [RF-40]. **Ajuste al implementar:** al llamarlo con la app en pantalla, el cambio de configuración dejaba las barras del sistema con el tema anterior [RF-36, RF-37], y volver a aplicarlas no lo corregía.
- Con el tema ya leído:
  - pinta `NotesAppTheme(darkTheme = …)` [RF-27 a RF-31];
  - llama a `enableEdgeToEdge` con `SystemBarStyle.light` o `dark` [RF-36, RF-37];
  - actualiza el fondo de la ventana, para que no asome el color anterior detrás de Compose.

**Ventana inicial (D2)**

- `values/colors.xml` y `values-notnight/colors.xml`: `window_background` con `#080808` y `#FAFBFC`.
- `values/themes.xml`: `android:windowBackground` y `android:windowDisablePreview = true`, porque antes de API 31 el sistema no permite elegir el tema de la ventana inicial.
- `values-v31/themes.xml`: `android:windowSplashScreenBackground`.

**Manifiesto y versión**

- `android:configChanges="uiMode"` en `MainActivity`, para que el cambio de modo no recree la actividad [RF-19, RF-26, CL-4, CL-5, CL-6].
- `buildFeatures.buildConfig = true`, y `BuildConfig.VERSION_NAME` se pasa a `NotesNavHost` [RF-8, CL-9].

## 5. Decisiones técnicas

- **D1. DataStore Preferences.** Elección del usuario. Ofrece lectura como `Flow` y lectura y escritura sin bloquear el hilo principal, y sus fallos llegan como `IOException`, que es lo que necesitan RF-25, RF-38 y RF-39. Se usa la versión estable 1.3.0. Pide `kotlin-stdlib` 2.2.20; el proyecto compila con Kotlin 2.2.10 y hoy resuelve `kotlin-stdlib` 2.3.21, así que se confirma compilando antes de seguir (sección 9).
- **D2. Ventana inicial según la versión de Android.** En API 31+ se usa el modo noche por aplicación, fijado al salir de la pantalla. Antes de API 31 se desactiva esa ventana, así que no hay destello, aunque la app puede tardar un poco más en verse. La primera apertura tras actualizar puede usar el modo del teléfono en esa ventana, porque el sistema aún no conoce la elección; RF-40 solo aplica con un tema Claro guardado.
- **D3. `accent.content` en vez de cambiar `primary`.** `primary` se usa sobre todo como relleno (botón «Nueva nota», «Aplicar color», casillas y chips) y en claro sigue siendo Aqua. Cambiarlo obligaría a tocar todos los rellenos.
- **D4. `Todas` en Aqua solo en claro.** El código actual lo pinta con `tertiary` (Mint) en oscuro, y RF-31 pide no cambiar ese tema. Por eso `tertiary` difiere entre paletas.
- **D5. Nombres de fila en seminegrita.** Figma usa Inter Medium, pero la app solo incluye Regular y SemiBold. Agregar el archivo de fuente es un paso aparte que se puede hacer después sin tocar la estructura.
- **D6. `Slate` se mantiene en `#3B4C50`.** Figma usa `#405357` en ambos temas, pero la spec 003 lo oscureció para llegar a 4,5:1.
- **D7. Versión desde `:app`.** `:presentation` no conoce la versión de la aplicación; pasarla como parámetro evita consultar `PackageManager` desde la pantalla.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:domain` | `GetThemeModeUseCaseTest`, `SetThemeModeUseCaseTest` | RF-22, RF-24, RF-25, propagación del error |
| `:data` | `SettingsRepositoryImplTest` (con `DataStore<Preferences>` fake), mappers | RF-22, RF-23, RF-25, RF-38, valor desconocido e `IOException` al leer y al escribir |
| `:data` (instrumentada) | `PreferencesModule` con un archivo temporal | Guardar y volver a leer tras crear otra instancia [RF-23]; archivo corrupto [RF-25] |
| `:presentation` (unitarias) | `SettingsViewModelTest`, `AppThemeViewModelTest` | Estado y efecto por intent; RF-2, RF-16 a RF-21, RF-39, CL-2, CL-3; `null` hasta leer |
| `:presentation` (UI) | `SettingsScreenTest` | RF-4 a RF-18, RF-21, RNF-3, CL-9, CL-10, en ambos temas |
| `:presentation` (UI) | `NoteListScreenTest`, `NoteEditorScreenTest`, `NoteColorSheet` | Colores de nota y acento en claro: RF-32 a RF-35 |
| Manual | Emulador API 36 y emulador API 30 | RF-1 a RF-3 de punta a punta, RF-23, RF-26 a RF-31, RF-36, RF-37, RF-40, CL-1, CL-4 a CL-8, comparación con Figma |

Para forzar RF-38 y RF-39 en las pruebas se usa un `DataStore` fake cuyo `updateData` lanza `IOException`. No se añade la prueba de flujo con `HiltTestRunner`: sigue pendiente como trabajo aparte.

## 7. Orden de implementación

1. `:domain`: modelos, repositorio y casos de uso, con pruebas.
2. `:data`: añadir DataStore al catálogo y comprobar que compila; repositorio, mappers, módulos Hilt y exclusión de copias de seguridad, con pruebas.
3. `theme/`: paleta clara, `accent.content` y tokens. Sustituir los usos de acento. Revisar las previews en ambos temas.
4. `AppThemeViewModel` y `MainActivity`: tema, barras, ventana inicial y manifiesto. Probar a mano cambiando el valor por defecto.
5. `settings/`: estado, ViewModel, pantalla y hoja, con pruebas.
6. Navegación y versión.
7. Revisión frente a Figma de las cuatro pantallas en claro, y de las oscuras para confirmar que no cambian.
8. `./gradlew test`, pruebas instrumentadas, comprobaciones manuales y revisión con `architecture-reviewer`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1 a RF-3 | Navegación, `SettingsViewModel` |
| RF-4 a RF-8 | `SettingsScreen`, `BuildConfig` |
| RF-9 a RF-15 | `SettingsRow`, `SettingsScreen` |
| RF-16 a RF-21 | `SettingsViewModel`, `ThemeSheet` |
| RF-22 a RF-25 | `SettingsRepositoryImpl`, casos de uso |
| RF-26 | `MainActivity`, `configChanges` |
| RF-27 a RF-35 | Paleta, `accent.content`, `NoteSurfaceTheme`, `NoteColorSheet` |
| RF-36, RF-37 | `MainActivity` |
| RF-38, RF-39 | `SettingsRepositoryImpl`, `SettingsViewModel`, `SettingsRoute` |
| RF-40 | `MainActivity`, temas XML, `UiModeManager` |
| RNF-1 a RNF-5 | Secciones 4.2 y 4.3, `strings.xml` |
| CL-1 a CL-10 | Sección 6 |

## 9. Riesgos

- **Ventana inicial (RF-40):** es lo único que depende de cómo se comporta el sistema. Hay que comprobar `windowDisablePreview` en API 30 y `setApplicationNightMode` en API 36. Si alguno no cumple RF-40, se revisa la spec antes de seguir. Si el proceso muere sin pasar por `onStop`, la ventana de la siguiente apertura puede usar el tema anterior.
- **API 24 y 25:** la barra de navegación solo admite íconos claros, así que en tema Claro lleva un velo oscuro. No hay emulador de esas versiones para comprobarlo.
- **Compatibilidad de DataStore:** la versión 1.3.0 se compiló con una versión de Kotlin posterior a la del proyecto. Si no compila, se usa la 1.2.1, que pide `kotlin-stdlib` 2.0.21 y ofrece las mismas funciones que usa este plan, sin subir Kotlin.
- **Tiempo de lectura:** DataStore lee el archivo sin bloquear, y `MainActivity` no dibuja hasta recibir el tema. Se comprueba que la apertura no se note más lenta en el emulador.
- **Colores no previstos en claro:** los componentes de Material usan roles que hoy no están definidos, como los de `DropdownMenu` y `AlertDialog`. Se revisan a mano con RF-30, y cualquier rol necesario se añade a la paleta clara.
- **Interacción con el tema de la nota:** `NoteSurfaceTheme` y `AppColorsTheme` suponen hoy un tema oscuro. Se revisa el editor con y sin color en ambos temas.
- **Aviso al salir:** si el usuario sale de Ajustes justo tras un fallo al guardar, puede no llegar a ver el aviso de RF-39. Es poco probable y el tema sigue aplicado.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
