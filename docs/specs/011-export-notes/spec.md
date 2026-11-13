# Spec 011 — Exportar notas

> **Estado:** implementada · **Fecha:** 2026-11-12 · **Diseño:** fila «Exportar notas» de Ajustes (Figma `JyeMmyDAEO1Y52QnwKwgGH`, spec 006). La pantalla de selección no tiene diseño en Figma: se construye con los componentes y el tema existentes de la app.

## Contexto y objetivo

Las notas solo se pueden leer dentro de la app. La spec 010 permite enviar una nota cada vez, como texto, pero no hay forma de sacar varias notas a la vez en un documento que se lea fuera de la app. Ajustes ya muestra la fila «Exportar notas» («Crea una copia de tus ideas»), pero atenuada.

**Objetivo:** que el usuario pueda elegir varias notas y guardarlas en su dispositivo como PDF, un archivo por nota, para leerlas o imprimirlas fuera de la app.

**Relación con otras specs:** esta spec sustituye en parte la RF-9 de la spec 006, porque la fila Exportar notas pasa a funcionar, y su línea de «Fuera de alcance» sobre Exportar notas. También sustituye la línea de la spec 010 «Exportar todas las notas».

## Usuarios / actores

- **Usuario que quiere leer sus notas fuera de la app:** en el ordenador, en otra app o en papel.
- **Usuario que quiere guardar una copia legible de algunas notas:** en Descargas, Drive u otra carpeta.
- **Selector de documentos del sistema:** decide dónde se guarda el archivo.

## Historias de usuario

- **H1.** Como usuario, quiero elegir qué notas exportar, para no sacar las que no me interesan.
- **H2.** Como usuario, quiero que cada nota sea un PDF aparte, para abrir o enviar solo la que necesito.
- **H3.** Como usuario, quiero que el PDF conserve el formato, las tareas y las imágenes, para que se vea como en la app.
- **H4.** Como usuario, quiero elegir dónde se guardan, para encontrarlas luego.

## Requisitos funcionales (criterios de aceptación en EARS)

### Entrada
- **RF-1:** EL SISTEMA mostrará habilitada la fila Exportar notas de Ajustes.
- **RF-2:** CUANDO el usuario toque la fila Exportar notas, EL SISTEMA abrirá la pantalla de selección de notas.
- **RF-3:** SI no hay ninguna nota, ENTONCES EL SISTEMA mostrará en la pantalla de selección el aviso «No hay notas para exportar».
- **RF-48:** SI no se pueden leer las notas, ENTONCES EL SISTEMA mostrará en la pantalla de selección el aviso «No se pudieron cargar las notas» en lugar de la lista.

### Selección
- **RF-4:** EL SISTEMA listará en la pantalla de selección todas las notas, en el mismo orden que Mis notas con el filtro «Todas».
- **RF-5:** EL SISTEMA mostrará en cada nota de la lista su título, o «Sin título» si no lo tiene.
- **RF-6:** CUANDO el usuario abra la pantalla de selección, EL SISTEMA mostrará todas las notas sin seleccionar.
- **RF-7:** CUANDO el usuario toque una nota de la lista, EL SISTEMA cambiará su estado entre seleccionada y no seleccionada.
- **RF-8:** CUANDO el usuario toque «Seleccionar todas», EL SISTEMA seleccionará todas las notas.
- **RF-9:** MIENTRAS todas las notas estén seleccionadas, EL SISTEMA mostrará «Quitar selección» en lugar de «Seleccionar todas».
- **RF-10:** CUANDO el usuario toque «Quitar selección», EL SISTEMA dejará todas las notas sin seleccionar.
- **RF-11:** EL SISTEMA mostrará el número de notas seleccionadas.
- **RF-12:** MIENTRAS no haya ninguna nota seleccionada, EL SISTEMA mostrará el botón Exportar deshabilitado.

### Guardar
- **RF-13:** CUANDO el usuario toque Exportar, EL SISTEMA abrirá el selector de documentos del sistema para crear un archivo .zip después de generar los PDF.
- **RF-14:** EL SISTEMA propondrá como nombre del .zip «Notas AAAA-MM-DD.zip», con la fecha del día.
- **RF-15:** CUANDO el usuario confirme el selector de documentos, EL SISTEMA guardará un .zip con un PDF por cada nota seleccionada.
- **RF-16:** SI el usuario cierra el selector de documentos sin confirmar, ENTONCES EL SISTEMA no creará ningún archivo.
- **RF-17:** SI el usuario cierra el selector de documentos sin confirmar, ENTONCES EL SISTEMA mantendrá la misma selección de notas.
- **RF-18:** MIENTRAS se exportan las notas (se generan los PDF o se guarda el .zip), EL SISTEMA mostrará un indicador de progreso.
- **RF-19:** MIENTRAS se exportan las notas, EL SISTEMA no permitirá cambiar la selección.
- **RF-20:** CUANDO termine de guardar el .zip, EL SISTEMA mostrará el aviso «N notas exportadas» («1 nota exportada» con una sola), con N igual al número de PDF del .zip.
- **RF-21:** CUANDO termine de guardar el .zip, EL SISTEMA volverá a Ajustes.
- **RF-22:** SI no se puede escribir el .zip, ENTONCES EL SISTEMA mostrará el aviso «No se pudieron exportar las notas».
- **RF-23:** SI no se puede escribir el .zip, ENTONCES EL SISTEMA no dejará en el destino un archivo incompleto.
- **RF-24:** CUANDO el usuario exporte notas, EL SISTEMA no modificará ninguna nota ni su fecha de última edición.
- **RF-25:** SI el usuario gira la pantalla mientras se generan los PDF, ENTONCES EL SISTEMA continuará la exportación sin perder el progreso.
- **RF-26:** SI la app se cierra mientras se generan los PDF, ENTONCES EL SISTEMA no dejará en el destino un archivo incompleto.

### Nombres de los PDF
- **RF-27:** EL SISTEMA nombrará cada PDF con el título de la nota y la extensión .pdf.
- **RF-28:** SI la nota no tiene título, ENTONCES EL SISTEMA nombrará su PDF «Sin título.pdf».
- **RF-29:** SI el título contiene caracteres no permitidos en nombres de archivo (`/ \ : * ? " < > |`), ENTONCES EL SISTEMA los sustituirá por «_».
- **RF-30:** SI el título tiene más de 100 caracteres, ENTONCES EL SISTEMA usará solo los 100 primeros en el nombre del PDF.
- **RF-31:** SI dos PDF del mismo .zip tendrían el mismo nombre (sin distinguir mayúsculas), ENTONCES EL SISTEMA añadirá « (2)», « (3)», … a los siguientes, en el orden de la lista.

### Contenido de cada PDF
- **RF-32:** EL SISTEMA mostrará el título de la nota como encabezado del PDF.
- **RF-33:** SI la nota no tiene título, ENTONCES EL SISTEMA no mostrará encabezado.
- **RF-34:** DONDE la nota tenga categoría, EL SISTEMA mostrará su nombre (Trabajo o Personal) bajo el encabezado.
- **RF-35:** EL SISTEMA mostrará la fecha de última edición de la nota bajo el encabezado, con día, mes abreviado, año y hora, como «12 nov 2026, 10:30». La fecha es siempre absoluta, nunca «Hoy» ni «Ayer», porque el PDF se lee después.
- **RF-36:** EL SISTEMA mostrará el contenido de la nota con su negrita, subrayado y tachado.
- **RF-37:** EL SISTEMA mostrará cada tarea con una casilla marcada si está hecha y vacía si está pendiente.
- **RF-38:** DONDE la nota tenga imágenes, EL SISTEMA las mostrará después del contenido, en el mismo orden que en el editor.
- **RF-39:** EL SISTEMA ajustará cada imagen al ancho útil de la página sin deformarla.
- **RF-40:** SI una imagen es más alta que la página, ENTONCES EL SISTEMA la reducirá hasta que quepa en una página.
- **RF-41:** SI el contenido no cabe en una página, ENTONCES EL SISTEMA lo continuará en las páginas siguientes.
- **RF-42:** EL SISTEMA generará los PDF con fondo blanco y texto negro, sea cual sea el tema de la app o el color de la nota.
- **RF-43:** EL SISTEMA generará páginas de tamaño A4 (210 × 297 mm).
- **RF-44:** SI una imagen de la nota no se puede leer, ENTONCES EL SISTEMA generará el PDF sin ella.
- **RF-45:** SI se omitió alguna imagen, ENTONCES EL SISTEMA mostrará, al terminar, el aviso «Algunas imágenes no se pudieron incluir».

### Accesibilidad
- **RF-46:** EL SISTEMA anunciará con el lector de pantalla si cada nota de la lista está seleccionada o no.
- **RF-47:** EL SISTEMA dará una etiqueta accesible a «Seleccionar todas», «Quitar selección» y Exportar.

## Requisitos no funcionales

- **Privacidad:** los archivos solo se escriben en el destino que elige el usuario. La app no envía nada a ningún servidor.
- **Idioma:** todos los textos nuevos en español.
- **Temas:** la pantalla de selección se ve correctamente en tema claro y oscuro. Los PDF no dependen del tema (RF-42).

## Casos límite

- Una sola nota seleccionada: se guarda igualmente un .zip con un PDF (RF-15).
- Nota solo con imágenes, sin título ni contenido: PDF «Sin título.pdf» con la fecha y las imágenes.
- Nota solo con título: PDF con el encabezado y la fecha.
- Títulos iguales, o iguales salvo mayúsculas: sufijos « (2)», « (3)» (RF-31).
- Título que solo tiene caracteres no permitidos, como «///»: nombre «___.pdf» (RF-29).
- Título con emojis o tildes: se conservan en el nombre y en el PDF.
- Tareas vacías: casilla sin texto.
- Imagen borrada del almacenamiento de la app: se omite con aviso (RF-44, RF-45).
- Imagen muy alta, como una captura de pantalla larga: se reduce a una página (RF-40).
- Girar la pantalla mientras se exporta: la exportación continúa (RF-25).
- Cerrar la app mientras se exporta: la exportación se pierde, pero no queda un archivo incompleto (RF-26).
- Destino lleno o sin permiso de escritura: aviso de error (RF-22, RF-23).
- Volver atrás desde la selección: se descarta la selección sin exportar.

## Fuera de alcance

- Importar notas o restaurar una copia de seguridad.
- Otros formatos (texto, Markdown, HTML) o un solo PDF con todas las notas.
- Exportar con el color de la nota como fondo.
- Seleccionar por categoría o con búsqueda en la pantalla de selección.
- Exportar desde Mis notas o desde el editor.
- Enviar los PDF por el menú de compartir del sistema.
- Proteger el .zip o los PDF con contraseña.
- Elegir el tamaño de página, o usar Carta u otro tamaño según la región.
- Exportaciones programadas o automáticas.
- Continuar la exportación en segundo plano después de cerrar la app.
- Papelera y Ayuda y comentarios, que siguen atenuadas.

## Criterios de finalización

- Todos los RF con prueba en verde (unitarias e instrumentadas) y `./gradlew test` pasando.
- Demo manual en el emulador, en ambos temas:
  - exportar varias notas con formato, tareas, categoría e imágenes, y abrir los PDF;
  - títulos repetidos y una nota sin título;
  - cancelar el selector de documentos.
- Comparación con Figma de la fila de Ajustes. La pantalla de selección, sin diseño, se revisa contigo en la demo.
- Specs 006 y 010 actualizadas para que no contradigan esta.

## Dudas abiertas

Ninguna.
