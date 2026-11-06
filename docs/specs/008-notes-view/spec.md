# Spec 008 — Vista de notas: cuadrícula o lista

> **Estado:** implementada · **Fecha:** 2026-11-04 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`: Mis notas en lista `62:311` (oscuro) y `55:880` (claro); Ajustes `4:1794` (oscuro) y `40:1211` (claro)

## Contexto y objetivo

Mis notas solo se puede ver en una cuadrícula de dos columnas, con tarjetas estrechas que recortan pronto el texto. La fila «Vista de notas» de Ajustes está atenuada y siempre indica «Cuadrícula». Quien prefiere leer más de cada nota sin abrirla, o ver su categoría de un vistazo, no tiene alternativa.

**Objetivo:** que el usuario pueda elegir entre ver Mis notas en cuadrícula o en lista, desde la propia pantalla o desde Ajustes, y que la elección se conserve.

**Relación con otras specs:** esta spec sustituye los RF-9 (solo en lo relativo a esta fila) y RF-12 de la spec 006. Lo que las specs 001, 002 y 004 definen para «la cuadrícula» o «la tarjeta» de Mis notas aplica a ambas vistas, salvo lo que indica esta spec.

## Usuarios / actores

- **Usuario que lee sus notas en el listado:** quiere ver más texto de cada nota sin abrirla.
- **Usuario que organiza por categorías:** quiere ver a qué categoría pertenece cada nota.
- **Usuario con muchas notas:** quiere seguir viendo varias a la vez en cuadrícula.

## Historias de usuario

- **H1.** Como usuario, quiero cambiar la vista desde Mis notas, para probar cuál me sirve sin salir de la pantalla.
- **H2.** Como usuario, quiero cambiar la vista también desde Ajustes, para encontrarla junto al resto de opciones de apariencia.
- **H3.** Como usuario, quiero ver en lista la categoría de cada nota, para distinguirlas sin abrirlas.
- **H4.** Como usuario, quiero que la app recuerde mi vista, para no elegirla cada vez que la abro.

## Requisitos funcionales (criterios de aceptación en EARS)

### Selector en Mis notas
- **RF-1:** EL SISTEMA mostrará en Mis notas un selector con las opciones Cuadrícula y Lista, en ese orden, debajo de los filtros de categoría.
- **RF-2:** EL SISTEMA marcará en el selector la opción de la vista vigente.
- **RF-3:** CUANDO el usuario toque en el selector la opción no marcada, EL SISTEMA mostrará las notas en esa vista.
- **RF-4:** CUANDO el usuario toque en el selector la opción marcada, EL SISTEMA mantendrá la vista vigente.
- **RF-5:** MIENTRAS haya una búsqueda activa, EL SISTEMA mantendrá el selector activo.
- **RF-6:** MIENTRAS el usuario no tenga ninguna nota, EL SISTEMA mantendrá el selector activo.

### Fila en Ajustes
- **RF-7:** EL SISTEMA mostrará habilitada la fila Vista de notas.
- **RF-8:** MIENTRAS Ajustes esté abierta, EL SISTEMA mostrará en la fila Vista de notas el nombre de la vista vigente.
- **RF-9:** CUANDO el usuario toque la fila Vista de notas, EL SISTEMA abrirá una hoja inferior con las opciones Cuadrícula y Lista.
- **RF-10:** MIENTRAS la hoja esté abierta, EL SISTEMA marcará como seleccionada la opción de la vista vigente.
- **RF-11:** CUANDO el usuario toque una opción de la hoja, EL SISTEMA establecerá esa vista como vigente.
- **RF-12:** CUANDO el usuario toque una opción de la hoja, EL SISTEMA cerrará la hoja.
- **RF-13:** CUANDO el usuario cierre la hoja sin tocar ninguna opción, EL SISTEMA mantendrá la vista vigente.
- **RF-14:** CUANDO el usuario cambie la vista en Ajustes, EL SISTEMA mostrará esa vista en Mis notas al volver.
- **RF-15:** CUANDO el usuario cambie la vista en Mis notas, EL SISTEMA mostrará esa vista en la fila de Ajustes.

### Vista de cuadrícula
- **RF-16:** MIENTRAS la vista Cuadrícula esté vigente, EL SISTEMA mostrará las tarjetas igual que antes de esta funcionalidad.

### Vista de lista
- **RF-17:** MIENTRAS la vista Lista esté vigente, EL SISTEMA mostrará una tarjeta por fila, a todo el ancho del contenido.
- **RF-18:** MIENTRAS la vista Lista esté vigente, EL SISTEMA mostrará hasta cuatro líneas de contenido en cada tarjeta.
- **RF-19:** SI el contenido de una tarjeta en lista no cabe en cuatro líneas, ENTONCES EL SISTEMA lo recortará con puntos suspensivos.
- **RF-20:** MIENTRAS la vista Lista esté vigente, EL SISTEMA mostrará las tareas consecutivas de una tarjeta seguidas en la misma línea.
- **RF-21:** SI una tarea no cabe en lo que queda de la línea, ENTONCES EL SISTEMA la pasará a la línea siguiente.
- **RF-22:** MIENTRAS la vista Lista esté vigente, EL SISTEMA conservará los saltos de línea del contenido que no separan dos tareas.
- **RF-23:** EL SISTEMA mostrará tachado en la tarjeta de lista el texto de las tareas hechas (spec 004, RF-30).
- **RF-24:** DONDE la nota tenga categoría, EL SISTEMA mostrará en su tarjeta de lista una etiqueta con el nombre de la categoría.
- **RF-25:** DONDE la nota no tenga categoría, EL SISTEMA no mostrará ninguna etiqueta en su tarjeta de lista.
- **RF-26:** EL SISTEMA mostrará la fecha de la tarjeta de lista en la misma línea que la etiqueta, alineada a la derecha.
- **RF-27:** DONDE la nota esté fijada, EL SISTEMA mostrará el ícono de fijada en su tarjeta de lista.
- **RF-28:** DONDE la nota tenga imagen, EL SISTEMA mostrará la imagen en la parte superior de su tarjeta de lista, a todo su ancho.
- **RF-29:** EL SISTEMA aplicará a la tarjeta de lista el color y el formato de texto de la nota, igual que en la cuadrícula.
- **RF-30:** CUANDO el usuario toque una tarjeta de lista, EL SISTEMA abrirá la nota.

### Ambas vistas
- **RF-31:** EL SISTEMA mostrará las notas en el mismo orden en ambas vistas.
- **RF-32:** CUANDO el usuario cambie de vista, EL SISTEMA conservará el filtro de categoría seleccionado.
- **RF-33:** CUANDO el usuario cambie de vista, EL SISTEMA conservará el texto buscado.
- **RF-34:** MIENTRAS haya una búsqueda activa, EL SISTEMA mostrará los resultados en la vista vigente.
- **RF-35:** CUANDO el usuario cambie de vista, EL SISTEMA mantendrá sin cambios las notas y su fecha de edición.

### Conservar la elección
- **RF-36:** CUANDO el usuario cambie la vista, EL SISTEMA la guardará en el dispositivo.
- **RF-37:** CUANDO la app se abra con una vista guardada, EL SISTEMA mostrará las notas en esa vista, sin mostrarlas antes en la otra.
- **RF-38:** SI no hay ninguna vista guardada, ENTONCES EL SISTEMA usará la vista Cuadrícula.
- **RF-39:** SI la vista guardada no se puede leer, ENTONCES EL SISTEMA usará la vista Cuadrícula.

### Fallo al guardar
- **RF-40:** SI la vista elegida no se puede guardar, ENTONCES EL SISTEMA la mantendrá vigente hasta que se cierre la app.
- **RF-41:** SI la vista elegida no se puede guardar, ENTONCES EL SISTEMA mostrará el aviso «No se pudo guardar la vista».

### Accesibilidad
- **RF-42:** EL SISTEMA dará a cada opción del selector una etiqueta accesible con el nombre de la vista.
- **RF-43:** EL SISTEMA anunciará a los lectores de pantalla si cada opción del selector y de la hoja está seleccionada o no.

## Requisitos no funcionales

- **Aspecto:** el selector y la tarjeta de lista siguen los frames `62:311` (oscuro) y `55:880` (claro). Ambos frames marcan Cuadrícula mientras muestran la lista; se considera un error del diseño y manda RF-2. El selector se muestra en ambas vistas, aunque no hay frame de cuadrícula con selector. La hoja de Ajustes no tiene frame propio: sigue el estilo de la hoja de tema. La fila sigue los frames `4:1794` y `40:1211`.
- **Accesibilidad:** las zonas táctiles de cada opción del selector, de la fila y de cada opción de la hoja miden al menos 44 × 44 dp.
- **Contraste:** el texto de la etiqueta de categoría cumple un contraste mínimo de 4,5:1 sobre cada color de nota.
- **Datos:** la elección se guarda solo en el dispositivo.
- **Idioma:** todos los textos nuevos en español, en `strings.xml`.

## Casos límite

- **Primera apertura tras actualizar la app:** se ve en Cuadrícula, igual que antes de actualizar (RF-38).
- **Sin notas:** el cambio de vista se acepta y se aplica a las notas que se creen después (RF-6).
- **Búsqueda sin resultados, error o carga:** se muestra el mismo mensaje en ambas vistas.
- **Nota sin categoría:** la fecha queda alineada a la derecha, sin etiqueta (RF-25, RF-26).
- **Nota sin contenido:** la tarjeta de lista muestra el título, la etiqueta si la hay y la fecha.
- **Título muy largo:** se recorta como en la cuadrícula.
- **Tarea más larga que una línea:** ocupa las líneas que necesite dentro del límite de cuatro (RF-18, RF-21).
- **Elegir en la hoja la vista vigente:** la hoja se cierra y no cambia nada en pantalla.
- **Girar la pantalla con la hoja abierta:** la hoja sigue abierta y la vista no cambia.
- **Girar la pantalla en Mis notas:** la vista vigente no cambia.
- **Fallo al guardar y cierre de la app:** al reabrirla vuelve la última vista guardada, o Cuadrícula si no hay ninguna (RF-37, RF-38).

## Fuera de alcance

- Vistas distintas de Cuadrícula y Lista, como una lista compacta sin vista previa.
- Elegir el número de columnas de la cuadrícula.
- Mostrar la etiqueta de categoría o las tareas seguidas en la vista Cuadrícula.
- Vistas distintas por categoría o por búsqueda.
- Conservar la nota exacta visible en pantalla al cambiar de vista.
- Marcar o desmarcar tareas desde la tarjeta de lista.
- Mostrar más de una imagen por tarjeta.
- Hacer funcionar las demás filas atenuadas de Ajustes.
- Sincronizar la vista entre dispositivos.

## Criterios de finalización

- Todos los RF con prueba en verde (unitarias e instrumentadas) y `./gradlew test` pasando.
- Demo manual en el emulador, en ambos temas:
  - Cambiar de vista desde Mis notas y desde Ajustes, y comprobar que ambos lugares coinciden.
  - Ver en lista notas con tareas, imagen, formato, sin categoría y fijadas.
  - Cambiar de vista con un filtro y con una búsqueda activos.
  - Cerrar la app por completo, volver a abrirla y comprobar que la vista se conserva.
- El selector y la tarjeta de lista comparados con las capturas de Figma, salvo la marca del selector.

## Dudas abiertas

Ninguna.
