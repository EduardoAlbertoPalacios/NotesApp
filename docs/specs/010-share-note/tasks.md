# Tareas 010 — Compartir una nota

> **Estado:** en curso · **Fecha:** 2026-11-10 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/share-note` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/010-share-note/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/share-note` y su base es el último commit de `origin/main`.

## Fase 1 — Dominio (`:domain`)

- [x] **T02. Modelo y caso de uso.** Crear `NoteShare` y `BuildNoteShareUseCase`, según la sección 4.1 del plan. Añadir `exists(path)` a `ImageStorage`.
  - **Cubre:** RF-9 a RF-17, RF-19
  - **Hecho cuando:** `./gradlew :domain:compileKotlin` compila.

- [x] **T03. Pruebas de dominio** (`test-writer`). Probar `BuildNoteShareUseCase`: solo título, solo contenido, ambos, espacios, tareas hechas y pendientes, tarea vacía, formato ignorado, solo imágenes, imágenes que faltan y nota vacía. Ampliar el fake de `ImageStorage`.
  - **Cubre:** RF-9 a RF-17, RF-19; casos límite de texto
  - **Hecho cuando:** `./gradlew :domain:test` pasa.

## Fase 2 — Datos (`:data`)

- [x] **T04. `exists()` en el almacenamiento.** Implementar `ImageStorageImpl.exists()` con la misma comprobación de carpeta que `delete`.
  - **Cubre:** RF-19
  - **Hecho cuando:** `./gradlew :data:compileDebugKotlin` compila.

- [x] **T05. Pruebas de datos** (`test-writer`). Ampliar `ImageStorageImplTest` con un archivo existente, uno que falta y una ruta fuera de la carpeta.
  - **Cubre:** RF-19
  - **Hecho cuando:** `./gradlew :data:test` y `./gradlew :data:connectedDebugAndroidTest` pasan.

## Fase 3 — App (`:app`)

- [x] **T06. `FileProvider`.** Declarar el provider en el manifiesto con la autoridad `${applicationId}.fileprovider` y crear `res/xml/file_paths.xml` solo con `note_images/`.
  - **Cubre:** RF-18, requisito de privacidad
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

## Fase 4 — Editor (`:presentation`)

- [x] **T07. Dependencia.** Declarar `libs.androidx.core.ktx` en `:presentation` (D3).
  - **Cubre:** D3 del plan
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T08. Estado, intent y efecto.** Añadir `canShare`, `ShareClicked` y `ShareNote`. Inyectar `BuildNoteShareUseCase` en el ViewModel y llamarlo con el estado visible sin guardar. Emitir el aviso de imágenes que faltan.
  - **Cubre:** RF-2, RF-6 a RF-8, RF-20
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila.

- [x] **T09. Botón.** Descargar `ic_share` de Figma (`4:1928`). Añadir el botón en la barra superior entre Fijar y Más opciones, solo con `showNoteActions`, deshabilitado sin `canShare` y con la etiqueta «Compartir nota». Añadir los textos nuevos.
  - **Cubre:** RF-1 a RF-4, RF-23, RF-24
  - **Hecho cuando:** `./gradlew :presentation:compileDebugKotlin` compila y las previews muestran el botón habilitado y deshabilitado en ambos temas.

- [x] **T10. Selector del sistema.** En `NoteEditorRoute`, convertir `ShareNote` en el `Intent` de la sección 4.3 del plan (URI de `FileProvider`, `ClipData` y permiso de lectura) y abrir `createChooser`. Mostrar el aviso si no hay apps.
  - **Cubre:** RF-5, RF-14, RF-15, RF-18, RF-21, RF-22
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

- [x] **T11. Pruebas del editor** (`test-writer`). Ampliar `NoteEditorViewModelTest` con `canShare`, el efecto con el estado visible en ambos modos de guardado, que no guarda ni cambia la fecha y el aviso de imágenes que faltan. Ampliar `NoteEditorScreenTest` con el botón en ambos temas.
  - **Cubre:** RF-1 a RF-4, RF-6 a RF-8, RF-20, RF-23, RF-24
  - **Hecho cuando:** `./gradlew :presentation:test` y `./gradlew :presentation:connectedDebugAndroidTest` pasan.

## Fase 5 — Documentación y cierre

- [x] **T12. Actualizar las specs 004 y 006.** Hacer que sus líneas de «Fuera de alcance» sobre compartir remitan a la 010.
  - **Cubre:** principio 2
  - **Hecho cuando:** ninguna spec contradice a la 010.

- [x] **T13. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest`.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T14. Demo manual en el emulador.** En ambos temas: compartir una nota con título, contenido, tareas e imágenes y comprobar lo recibido en al menos dos apps; una solo con texto y otra solo con imágenes; con el guardado automático apagado, compartir con cambios e imágenes pendientes y comprobar que sigue sin guardar; cerrar el selector sin elegir; comprobar que la barra superior cabe a 360 dp.
  - **Cubre:** RF-5 a RF-8, RF-15 a RF-18, RF-22, riesgos del plan, criterios de finalización
  - **Hecho cuando:** todo se comporta según la spec.

- [ ] **T15. Comparar con Figma.** Comparar el botón con los frames `4:1656` y `40:1037`.
  - **Cubre:** requisito de aspecto
  - **Hecho cuando:** no quedan diferencias o están resueltas contigo.

- [x] **T16. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T17. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas. Corregir que la spec 009 ya está en `main`.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja las specs 009 y 010.

- [ ] **T18. Commit y PR** (con tu confirmación). Marcar spec, plan y tareas como implementados. Hacer el commit con Conventional Commits y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
