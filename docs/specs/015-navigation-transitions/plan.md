# Plan 015 — Animación entre pantallas

> **Estado:** implementado · **Fecha:** 2026-11-20 · **Spec:** [spec.md](spec.md) (aprobada) · **Diseño:** sin frame; lo define la spec

Plan técnico para implementar la spec 015. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

- **Transiciones del `NavHost`.** Se definen una vez en `NotesNavHost` y valen para todas las pantallas [RF-1 a RF-5]:
  - al avanzar, la pantalla nueva entra deslizándose desde la derecha y la anterior se desplaza un tercio a la izquierda;
  - al volver, al revés;
  - duran 300 ms y sin fundido.
  - Las vueltas automáticas ya usan `navigateUp`/`popBackStack`, así que reciben la animación de volver sin más cambios.
- **Escala del sistema.** Compose ya aplica la escala de duración de animación del sistema (y la escala 0 de «Quitar animaciones») a todas sus animaciones, así que RF-6 a RF-8 se cumplen por construcción. Las pruebas lo comprueban.
- **Atrás predictivo.**
  - Navigation Compose 2.10 ya anima el gesto con las transiciones de volver [RF-9 a RF-12].
  - Falta activarlo en el manifiesto para Android 14 y 15 (en 16 o superior viene activado con `targetSdk` 37).
  - Hay que dejar de interceptar Atrás en las pantallas que no lo necesitan (D2).
- **Toques durante la animación.**
  - Un modificador bloquea los toques en la pantalla que sale [RF-18].
  - Las navegaciones hacia delante solo se aceptan desde la pantalla activa [RF-19].
  - Atrás no se bloquea [RF-20, RF-21].

**Sin dependencias nuevas, sin cambios de esquema.** `androidx.navigation:navigation-compose` 2.10.2 y Compose ya traen todo lo necesario.

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Navigation Compose y animaciones de Compose, que ya están. |
| 2. La spec manda | Cada parte cita sus RF. D3 propone ajustar RF-14 de la spec antes de implementar. |
| 3. Lógica fuera de la UI | Cuándo hay que interceptar Atrás lo decide el ViewModel (campo del `UiState`); la UI solo lo aplica. |
| 4. Sin pruebas no hay commit | Pruebas de UI de las transiciones, del bloqueo de toques y del gesto, y unitarias de los ViewModels (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | El editor sigue guardando antes de salir: solo se muestra la vista previa cuando no queda nada por guardar (D2). |
| 6. Idioma | Identificadores en inglés; sin textos nuevos visibles. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:presentation` | `navigation/NavTransitions.kt` (nuevo): transiciones, bloqueo de toques y guarda de navegación. `NotesNavHost`: los usa. Rutas de editor, Exportar, Papelera y Ayuda: Atrás condicionado o sin `BackHandler` (D2). `NoteEditorViewModel` y `ExportNotesViewModel`: nuevo campo `interceptsBack`. | RF-1 a RF-23 |
| `:app` | `AndroidManifest.xml`: `android:enableOnBackInvokedCallback="true"`. | RF-9 a RF-12, RF-17 |
| `:domain`, `:data`, `:common` | Ninguno. | — |
| Documentación | Spec 015 (RF-14, D3) y `.memory/MEMORY.md`. | — |

## 4. Diseño por parte

### 4.1 Transiciones [RF-1 a RF-8, RF-22, RF-23]

`navigation/NavTransitions.kt`, `internal`:

| Parámetro de `NavHost` | Transición |
|---|---|
| `enterTransition` | `slideInHorizontally { fullWidth -> fullWidth }` |
| `exitTransition` | `slideOutHorizontally { fullWidth -> -fullWidth / 3 }` |
| `popEnterTransition` | `slideInHorizontally { fullWidth -> -fullWidth / 3 }` |
| `popExitTransition` | `slideOutHorizontally { fullWidth -> fullWidth }` |

- **Animación:** todas con `tween(NAV_TRANSITION_MILLIS = 300, easing = FastOutSlowInEasing)` y sin `fadeIn`/`fadeOut`. `NavHost` mantiene encima la pantalla que entra al avanzar y la que sale al volver.
- **Escala del sistema [RF-6 a RF-8]:** no se programa nada. Compose lee `ANIMATOR_DURATION_SCALE` (su `MotionDurationScale`) y escala o anula las animaciones de tiempo.
- **Arranque [RF-22]:** el destino inicial no tiene transición de entrada al crear el `NavHost`.
- **Hojas, diálogos y avisos [RF-23]:** no pasan por `NavHost`, así que no cambian.
- **Fondos:** cada pantalla debe pintar su propio fondo opaco para que no se vea la de debajo; ya lo hacen todas. Se revisa en la demo en los dos temas (RNF Temas).

### 4.2 Atrás predictivo [RF-9 a RF-17]

- **Manifiesto:** `android:enableOnBackInvokedCallback="true"` en `<application>`. En Android 14 y 15 activa el gesto predictivo; en Android 13 o anterior no tiene efecto, y Atrás lanza la animación completa [RF-17]. `MainActivity` no sobrescribe `onBackPressed`.
- **Quién atiende Atrás.** `NavHost` solo anima el gesto si nadie más lo intercepta. Hoy cuatro rutas tienen un `BackHandler` siempre activo, y eso desactiva la vista previa. Cambia así (D2):

| Pantalla | Hoy | Con este plan |
|---|---|---|
| Ayuda | `BackHandler` → `NavigateBack` | Sin `BackHandler`: Atrás lo atiende `NavHost`. La flecha sigue enviando `BackClicked`. |
| Papelera | `BackHandler` → `NavigateBack` | Igual que Ayuda. Las hojas y diálogos abiertos siguen cerrándose con Atrás antes de salir, porque tienen su propio manejador. |
| Exportar | `BackHandler` → `leave()` | `BackHandler(enabled = state.interceptsBack)`. `interceptsBack = isExporting`: solo hay que cancelar y limpiar mientras se genera la exportación. |
| Editor | `BackHandler` → `leave()` | `BackHandler(enabled = state.interceptsBack)` (ver abajo). |

- **Cuándo intercepta el editor.** `interceptsBack` es verdadero si:
  - **modo manual con cambios sin guardar:** hace falta el diálogo [RF-14, RF-15];
  - **hay cambios pendientes de autoguardado** (escritos hace menos de 500 ms) **o un guardado en curso:** hay que guardar antes de salir;
  - **se está copiando una imagen;**
  - **la nota existente quedó vacía:** al salir se elimina (comportamiento actual).

  En esos casos Atrás pasa por `leave()` como hoy, sin vista previa. Si el guardado falla, el editor se queda y muestra su aviso [RF-16]. En el resto de casos no queda nada por hacer, así que `NavHost` cierra el editor con el gesto y la vista previa.
- **Cancelar el gesto [RF-12, RF-13]:** `NavHost` devuelve la transición al inicio sin sacar la pantalla de la pila. El `ViewModel`, el texto, la selección y el desplazamiento siguen intactos.

### 4.3 Toques durante la animación [RF-18 a RF-21]

- **`Modifier.blockTouchesWhileExiting(scope: AnimatedVisibilityScope)`:**
  - si `scope.transition.targetState == EnterExitState.PostExit`, consume todos los eventos de puntero;
  - se aplica a cada destino en `NotesNavHost` [RF-18].
- **Guarda de navegación `NavBackStackEntry.ifResumed { … }`:** las lambdas que avanzan (abrir nota, nota nueva, Ajustes, Exportar, Papelera, Ayuda) solo navegan si la entrada actual está en `RESUMED`. Durante una transición, Navigation limita las entradas a `STARTED`, así que el segundo toque no abre otra pantalla [RF-19].
- **Atrás:** `NavHost` atiende Atrás también durante una transición: vuelve a la pantalla de partida [RF-20] y retrocede una entrada por pulsación [RF-21]. En Mis notas, `NavHost` desactiva su manejador y el sistema cierra la app (CL-2).

### 4.4 Imágenes huérfanas (D4, descartada)

Al cerrar con el gesto, el editor no pasa por `finishSession()`, que llama a `CleanUpImageFilesUseCase` (red de seguridad que borra copias de imágenes que ninguna nota usa). No hace falta compensarlo:
- El gesto solo cierra el editor sin interceptar cuando no queda nada pendiente (D2), así que no deja huérfanas nuevas: en modo automático las imágenes se asocian al instante, y en modo manual una imagen sin guardar es un cambio pendiente.
- La limpieza sigue en `finishSession()`, que se ejecuta en cada salida interceptada. Las huérfanas antiguas (por ejemplo, de borradores perdidos al cerrar desde recientes) se limpian en la próxima.

### 4.5 Documentación

- **Spec 015:** RF-14 pasa a decir «MIENTRAS volver desde la pantalla actual requiera una confirmación o termine un trabajo en curso (guardar la nota, generar la exportación)…» (D3).
- **`.memory/MEMORY.md`:** estado y decisiones.

## 5. Decisiones técnicas

- **D1. Transiciones globales en el `NavHost`.** **Aprobado** el 2026-11-20. Todas las pantallas se animan igual (spec, decisión a), así que basta con definirlas una vez en `NavHost` y no en cada `composable`. **Alternativa:** por destino; solo hace falta si un día cambian por pantalla.
- **D2. Atrás interceptado solo cuando hace falta (`interceptsBack` en el `UiState`).** **Aprobado** el 2026-11-20. Es la única forma de que `NavHost` muestre la vista previa y, a la vez, el editor siga guardando antes de salir sin perder datos (principio 5). Lo decide el ViewModel, que es quien sabe si queda algo pendiente. **Alternativas:**
  - Guardar al confirmar el gesto, ya fuera del editor. `NavHost` saca la pantalla al instante y un fallo de guardado no podría devolver al usuario a la nota (RF-16), así que podría perder lo último escrito.
  - Mantener el `BackHandler` siempre en el editor: el editor, la pantalla más usada, nunca tendría vista previa (RF-9).
- **D3. Ajustar RF-14 de la spec.** **Aprobado** el 2026-11-20. Hoy RF-14 solo exime de la vista previa a las confirmaciones. D2 también la omite durante el medio segundo tras escribir, mientras se guarda, mientras se copia una imagen o se genera una exportación. Propongo ampliar RF-14 con «o termine un trabajo en curso» antes de implementar, para que la spec no prometa lo que el código no hace.
- **D4. ~~Limpieza de imágenes huérfanas también al arrancar.~~ Descartada** el 2026-11-20, tras aprobarse. Al arrancar se puede restaurar un editor en modo manual cuyo borrador apunta a imágenes copiadas pero aún no guardadas: la limpieza las borraría (principio 5). Y no hace falta (sección 4.4). **Alternativa:** que el editor intercepte Atrás siempre que haya imágenes. Quitaría la vista previa en todas las notas con imágenes.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:presentation` (UI) | `NavTransitionsTest`: un `NavHost` de prueba con dos destinos simples, transiciones de `NavTransitions` y `mainClock.autoAdvance = false` | A mitad de avanzar (150 ms): la nueva entre 0 y el ancho y la anterior entre 0 y −⅓. A 300 ms: la nueva en 0 y la anterior fuera. Lo mismo al volver [RF-1 a RF-6]. |
| `:presentation` (UI) | `NavTransitionsTest` | Toques en la pantalla que sale ignorados [RF-18]. Dos toques rápidos navegan una sola vez [RF-19]. Atrás durante la animación de avanzar vuelve [RF-20]. |
| `:presentation` (UI) | `NavTransitionsTest`, gesto simulado con `OnBackPressedDispatcher.dispatchOnBackStarted/Progressed/Cancelled` | Con la mitad del gesto, la pantalla está desplazada; al cancelar vuelve a 0 y sigue en la pila [RF-9, RF-10, RF-12]. |
| `:presentation` (unitarias) | `NoteEditorViewModelTest` | `interceptsBack` en cada caso de 4.2 (manual con cambios, cambios pendientes, guardado en curso, imagen copiándose, nota vaciada) y falso sin pendientes [RF-14 a RF-16]. |
| `:presentation` (unitarias) | `ExportNotesViewModelTest` | `interceptsBack` verdadero solo mientras se exporta. |
| `:presentation` (UI) | Pruebas existentes de rutas y pantallas | Siguen pasando sin el `BackHandler` de Ayuda y Papelera. |
| Manual | Emuladores API 36 y API 25 | Los puntos de la demo de la spec: escala 10× y «Quitar animaciones», gesto completado y cancelado, giro durante la animación, ambos temas. |

La escala de duración no se puede cambiar desde una prueba sin permisos de sistema: RF-7 y RF-8 se comprueban en la demo.

## 7. Orden de implementación

1. Crear la rama `feat/navigation-transitions` desde `main`.
2. Ajustar RF-14 de la spec (D3).
3. `NavTransitions.kt` y su uso en `NotesNavHost`: transiciones, bloqueo de toques y guarda de navegación.
4. Manifiesto: activar el gesto predictivo.
5. `interceptsBack` en el editor y en Exportar, y los `BackHandler` condicionados o eliminados.
6. Pruebas (`test-writer`).
7. Ejecutar `./gradlew test` y las pruebas instrumentadas en el emulador, y pasar `architecture-reviewer`.
8. Demo manual en API 36 y API 25.
9. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1 a RF-5 | `NavTransitions`, parámetros de `NavHost` |
| RF-6 a RF-8 | `tween(300)` más `MotionDurationScale` de Compose |
| RF-9 a RF-13 | Manifiesto, `NavHost` (gesto predictivo), rutas sin `BackHandler` fijo |
| RF-14 a RF-16 | `interceptsBack` del editor y de Exportar, `leave()` sin cambios |
| RF-17 | Comportamiento de `NavHost` en versiones sin gesto predictivo |
| RF-18 | `blockTouchesWhileExiting` |
| RF-19 | Guarda `ifResumed` en las lambdas que avanzan |
| RF-20, RF-21 | `NavHost` (Atrás durante la transición) |
| RF-22, RF-23 | Destino inicial sin transición; hojas y diálogos fuera de `NavHost` |

## 9. Riesgos

- **Editor sin vista previa justo después de escribir:** durante el medio segundo del autoguardado, el gesto lanza la animación completa al soltar, sin vista previa. Es el precio de no perder lo escrito (D2 y D3).
- **Un fotograma de margen (revisión de arquitectura):** `interceptsBack` llega al `BackHandler` en la siguiente recomposición (unos 16 ms). Si una tecla y Atrás caen en el mismo fotograma, `NavHost` cerraría el editor sin guardar esa última tecla. Es muy improbable; se acepta y queda anotado porque D2 no cubre el principio 5 al 100 %.
- **Clics de accesibilidad y teclado en la pantalla que sale:** `blockTouchesWhileExiting` bloquea los toques (RF-18), no las acciones de TalkBack ni de teclado o D-pad. Las navegaciones hacia delante siguen protegidas por `ifResumed` (RF-19).
- **Doble salida (revisión de arquitectura):** un segundo Atrás durante la salida del editor o de Exportar emitía otro `NavigateBack` y sacaba dos pantallas. `leave()`, `leaveSaving()`, `leaveDiscarding()` y `moveToTrash()` ignoran una salida ya en curso (RF-21).
- **Pantallas pesadas durante la animación:** Mis notas con muchas imágenes puede dar tirones al deslizarse. Se mide en la demo y, si hace falta, se revisa en otra spec.
- **Pruebas existentes que pulsan Atrás** (Ayuda, Papelera): pasan a depender de `NavHost`. Las de pantalla sin estado no cambian; las de ruta pueden necesitar un `NavHost` de prueba.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
