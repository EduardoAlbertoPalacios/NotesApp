# Plan 010 — Compartir una nota

> **Estado:** implementado · **Fecha:** 2026-11-10 · **Spec:** [spec.md](spec.md) (aprobada) · **Diseño:** Figma `4:1656` y `40:1037`, barra superior del editor (`share-2`)

Plan técnico para implementar la spec 010. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

Un caso de uso de `:domain` arma lo que se comparte a partir de lo que se ve en el editor: el asunto, el texto plano con las tareas marcadas y las rutas de las imágenes que existen. El ViewModel lo envía como efecto, y la `Route` construye el `Intent` del sistema, porque es la única capa que tiene un `Context`.

Las imágenes viven en `filesDir/note_images`, que otras apps no pueden leer. Se comparten con un `FileProvider` declarado en `:app` que solo expone esa carpeta, con permiso temporal de lectura [RF-18].

No hay cambios en el esquema de Room ni en `:common`.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | No hay librerías nuevas en el proyecto. `FileProvider` es de `androidx.core`, que ya está en el catálogo y en `:app`. `:presentation` lo recibe hoy de forma transitiva, y se declara explícitamente (D3, aprobado). |
| 2. La spec manda | Cada parte cita sus RF. El mismo commit actualiza las specs 004 y 006 (sección 4.5). |
| 3. Lógica fuera de la UI | Qué se comparte y con qué formato se decide en `:domain`. La `Route` solo traduce el resultado a un `Intent`. |
| 4. Sin pruebas no hay commit | Pruebas del caso de uso, del ViewModel y de UI (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | Compartir no escribe nada [RF-7, RF-8]. |
| 6. Idioma | Identificadores y commits en inglés; comentarios y este plan en español; textos nuevos en `strings.xml`. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:domain` | `NoteShare`, `BuildNoteShareUseCase` e `ImageStorage.exists()`. | RF-9 a RF-17, RF-19 |
| `:data` | `ImageStorageImpl.exists()`. | RF-19 |
| `:presentation` | Botón, estado, intent, efecto, `Intent` del sistema, avisos y textos. | RF-1 a RF-8, RF-18, RF-20 a RF-24 |
| `:app` | `FileProvider` en el manifiesto y `res/xml/file_paths.xml`. | RF-18 |
| Documentación | Specs 004 y 006, y `.memory/MEMORY.md`. | — |

## 4. Diseño por capa

### 4.1 `:domain` (paquete `notes`)

- `data class NoteShare(subject: String?, text: String?, imagePaths: List<String>, missingImageCount: Int)`.
- `BuildNoteShareUseCase(title, content, tasks, images)`:
  - **Título y contenido:** se recortan los espacios de los extremos; si quedan vacíos, cuentan como ausentes. El texto es título, línea en blanco y contenido, o solo la parte que exista [RF-9, RF-10]. Sin ninguno de los dos, `text` es `null`.
  - **Tareas:** cada línea marcada como tarea en `tasks` empieza por «☐ » o «☑ » según su estado [RF-11, RF-12].
  - **Formato:** el contenido entra como texto plano; los tramos de formato se ignoran [RF-13].
  - **Asunto:** el título recortado, o `null` [RF-14].
  - **Imágenes:** recibe las que se ven en el editor, así que ya incluye las pendientes y excluye las quitadas [RF-15 a RF-17]. Las que no existen, según `ImageStorage.exists()`, se omiten y se cuentan en `missingImageCount` [RF-19].
- `ImageStorage.exists(path): Boolean`.

### 4.2 `:data`

- **`ImageStorageImpl.exists()`:** comprueba que el archivo exista y esté dentro de la carpeta de imágenes de la app, con la misma comprobación de carpeta que `delete`.

### 4.3 `:presentation`

| Clase | Cambios |
|---|---|
| `NoteEditorUiState` | `canShare: Boolean`, derivado: título o contenido con algo más que espacios, o alguna imagen [RF-2] |
| `NoteEditorIntent` | `ShareClicked` |
| `NoteEditorEffect` | `ShareNote(subject, text, imagePaths)` |
| `NoteEditorViewModel` | Recibe `BuildNoteShareUseCase`. Con `ShareClicked` lo llama con el estado actual (título, contenido, tareas e imágenes visibles), sin guardar nada [RF-6 a RF-8]. Si faltan imágenes, emite el aviso de RF-20 además del efecto |
| `NoteEditorScreen` | `EditorIconButton` con `ic_share` entre Fijar y Más opciones, solo con `showNoteActions` [RF-1, RF-3, RF-4]. Deshabilitado sin `canShare`, con la etiqueta «Compartir nota» [RF-2, RF-23, RF-24] |
| `NoteEditorRoute` | `ShareNote` arma el `Intent` y abre `Intent.createChooser` [RF-5] |

**El `Intent` en `NoteEditorRoute`**

- **Tipo de envío:** sin imágenes, `ACTION_SEND` con `text/plain`. Con una imagen, `ACTION_SEND` con `EXTRA_STREAM`. Con varias, `ACTION_SEND_MULTIPLE`. Con imágenes el tipo MIME es `image/*` aunque haya texto: con `*/*`, WhatsApp descarta la imagen (visto en la revisión en un Pixel 10).
- **Texto y asunto:** `EXTRA_TEXT` lleva el texto y `EXTRA_SUBJECT` el asunto [RF-14].
- **Imágenes:**
  - Cada ruta se convierte con `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)`.
  - Se añade `FLAG_GRANT_READ_URI_PERMISSION`, y las URI van también en `ClipData` para que el permiso llegue a través del selector [RF-18].
- **Sin apps:** si `startActivity` lanza `ActivityNotFoundException`, se muestra el aviso de RF-21. Es la única excepción que se atrapa.
- **Al cerrar el selector:** no hay nada que hacer, porque compartir no cambió el estado [RF-22].
- **Ícono:** `ic_share` se descarga de Figma (`share-2`, nodo `4:1928`).
- **Textos nuevos:** `note_editor_share` («Compartir nota»), `note_editor_share_missing_images` y `note_editor_share_no_apps`.

### 4.4 `:app`

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />
</provider>
```

`file_paths.xml` declara solo `<files-path name="note_images" path="note_images/" />`. Ninguna otra carpeta de la app queda expuesta, como pide el requisito de privacidad.

### 4.5 Documentación

- **Spec 006:** la línea de «Fuera de alcance» sobre el botón Compartir remite a la spec 010.
- **Spec 004:** la línea sobre compartir las tareas con sus casillas aclara que, como texto con «☐» y «☑», lo cubre la spec 010.
- **`.memory/MEMORY.md`:** estado y decisiones. También se corrige que la spec 009 ya está en `main`.

## 5. Decisiones técnicas

- **D1. El texto se arma en `:domain`.** Es una regla de producto (formato, tareas, qué cuenta como vacío) y se prueba con pruebas JVM sin Android.
- **D2. `FileProvider` en `:app`.** La autoridad depende del `applicationId`, que solo conoce `:app`. La `Route` la deriva de `context.packageName`, que coincide con él.
- **D3. Declarar `androidx.core` en `:presentation`.** La `Route` usa `FileProvider` directamente. Hoy compila porque `activity-compose` lo trae de forma transitiva, pero depender de eso es frágil. Es la misma entrada del catálogo que ya usa `:app`. **Aprobado** el 2026-11-10, según la restricción de dependencias de `CLAUDE.md`.
- **D4. Compartir lo que se ve.** El caso de uso recibe el estado del editor y no lee la base, lo que cumple RF-6 en ambos modos de guardado.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:domain` | `BuildNoteShareUseCaseTest` | RF-9 a RF-17, RF-19: solo título, solo contenido, ambos, espacios, tareas hechas y pendientes, tarea vacía, formato ignorado, imágenes que faltan |
| `:data` (instrumentada) | `ImageStorageImplTest` | `exists()` dentro y fuera de la carpeta |
| `:presentation` (unitarias) | `NoteEditorViewModelTest` | `canShare`; efecto con el estado visible en ambos modos; no guarda ni cambia la fecha; aviso de imágenes que faltan |
| `:presentation` (UI) | `NoteEditorScreenTest` o una clase nueva parametrizada | RF-1 a RF-4, RF-23, RF-24, en ambos temas |
| Manual | Emulador | Compartir a Gmail o Mensajes y comprobar el texto, el asunto y las imágenes; selector cerrado sin elegir; modo manual con cambios pendientes |

El `Intent` de la `Route` no tiene prueba automática, porque abre otra app. Se comprueba en la demo.

## 7. Orden de implementación

1. Crear la rama `feat/share-note`.
2. `:domain` y `:data`: caso de uso y `exists()`, con pruebas.
3. `:app`: `FileProvider` y `file_paths.xml`.
4. `:presentation`: estado, intent, efecto, botón, `Intent` y textos, con pruebas.
5. Actualizar las specs 004 y 006.
6. `./gradlew test`, pruebas instrumentadas, demo manual en ambos temas, comparación con Figma y revisión con `architecture-reviewer`.
7. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1 a RF-4, RF-23, RF-24 | Botón en `NoteEditorTopBar`, `canShare` |
| RF-5, RF-21, RF-22 | `NoteEditorRoute` |
| RF-6 a RF-8 | `NoteEditorViewModel` con el estado visible, sin guardar |
| RF-9 a RF-17, RF-19 | `BuildNoteShareUseCase` |
| RF-18 | `FileProvider`, `file_paths.xml`, flags del `Intent` |
| RF-20 | `NoteEditorViewModel` |
| Requisitos no funcionales | Secciones 4.3 y 4.4, `strings.xml` |
| Casos límite | Sección 6 |

## 9. Riesgos

- **Permisos a través del selector:** sin `ClipData`, algunas apps no reciben el permiso de lectura de las imágenes. Se comprueba en la demo con al menos dos apps.
- **Apps instaladas en el emulador:** puede que solo haya Mensajes o Gmail sin cuenta. Si ninguna acepta imágenes, la demo solo podrá confirmar que el selector las ofrece.
- **Barra superior llena:** con el guardado automático apagado conviven Guardar, Fijar, Compartir y Más opciones. Se comprueba que caben a 360 dp de ancho.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
