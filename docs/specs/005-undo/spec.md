# Spec 005 — Deshacer en el editor

> **Estado:** implementada · **Fecha:** 2026-10-29 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`, nodo `4:1656` (barra del editor, botón «Deshacer»)

## Contexto y objetivo

La barra del editor muestra el botón «Deshacer», pero está deshabilitado. Si el usuario borra un párrafo por error, pega encima de un texto, quita un formato o convierte líneas en tareas sin querer, solo puede rehacerlo a mano. Como la nota se guarda sola, ese error queda guardado enseguida.

**Objetivo:** que el usuario pueda volver atrás, paso a paso, los cambios que hizo en el título y el contenido de la nota mientras la tiene abierta.

## Usuarios / actores

- **Usuario que escribe o edita una nota:** quiere recuperar lo que borró o cambió por error sin reescribirlo.

## Historias de usuario

- **H1.** Como usuario, quiero deshacer lo último que escribí o borré, para recuperar el texto tal como estaba.
- **H2.** Como usuario, quiero deshacer un formato o un cambio en las tareas, para volver al aspecto anterior de la nota.
- **H3.** Como usuario, quiero deshacer varios pasos seguidos, para volver a un punto anterior de la edición.
- **H4.** Como usuario, quiero saber si hay algo que deshacer, para no tocar un botón que no hará nada.

## Requisitos funcionales (criterios de aceptación en EARS)

### Botón «Deshacer»
- **RF-1:** EL SISTEMA habilitará el botón «Deshacer» de la barra del editor.
- **RF-2:** MIENTRAS no haya ningún paso que deshacer, EL SISTEMA mostrará deshabilitado el botón «Deshacer».
- **RF-3:** CUANDO el usuario abra una nota, nueva o existente, EL SISTEMA empezará con el historial de deshacer vacío.
- **RF-4:** MIENTRAS haya pasos que deshacer, EL SISTEMA mantendrá habilitado el botón «Deshacer» tanto con el cursor en el título como en el contenido.

### Qué entra en el historial
- **RF-5:** CUANDO el usuario escriba o borre texto en el título, EL SISTEMA lo registrará como un cambio deshacible.
- **RF-6:** CUANDO el usuario escriba o borre texto en el contenido, EL SISTEMA lo registrará como un cambio deshacible.
- **RF-7:** CUANDO el usuario pegue o corte texto en el título o el contenido, EL SISTEMA lo registrará como un paso propio.
- **RF-8:** CUANDO el usuario aplique o quite un formato a una selección del contenido, EL SISTEMA lo registrará como un paso propio.
- **RF-9:** CUANDO el usuario toque el botón «Lista de tareas» y cambien las tareas, EL SISTEMA lo registrará como un paso propio.
- **RF-10:** CUANDO el usuario marque o desmarque una tarea, EL SISTEMA lo registrará como un paso propio.
- **RF-11:** CUANDO el usuario active o desactive un formato sin texto seleccionado, EL SISTEMA no registrará ningún paso.
- **RF-12:** CUANDO el usuario cambie el color, la categoría, la fijación o las imágenes de la nota, EL SISTEMA no registrará ningún paso.

### Agrupación de lo escrito
- **RF-13:** MIENTRAS el usuario escriba caracteres seguidos en un mismo campo, EL SISTEMA los agrupará en un solo paso.
- **RF-14:** MIENTRAS el usuario borre caracteres seguidos en un mismo campo, EL SISTEMA los agrupará en un solo paso.
- **RF-15:** CUANDO el usuario escriba un espacio, EL SISTEMA cerrará el paso de escritura en curso, incluido ese espacio.
- **RF-16:** CUANDO el usuario escriba un salto de línea, EL SISTEMA cerrará el paso de escritura en curso, incluido ese salto y los cambios que provoque en las tareas.
- **RF-17:** CUANDO pase 1 segundo sin que el usuario escriba ni borre, EL SISTEMA cerrará el paso en curso.
- **RF-18:** CUANDO el usuario pase de escribir a borrar, o de borrar a escribir, EL SISTEMA cerrará el paso en curso.
- **RF-19:** CUANDO el usuario mueva el cursor a otra posición o a otro campo, EL SISTEMA cerrará el paso en curso.
- **RF-20:** CUANDO el usuario haga un cambio que es un paso propio (RF-7 a RF-10), EL SISTEMA cerrará antes el paso de escritura en curso.

### Deshacer
- **RF-21:** CUANDO el usuario toque «Deshacer», EL SISTEMA devolverá el título y el contenido al estado anterior al último paso.
- **RF-22:** CUANDO el usuario deshaga un paso, EL SISTEMA restaurará el formato del contenido tal como estaba antes de ese paso.
- **RF-23:** CUANDO el usuario deshaga un paso, EL SISTEMA restaurará las tareas, con su estado de hecha o pendiente, tal como estaban antes de ese paso.
- **RF-24:** CUANDO el usuario deshaga un paso, EL SISTEMA quitará ese paso del historial.
- **RF-25:** CUANDO el usuario toque «Deshacer» varias veces, EL SISTEMA deshará los pasos uno a uno, del más reciente al más antiguo.
- **RF-26:** CUANDO el usuario deshaga un paso, EL SISTEMA colocará el cursor en el campo y la posición donde estaba antes de ese paso.
- **RF-27:** CUANDO el usuario deshaga un paso, EL SISTEMA tomará como formatos activos los del carácter inmediatamente anterior al cursor, como al mover el cursor (spec 002, RF-14).
- **RF-28:** CUANDO el usuario deshaga un paso, EL SISTEMA no cambiará el color, la categoría, la fijación ni las imágenes de la nota.
- **RF-29:** SI el historial supera los 100 pasos, ENTONCES EL SISTEMA descartará el paso más antiguo.

### Duración del historial
- **RF-30:** CUANDO el usuario gire la pantalla, EL SISTEMA conservará el historial de deshacer.
- **RF-31:** CUANDO el usuario salga del editor, EL SISTEMA descartará el historial de deshacer de esa nota.

### Guardado
- **RF-32:** CUANDO el usuario deshaga un paso, EL SISTEMA guardará la nota igual que con un cambio de texto.
- **RF-33:** CUANDO el usuario deshaga un paso, EL SISTEMA actualizará la fecha de última edición de la nota.
- **RF-34:** SI al deshacer un paso la nota queda igual que al abrirla (título, contenido, formato, tareas, color, categoría, fijación e imágenes), ENTONCES EL SISTEMA restaurará la fecha de última edición que tenía al abrirla, en lugar de aplicar RF-33.

## Requisitos no funcionales

- **RNF-1. Aspecto.** El botón sigue el diseño de Figma `4:1656`; deshabilitado, se ve como los demás botones deshabilitados de la barra.
- **RNF-2. Respuesta.** Deshacer un paso se refleja en pantalla en menos de 100 ms en una nota de 10 000 caracteres en el emulador de referencia (`Medium_Phone_API_36.0`).
- **RNF-3. Accesibilidad.** El lector de pantalla anuncia el botón como «Deshacer» y si está deshabilitado. La zona táctil mide al menos 44 × 44 dp, como el resto de acciones de la app.
- **RNF-4. Datos.** El historial no se guarda en la base de datos ni fuera de la sesión de edición.
- **RNF-5. Idioma.** Los textos nuevos, si los hay, están en español, en `strings.xml`.

## Casos límite

- **CL-1.** Escribir «Hola mundo» de corrido: dos pasos, «Hola » y «mundo»; el primer «Deshacer» deja «Hola ».
- **CL-2.** Escribir una palabra larga con una pausa de más de 1 segundo a la mitad: dos pasos, uno por cada parte (RF-17).
- **CL-3.** Pegar texto encima de una selección: un solo paso; al deshacerlo vuelve el texto seleccionado con su formato y sus tareas (RF-7, RF-22, RF-23).
- **CL-4.** Intro en una tarea, que crea una tarea nueva: un paso; al deshacerlo desaparece el salto y la tarea nueva (RF-16).
- **CL-5.** Intro en una tarea vacía, que la convierte en texto normal (spec 004): un paso; al deshacerlo la línea vuelve a ser tarea.
- **CL-6.** Borrar al inicio de una tarea, que la convierte en texto (spec 004): un paso; al deshacerlo vuelve a ser tarea.
- **CL-7.** Marcar una tarea con la casilla mientras el cursor está en el título: se registra el paso, y al deshacerlo el cursor va al contenido, a donde estaba antes del paso (RF-26).
- **CL-8.** Escribir en el contenido, cambiar el color y tocar «Deshacer»: se deshace la escritura y el color se queda (RF-12, RF-28).
- **CL-9.** Escribir en el título y luego en el contenido, y tocar «Deshacer» dos veces: primero se deshace el contenido y después el título, aunque el cursor esté en el contenido (historial común).
- **CL-10.** Deshacer todos los pasos de una nota nueva: queda vacía y se aplican las reglas actuales de nota vacía; el botón queda deshabilitado.
- **CL-11.** Deshacer hasta el estado de apertura en una nota existente: título, contenido, formato y tareas iguales a los del momento de abrirla; el botón queda deshabilitado y la nota recupera su fecha de última edición original (RF-34).
- **CL-12.** Activar negrita sin selección y escribir: el texto escrito en negrita es un paso de escritura normal; deshacerlo lo quita, y la negrita activa se calcula según RF-27.
- **CL-13.** Escribir sin pausa ni espacios más de 1 segundo seguido (tecleo rápido continuo): un solo paso hasta el primer espacio, salto de línea o pausa.
- **CL-14.** Seleccionar texto y borrarlo con la tecla de borrar: un paso de borrado; al deshacerlo vuelve el texto con su formato y sus tareas.
- **CL-15.** Deshacer hasta el estado de apertura después de cambiar el color: el color sigue siendo el nuevo, así que la nota no es igual que al abrirla y la fecha no se restaura (RF-33 se aplica).
- **CL-16.** Hacer 101 pasos y tocar «Deshacer» 101 veces: se deshacen los 100 más recientes y el botón queda deshabilitado; el primer paso no se puede deshacer (RF-29).

## Fuera de alcance

- Rehacer (volver a aplicar lo deshecho) y cualquier botón o gesto para ello.
- Deshacer cambios de color, categoría, fijación o imágenes.
- Deshacer el borrado de una nota completa.
- Conservar el historial al salir del editor, entre sesiones o si el sistema cierra la app en segundo plano.
- Atajos de teclado (Ctrl+Z) y gestos (agitar el teléfono) para deshacer.
- Mostrar una lista o vista previa del historial de cambios.
- Deshacer desde la pantalla Mis notas.

## Criterios de finalización

- Todos los requisitos RF-1 a RF-34 y los casos límite CL-1 a CL-16 tienen pruebas que los verifican y pasan.
- `./gradlew test` y las pruebas instrumentadas pasan, según la constitución del proyecto.
- Se comprueba a mano en el emulador:
  - escribir varias palabras y deshacerlas una a una;
  - pegar y deshacer;
  - aplicar negrita a una selección y deshacer;
  - crear tareas con Intro, marcar una y deshacer ambos pasos;
  - girar la pantalla y seguir deshaciendo;
  - salir y reabrir la nota: el botón aparece deshabilitado.

## Dudas abiertas

Ninguna.
