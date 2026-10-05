---
name: room-migration
description: Aplica un cambio de esquema en la base de datos Room de la app de notas (agregar, renombrar o quitar columnas, tablas o índices) con su Migration, esquema exportado y prueba de migración. Úsala siempre que se modifique una @Entity, la versión de la base o se pida una migración.
---

# room-migration

Lleva un cambio de esquema de Room de principio a fin **sin perder los datos del usuario**.

> Todo cambio en una `@Entity` que altere tablas, columnas o índices requiere subir la versión y una migración. Sin excepciones.

## Antes de tocar nada

1. Lee `CLAUDE.md`. Si algo de esta skill lo contradice, manda `CLAUDE.md`.
2. Lee `NotesDatabase`, las `@Entity` afectadas, sus DAOs y `DatabaseModule`.
3. Anota la **versión actual** y revisa el último JSON en `data/schemas/<paquete>.NotesDatabase/`.
4. Revisa las migraciones existentes e imita su estilo y ubicación.
5. Describe el cambio en una frase (ej. "agregar `is_pinned` a `notes`, versión 2 → 3") y confirma que es lo pedido.

**Detente y pregunta** si el cambio implica borrar datos (quitar una columna o tabla con información del usuario) o si no hay forma de dar un valor a las filas existentes.

## Requisitos de configuración

Verifica que existan; si falta alguno, agrégalo y avísalo.

| Requisito | Dónde |
|---|---|
| `exportSchema = true` | `@Database` en `NotesDatabase` |
| Carpeta de esquemas (`schemas/`) | Plugin de Room o argumento `room.schemaLocation` en `data/build.gradle.kts` |
| Esquemas visibles en pruebas | `androidTest` de `:data` con `schemas/` como assets |
| `androidx.room:room-testing` | `androidTestImplementation` en `:data`, vía version catalog |
| JSON de esquemas en git | `data/schemas/` **no** debe estar en `.gitignore` |

Si falta el JSON de la versión actual, genéralo compilando **antes** de modificar las entidades.

## Elegir el tipo de migración

| Cambio | Tipo | Notas |
|---|---|---|
| Agregar tabla | Automática | |
| Agregar columna | Automática | Requiere `@ColumnInfo(defaultValue = ...)` si es no nula |
| Agregar índice | Automática | |
| Renombrar columna o tabla | Automática + `AutoMigrationSpec` | `@RenameColumn`, `@RenameTable` |
| Borrar columna o tabla | Automática + `AutoMigrationSpec` | `@DeleteColumn`, `@DeleteTable`; confirma antes |
| Cambiar tipo de columna | **Manual** | Recrear la tabla |
| Transformar o mover datos | **Manual** | SQL a mano |
| Partir o unir tablas | **Manual** | |
| Cambiar clave primaria o nulabilidad | **Manual** | Recrear la tabla |

Usa la automática cuando alcance; la manual solo cuando haga falta.

## Pasos

### 1. Modificar la entidad

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    @ColumnInfo(name = "is_pinned", defaultValue = "0") val isPinned: Boolean = false,
)
```

El `defaultValue` de `@ColumnInfo` y el `DEFAULT` del SQL de la migración deben ser **idénticos**, o Room falla al validar el esquema.

### 2. Subir la versión y declarar la migración

**Automática:**

```kotlin
@Database(
    entities = [NoteEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class NotesDatabase : RoomDatabase()
```

**Automática con spec** (renombrar o borrar):

```kotlin
@RenameColumn(tableName = "notes", fromColumnName = "body", toColumnName = "content")
class Migration3To4Spec : AutoMigrationSpec

// en @Database:
AutoMigration(from = 3, to = 4, spec = Migration3To4Spec::class)
```

**Manual**, en `data/.../local/migration/Migrations.kt`:

```kotlin
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
    }
}

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_2_3)
```

Y regístrala en `DatabaseModule`:

```kotlin
Room.databaseBuilder(context, NotesDatabase::class.java, "notes.db")
    .addMigrations(*ALL_MIGRATIONS)
    .build()
```

### 3. Recrear una tabla (migración manual)

SQLite no permite cambiar tipo, nulabilidad ni clave primaria con `ALTER TABLE`. Patrón:

```kotlin
override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE notes_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            title TEXT NOT NULL,
            content TEXT NOT NULL,
            created_at INTEGER NOT NULL
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO notes_new (id, title, content, created_at)
        SELECT id, title, content, CAST(created_at AS INTEGER) FROM notes
        """.trimIndent()
    )
    db.execSQL("DROP TABLE notes")
    db.execSQL("ALTER TABLE notes_new RENAME TO notes")
    // Recrea aquí los índices de la tabla.
}
```

Copia el `CREATE TABLE` exacto del JSON de esquema de la versión nueva (campo `createSql`) para que coincida con lo que Room espera.

### 4. Generar el esquema

```bash
./gradlew :data:kspDebugKotlin   # o :data:compileDebugKotlin según el procesador
```

Debe aparecer `data/schemas/<paquete>.NotesDatabase/<versión>.json`. Agrégalo a git. **Nunca edites ni borres** los JSON de versiones anteriores.

### 5. Propagar el cambio

- Actualiza los mappers `toDomain()` / `toEntity()`.
- Actualiza el modelo de dominio y las consultas del DAO si aplica.
- Ajusta fakes y fábricas de datos de las pruebas.

### 6. Prueba de migración

En `data/src/androidTest/.../local/migration/MigrationTest.kt`:

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NotesDatabase::class.java,
    )

    @Test
    fun migrate_2_to_3_keeps_notes_and_sets_default() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL("INSERT INTO notes (id, title, content) VALUES (1, 'Compras', 'Leche')")
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.query("SELECT title, is_pinned FROM notes WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Compras", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
    }

    @Test
    fun migrate_all_versions_to_latest() {
        helper.createDatabase(TEST_DB, 1).close()

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NotesDatabase::class.java,
            TEST_DB,
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase
            close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
```

Cada migración nueva lleva una prueba que:

- inserta datos **en la versión anterior** con SQL crudo (no con el DAO, que ya es de la versión nueva),
- ejecuta la migración con validación de esquema,
- verifica que los datos previos siguen ahí y que lo nuevo tiene el valor esperado.

Para migraciones automáticas, `runMigrationsAndValidate` se llama sin pasar la migración.

### 7. Verificar

```bash
./gradlew :data:testDebugUnitTest
./gradlew :data:connectedDebugAndroidTest --tests "*MigrationTest"   # requiere emulador o dispositivo
./gradlew assembleDebug
```

Si no hay emulador, la migración **no está verificada**: dilo explícitamente en el resumen.

## Reglas

- ❌ Nunca uses `fallbackToDestructiveMigration` para "arreglar" un fallo de migración.
- ❌ Nunca modifiques una migración que ya salió en una versión publicada; crea una nueva.
- ❌ Nunca saltes versiones ni reutilices un número de versión.
- ❌ Nunca edites a mano los JSON de `schemas/`.
- ✅ Una migración por cambio de versión, con un solo propósito.
- ✅ SQL con nombres de tabla y columna explícitos, sin `SELECT *` al copiar datos.

## Errores frecuentes

| Síntoma | Causa habitual |
|---|---|
| `Migration didn't properly handle: notes` | El SQL no coincide con la entidad: tipo, `NOT NULL`, `DEFAULT` o índice distinto |
| `A migration from X to Y was required but not found` | Migración no registrada en `addMigrations` o `autoMigrations` |
| `Cannot find the schema file` | Falta el JSON de la versión anterior o `schemas/` no está en los assets de `androidTest` |
| `Schema export directory was not provided` | Falta configurar la ubicación de esquemas en Gradle |

## Al terminar

Resume: cambio aplicado, versión anterior → nueva, tipo de migración, archivos tocados, JSON generado, pruebas ejecutadas y su resultado. Propón el commit: `feat(data): migrate database to v<N> (<cambio>)`.
