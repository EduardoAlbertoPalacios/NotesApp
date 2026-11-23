# Tareas 015 — Animación entre pantallas

> **Estado:** implementadas · **Fecha:** 2026-11-20 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

**Paralelismo posible:** una vez terminada T02:
- T03 (transiciones) y T05 (editor y Exportar) tocan archivos distintos y pueden repartirse entre agentes;
- T04 va después de T03, porque ambas tocan `NotesNavHost`;
- las pruebas de T07 se reparten igual: navegación, por un lado, y ViewModels, por otro.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/navigation-transitions` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/015-navigation-transitions/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/navigation-transitions` y su base es el último commit de `origin/main`.

- [x] **T02. Ajustar RF-14 de la spec (D3).** «MIENTRAS volver desde la pantalla actual requiera una confirmación o termine un trabajo en curso (guardar la nota, copiar una imagen, generar la exportación)…». Revisar que RF-15, RF-16, CL-4 y los criterios de finalización sigan siendo coherentes.
  - **Cubre:** principio 2
  - **Hecho cuando:** la spec describe lo que hará el código según D2.

## Fase 1 — Navegación (`:presentation`, `:app`)

- [x] **T03. Transiciones (D1).**
  - Crear `navigation/NavTransitions.kt` (`internal`):
    - las cuatro transiciones (entrar, salir, volver a entrar, volver a salir) con `tween(300, FastOutSlowInEasing)` y sin fundido;
    - `Modifier.blockTouchesWhileExiting(scope)`;
    - la guarda `NavBackStackEntry.ifResumed { … }`.
  - Usarlas en `NotesNavHost`:
    - transiciones en el `NavHost`;
    - bloqueo de toques en cada destino;
    - guarda en todas las lambdas que avanzan.
  - **Cubre:** RF-1 a RF-8, RF-18 a RF-23
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T04. Atrás predictivo.**
  - Añadir `android:enableOnBackInvokedCallback="true"` al `<application>` del manifiesto de `:app`.
  - Quitar el `BackHandler` de `HelpRoute` y de `TrashRoute`. La flecha sigue enviando `BackClicked`.
  - **Cubre:** RF-9 a RF-13, RF-17
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

- [x] **T05. Atrás interceptado solo cuando hace falta (D2).**
  - **`NoteEditorUiState.interceptsBack`,** calculado por el ViewModel. Es verdadero si se da cualquiera de estos casos:
    - modo manual con cambios sin guardar;
    - cambios pendientes de autoguardado;
    - guardado en curso;
    - imagen copiándose;
    - nota existente vaciada.
  - **`ExportNotesUiState.interceptsBack`:** igual a `isExporting`.
  - **Rutas:** `BackHandler(enabled = state.interceptsBack)` en `NoteEditorRoute` y en `ExportNotesRoute`.
  - **Cubre:** RF-14 a RF-16
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las pruebas unitarias existentes del editor y de Exportar siguen pasando.

- [x] **T06. ~~Limpieza de imágenes al arrancar (D4).~~ Descartada:** D4 se retiró del plan (podía borrar imágenes de borradores restaurados). Se deshizo el cambio.
  - **Cubre:** —

## Fase 2 — Pruebas

- [x] **T07. Pruebas** (`test-writer`).
  - **`NavTransitionsTest`** (UI, un `NavHost` de prueba con reloj controlado):
    - posiciones a 150 ms y a 300 ms al avanzar y al volver;
    - toques ignorados en la pantalla que sale;
    - dos toques rápidos navegan una sola vez;
    - Atrás durante la animación de avanzar;
    - gesto predictivo simulado, a mitad, completado y cancelado.
  - **`NoteEditorViewModelTest`:** `interceptsBack` en cada caso de T05, y falso sin nada pendiente.
  - **`ExportNotesViewModelTest`:** `interceptsBack` solo mientras se exporta.
  - **Pruebas existentes** de Ayuda y Papelera que pulsan Atrás: adaptarlas si dependían del `BackHandler`.
  - **Cubre:** RF-1 a RF-6, RF-9 a RF-16, RF-18 a RF-20
  - **Hecho cuando:** `./gradlew :presentation:testDebugUnitTest` y `./gradlew :presentation:connectedDebugAndroidTest` pasan en el emulador de API 36.

## Fase 3 — Verificación y cierre

- [x] **T08. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest` con `ANDROID_SERIAL=emulator-5554`, nunca en el teléfono real.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T09. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T10. Demo manual.** En ambos temas:
  - avanzar y volver entre todas las pantallas, con la flecha y con Atrás;
  - volver automáticamente al mover una nota a la papelera y al exportar;
  - en API 36, el gesto predictivo completado y cancelado en cada pantalla, también:
    - en el editor justo después de escribir;
    - en el editor con el guardado automático apagado y cambios;
  - en API 25, Atrás con la animación completa;
  - «Quitar animaciones» activado y escala de animación a 10×;
  - dos toques rápidos sobre una nota y giro durante la animación;
  - Mis notas con imágenes, para comprobar que no hay tirones.
  - **Cubre:** RF-1 a RF-23
  - **Hecho cuando:** todo se comporta según la spec y revisas conmigo la animación.

- [x] **T11. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja la spec 015.

- [x] **T12. Commit y PR** (con tu confirmación).
  - Marcar spec, plan y tareas como implementados.
  - Hacer el commit con Conventional Commits.
  - Subir la rama y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
