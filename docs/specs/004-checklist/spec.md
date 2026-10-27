# Spec 004 — Lista de tareas en las notas

> **Estado:** implementada · **Fecha:** 2026-10-26 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`, nodo `4:1656` (editor, sección «Mis pendientes»)

## Contexto y objetivo

El editor muestra en su barra el botón «Lista de tareas», pero está deshabilitado. Quien usa una nota para pendientes o para la compra tiene que simular las casillas escribiendo caracteres como «☐» y «☑», que no se pueden marcar y no se distinguen del resto del texto; la propia nota semilla «La compra» lo hace así.

**Objetivo:** que el usuario pueda convertir líneas del contenido en tareas con una casilla, marcarlas como hechas, y ver ese estado en el editor y en la tarjeta de Mis notas, tal como aparece en el diseño.

## Usuarios / actores

- **Usuario que lleva pendientes en sus notas:** quiere ir marcando lo que ya hizo sin borrarlo.
- **Usuario que hace listas (compra, equipaje):** quiere escribir varios elementos seguidos y tacharlos al completarlos.

## Historias de usuario

- **H1.** Como usuario, quiero convertir una o varias líneas del contenido en tareas, para llevar una lista de pendientes dentro de la nota.
- **H2.** Como usuario, quiero marcar una tarea como hecha tocando su casilla, para ver de un vistazo qué me queda.
- **H3.** Como usuario, quiero seguir escribiendo tareas seguidas con Intro, para hacer listas largas sin volver a tocar el botón.
- **H4.** Como usuario, quiero ver en la tarjeta de Mis notas qué tareas están hechas, para saber cómo voy sin abrir la nota.
- **H5.** Como usuario, quiero que las listas que ya escribí con «☐» y «☑» se conviertan en tareas reales, para no tener que rehacerlas.

## Requisitos funcionales (criterios de aceptación en EARS)

### Botón «Lista de tareas»
- **RF-1:** EL SISTEMA habilitará el botón «Lista de tareas» de la barra del editor.
- **RF-2:** MIENTRAS el cursor esté en el título de la nota, EL SISTEMA mostrará deshabilitado el botón «Lista de tareas».
- **RF-3:** CUANDO el usuario toque el botón con el cursor en una línea del contenido que no es tarea, EL SISTEMA convertirá esa línea en una tarea pendiente.
- **RF-4:** CUANDO el usuario toque el botón con el cursor en una línea que ya es tarea, EL SISTEMA convertirá esa línea en texto normal.
- **RF-5:** CUANDO el usuario toque el botón con una selección que abarca varias líneas y alguna de ellas no es tarea, EL SISTEMA convertirá en tarea pendiente cada línea abarcada que no lo sea.
- **RF-6:** CUANDO el usuario toque el botón con una selección cuyas líneas abarcadas ya son todas tareas, EL SISTEMA convertirá todas esas líneas en texto normal.
- **RF-7:** CUANDO una línea pase de tarea a texto normal, EL SISTEMA conservará su texto.
- **RF-8:** CUANDO una línea pase de tarea a texto normal, EL SISTEMA descartará su estado de hecha o pendiente.
- **RF-9:** CUANDO el usuario convierta líneas en tareas o en texto, EL SISTEMA conservará el formato (negrita, subrayado, tachado) de su texto.

### Escribir en una lista
- **RF-10:** CUANDO el usuario pulse Intro al final o en medio de una tarea con texto, EL SISTEMA creará una tarea pendiente nueva en la línea siguiente, con el texto que quedaba a la derecha del cursor.
- **RF-11:** CUANDO el usuario pulse Intro en una tarea sin texto, EL SISTEMA convertirá esa línea en texto normal vacío, sin crear una tarea nueva.
- **RF-12:** CUANDO el usuario pulse borrar con el cursor al inicio del texto de una tarea, EL SISTEMA convertirá esa línea en texto normal y conservará su texto.
- **RF-13:** CUANDO el usuario pulse borrar al inicio de una línea de texto normal que antes fue tarea por RF-12, EL SISTEMA la unirá con la línea anterior, como con cualquier otra línea.
- **RF-14:** CUANDO el usuario pegue un texto de varias líneas dentro de una tarea, EL SISTEMA convertirá en tarea pendiente cada línea pegada después de la primera.

### Marcar tareas
- **RF-15:** EL SISTEMA mostrará cada tarea con una casilla delante de su texto.
- **RF-16:** CUANDO el usuario toque la casilla de una tarea pendiente en el editor, EL SISTEMA la marcará como hecha.
- **RF-17:** CUANDO el usuario toque la casilla de una tarea hecha en el editor, EL SISTEMA la marcará como pendiente.
- **RF-18:** MIENTRAS una tarea esté hecha, EL SISTEMA mostrará su casilla marcada con una marca de verificación.
- **RF-19:** MIENTRAS una tarea esté hecha, EL SISTEMA mostrará su texto tachado y con el color de texto secundario.
- **RF-20:** CUANDO el usuario marque o desmarque una tarea, EL SISTEMA la mantendrá en la misma posición.
- **RF-21:** CUANDO el usuario marque o desmarque una tarea, EL SISTEMA no cambiará el formato guardado de su texto (el tachado de RF-19 es solo visual).

### Guardado
- **RF-22:** CUANDO el usuario convierta líneas, marque o desmarque una tarea, EL SISTEMA guardará la nota igual que con un cambio de texto.
- **RF-23:** CUANDO el usuario convierta líneas, marque o desmarque una tarea, EL SISTEMA actualizará la fecha de última edición de la nota.
- **RF-24:** CUANDO el usuario vuelva a abrir una nota con tareas, EL SISTEMA mostrará cada tarea en la misma línea y con el mismo estado con que se guardó.

### Notas existentes
- **RF-25:** CUANDO se instale esta funcionalidad, EL SISTEMA convertirá en tarea pendiente cada línea de una nota existente que empiece por «☐», y en tarea hecha cada línea que empiece por «☑».
- **RF-26:** CUANDO convierta una línea por RF-25, EL SISTEMA quitará de su texto el carácter «☐» o «☑» y los espacios que lo siguen.
- **RF-27:** EL SISTEMA mostrará sin cambios las demás líneas y las notas que no tengan esos caracteres al inicio de una línea.
- **RF-28:** EL SISTEMA creará la nota semilla «La compra» de las instalaciones nuevas con tareas reales, no con los caracteres «☐» y «☑».

### Cuadrícula de Mis notas
- **RF-29:** EL SISTEMA mostrará en la vista previa de cada tarjeta las tareas con su casilla vacía o marcada.
- **RF-30:** EL SISTEMA mostrará en la tarjeta el texto de las tareas hechas tachado.
- **RF-31:** CUANDO el usuario toque una tarea en la tarjeta, EL SISTEMA abrirá la nota, igual que al tocar cualquier otra parte de la tarjeta, sin cambiar el estado de la tarea.

### Búsqueda
- **RF-32:** EL SISTEMA buscará en el texto de las tareas igual que en el resto del contenido.

### Accesibilidad
- **RF-33:** EL SISTEMA dará a cada casilla del editor una etiqueta accesible con el texto de su tarea.
- **RF-34:** EL SISTEMA anunciará a los lectores de pantalla si cada tarea está hecha o pendiente.

## Requisitos no funcionales

- **RNF-1. Datos.** Activar esta funcionalidad no pierde ni altera el texto de ninguna nota existente, salvo quitar los caracteres «☐»/«☑» y sus espacios según RF-26.
- **RNF-2. Aspecto.** Las casillas siguen el diseño de Figma `4:1656`: pendiente con borde y fondo vacío; hecha rellena con el color de acento y una marca de verificación. Sobre una nota con color, siguen siendo distinguibles (contraste mínimo de 3:1 del borde y del relleno respecto al fondo).
- **RNF-3. Tamaño táctil.** La zona táctil de cada casilla en el editor mide al menos 44 × 44 dp, como el resto de acciones de la app.
- **RNF-4. Accesibilidad.** El estado hecho no depende solo del color: lleva marca de verificación y tachado.
- **RNF-5. Idioma.** Los textos nuevos (por ejemplo, los estados «Hecha» y «Pendiente») están en español, en `strings.xml`.

## Casos límite

- **CL-1.** Selección que mezcla tareas y texto normal: todas pasan a ser tareas (RF-5).
- **CL-2.** Selección que mezcla tareas hechas y pendientes, todas tareas: todas pasan a texto normal y pierden su estado (RF-6, RF-8).
- **CL-3.** Línea vacía convertida en tarea: queda una tarea vacía con su casilla y el cursor en ella.
- **CL-4.** Contenido vacío al tocar el botón: la primera línea pasa a ser una tarea vacía.
- **CL-5.** Intro en una tarea vacía en mitad de una lista: esa línea pasa a texto normal y las tareas de debajo siguen siendo tareas (RF-11).
- **CL-6.** Borrar todo el texto de una tarea sin borrar la casilla: queda una tarea vacía (RF-12 se aplica solo al pulsar borrar con el cursor al inicio).
- **CL-7.** Una tarea con texto en negrita: al marcarla como hecha se ve en negrita y tachada; al desmarcarla, solo en negrita (RF-21).
- **CL-8.** Una línea con «☐» en mitad del texto (no al inicio) en una nota existente: se queda como texto (RF-27).
- **CL-9.** Tarjeta con más tareas de las que caben en la vista previa: se recorta como hoy, conservando las casillas de las líneas visibles.
- **CL-10.** Vaciar una nota que solo tenía tareas vacías: se aplican las reglas actuales de nota vacía.
- **CL-11.** Pegar varias líneas dentro de una tarea: la primera se añade a esa tarea y cada línea siguiente pasa a ser una tarea pendiente, como con Intro (RF-14).

## Fuera de alcance

- Marcar o desmarcar tareas desde la tarjeta de Mis notas.
- Mover las tareas hechas al final o reordenar tareas arrastrándolas.
- Listas anidadas o con sangría, viñetas y listas numeradas.
- Tareas en el título de la nota.
- Fechas de vencimiento, recordatorios o asignación de tareas.
- Contadores de progreso («2 de 5 hechas») en el editor o en la tarjeta.
- Filtrar o buscar por estado de las tareas.
- Convertir otros caracteres distintos de «☐» y «☑» (por ejemplo «[ ]», «- [x]» o «✓»).
- Copiar o compartir las tareas hacia otras apps con sus casillas.
- Deshacer y rehacer (el botón «Deshacer» sigue deshabilitado).

## Criterios de finalización

- Todos los requisitos RF-1 a RF-34 y los casos límite CL-1 a CL-11 tienen pruebas que los verifican y pasan.
- `./gradlew test` y las pruebas instrumentadas pasan, según la constitución del proyecto.
- La conversión de las notas existentes (RF-25 a RF-27) se prueba con datos reales: instalar la versión actual con «La compra» y actualizar encima.
- Se comprueba a mano en el emulador:
  - convertir una línea y varias líneas en tareas y volver a texto;
  - escribir una lista con Intro y salir de ella con Intro en una tarea vacía;
  - borrar al inicio de una tarea;
  - marcar y desmarcar, cerrar y reabrir la nota;
  - ver las tareas en la tarjeta y sobre una nota con color.

## Dudas abiertas

Ninguna.
