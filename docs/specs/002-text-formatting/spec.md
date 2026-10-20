# Spec 002 — Formato de texto en las notas

> **Estado:** implementada · **Fecha:** 2026-10-19

## Contexto y objetivo

El editor muestra en su barra el botón «Formato de texto» («Aa»), pero está deshabilitado: hoy el contenido de una nota es solo texto plano. Sin formato, el usuario no puede destacar lo importante (una fecha, un acuerdo) ni marcar lo que ya no aplica sin borrarlo.

**Objetivo:** que el usuario pueda aplicar negrita, subrayado y tachado al contenido de sus notas, y que ese formato se conserve y se vea también en la cuadrícula de Mis notas.

## Usuarios / actores

- **Usuario que toma notas de reuniones o tareas:** quiere destacar decisiones o fechas con negrita o subrayado.
- **Usuario que lleva listas en texto:** quiere tachar lo que ya hizo sin perderlo de vista.

## Historias de usuario

- **H1.** Como usuario, quiero seleccionar un fragmento del contenido y ponerlo en negrita, subrayado o tachado, para destacarlo o marcarlo como hecho.
- **H2.** Como usuario, quiero activar un formato antes de escribir y que lo siguiente que escriba salga con ese formato, para no tener que volver atrás a seleccionarlo.
- **H3.** Como usuario, quiero ver en la cuadrícula de Mis notas el formato que apliqué, para reconocer de un vistazo lo importante de cada nota.
- **H4.** Como usuario, quiero que el formato se guarde con la nota, para encontrarlo igual cuando la vuelva a abrir.

## Requisitos funcionales (criterios de aceptación en EARS)

### Menú de formato
- **RF-1:** EL SISTEMA habilitará el botón «Formato de texto» de la barra del editor.
- **RF-2:** CUANDO el usuario toque el botón «Formato de texto», EL SISTEMA mostrará un menú emergente con las opciones Negrita, Subrayado y Tachado.
- **RF-3:** CUANDO el usuario elija una opción del menú, EL SISTEMA cerrará el menú.
- **RF-4:** CUANDO el usuario toque fuera del menú abierto, EL SISTEMA cerrará el menú sin cambiar el formato.
- **RF-5:** MIENTRAS el menú esté abierto, EL SISTEMA marcará como activa cada opción que ya esté aplicada al texto seleccionado o, sin selección, a lo que se escribirá a continuación.
- **RF-6:** MIENTRAS el cursor esté en el título de la nota, EL SISTEMA mostrará deshabilitado el botón «Formato de texto».

### Aplicar formato a una selección
- **RF-7:** CUANDO el usuario elija un formato con un fragmento del contenido seleccionado que no lo tiene completo, EL SISTEMA aplicará ese formato a todo el fragmento.
- **RF-8:** CUANDO el usuario elija un formato con un fragmento del contenido seleccionado que ya lo tiene completo, EL SISTEMA quitará ese formato del fragmento.
- **RF-9:** CUANDO el usuario aplique o quite un formato a una selección, EL SISTEMA conservará los demás formatos de ese fragmento (por ejemplo, un texto en negrita al que se añade subrayado queda en negrita y subrayado).
- **RF-10:** CUANDO el usuario aplique o quite un formato a una selección, EL SISTEMA mantendrá la misma selección.

### Formato activo al escribir
- **RF-11:** CUANDO el usuario elija un formato sin texto seleccionado, EL SISTEMA activará ese formato para el texto que escriba a continuación.
- **RF-12:** MIENTRAS un formato esté activo, EL SISTEMA aplicará ese formato a cada carácter que el usuario escriba.
- **RF-13:** CUANDO el usuario elija sin selección un formato que ya está activo, EL SISTEMA lo desactivará para el texto que escriba a continuación.
- **RF-14:** CUANDO el usuario mueva el cursor a otra posición del contenido, EL SISTEMA tomará como formatos activos los del carácter inmediatamente anterior al cursor.

### Alcance del formato
- **RF-15:** EL SISTEMA permitirá aplicar formato solo al contenido de la nota.
- **RF-16:** EL SISTEMA mostrará siempre el título de la nota en texto plano.
- **RF-17:** CUANDO el usuario pegue texto en el contenido, EL SISTEMA lo insertará como texto plano, descartando cualquier formato de origen.
- **RF-18:** CUANDO el usuario pegue texto mientras haya formatos activos, EL SISTEMA aplicará los formatos activos al texto pegado.

### Guardado
- **RF-19:** CUANDO el usuario aplique o quite un formato, EL SISTEMA guardará la nota igual que con un cambio de texto.
- **RF-20:** CUANDO el usuario aplique o quite un formato, EL SISTEMA actualizará la fecha de última edición de la nota.
- **RF-21:** CUANDO el usuario vuelva a abrir una nota con formato, EL SISTEMA mostrará el contenido con el mismo formato con el que se guardó.
- **RF-22:** EL SISTEMA mostrará las notas creadas antes de esta funcionalidad igual que hasta ahora, como texto sin formato.

### Cuadrícula de Mis notas
- **RF-23:** EL SISTEMA mostrará en la vista previa de cada tarjeta el contenido con su negrita, subrayado y tachado.
- **RF-24:** SI la vista previa de la tarjeta recorta el contenido, ENTONCES EL SISTEMA conservará el formato del fragmento visible.

### Búsqueda
- **RF-25:** EL SISTEMA buscará en el contenido de las notas sin tener en cuenta el formato (por ejemplo, «reunión» encuentra «**reu**nión»).

## Requisitos no funcionales

- **RNF-1. Datos.** Activar esta funcionalidad no debe perder ni alterar el contenido de ninguna nota existente.
- **RNF-2. Accesibilidad.** Cada opción del menú tiene una etiqueta accesible y los lectores de pantalla anuncian si está activa o no.
- **RNF-3. Accesibilidad.** Ningún formato depende solo del color para distinguirse.
- **RNF-4. Apariencia.** El menú de formato sigue el estilo actual de la app (colores, tipografía e iconos del tema); no tiene un diseño propio en Figma.
- **RNF-5. Idioma.** Los textos nuevos del menú (Negrita, Subrayado, Tachado) están en español.

## Casos límite

- **CL-1.** Selección que mezcla texto con y sin un formato: elegir ese formato lo aplica a todo el fragmento (RF-7).
- **CL-2.** Selección que incluye saltos de línea: el formato se aplica a todas las líneas seleccionadas.
- **CL-3.** Borrar todo el texto con formato de una nota: la nota queda vacía y se aplican las reglas actuales de nota vacía.
- **CL-4.** Aplicar los tres formatos a la vez al mismo fragmento: se muestran los tres combinados.
- **CL-5.** Abrir el menú sin texto en el contenido: se puede activar un formato para empezar a escribir con él (RF-11).
- **CL-6.** Elegir un formato y no escribir nada antes de salir del editor: la nota no cambia y su fecha de última edición tampoco.
- **CL-7.** Pegar un texto con formato de otra app: se pega como texto plano (RF-17).
- **CL-8.** Pegar con Negrita activa un texto con formato de otra app: llega en negrita y sin ningún otro formato de origen (RF-17, RF-18).
- **CL-9.** Activar Negrita sin escribir y tocar otra parte del contenido: la Negrita deja de estar activa (RF-14).

## Fuera de alcance

- Cursiva, títulos o tamaños de letra, colores de texto, resaltado, enlaces y listas con viñetas o numeradas.
- Formato en el título de la nota.
- Conservar el formato al pegar texto de otras apps.
- Copiar texto con formato desde la nota hacia otras apps.
- Atajos de teclado para los formatos.
- Deshacer y rehacer (el botón «Deshacer» sigue deshabilitado).
- Compartir o exportar notas con formato.
- Mostrar el formato en otros lugares que no sean el editor y la cuadrícula de Mis notas.

## Criterios de finalización

- Todos los requisitos RF-1 a RF-25 tienen pruebas que los verifican y pasan.
- Los casos límite CL-1 a CL-9 tienen pruebas.
- Las pruebas del proyecto pasan, según la constitución del proyecto.
- Se comprueba a mano en un dispositivo o emulador:
  - aplicar y quitar cada formato a una selección;
  - activar un formato y escribir con él;
  - cerrar y reabrir la nota y ver el mismo formato;
  - ver el formato en la tarjeta de Mis notas;
  - pegar un texto con formato y ver que llega plano.
- Una nota creada antes de esta funcionalidad se abre y se muestra igual que antes.

## Dudas abiertas

Ninguna por ahora.
