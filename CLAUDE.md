# 📝 CLAUDE.md — App de Notas (Android)

> Este archivo define la **arquitectura** y las **convenciones** que debes seguir al trabajar en este repositorio.

## Índice

1. [Stack](#stack)
2. [Módulos](#módulos)
3. [Arquitectura](#arquitectura)
4. [Pruebas](#pruebas)
5. [Comandos](#comandos)
6. [Convenciones](#convenciones)

---

## Stack

| Área | Tecnología |
|---|---|
| **Lenguaje** | Kotlin, corrutinas y Flow |
| **UI** | Jetpack Compose (Material 3) + Navigation Compose |
| **Inyección de dependencias** | Hilt |
| **Persistencia** | Room |
| **Build** | Gradle con Kotlin DSL y version catalog (`gradle/libs.versions.toml`) |

> **Importante:** todas las versiones viven en el version catalog. No escribas versiones directamente en los `build.gradle.kts`.

---

## Módulos

| Módulo | Tipo | Contenido |
|---|---|---|
| `:app` | Android application | `Application` con `@HiltAndroidApp`, `MainActivity`, grafo de navegación raíz |
| `:presentation` | Android library | Pantallas Compose, ViewModels, estado de UI, tema, navegación |
| `:domain` | Kotlin/JVM puro | Modelos, interfaces de repositorio, casos de uso |
| `:data` | Android library | Room (entidades, DAOs, base de datos), implementaciones de repositorio, mappers, módulos Hilt |
| `:common` | Kotlin/JVM puro | Utilidades compartidas: `Result`/errores, dispatchers, extensiones |

### Regla de dependencias

```text
app ──> presentation ──> domain ──> common
 └────> data ──────────> domain
```

- **`:domain`** no depende de Android, Room, Compose ni Hilt (solo `javax.inject` para `@Inject`).
- **`:presentation`** y **`:data`** no se conocen entre sí. `:app` los une para que Hilt arme el grafo.
- **`:common`** no depende de ningún otro módulo.
- **Nunca** expongas entidades de Room fuera de `:data`; se mapean a modelos de dominio.

---

## Arquitectura

**Clean Architecture** con **MVI** en la capa de presentación.

### Presentation (MVI)

Cada pantalla vive en su propio paquete (`notes/list`, `notes/detail`, ...) con:

| Clase | Responsabilidad |
|---|---|
| `XxxUiState` | `data class` inmutable, única fuente de verdad de la pantalla |
| `XxxIntent` | `sealed interface` con las acciones del usuario |
| `XxxEffect` | `sealed interface` para eventos de una sola vez (navegar, snackbar) |
| `XxxViewModel` | `@HiltViewModel`; expone `StateFlow<XxxUiState>` y un `Flow<XxxEffect>` (Channel); recibe intents con `onIntent(intent)` |
| `XxxRoute` | Composable que obtiene el ViewModel, recolecta el estado con `collectAsStateWithLifecycle()` y maneja efectos |
| `XxxScreen` | Composable **sin estado** que recibe `state` y `onIntent`. Es el que se previsualiza y se prueba |

**Reglas:**

- Los ViewModels solo dependen de **casos de uso**, nunca de repositorios ni DAOs.
- El estado se actualiza con `_state.update { it.copy(...) }`.
- Sin lógica de negocio en composables.
- Agrega `testTag` a los elementos con los que interactúan las pruebas de UI.

### Domain

- **Modelos:** `data class` de Kotlin puro (por ejemplo `Note`).
- **Repositorios:** solo interfaces (`NoteRepository`).
- **Casos de uso:** una clase por acción, con `operator fun invoke`, nombradas `VerboSustantivoUseCase` (`GetNotesUseCase`, `SaveNoteUseCase`, `DeleteNoteUseCase`).
- Las lecturas observables devuelven `Flow<T>`; las escrituras son `suspend`.

### Data

- `NoteEntity`, `NoteDao`, `NotesDatabase` en el paquete `local`.
- `NoteRepositoryImpl` implementa la interfaz de dominio y es `internal`.
- **Mappers** como funciones de extensión: `NoteEntity.toDomain()`, `Note.toEntity()`.
- **Módulos Hilt:** `DatabaseModule` (`@Provides`) y `RepositoryModule` (`@Binds`), instalados en `SingletonComponent`.
- Exporta el esquema de Room (`exportSchema = true`) y escribe una `Migration` en cada cambio de versión.

> **Nota:** no uses `fallbackToDestructiveMigration` salvo en desarrollo temprano.

### Common

- **Dispatchers inyectables** mediante qualifiers (`@IoDispatcher`, `@DefaultDispatcher`). No uses `Dispatchers.IO` directamente en clases de producción.
- **Tipo de resultado/error** compartido para operaciones que pueden fallar.

---

## Pruebas

> Todo caso de uso, ViewModel, repositorio y mapper nuevo lleva pruebas. Toda pantalla nueva lleva al menos una prueba de UI.

### Unitarias (`src/test`)

**Herramientas:** JUnit, `kotlinx-coroutines-test`, Turbine para Flows, MockK (o fakes cuando sean más simples).

| Módulo | Qué se prueba |
|---|---|
| `:domain` | Casos de uso con repositorios fake |
| `:presentation` | ViewModels: estado emitido y efectos por cada intent (usa `MainDispatcherRule`) |
| `:data` | Mappers y repositorio con DAO fake |

Nombres de prueba con backticks:

```kotlin
@Test
fun `given empty title when saving note then emits validation error`() = runTest {
    // ...
}
```

### Instrumentadas (`src/androidTest`)

| Tipo | Cómo |
|---|---|
| **UI** | Compose UI Test (`createComposeRule`) sobre los `XxxScreen` sin estado, pasando un `UiState` fijo |
| **Flujos completos** | `createAndroidComposeRule` + `@HiltAndroidTest` con `HiltTestRunner` personalizado y `@TestInstallIn` para reemplazar módulos |
| **Room** | Pruebas de DAO con `Room.inMemoryDatabaseBuilder` |

---

## Comandos

```bash
./gradlew assembleDebug              # compilar
./gradlew test                       # pruebas unitarias de todos los módulos
./gradlew :domain:test               # pruebas de un módulo
./gradlew connectedDebugAndroidTest  # pruebas instrumentadas (requiere emulador/dispositivo)
./gradlew lint                       # lint
```

> Antes de dar una tarea por terminada, ejecuta las pruebas unitarias de los módulos que tocaste.

---

## Memoria
- Al empezar, lee `MEMORY.md` para conocer el estado del proyecto y las decisiones tomadas.
- Al terminar una tarea, actualízalo: estado actual, decisiones importantes (con su porqué) y errores a evitar.
- Mantenlo breve (máximo ~50 líneas): resume o elimina lo que ya no aporte.
- Si algo se convierte en una regla permanente, propón moverlo a `CLAUDE.md` en lugar de dejarlo en la memoria.
- No guardes nunca datos sensibles (claves, tokens, datos personales).

## Convenciones

### Código

- **Kotlin idiomático:** inmutabilidad, `sealed interface` para estados cerrados, funciones de extensión para mappers, sin `!!`.
- **Visibilidad** `internal` por defecto en `:data` y para implementaciones.
- **Paquetes por feature** dentro de cada módulo, no por tipo de clase.
- **Strings** visibles al usuario en `strings.xml`, nunca en el código.

### Commits

Conventional Commits, con el módulo como scope cuando aplique:

```text
feat(data): add NoteDao
fix(presentation): restore scroll position on note list
test(domain): cover SaveNoteUseCase validation
```

Tipos permitidos: `feat`, `fix`, `test`, `refactor`, `chore`, `docs`.

### Restricciones

- ❌ No agregues dependencias nuevas sin consultarlo antes.
- ❌ No crees módulos nuevos; por ahora solo existen los cinco listados arriba.
