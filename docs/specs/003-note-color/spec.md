# Spec 003 — Color de la nota

> **Estado:** implementada · **Fecha:** 2026-10-21 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`, nodo `4:1710` («Color · Personalizar nota»)

## Contexto y objetivo

Cada nota ya tiene un color que se ve en su tarjeta de Mis notas, pero el usuario no puede elegirlo: las notas nuevas salen siempre en Cielo y el botón «Color de la nota» de la barra del editor está deshabilitado. Sin poder cambiarlo, el color no ayuda a distinguir ni agrupar las notas a simple vista.

**Objetivo:** que el usuario pueda elegir el color de cada nota desde el editor con el selector del diseño (ocho opciones, incluida «Sin color»), y que ese color se vea en su tarjeta y en el fondo del editor.

## Usuarios / actores

- **Usuario con muchas notas:** quiere distinguirlas por color en la cuadrícula para encontrar antes la que busca.
- **Usuario que prefiere una cuadrícula sobria:** quiere notas sin color.

## Historias de usuario

- **H1.** Como usuario, quiero elegir el color de una nota desde el editor, para reconocerla de un vistazo en Mis notas.
- **H2.** Como usuario, quiero ver cómo quedará la tarjeta antes de aplicar el color, para no tener que probar varios.
- **H3.** Como usuario, quiero poder cerrar el selector sin cambiar nada, para no aplicar un color por accidente.
- **H4.** Como usuario, quiero poder dejar una nota sin color, para que no destaque.

## Requisitos funcionales (criterios de aceptación en EARS)

### Abrir el selector
- **RF-1:** EL SISTEMA habilitará el botón «Color de la nota» de la barra del editor.
- **RF-2:** CUANDO el usuario toque el botón «Color de la nota», EL SISTEMA mostrará una hoja inferior titulada «Color de la nota» con la descripción «Dale a cada idea su propio color.».
- **RF-3:** EL SISTEMA mostrará en la hoja ocho opciones, en este orden: Sin color, Cielo, Rosa, Vainilla, Lavanda, Menta, Melocotón y Arena.
- **RF-4:** CUANDO se abra la hoja, EL SISTEMA marcará como elegida la opción del color actual de la nota.

### Elegir un color
- **RF-5:** CUANDO el usuario toque una opción, EL SISTEMA la marcará como elegida con una marca de verificación y su nombre resaltado.
- **RF-6:** CUANDO el usuario toque una opción, EL SISTEMA quitará la marca de la opción elegida antes.
- **RF-7:** MIENTRAS la hoja esté abierta, EL SISTEMA mostrará una vista previa de la tarjeta con el color elegido.
- **RF-8:** MIENTRAS la hoja esté abierta, EL SISTEMA mostrará en la vista previa el título de la nota.
- **RF-9:** SI la nota no tiene título, ENTONCES EL SISTEMA mostrará «Sin título» en la vista previa.
- **RF-10:** MIENTRAS la hoja esté abierta, EL SISTEMA mostrará bajo el título de la vista previa el texto «Así se verá en tus notas · <nombre del color elegido>».
- **RF-11:** MIENTRAS la hoja esté abierta, EL SISTEMA mantendrá el color de la nota sin cambios hasta que el usuario pulse «Aplicar color».

### Aplicar o descartar
- **RF-12:** CUANDO el usuario pulse «Aplicar color», EL SISTEMA asignará a la nota el color elegido.
- **RF-13:** CUANDO el usuario pulse «Aplicar color», EL SISTEMA cerrará la hoja.
- **RF-14:** CUANDO el usuario cierre la hoja sin pulsar «Aplicar color» (con la X, deslizándola hacia abajo, tocando fuera de ella o con el botón atrás del sistema), EL SISTEMA conservará el color que la nota tenía.
- **RF-15:** CUANDO el usuario vuelva a abrir la hoja tras cerrarla sin aplicar, EL SISTEMA marcará como elegido el color actual de la nota, no el que se eligió y se descartó.

### Guardado
- **RF-16:** CUANDO el usuario aplique un color distinto del actual a una nota guardada, EL SISTEMA guardará la nota igual que con un cambio de texto.
- **RF-17:** CUANDO el usuario aplique un color distinto del actual a una nota guardada, EL SISTEMA actualizará su fecha de última edición.
- **RF-18:** CUANDO el usuario aplique el mismo color que la nota ya tenía, EL SISTEMA no cambiará su fecha de última edición.
- **RF-19:** SI el usuario aplica un color a una nota nueva sin título ni contenido, ENTONCES EL SISTEMA no guardará la nota hasta que el usuario escriba algo.
- **RF-20:** CUANDO el usuario escriba en una nota nueva a la que ya aplicó un color, EL SISTEMA guardará la nota con ese color.
- **RF-21:** CUANDO el usuario vuelva a abrir una nota, EL SISTEMA mostrará el color con el que se guardó.

### Dónde se ve el color
- **RF-22:** EL SISTEMA creará las notas nuevas con el color Cielo.
- **RF-23:** EL SISTEMA pintará cada tarjeta de Mis notas con el color de su nota.
- **RF-24:** DONDE la nota tenga un color distinto de «Sin color», EL SISTEMA pintará el fondo del editor con ese color.
- **RF-25:** DONDE la nota tenga un color distinto de «Sin color», EL SISTEMA mostrará en oscuro los textos e íconos del editor que van sobre ese fondo.
- **RF-26:** DONDE la nota tenga «Sin color», EL SISTEMA pintará su tarjeta con el gris oscuro de las superficies de la app y su texto en claro.
- **RF-27:** DONDE la nota tenga «Sin color», EL SISTEMA mostrará el editor con el fondo negro actual.
- **RF-28:** CUANDO el usuario aplique un color, EL SISTEMA actualizará el fondo del editor sin salir de la nota.

### Notas existentes
- **RF-29:** EL SISTEMA mostrará cada nota creada antes de esta funcionalidad con el mismo color que se veía hasta ahora.

### Accesibilidad
- **RF-30:** EL SISTEMA dará a cada opción de color una etiqueta accesible con el nombre del color.
- **RF-31:** EL SISTEMA anunciará a los lectores de pantalla si cada opción está elegida o no.
- **RF-32:** EL SISTEMA dará al botón de cerrar la hoja una etiqueta accesible.

## Requisitos no funcionales

- **Aspecto:** la hoja, la paleta, la vista previa y el botón siguen el diseño de Figma `4:1710` (tamaños, espaciados, radios, colores y tipografía).
- **Idioma:** todos los textos nuevos en español, en `strings.xml`.
- **Contraste:** el texto oscuro sobre cada color de nota y el texto claro sobre «Sin color» cumplen un contraste mínimo de 4,5:1.

## Casos límite

- **Título muy largo:** la vista previa lo muestra en una sola línea, recortado con puntos suspensivos.
- **Abrir y cerrar varias veces sin aplicar:** el color de la nota no cambia nunca (RF-14, RF-15).
- **Aplicar el color que ya tenía:** se cierra la hoja y la fecha de edición no cambia (RF-18).
- **Nota nueva vacía con color aplicado y luego abandonada:** no se guarda nada (RF-19).
- **Error al guardar el color:** se informa como cualquier otro error de guardado del editor y el color en pantalla no se pierde mientras la nota siga abierta.
- **Girar la pantalla o pasar a segundo plano con la hoja abierta:** la hoja sigue abierta con la misma opción elegida.

## Fuera de alcance

- Colores personalizados o elegidos con un selector libre (solo las ocho opciones del diseño).
- Filtrar u ordenar Mis notas por color.
- Cambiar el color de varias notas a la vez o desde el listado.
- Aplicar colores al texto del contenido (solo a la nota entera).
- Tema claro o adaptar el resto de la app al color de la nota (solo el editor y la tarjeta).
- Elegir el color por defecto de las notas nuevas (siempre Cielo).

## Criterios de finalización

- Todos los RF con prueba en verde (unitarias e instrumentadas) y `./gradlew test` pasando.
- Migración de base de datos probada si cambia el esquema, sin pérdida de notas ni de su color.
- Demo manual en el emulador:
  - Abrir el selector, elegir un color, aplicarlo y comprobar el editor y la tarjeta.
  - Cerrar sin aplicar por cada una de las cuatro vías.
  - Usar «Sin color».
  - Aplicar un color a una nota nueva vacía y salir.
  - Verificar que las notas existentes conservan su color.
- La hoja comparada con la captura de Figma.

## Dudas abiertas

Ninguna.
