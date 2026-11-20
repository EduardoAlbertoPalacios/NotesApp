# Tareas 014 — Icono y nombre de la app

> **Estado:** implementadas · **Fecha:** 2026-11-19 · **Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Tareas en orden de dependencia: cada una solo necesita las anteriores. Las pruebas las escribe el agente `test-writer`, y ningún commit se hace sin que pasen las pruebas (principio 4) ni sin tu confirmación.

**Paralelismo posible:** T02, T03 y T04 tocan archivos distintos y pueden hacerse a la vez. T05 (emulador de Android 7.1) ya está en marcha en segundo plano.

## Fase 0 — Preparación

- [x] **T01. Crear la rama de trabajo.** Crear `feat/app-icon-name` desde `main`, actualizado con `origin`. Llevar a ella `docs/specs/014-app-icon-name/`.
  - **Cubre:** —
  - **Hecho cuando:** `git branch --show-current` muestra `feat/app-icon-name` y su base es el último commit de `origin/main`.

## Fase 1 — Recursos

- [x] **T02. Nombre.**
  - `app/src/main/res/values/strings.xml`: `app_name` = «Notelet», con `translatable="false"`.
  - `presentation/src/main/res/values/strings.xml`: `settings_version` = «Notelet · Versión %1$s» (D3).
  - **Cubre:** RF-1 a RF-4
  - **Hecho cuando:** `./gradlew :app:assembleDebug` compila.

- [x] **T03. Icono adaptable (D1).** Copiar sin modificar, desde `notelet-android-icon/res` a `app/src/main/res`:
  - `drawable/ic_launcher_background.xml`, `drawable/ic_launcher_foreground.xml` y `drawable/ic_launcher_monochrome.xml`;
  - `values/ic_launcher_colors.xml`;
  - `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`.
  - **Cubre:** RF-5 a RF-8, RF-10
  - **Hecho cuando:** `diff -r` entre los archivos entregados y los copiados no muestra diferencias, y `./gradlew :app:assembleDebug` compila.

- [x] **T04. Icono de Android 7.x (D2).**
  - Borrar los diez `.webp` de `mipmap-{m,h,xh,xxh,xxxh}dpi`.
  - Crear `mipmap-anydpi/ic_launcher.xml` (cuadrado redondeado) y `mipmap-anydpi/ic_launcher_round.xml` (círculo), vectores de 48 dp:
    - encuadre de la zona visible (18 a 90 de la cuadrícula de 108);
    - fondo con `@color/ic_launcher_background`;
    - los trazados del dibujo, copiados sin cambios.
  - **Cubre:** RF-9, RF-11
  - **Hecho cuando:**
    - `./gradlew :app:assembleDebug` compila;
    - no quedan archivos `ic_launcher*.webp` en el proyecto;
    - el APK no contiene ningún recurso del robot.

## Fase 2 — Pruebas

- [x] **T05. Emulador de Android 7.1 (D4).**
  - Instalar las herramientas de línea de comandos del SDK y la imagen `system-images;android-25;google_apis;arm64-v8a`.
  - Crear el AVD `Notelet_API_25`.
  - **Cubre:** RF-9 (verificación)
  - **Hecho cuando:** `emulator -list-avds` lo muestra y arranca.

- [x] **T06. Pruebas** (`test-writer`).
  - **`SettingsScreenTest`:** el pie muestra el texto literal «Notelet · Versión 9.8.7-test», en ambos temas.
  - **`AppIdentityTest`** (nueva, instrumentada en `:app`):
    - la etiqueta de la aplicación y la de `MainActivity` son «Notelet»;
    - el icono y el icono redondo son `AdaptiveIconDrawable` con el fondo `#E8484F`;
    - en API 33 o superior, `monochrome` no es nulo.
  - **Cubre:** RF-1 a RF-5, RF-7, RF-8
  - **Hecho cuando:** `./gradlew :presentation:connectedDebugAndroidTest` y `./gradlew :app:connectedDebugAndroidTest` pasan en el emulador de API 36.

## Fase 3 — Verificación y cierre

- [x] **T07. Pruebas completas.** Ejecutar `./gradlew test` y `./gradlew connectedDebugAndroidTest` con `ANDROID_SERIAL=emulator-5554`, nunca en el teléfono real.
  - **Cubre:** criterios de finalización
  - **Hecho cuando:** ambos pasan.

- [x] **T08. Revisión de arquitectura.** Pasar `architecture-reviewer` sobre los cambios.
  - **Cubre:** principio 3
  - **Hecho cuando:** no quedan hallazgos pendientes.

- [x] **T09. Demo manual.**
  - **En API 36,** después de desinstalar la versión anterior del emulador (CL-1):
    - el nombre en el lanzador, en la lista de apps y en las recientes;
    - el icono con dos formas de recorte;
    - los iconos temáticos activados y desactivados;
    - la pantalla de carga en tema claro y en tema oscuro;
    - el pie de Ajustes.
  - **En API 25:** el icono cuadrado y el redondo en el lanzador, comparados con el PNG entregado. Si hace falta, ajustar las medidas del fondo de T04.
  - **Cubre:** RF-1 a RF-11
  - **Hecho cuando:** todo se comporta según la spec y revisas conmigo el icono frente a los archivos entregados.

- [x] **T10. Actualizar la memoria.** Actualizar `.memory/MEMORY.md` con el estado y las decisiones, sin pasar de ~50 líneas.
  - **Cubre:** —
  - **Hecho cuando:** el archivo refleja la spec 014.

- [x] **T11. Commit y PR** (con tu confirmación).
  - Marcar spec, plan y tareas como implementados.
  - Hacer el commit con Conventional Commits.
  - Subir la rama y abrir el PR hacia `main`.
  - **Cubre:** principios 2 y 4
  - **Hecho cuando:** el PR está abierto.
