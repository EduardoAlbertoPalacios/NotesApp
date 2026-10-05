---
name: new-feature
description: Crea el andamiaje completo de una pantalla o feature nueva en la app de notas (UiState, Intent, Effect, ViewModel, Route, Screen, caso de uso, navegación y pruebas) siguiendo Clean Architecture + MVI. Úsala cuando se pida crear una pantalla, feature o flujo nuevo.
---

# new-feature

Genera una feature nueva de punta a punta respetando la arquitectura definida en `CLAUDE.md`.

## Antes de escribir código

1. Lee `CLAUDE.md` en la raíz del proyecto. Si algo de esta skill lo contradice, manda `CLAUDE.md`.
2. Detecta el **paquete base** leyendo el `namespace` de los `build.gradle.kts` de cada módulo. No lo inventes.
3. Revisa una feature existente (si la hay) e imita su estilo, nombres y estructura de paquetes.
4. Define con el usuario, si no quedó claro en la petición:
    - **Nombre de la feature** en PascalCase (ej. `NoteList`, `NoteDetail`) y su paquete (ej. `notes/list`).
    - **Qué muestra** la pantalla y **qué acciones** tiene el usuario.
    - Si necesita **datos nuevos** (modelo, método de repositorio, cambio en Room) o reutiliza los existentes.

Si la feature requiere cambiar el esquema de Room, detente y avisa: ese cambio se hace con una migración aparte, no dentro de esta skill.

## Archivos a crear

Sustituye `Xxx` por el nombre de la feature y `feature/path` por su paquete.

```text
domain/
  src/main/kotlin/<base>/domain/usecase/VerboSustantivoUseCase.kt
  src/test/kotlin/<base>/domain/usecase/VerboSustantivoUseCaseTest.kt
presentation/
  src/main/kotlin/<base>/presentation/feature/path/
    XxxUiState.kt
    XxxIntent.kt
    XxxEffect.kt
    XxxViewModel.kt
    XxxRoute.kt
    XxxScreen.kt
  src/test/kotlin/<base>/presentation/feature/path/XxxViewModelTest.kt
  src/androidTest/kotlin/<base>/presentation/feature/path/XxxScreenTest.kt
```

Solo toca `:data` si hace falta un método nuevo de repositorio o DAO.

## Pasos

### 1. Domain

- Si hace falta, agrega el método a la interfaz del repositorio (`Flow<T>` para lecturas observables, `suspend` para escrituras).
- Crea un caso de uso por acción:

```kotlin
class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    operator fun invoke(): Flow<List<Note>> = repository.observeNotes()
}
```

- Las validaciones de negocio (título vacío, etc.) viven en el caso de uso, no en el ViewModel.

### 2. Data (solo si aplica)

- Agrega la consulta al DAO y la implementación en `NoteRepositoryImpl` (`internal`).
- Mapea con `toDomain()` / `toEntity()`. Ninguna entidad de Room sale de `:data`.

### 3. Presentation: contrato MVI

```kotlin
// XxxUiState.kt
data class NoteListUiState(
    val isLoading: Boolean = true,
    val notes: List<Note> = emptyList(),
    val errorMessage: String? = null,
)

// XxxIntent.kt
sealed interface NoteListIntent {
    data class NoteClicked(val id: Long) : NoteListIntent
    data class DeleteClicked(val id: Long) : NoteListIntent
    data object AddClicked : NoteListIntent
}

// XxxEffect.kt
sealed interface NoteListEffect {
    data class NavigateToDetail(val id: Long?) : NoteListEffect
    data class ShowMessage(@StringRes val messageRes: Int) : NoteListEffect
}
```

- `UiState` siempre con valores por defecto, para poder construirlo fácil en previews y pruebas.
- Los intents se nombran por lo que **hizo el usuario** (`AddClicked`), no por lo que debe pasar (`NavigateToAdd`).

### 4. Presentation: ViewModel

```kotlin
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val getNotes: GetNotesUseCase,
    private val deleteNote: DeleteNoteUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteListUiState())
    val state: StateFlow<NoteListUiState> = _state.asStateFlow()

    private val _effects = Channel<NoteListEffect>(Channel.BUFFERED)
    val effects: Flow<NoteListEffect> = _effects.receiveAsFlow()

    init {
        observeNotes()
    }

    fun onIntent(intent: NoteListIntent) {
        when (intent) {
            is NoteListIntent.NoteClicked -> send(NoteListEffect.NavigateToDetail(intent.id))
            is NoteListIntent.DeleteClicked -> delete(intent.id)
            NoteListIntent.AddClicked -> send(NoteListEffect.NavigateToDetail(null))
        }
    }

    private fun observeNotes() {
        getNotes()
            .onEach { notes -> _state.update { it.copy(isLoading = false, notes = notes) } }
            .launchIn(viewModelScope)
    }

    private fun delete(id: Long) {
        viewModelScope.launch { deleteNote(id) }
    }

    private fun send(effect: NoteListEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
```

- Solo depende de casos de uso.
- El `when` de `onIntent` debe ser exhaustivo, sin `else`.
- Los argumentos de navegación se leen de `SavedStateHandle`.

### 5. Presentation: Route y Screen

```kotlin
@Composable
fun NoteListRoute(
    onNavigateToDetail: (Long?) -> Unit,
    viewModel: NoteListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NoteListEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
                is NoteListEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))
            }
        }
    }

    NoteListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun NoteListScreen(
    state: NoteListUiState,
    onIntent: (NoteListIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Scaffold + contenido. Sin ViewModel, sin lógica de negocio.
}
```

- `Screen` no conoce el ViewModel ni la navegación.
- Cubre los estados: cargando, vacío, con contenido y error.
- Agrega `testTag` a los elementos interactivos, con constantes en un `object XxxTestTags`.
- Textos en `strings.xml`.
- Agrega al menos un `@Preview` por estado relevante.

### 6. Navegación

- Declara la ruta de la pantalla y regístrala en el grafo de navegación con `XxxRoute`.
- La navegación se pasa como lambdas desde el grafo; el `NavController` no entra a `Route` ni a `Screen`.

### 7. Pruebas

**Caso de uso** (`:domain`, con repositorio fake):

```kotlin
@Test
fun `given notes in repository when invoked then emits them`() = runTest {
    val repository = FakeNoteRepository(notes = listOf(note))

    GetNotesUseCase(repository)().test {
        assertEquals(listOf(note), awaitItem())
    }
}
```

**ViewModel** (`:presentation`, con `MainDispatcherRule` y Turbine):

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()

@Test
fun `given note clicked when intent received then emits navigate effect`() = runTest {
    val viewModel = createViewModel()

    viewModel.effects.test {
        viewModel.onIntent(NoteListIntent.NoteClicked(id = 1))
        assertEquals(NoteListEffect.NavigateToDetail(1), awaitItem())
    }
}
```

Cubre: estado inicial, cada intent, y el camino de error.

**UI** (`androidTest`, sobre el `Screen` sin estado):

```kotlin
@get:Rule
val composeRule = createComposeRule()

@Test
fun given_notes_when_displayed_then_shows_titles() {
    composeRule.setContent {
        AppTheme {
            NoteListScreen(
                state = NoteListUiState(isLoading = false, notes = listOf(note)),
                onIntent = {},
            )
        }
    }

    composeRule.onNodeWithText(note.title).assertIsDisplayed()
}
```

Los nombres con backticks y espacios no funcionan en `androidTest` con API bajas; ahí usa guiones bajos.

### 8. Verificación

Ejecuta y corrige hasta que pase:

```bash
./gradlew :domain:test :presentation:testDebugUnitTest
./gradlew :presentation:compileDebugAndroidTestKotlin
```

Corre también `:data:testDebugUnitTest` si tocaste `:data`. Las pruebas instrumentadas (`connectedDebugAndroidTest`) solo si hay emulador o dispositivo; si no, dilo en el resumen.

## Checklist final

- [ ] `:domain` sin imports de Android, Room, Compose ni Hilt
- [ ] ViewModel depende solo de casos de uso
- [ ] `Screen` sin estado, con previews y `testTag`
- [ ] Sin strings en el código
- [ ] Ruta registrada en el grafo de navegación
- [ ] Pruebas de caso de uso, ViewModel y UI escritas y pasando
- [ ] Sin dependencias ni módulos nuevos

## Al terminar

Resume en pocas líneas: archivos creados por módulo, pruebas ejecutadas y su resultado, y cualquier cosa pendiente (por ejemplo, pruebas instrumentadas sin ejecutar). Propón el mensaje de commit: `feat(presentation): add <feature> screen`.
