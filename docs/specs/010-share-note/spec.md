# Spec 010 — Compartir una nota

> **Estado:** implementada · **Fecha:** 2026-11-10 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`: botón Compartir en la barra superior del editor, entre Fijar y Más opciones, `4:1656` (oscuro) y `40:1037` (claro)

## Contexto y objetivo

Las notas solo se pueden leer dentro de la app. Para mandar una lista de la compra o las ideas de una reunión a otra persona, el usuario tiene que copiar el texto a mano y no puede enviar las imágenes. El diseño ya incluye un botón Compartir en el editor, pero está oculto.

**Objetivo:** que el usuario pueda enviar una nota a otras apps (mensajería, correo, almacenamiento) desde el editor, con su texto y sus imágenes.

**Relación con otras specs:** esta spec sustituye la línea de «Fuera de alcance» de la spec 006 sobre el botón Compartir. También sustituye en parte la de la spec 004 sobre compartir las tareas con sus casillas: las tareas se comparten como texto con «☐» y «☑». Compartir con formato sigue fuera de alcance (spec 002).

## Usuarios / actores

- **Usuario que coordina con otras personas:** quiere enviar una lista o unas notas por mensajería o correo.
- **Usuario que guarda copias fuera de la app:** quiere mandar una nota a su correo o a un servicio de almacenamiento.
- **Apps de destino:** reciben el texto y las imágenes a través del menú de compartir del sistema.

## Historias de usuario

- **H1.** Como usuario, quiero compartir una nota desde el editor, para enviarla sin copiar el texto a mano.
- **H2.** Como usuario, quiero que se compartan también sus imágenes, para que quien la reciba vea la nota completa.
- **H3.** Como usuario, quiero que las tareas se vean como tareas al compartirlas, para que se entienda qué está hecho.
- **H4.** Como usuario, quiero compartir lo que estoy viendo aunque no lo haya guardado, para no tener que guardar antes.

## Requisitos funcionales (criterios de aceptación en EARS)

### Botón
- **RF-1:** EL SISTEMA mostrará el botón Compartir en la barra superior del editor, entre Fijar y Más opciones.
- **RF-2:** MIENTRAS la nota no tenga título, contenido ni imágenes, EL SISTEMA mostrará el botón Compartir deshabilitado.
- **RF-3:** MIENTRAS la nota se esté cargando o no se haya podido abrir, EL SISTEMA no mostrará el botón Compartir.
- **RF-4:** EL SISTEMA mostrará el botón Compartir igual con el guardado automático encendido o apagado.

### Compartir
- **RF-5:** CUANDO el usuario toque el botón Compartir, EL SISTEMA abrirá el menú de compartir del sistema.
- **RF-6:** EL SISTEMA compartirá la nota tal como se ve en el editor en ese momento, incluidos los cambios sin guardar.
- **RF-7:** CUANDO el usuario comparta una nota, EL SISTEMA no la guardará.
- **RF-8:** CUANDO el usuario comparta una nota, EL SISTEMA no cambiará su fecha de última edición.

### Texto compartido
- **RF-9:** DONDE la nota tenga título y contenido, EL SISTEMA compartirá el título, una línea en blanco y el contenido.
- **RF-10:** DONDE la nota tenga solo título o solo contenido, EL SISTEMA compartirá solo esa parte.
- **RF-11:** EL SISTEMA compartirá cada tarea pendiente como una línea que empieza por «☐ ».
- **RF-12:** EL SISTEMA compartirá cada tarea hecha como una línea que empieza por «☑ ».
- **RF-13:** EL SISTEMA compartirá el texto sin formato (sin negrita, subrayado ni tachado).
- **RF-14:** DONDE la nota tenga título, EL SISTEMA lo usará como asunto para las apps que lo admitan, como el correo.

### Imágenes
- **RF-15:** DONDE la nota tenga imágenes, EL SISTEMA las compartirá junto con el texto.
- **RF-16:** EL SISTEMA compartirá las imágenes pendientes de guardar que se vean en el editor.
- **RF-17:** EL SISTEMA no compartirá las imágenes que el usuario quitó, aunque el cambio no esté guardado.
- **RF-18:** EL SISTEMA dará a la app de destino acceso solo de lectura a las imágenes compartidas.
- **RF-19:** SI alguna imagen no se puede compartir, ENTONCES EL SISTEMA compartirá el texto y las demás imágenes.
- **RF-20:** SI alguna imagen no se puede compartir, ENTONCES EL SISTEMA mostrará el aviso «Algunas imágenes no se pudieron compartir».

### Errores
- **RF-21:** SI no hay ninguna app que pueda recibir la nota, ENTONCES EL SISTEMA mostrará el aviso «No hay apps para compartir esta nota».
- **RF-22:** CUANDO el usuario cierre el menú de compartir sin elegir una app, EL SISTEMA volverá al editor sin cambios en la nota.

### Accesibilidad
- **RF-23:** EL SISTEMA dará al botón Compartir la etiqueta accesible «Compartir nota».
- **RF-24:** EL SISTEMA anunciará a los lectores de pantalla si el botón Compartir está deshabilitado.

## Requisitos no funcionales

- **Aspecto:** el botón sigue los frames `4:1656` y `40:1037`, en la posición y con el ícono del diseño.
- **Accesibilidad:** la zona táctil del botón mide al menos 44 × 44 dp.
- **Privacidad:** la app solo comparte la nota cuando el usuario elige una app de destino. No envía nada por su cuenta ni da acceso a otras imágenes o notas.
- **Idioma:** todos los textos nuevos en español, en `strings.xml`.

## Casos límite

- **Nota nueva vacía:** el botón está deshabilitado (RF-2).
- **Nota solo con imágenes:** se comparten las imágenes, sin texto (RF-10, RF-15).
- **Título o contenido solo con espacios:** cuentan como vacíos para RF-2, RF-9 y RF-10.
- **Nota con muchas imágenes o imágenes grandes:** se comparten todas; cada app de destino decide cuántas acepta.
- **App de destino que solo acepta texto:** recibe el texto; las que solo aceptan imágenes reciben las imágenes. Lo decide cada app.
- **Tarea vacía:** se comparte como «☐ » o «☑ » sin texto.
- **Compartir con el selector de color abierto:** no aplica, porque la hoja tapa la barra superior.
- **Compartir dos veces seguidas:** cada toque abre el menú de nuevo con el contenido de ese momento.
- **Girar la pantalla con el menú de compartir abierto:** el menú es del sistema; al volver, el editor sigue igual.

## Fuera de alcance

- Compartir desde Mis notas o compartir varias notas a la vez.
- Compartir con formato (negrita, subrayado, tachado) o como archivo (PDF, documento).
- Elegir qué partes compartir, como solo el texto o solo algunas imágenes.
- Recibir notas o texto compartidos desde otras apps.
- Enlaces para compartir o colaboración en tiempo real.
- Exportar varias notas: lo cubre la spec 011 («Exportar notas» en Ajustes).
- Recordar a qué apps se compartió una nota.

## Criterios de finalización

- Todos los RF con prueba en verde (unitarias e instrumentadas) y `./gradlew test` pasando.
- Demo manual en el emulador, en ambos temas:
  - Compartir una nota con título, contenido, tareas e imágenes, y comprobar lo recibido en otra app.
  - Compartir una nota solo con texto y otra solo con imágenes.
  - Con el guardado automático apagado, compartir con cambios e imágenes pendientes, y comprobar que la nota sigue sin guardar.
  - Cerrar el menú sin elegir app.
- El botón comparado con los frames de Figma.

## Dudas abiertas

Ninguna.
