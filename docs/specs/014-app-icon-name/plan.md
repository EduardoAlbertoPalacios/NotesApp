# Plan 014 — Icono y nombre de la app

> **Estado:** implementado · **Fecha:** 2026-11-19 · **Spec:** [spec.md](spec.md) (aprobada) · **Diseño:** archivos `notelet-android-icon` entregados por el usuario

Plan técnico para implementar la spec 014. Cada sección indica entre corchetes los requisitos que cubre.

## 1. Resumen

Es un cambio de recursos, sin código de producción en Kotlin.

- **Nombre.** `app_name` pasa a «Notelet» en `:app`, y el texto del pie de Ajustes pasa a «Notelet · Versión %1$s» en `:presentation` [RF-1 a RF-4].
- **Icono adaptable (Android 8 o superior).** Se copian tal cual los archivos entregados: fondo, dibujo, versión monocroma y color. Sustituyen a los del robot [RF-5 a RF-8, RF-10].
- **Android 7.0 y 7.1.** Se sustituyen los `.webp` del robot por dos iconos vectoriales en `mipmap-anydpi`, uno cuadrado y otro redondo. Se construyen con los mismos trazados del dibujo entregado (D2) [RF-9, RF-11].
- **Pantalla de carga.** No se toca: Android 12 o superior ya usa el icono del lanzador sobre `window_background` [RF-10].

**Sin dependencias nuevas, sin cambios de esquema, sin herramientas nuevas en el proyecto.**

## 2. Cumplimiento de la constitución

| Principio | Cómo lo respeta el plan |
|---|---|
| 1. Stack mínimo | Solo recursos de Android (vectores e iconos adaptables). |
| 2. La spec manda | Cada parte cita sus RF. La spec 006 no cambia: su RF-8 sigue siendo cierto. |
| 3. Lógica fuera de la UI | No hay lógica nueva. |
| 4. Sin pruebas no hay commit | Prueba de UI del pie y prueba instrumentada de identidad de la app (sección 6). Las escribe `test-writer`. |
| 5. Los datos no se pierden | No toca Room, DataStore ni el identificador de la app: actualizar conserva notas y ajustes. |
| 6. Idioma | «Notelet» es un nombre propio (`translatable="false"`); el resto del pie sigue en español. |

## 3. Estructura de módulos

| Módulo | Cambios | RF |
|---|---|---|
| `:app` | `strings.xml`: `app_name` = «Notelet». Icono adaptable, color, versión monocroma y los dos vectores de Android 7.x. Se borran los diez `.webp` del robot. Prueba instrumentada nueva. | RF-1 a RF-3, RF-5 a RF-11 |
| `:presentation` | `strings.xml`: `settings_version` = «Notelet · Versión %1$s». Prueba de UI del pie ajustada. | RF-4 |
| `:domain`, `:data`, `:common` | Ninguno. | — |
| Documentación | `.memory/MEMORY.md`. | — |

## 4. Diseño por parte

### 4.1 Nombre [RF-1 a RF-4]

- **`app/src/main/res/values/strings.xml`:** `<string name="app_name" translatable="false">Notelet</string>`. El manifiesto ya lo usa como `android:label` de la aplicación y de `MainActivity`, así que llega al lanzador, a la lista de apps y a las recientes sin más cambios [RF-1 a RF-3].
- **`presentation/src/main/res/values/strings.xml`:** `settings_version` = «Notelet · Versión %1$s» [RF-4]. Se repite el nombre en vez de leer `app_name` (D3).

### 4.2 Icono adaptable (Android 8 o superior) [RF-5 a RF-8, RF-10]

Se copian sin modificar los archivos de `notelet-android-icon/res` a `app/src/main/res`:

| Archivo | Acción |
|---|---|
| `drawable/ic_launcher_background.xml` | Sustituye al de la cuadrícula verde |
| `drawable/ic_launcher_foreground.xml` | Sustituye al del robot |
| `drawable/ic_launcher_monochrome.xml` | Nuevo |
| `values/ic_launcher_colors.xml` | Nuevo (`ic_launcher_background` = `#E8484F`) |
| `mipmap-anydpi-v26/ic_launcher.xml` y `ic_launcher_round.xml` | Sustituyen a los actuales. Hoy `<monochrome>` apunta al dibujo del robot; ahora apunta a la versión monocroma [RF-8] |

- El dibujo ya está dentro de la zona segura de 66 dp, así que ningún recorte lo corta [RF-6].
- La versión monocroma se usa tal como llegó, aunque su marcador sea un poco más estrecho que el de la versión a color (spec: sin redibujar).

### 4.3 Android 7.0 y 7.1 [RF-9, RF-11]

- **Borrar** `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.webp` e `ic_launcher_round.webp`, los diez archivos del robot [RF-11].
- **Crear** `mipmap-anydpi/ic_launcher.xml` (cuadrado redondeado) y `mipmap-anydpi/ic_launcher_round.xml` (círculo) como `<vector>` de 48 dp:
  - **Encuadre:** el viewport recorta la zona visible del icono adaptable (de 18 a 90 en la cuadrícula de 108), con un `<group>` desplazado. Así la página ocupa la misma proporción que en el PNG de 512 × 512 entregado.
  - **Fondo:** una forma de color `@color/ic_launcher_background`. Como referencia, sigue las líneas guía de Material para iconos de lanzador: círculo de 44 dp y cuadrado de 40 dp con esquinas de 4 dp sobre el lienzo de 48 dp. Se ajusta en la demo comparándolo con el PNG.
  - **Dibujo:** los mismos trazados y colores de `ic_launcher_foreground.xml`, copiados sin cambios.
- En Android 8 o superior, `mipmap-anydpi-v26` tiene prioridad sobre `mipmap-anydpi`, así que estos vectores solo se usan en Android 7.x.
- Con `minSdk = 24`, el sistema dibuja los vectores de forma nativa y el plugin de Android no genera PNG. Se ven nítidos en todas las densidades (RNF Nitidez).

### 4.4 Pantalla de carga [RF-10]

Sin cambios. `values-v31/themes.xml` fija `windowSplashScreenBackground` = `@color/window_background`, que sigue el tema elegido. El icono animado por defecto es el del lanzador, así que muestra el de Notelet.

### 4.5 Documentación

- **`.memory/MEMORY.md`:** estado y decisiones.
- La spec 006 no cambia: su RF-8 («nombre de la app y su versión») sigue siendo cierto.

## 5. Decisiones técnicas

- **D1. Copiar los recursos adaptables tal cual.** **Aprobado** el 2026-11-19. Ya están en el formato de Android y respetan la zona segura. Copiarlos garantiza la fidelidad (RNF). **Alternativa:** regenerarlos con Image Asset Studio de Android Studio, que reescribe los trazados y podría cambiar proporciones.
- **D2. Vectores en `mipmap-anydpi` para Android 7.x, en vez de PNG por densidad.** **Aprobado** el 2026-11-19. En esta máquina no hay herramientas para pasar el SVG a PNG (ni ImageMagick, ni rsvg, ni Pillow), y añadirlas sería instalar algo nuevo. Un vector con los mismos trazados se ve nítido en todas las densidades y es un único archivo por forma. **Alternativas:**
  - generar los PNG con Image Asset Studio (paso manual tuyo en Android Studio, cinco densidades × dos formas);
  - escalar el PNG de 512 × 512 con `sips`. No sirve tal cual, porque es un cuadrado rojo sin forma (sin esquinas ni círculo).
- **D3. El nombre se repite en dos `strings.xml`.** **Aprobado** el 2026-11-19. `:presentation` no ve los recursos de `:app`, y un texto con formato no puede incluir otro recurso. Las dos apariciones las cubren pruebas. **Alternativa:** mover `app_name` a `:presentation` y que el manifiesto de `:app` lo lea por la fusión de recursos. Es una sola fuente, pero esconde el nombre de la app fuera de `:app`.
- **D4. Demo en Android 7.1.** **Aprobado** el 2026-11-19. Ahora mismo solo hay emuladores con API 36. Para comprobar RF-9 de verdad hace falta descargar la imagen de sistema de Android 7.1 (API 25), unos 1 GB, y crear un AVD. No es una dependencia del proyecto, pero instala software en tu equipo, y lo autorizaste. **Alternativa:** verlo solo con la vista previa de vectores de Android 7.x en Android Studio, sin probarlo en un dispositivo.

## 6. Estrategia de pruebas

Las escribe `test-writer`.

| Módulo | Prueba | Qué cubre |
|---|---|---|
| `:presentation` (UI) | `SettingsScreenTest` | El pie muestra el texto literal «Notelet · Versión 9.8.7-test», no solo el recurso formateado, en ambos temas [RF-4] |
| `:app` (instrumentada) | `AppIdentityTest`, nueva | La etiqueta de la aplicación y la de `MainActivity` son «Notelet» [RF-1 a RF-3]. El icono y el icono redondo son `AdaptiveIconDrawable`, con el fondo `#E8484F` [RF-5, RF-7]. En API 33 o superior, `monochrome` no es nulo [RF-8] |
| Manual | Emulador API 36 | Lanzador, lista de apps, recientes, dos formas de recorte, iconos temáticos sí y no, pantalla de carga en claro y oscuro [RF-1 a RF-8, RF-10] |
| Manual | Emulador API 25 (D4) | Icono cuadrado y redondo en el lanzador de Android 7.1 [RF-9, RF-11] |

Las pruebas instrumentadas se ejecutan solo en el emulador (`ANDROID_SERIAL`), nunca en el teléfono real.

## 7. Orden de implementación

1. Crear la rama `feat/app-icon-name` desde `main`.
2. Nombre: los dos `strings.xml`.
3. Icono adaptable: copiar los recursos entregados (D1).
4. Android 7.x: borrar los `.webp` y crear los dos vectores (D2).
5. Pruebas (`test-writer`).
6. Ejecutar `./gradlew test` y las pruebas instrumentadas en el emulador, y pasar `architecture-reviewer`.
7. Demo manual en API 36 y, si lo apruebas, en API 25 (D4).
8. Actualizar `.memory/MEMORY.md`.

## 8. Trazabilidad de requisitos

| Requisitos | Dónde |
|---|---|
| RF-1 a RF-3 | `app_name` en `app/res/values/strings.xml`, manifiesto sin cambios |
| RF-4 | `settings_version` en `presentation/res/values/strings.xml` |
| RF-5 a RF-7 | `mipmap-anydpi-v26`, `drawable/ic_launcher_{background,foreground}.xml`, `ic_launcher_colors.xml` |
| RF-8 | `drawable/ic_launcher_monochrome.xml` y `<monochrome>` en `mipmap-anydpi-v26` |
| RF-9 | `mipmap-anydpi/ic_launcher{,_round}.xml` |
| RF-10 | Sin cambios (`values-v31/themes.xml` más el icono nuevo) |
| RF-11 | Borrado de los `.webp` y sustitución de los drawables del robot |

## 9. Riesgos

- **Caché del lanzador:** tras instalar, el emulador puede mostrar el icono o el nombre anteriores hasta reiniciar el lanzador (CL-1). En la demo se desinstala antes de instalar, o se reinicia el lanzador. Desinstalar borra los datos del emulador, nunca los del teléfono.
- **Encuadre del icono de Android 7.x:** las medidas del fondo (D2) son una propuesta. Se validan visualmente frente al PNG entregado.
- **Lint:** `./gradlew lint` ya falla por un error previo en `NoteEditorRoute.kt`; no forma parte de este trabajo.
