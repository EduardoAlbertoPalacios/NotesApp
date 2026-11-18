# Spec 013 — Ayuda y comentarios

> **Estado:** implementada · **Fecha:** 2026-11-18 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`, «Ayuda y comentarios · Oscuro» (`102:68`) y «Ayuda y comentarios · Claro» (`102:2`).

## Contexto y objetivo

La sección «Acerca de» de Ajustes muestra la fila «Ayuda y comentarios» («Nos encanta escucharte»), pero atenuada y sin acción: es la última fila de Ajustes que no funciona. Quien tiene una duda sobre la app (dónde están sus notas, cómo recuperar una eliminada, cómo cambiar un color) no tiene dónde consultarla.

**Objetivo:** que la fila abra una pantalla de ayuda con preguntas frecuentes que se pueden buscar y desplegar, y que deje preparado el acceso para valorar la app en Google Play cuando esté publicada.

**Relación con otras specs:**
- Sustituye la parte de la RF-9 de la spec 006 que deja atenuada la fila Ayuda y comentarios, y su línea de «Fuera de alcance» sobre ella.
- Las respuestas describen funciones de las specs 002 a 012 tal como funcionan hoy.

## Usuarios / actores

- **Usuario con una duda puntual:** busca una palabra y lee la respuesta.
- **Usuario que explora la app:** recorre las preguntas para descubrir funciones.

## Historias de usuario

- **H1.** Como usuario, quiero abrir la ayuda desde Ajustes, para resolver dudas sin salir de la app.
- **H2.** Como usuario, quiero buscar entre las preguntas, para encontrar la mía sin leerlas todas.
- **H3.** Como usuario, quiero desplegar solo las preguntas que me interesan, para leer sus respuestas sin ruido.

## Requisitos funcionales (criterios de aceptación en EARS)

### Entrar en la ayuda
- **RF-1:** EL SISTEMA mostrará habilitada la fila «Ayuda y comentarios» de Ajustes, con su descripción «Nos encanta escucharte».
- **RF-2:** CUANDO el usuario toque la fila «Ayuda y comentarios», EL SISTEMA abrirá la pantalla «Ayuda y comentarios».
- **RF-3:** CUANDO el usuario toque la flecha de volver o use Atrás del sistema en la ayuda, EL SISTEMA volverá a Ajustes.

### Pantalla
- **RF-4:** EL SISTEMA mostrará en la barra superior la flecha de volver y el título «Ayuda y comentarios».
- **RF-5:** EL SISTEMA mostrará la cabecera «¿En qué podemos ayudarte?» con el texto «Un poco de ayuda para que tus ideas sigan fluyendo.».
- **RF-6:** EL SISTEMA mostrará un campo de búsqueda con el texto de ayuda «Buscar una pregunta».
- **RF-7:** EL SISTEMA mostrará la sección «Preguntas frecuentes» con las preguntas de la tabla [Preguntas frecuentes](#preguntas-frecuentes), en ese orden.
- **RF-8:** EL SISTEMA mostrará la sección «Valora la app» con la fila «Calificar en Google Play» y su descripción «Comparte tu experiencia con la app».
- **RF-9:** EL SISTEMA mostrará debajo de la fila «Calificar en Google Play» el texto «Se abrirá Google Play para dejar tu reseña.».
- **RF-10:** EL SISTEMA seguirá el tema elegido en Ajustes (Claro u Oscuro), según el diseño de cada tema.

### Desplegar preguntas
- **RF-11:** CUANDO el usuario abra la ayuda, EL SISTEMA mostrará todas las preguntas cerradas, sin respuesta visible.
- **RF-12:** CUANDO el usuario toque una pregunta cerrada, EL SISTEMA mostrará su respuesta debajo de ella.
- **RF-13:** CUANDO el usuario toque una pregunta abierta, EL SISTEMA ocultará su respuesta.
- **RF-14:** EL SISTEMA permitirá tener varias preguntas abiertas a la vez.
- **RF-15:** EL SISTEMA mostrará en cada pregunta una flecha hacia la derecha (›) si está cerrada y una flecha hacia arriba (˄) si está abierta.
- **RF-16:** EL SISTEMA anunciará a los lectores de pantalla si cada pregunta está abierta o cerrada.

### Buscar
- **RF-17:** CUANDO el usuario escriba en el campo de búsqueda, EL SISTEMA mostrará solo las preguntas cuyo texto o cuya respuesta contengan lo escrito.
- **RF-18:** EL SISTEMA ignorará en la búsqueda las mayúsculas, las tildes y los espacios al principio y al final, con las mismas reglas que la búsqueda de notas (spec 001).
- **RF-19:** MIENTRAS el campo de búsqueda esté vacío o solo tenga espacios, EL SISTEMA mostrará todas las preguntas.
- **RF-20:** SI ninguna pregunta coincide, ENTONCES EL SISTEMA mostrará el aviso «No hay preguntas que coincidan con «texto»» en lugar de la lista.
- **RF-21:** MIENTRAS el campo de búsqueda tenga texto, EL SISTEMA mostrará un botón «Limpiar búsqueda».
- **RF-22:** CUANDO el usuario toque «Limpiar búsqueda», EL SISTEMA vaciará el campo y mostrará todas las preguntas.
- **RF-23:** EL SISTEMA conservará abiertas o cerradas las preguntas al filtrar y al limpiar la búsqueda.
- **RF-24:** MIENTRAS el campo de búsqueda tenga texto, EL SISTEMA seguirá mostrando la sección «Valora la app» debajo de los resultados o del aviso sin resultados.

### Valorar la app
- **RF-25:** EL SISTEMA mostrará deshabilitada la fila «Calificar en Google Play», atenuada como las filas deshabilitadas de Ajustes.
- **RF-26:** EL SISTEMA mostrará atenuado el texto «Se abrirá Google Play para dejar tu reseña.».
- **RF-27:** CUANDO el usuario toque la fila «Calificar en Google Play», EL SISTEMA no hará nada.
- **RF-28:** EL SISTEMA anunciará a los lectores de pantalla la fila «Calificar en Google Play» como deshabilitada.

### Conservar el estado
- **RF-29:** CUANDO el usuario gire el dispositivo, EL SISTEMA conservará el texto de búsqueda, las preguntas abiertas y la posición de desplazamiento.
- **RF-30:** CUANDO el usuario salga de la ayuda y vuelva a entrar, EL SISTEMA mostrará el campo de búsqueda vacío y todas las preguntas cerradas.

## Preguntas frecuentes

Contenido que muestra la pantalla (RF-7). Las tres primeras son las del diseño. Las respuestas describen la app tal como funciona hoy.

| # | Pregunta | Respuesta |
|---|---|---|
| 1 | ¿Dónde se guardan mis notas? | Tus notas se guardan en este dispositivo. Para tener una copia, ve a Ajustes → Exportar notas. |
| 2 | ¿Cómo recupero una nota eliminada? | Al eliminar una nota, pasa a la papelera. Toca «Deshacer» en el aviso que aparece, o ve a Ajustes → Papelera, toca la nota y elige «Restaurar». Las notas de la papelera se eliminan definitivamente cuando se cumple el plazo de «Vaciar automáticamente» (30 días si no lo cambias). |
| 3 | ¿Cómo cambio el color de mis notas? | En la nota, toca «Color de la nota» en la barra inferior, elige un color y toca «Aplicar color». Para elegir el color de las notas nuevas, ve a Ajustes → Color predeterminado. |
| 4 | ¿Cómo organizo mis notas por categoría? | En la nota, elige Trabajo o Personal en la sección «Categoría». En Mis notas, usa los filtros Todas, Trabajo y Personal. |
| 5 | ¿Cómo fijo una nota? | En la nota, toca «Fijar nota» en la barra superior. Para quitarla, toca «Dejar de fijar nota». |
| 6 | ¿Cómo creo una lista de tareas? | En la nota, toca «Lista de tareas» en la barra inferior. Toca la casilla de una tarea para marcarla como hecha. |
| 7 | ¿Cómo doy formato al texto? | En la nota, toca «Formato de texto» en la barra inferior y elige negrita, subrayado o tachado. |
| 8 | ¿Puedo deshacer un cambio? | Sí. En la nota, toca «Deshacer» en la barra inferior. Los cambios que puedes deshacer se pierden al salir de la nota. |
| 9 | ¿Se guardan mis cambios automáticamente? | Sí, mientras el guardado automático esté activado, que es lo predeterminado. Si lo desactivas en Ajustes → Guardado automático, toca «Guardar» antes de salir de la nota. |
| 10 | ¿Cómo comparto una nota? | En la nota, toca «Compartir nota» en la barra superior y elige la app con la que compartirla. Se comparten el título, el texto y las imágenes. |
| 11 | ¿Cómo exporto mis notas? | Ve a Ajustes → Exportar notas, elige las notas y toca «Exportar». Se guarda un archivo .zip con un PDF por nota en la carpeta que elijas. |
| 12 | ¿Cómo cambio entre tema claro y oscuro? | Ve a Ajustes → Tema y elige Claro u Oscuro. |

## Requisitos no funcionales

- **Sin red:** la pantalla funciona sin conexión; preguntas y respuestas van dentro de la app.
- **Idioma:** todos los textos en español en los recursos de la app.
- **Accesibilidad:** cada pregunta y la fila de Google Play tienen etiqueta accesible y su estado (abierta/cerrada, deshabilitada). Contraste mínimo 4,5:1 en ambos temas, salvo los textos atenuados de elementos deshabilitados.
- **Tamaño de pantalla:** con las 12 preguntas abiertas, la pantalla se desplaza sin cortar contenido en un teléfono de 360 dp de ancho.

## Casos límite

- **CL-1.** Búsqueda que coincide solo con la respuesta (por ejemplo «papelera»): la pregunta aparece aunque su texto no contenga la palabra (RF-17), cerrada si lo estaba (RF-23).
- **CL-2.** Búsqueda con «ñ»: «n» no encuentra «ñ», igual que en la búsqueda de notas (RF-18).
- **CL-3.** Pregunta abierta que deja de coincidir y vuelve a coincidir: reaparece abierta (RF-23).
- **CL-4.** Texto de búsqueda muy largo: el campo no limita la longitud; simplemente no hay resultados (RF-20).
- **CL-5.** Tamaño de letra del sistema al 200 %: preguntas y respuestas se ajustan en varias líneas sin cortarse.
- **CL-6.** Una respuesta deja de ser cierta porque cambia una función: se actualiza en la spec que cambia esa función (principio 2).

## Fuera de alcance

- Enviar comentarios (correo, formulario o similar): la pantalla no tiene esa opción en el diseño.
- Abrir Google Play o la reseña dentro de la app: llegará con otra spec cuando la app esté publicada.
- Enlaces desde las respuestas a las pantallas que mencionan.
- Preguntas descargadas de internet o editables por el usuario.
- Otras filas en «Acerca de» (licencias, política de privacidad) y el pie con la versión, que no cambia.
- Traducciones a otros idiomas.

## Criterios de finalización

- Todos los RF con pruebas en verde: unitarias de las reglas de búsqueda y del ViewModel, y de UI de la pantalla en ambos temas.
- La fila de Ajustes abre la ayuda y la prueba de UI de Ajustes lo cubre.
- Demo manual en el emulador, en ambos temas: buscar con y sin tildes, sin resultados, limpiar, abrir varias preguntas, girar y tocar la fila deshabilitada.
- Spec 006 actualizada (RF-9 y «Fuera de alcance»).
- Revisas conmigo la pantalla frente al diseño y el texto de las 12 respuestas.

## Dudas abiertas

Ninguna. RF-24 resuelta: la sección «Valora la app» sigue visible durante la búsqueda.
