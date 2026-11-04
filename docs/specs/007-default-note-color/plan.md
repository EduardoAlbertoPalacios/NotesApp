# Plan 007 — Color predeterminado de las notas nuevas

> **Estado:** implementado · **Fecha:** 2026-11-03 · **Spec:** [spec.md](spec.md) (implementada) · **Diseño:** Figma selector de color `4:1710` y `40:1115`; Ajustes `4:1794` y `40:1211`

Plan técnico para implementar la spec 007. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

El color predeterminado se guarda igual que el tema: con DataStore Preferences, detrás de `SettingsRepository`. Si falla el guardado, el repositorio mantiene la elección en memoria durante la sesión.

La hoja del editor se separa de sus intents para que Ajustes la reutilice con sus propios textos. Ajustes añade una fila activa y el estado del selector, con el mismo modelo que el editor.

Al abrir una nota nueva, el editor lee el color de forma asíncrona. Mientras tanto usa su estado de carga actual, que pinta el fondo de la app, de modo que nunca se ve Cielo si el predeterminado es otro.

No hay cambios en el esquema de Room, dependencias nuevas ni cambios en `:app` o `:common`.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Sin dependencias nuevas. DataStore ya se aprobó en la spec 006. |
| 2. La spec manda | Cada parte cita sus RF. El mismo commit actualiza las specs 003 y 006 (sección 4.4). |
| 3. Lógica fuera de la UI | El valor por defecto vive en `:domain`. Los composables solo pintan estado y emiten intents. |
| 4. Sin pruebas no hay commit | Pruebas de dominio, repositorio, mappers, ViewModels y UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | El esquema no cambia y ningún cambio del predeterminado toca las notas guardadas [RF-20]. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:domain` | `NoteColor.DEFAULT`; dos métodos en `SettingsRepository`; `GetDefaultNoteColorUseCase` y `SetDefaultNoteColorUseCase`. | RF-21, RF-23, RF-24, RF-25 |
| `:data` | Lectura, escritura y mappers en el paquete `settings`. | RF-21, RF-22, RF-24, RF-25 |
| `:presentation` | Hoja compartida, fila y estado en Ajustes, color inicial en el editor y textos. | RF-1 a RF-20, RF-26 a RF-29 |
| Documentación | Specs 003 y 006, y `.memory/MEMORY.md`. | — |

## 4. Diseño por capa

### 4.1 `:domain`

- `NoteColor` añade `companion object { val DEFAULT = AQUA }` [RF-23, RF-24].
- `SettingsRepository` añade:
  - `fun observeDefaultNoteColor(): Flow<NoteColor?>`: `null` significa que no hay color guardado o que no se puede leer.
  - `suspend fun setDefaultNoteColor(color: NoteColor): AppResult<Unit, SettingsError>`.
- `GetDefaultNoteColorUseCase`: convierte `null` en `NoteColor.DEFAULT` y devuelve `Flow<NoteColor>` [RF-23, RF-24].
- `SetDefaultNoteColorUseCase`: delega en el repositorio [RF-21].

### 4.2 `:data` (paquete `settings`)

`SettingsRepositoryImpl` repite el patrón del tema, con su propia clave `default_note_color` y su propio `MutableStateFlow`:

- **Lectura:** combina lo guardado con lo elegido en la sesión; lo elegido tiene prioridad [RF-22, RF-25].
- **Escritura:** pone la elección en memoria y guarda con `dataStore.edit`. Si lanza `IOException`, responde `AppResult.Error(SettingsError.Storage)` [RF-21, RF-25].
- **Lectura fallida o valor desconocido:** da `null` [RF-24].
- **Mappers:** `NoteColor.toStorageValue()` y `String?.toNoteColorOrNull()` en `SettingsMappers.kt`, con valores estables independientes del nombre del enum (`"none"`, `"aqua"`, `"rose"`, `"vanilla"`, `"lavender"`, `"mint"`, `"peach"`, `"sand"`).

La copia de seguridad ya excluye el archivo de preferencias entero, así que no hay que tocarla.

### 4.3 `:presentation`

**Hoja compartida (`notes/common/NoteColorPickerSheet.kt`)**

El contenido de `NoteColorSheet` (encabezado, paleta, vista previa, botón y cierre animado) se mueve aquí sin cambiar su aspecto. Recibe:

```kotlin
title: String, description: String, previewTitle: String, pickerColor: NoteColor,
onColorSelected: (NoteColor) -> Unit, onApply: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier
```

- `NoteColorSheet` queda como envoltura del editor: traduce los callbacks a `NoteEditorIntent` y pasa los textos actuales, por lo que su comportamiento no cambia.
- Las etiquetas pasan a `ColorPickerTestTags`. `NoteEditorTestTags.COLOR_*` apunta a ellas, así que las pruebas existentes siguen sirviendo.

**Ajustes (`settings/`)**

| Clase | Cambios |
|---|---|
| `SettingsUiState` | `defaultNoteColor: NoteColor = NoteColor.DEFAULT` y `defaultColorPicker: NoteColor? = null`, donde `null` significa hoja cerrada [RF-3, RF-6, RF-7] |
| `SettingsIntent` | `DefaultColorRowClicked`, `DefaultColorOptionSelected(color)`, `DefaultColorApplied`, `DefaultColorPickerDismissed` |
| `SettingsEffect` | `ShowDefaultColorSaveError` [RF-26] |
| `SettingsViewModel` | Observa `GetDefaultNoteColorUseCase`. Abrir copia el color vigente al selector [RF-6, RF-16]. Aplicar cierra el selector y, si el color cambió, llama a `SetDefaultNoteColorUseCase` [RF-13, RF-14, RF-21]. Descartar solo cierra [RF-12, RF-15] |
| `SettingsRoute` | Muestra el aviso en el `Snackbar` existente [RF-26] |
| `SettingsScreen` | `DefaultColorRow`, activa como `ThemeRow`, con `testTag` y el nombre del color en el gris de los frames [RF-1 a RF-3]. Muestra la hoja si `defaultColorPicker` no es nulo [RF-4 a RF-11] |

- El selector vive en el ViewModel, así que sobrevive a girar la pantalla y a pasar a segundo plano.
- Textos nuevos: `settings_default_color_preview_title` («Nota nueva») y `settings_default_color_save_error`. La hoja reutiliza `settings_default_color` y `settings_default_color_detail`. Se elimina `settings_default_color_value`, que deja de usarse.

**Editor (`notes/editor/`)**

`NoteEditorViewModel` recibe `GetDefaultNoteColorUseCase`. Con una nota nueva:

1. Pone `isLoading = true`, sin cambiar `isNewNote`.
2. Lee el primer valor con `first()`.
3. Asigna ese color a `note` y al estado, y quita `isLoading` [RF-17].

Las notas guardadas no lo leen [RF-20]. El editor no conoce `SetDefaultNoteColorUseCase`, por lo que no puede alterar el predeterminado [RF-19]. Aplicar otro color y abandonar una nota vacía siguen funcionando igual [RF-18].

### 4.4 Documentación

- **Spec 003:** RF-22 y su línea de «Fuera de alcance» remiten a la spec 007.
- **Spec 006:** RF-9, RF-11 y la línea de «Fuera de alcance» dejan de incluir esta fila y remiten a la spec 007.
- **`.memory/MEMORY.md`:** estado y decisiones.

## 5. Decisiones técnicas

- **D1. Extraer la hoja en vez de copiarla.** Es el único cambio estructural sobre código existente. Copiarla duplicaría unas 200 líneas que deben verse igual, y una hoja común con el `NoteEditorIntent` acoplaría Ajustes al editor.
- **D2. Estado de carga en la nota nueva.** Evita que se vea Cielo un instante y que el usuario escriba antes de conocer el color. Se descartó pasar el color como argumento de navegación, porque obligaría a Mis notas a observar un ajuste que no usa.
- **D3. Métodos en `SettingsRepository`.** El color predeterminado es un ajuste, no un dato de una nota. `:domain/settings` importa `NoteColor` de `:domain/notes`, dentro del mismo módulo.
- **D4. `NoteColor.DEFAULT` no sustituye a todos los `AQUA`.** El respaldo de `NoteMappers` para colores guardados desconocidos y las notas semilla siguen en `AQUA`: no son el predeterminado del usuario.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:domain` | `GetDefaultNoteColorUseCaseTest`, `SetDefaultNoteColorUseCaseTest` | RF-21, RF-23, RF-24, propagación del error |
| `:data` | `SettingsRepositoryImplTest`, mappers | RF-21, RF-22, RF-24, RF-25; los ocho valores, valor desconocido e `IOException` al leer y al escribir |
| `:presentation` (unitarias) | `SettingsViewModelTest` | RF-3, RF-6 a RF-8, RF-12 a RF-16, RF-25, RF-26; aplicar el mismo color; hoja abierta al recrear la pantalla |
| `:presentation` (unitarias) | `NoteEditorViewModelTest` | RF-17 a RF-20; carga de la nota nueva, «Sin color», nota nueva abandonada |
| `:presentation` (UI) | `SettingsScreenTest` | RF-1 a RF-5, RF-7 a RF-11, RF-27 a RF-29, en ambos temas |
| `:presentation` (UI) | `NoteEditorScreenTest` (existente) | Que el selector del editor no cambia tras extraer la hoja |
| Manual | Emulador | Demo de los criterios de finalización de la spec y comparación con Figma |

`SettingsScreenTest` deja de tratar esta fila como atenuada. Los dos `FakeSettingsRepository` de prueba añaden los métodos nuevos. RF-20 se comprueba con los mismos datos en el flujo manual, porque ninguna clase de Ajustes tiene acceso a las notas.

## 7. Orden de implementación

1. Crear la rama `feat/default-note-color`.
2. `:domain`: `NoteColor.DEFAULT`, repositorio y casos de uso, con pruebas.
3. `:data`: mappers y repositorio, con pruebas.
4. Extraer la hoja y comprobar que `NoteEditorScreenTest` sigue en verde sin cambiar su lógica.
5. Ajustes: estado, ViewModel, fila, hoja y textos, con pruebas.
6. Editor: color inicial de la nota nueva, con pruebas.
7. Actualizar las specs 003 y 006.
8. `./gradlew test`, pruebas instrumentadas, demo manual en ambos temas, comparación con Figma y revisión con `architecture-reviewer`.
9. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1 a RF-3 | `DefaultColorRow`, `SettingsViewModel` |
| RF-4 a RF-11 | `NoteColorPickerSheet`, `SettingsScreen` |
| RF-12 a RF-16 | `SettingsViewModel`, `NoteColorPickerSheet` |
| RF-17, RF-18 | `NoteEditorViewModel` |
| RF-19, RF-20 | `NoteEditorViewModel`, sin acceso entre Ajustes y notas |
| RF-21 a RF-24 | `SettingsRepositoryImpl`, mappers, casos de uso |
| RF-25, RF-26 | `SettingsRepositoryImpl`, `SettingsViewModel`, `SettingsRoute` |
| RF-27 a RF-29 | `NoteColorPickerSheet` |
| Requisitos no funcionales | Secciones 4.2 y 4.3, `strings.xml` |
| Casos límite | Sección 6 |

## 9. Riesgos

- **Indicador de carga al crear una nota:** con D2, la nota nueva puede mostrar el indicador durante un instante antes de pintarse. Se comprueba en el emulador. Si se nota, se oculta el indicador en la nota nueva durante esa lectura, sin cambiar el flujo.
- **Foco del título:** la nota nueva pide foco al componer el contenido, que ahora aparece después de la lectura. Se comprueba que el teclado sigue abriéndose.
- **Estilo del valor en la fila:** resuelto al implementar. El plan suponía el estilo del valor de Tema, pero `4:1794` y `40:1211` lo muestran gris y regular en ambos temas, así que usa `settingsValue` y `onSurfaceVariant`.
- **Aviso al salir:** si el usuario sale de Ajustes justo tras un fallo al guardar, puede no llegar a ver el aviso. Es el mismo caso que en la spec 006, y el color sigue vigente.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
