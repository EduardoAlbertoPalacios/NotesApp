---
name: write-tests
description: Escribe y ejecuta pruebas unitarias para una clase existente de la app de notas (caso de uso, ViewModel, repositorio, mapper o DAO) siguiendo las convenciones del proyecto. Úsala cuando se pida agregar, completar o arreglar pruebas de una clase o módulo.
---

# write-tests

Escribe las pruebas de una clase existente, las ejecuta y las deja pasando.

> Para pruebas de UI de un `XxxScreen` usa la skill de pruebas de Compose; esta cubre pruebas unitarias y de DAO.

## Antes de escribir

1. Lee `CLAUDE.md`. Si algo de esta skill lo contradice, manda `CLAUDE.md`.
2. Lee **completa** la clase bajo prueba y sus dependencias directas.
3. Busca pruebas existentes del mismo módulo e imita su estilo.
4. Busca fakes y utilidades ya creadas (`FakeNoteRepository`, `MainDispatcherRule`, fábricas de datos) antes de crear nuevas.
5. Lista los comportamientos a cubrir **antes** de escribir código (ver "Qué cubrir").

## Dónde va cada prueba

| Clase bajo prueba | Módulo | Carpeta | Herramientas |
|---|---|---|---|
| Caso de uso | `:domain` | `src/test` | JUnit, coroutines-test, Turbine, fake del repositorio |
| ViewModel | `:presentation` | `src/test` | `MainDispatcherRule`, Turbine, fakes o MockK |
| Repositorio | `:data` | `src/test` | DAO fake, coroutines-test, Turbine |
| Mapper | `:data` | `src/test` | JUnit puro |
| DAO de Room | `:data` | `src/androidTest` | `Room.inMemoryDatabaseBuilder` |

La prueba va en el **mismo paquete** que la clase, con el nombre `XxxTest`.

## Convenciones

- **Nombres** con backticks: `` `given X when Y then Z` ``. En `androidTest` usa guiones bajos: `given_x_when_y_then_z`.
- **Estructura** Given / When / Then separada por líneas en blanco, sin comentarios que la anuncien.
- **Una razón para fallar** por prueba. Varias aserciones están bien si verifican el mismo comportamiento.
- **Fakes antes que mocks.** Usa MockK solo cuando un fake sea más costoso o necesites verificar una interacción.
- **Corrutinas:** siempre `runTest`. Nada de `runBlocking`, `Thread.sleep` ni `delay` reales.
- **Flows:** siempre con Turbine (`test { awaitItem() }`), nunca `first()` o `toList()` sobre flows que no terminan.
- **Dispatchers:** inyecta `StandardTestDispatcher` o `UnconfinedTestDispatcher` en lugar de los reales.
- **Datos de prueba:** fábricas con valores por defecto (`fun note(id: Long = 1, title: String = "Título") = Note(...)`), no objetos repetidos en cada prueba.
- **Sujeto bajo prueba:** créalo con una función `createXxx()` o en `@Before`, no como propiedad inicializada al declararse.

## Qué cubrir

| Tipo | Casos mínimos |
|---|---|
| **Caso de uso** | Camino feliz; cada regla de validación; error del repositorio |
| **ViewModel** | Estado inicial; cada `Intent` (estado resultante y efectos); carga; error |
| **Repositorio** | Mapeo entidad ↔ dominio en lectura y escritura; propagación de errores; emisiones del `Flow` al cambiar los datos |
| **Mapper** | Ida, vuelta y campos nulos u opcionales |
| **DAO** | Insertar, leer, actualizar, borrar; orden de las consultas; emisión del `Flow` tras un cambio |

No pruebes código generado, getters triviales ni el framework.

## Plantillas

### Fake de repositorio

Vive en `src/test` del módulo que lo usa (o en un source set compartido si ya existe uno).

```kotlin
class FakeNoteRepository(
    initialNotes: List<Note> = emptyList(),
) : NoteRepository {

    private val notes = MutableStateFlow(initialNotes)
    var shouldFail = false

    override fun observeNotes(): Flow<List<Note>> = notes

    override suspend fun save(note: Note) {
        if (shouldFail) throw IOException("fake failure")
        notes.update { current -> current.filterNot { it.id == note.id } + note }
    }

    override suspend fun delete(id: Long) {
        notes.update { current -> current.filterNot { it.id == id } }
    }
}
```

### Caso de uso

```kotlin
class SaveNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val saveNote = SaveNoteUseCase(repository)

    @Test
    fun `given blank title when saving then returns validation error`() = runTest {
        val note = note(title = "  ")

        val result = saveNote(note)

        assertTrue(result.isFailure)
    }

    @Test
    fun `given valid note when saving then repository contains it`() = runTest {
        val note = note(title = "Compras")

        saveNote(note)

        repository.observeNotes().test {
            assertEquals(listOf(note), awaitItem())
        }
    }
}
```

### MainDispatcherRule

Si no existe, créala en `:presentation/src/test`:

```kotlin
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

### ViewModel

```kotlin
class NoteListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNoteRepository()

    private fun createViewModel() = NoteListViewModel(
        getNotes = GetNotesUseCase(repository),
        deleteNote = DeleteNoteUseCase(repository),
    )

    @Test
    fun `given notes in repository when created then state shows them`() = runTest {
        repository.save(note(id = 1))

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.notes.size)
        }
    }

    @Test
    fun `given note clicked when intent received then emits navigate effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.NoteClicked(id = 1))

            assertEquals(NoteListEffect.NavigateToDetail(1), awaitItem())
        }
    }
}
```

Usa casos de uso reales con repositorio fake cuando sean simples; así la prueba no se rompe al refactorizar el interior.

### Mapper

```kotlin
@Test
fun `given entity when mapped to domain and back then is unchanged`() {
    val entity = noteEntity()

    assertEquals(entity, entity.toDomain().toEntity())
}
```

### DAO (`androidTest`)

```kotlin
@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: NotesDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
        ).build()
        dao = database.noteDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun given_inserted_note_when_observing_then_emits_it() = runTest {
        dao.upsert(noteEntity(id = 1))

        dao.observeAll().test {
            assertEquals(1, awaitItem().size)
        }
    }
}
```

## Ejecutar

```bash
./gradlew :domain:test --tests "*SaveNoteUseCaseTest"
./gradlew :presentation:testDebugUnitTest --tests "*NoteListViewModelTest"
./gradlew :data:testDebugUnitTest --tests "*NoteMapperTest"
./gradlew :data:connectedDebugAndroidTest   # DAO; requiere emulador o dispositivo
```

Ejecuta primero solo la clase nueva y al final todas las pruebas del módulo.

## Cuando una prueba falla

1. Lee el mensaje completo antes de cambiar nada.
2. Decide si el error está en la **prueba** o en el **código de producción**.
3. Si es un bug de producción, **no ajustes la prueba para que pase**: detente, explica el bug y pregunta si se corrige.
4. Nunca borres, ignores (`@Ignore`) ni debilites una aserción para poner la prueba en verde.

## Restricciones

- ❌ No cambies código de producción salvo lo mínimo para hacerlo testeable (por ejemplo, inyectar un dispatcher), y avísalo.
- ❌ No agregues librerías de pruebas nuevas sin consultarlo.
- ❌ No uses `Dispatchers.IO` ni `Dispatchers.Main` reales en pruebas.

## Al terminar

Resume: clases de prueba creadas, casos cubiertos, comando ejecutado y resultado, y lo que quedó sin cubrir o sin ejecutar (por ejemplo, pruebas de DAO sin emulador). Propón el commit: `test(<módulo>): cover <Clase>`.
