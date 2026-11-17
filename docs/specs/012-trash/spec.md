# Spec 012 — Papelera de notas eliminadas

> **Estado:** implementada · **Fecha:** 2026-11-17 · **Diseño:** sin diseño en Figma. La papelera se construye con los componentes de la lista de notas, y la elección del plazo con una hoja inferior como la de Tema (spec 006).

## Contexto y objetivo

Hoy, al eliminar una nota desde el editor, la app pide confirmación («¿Eliminar esta nota? No podrás recuperarla.») y la borra para siempre, con sus imágenes. Un toque equivocado o un arrepentimiento posterior hacen perder la nota sin remedio. Ajustes ya muestra la fila Papelera, pero atenuada y con el texto «Próximamente».

**Objetivo:** que eliminar una nota sea reversible. La nota pasa a una papelera, donde se puede restaurar o eliminar definitivamente, y se borra sola cuando se cumple el plazo que el usuario elige en la propia papelera.

**Relación con otras specs:**
- Sustituye en parte la RF-9 de la spec 006, porque la fila Papelera pasa a funcionar, y también sus RF-14 y RF-15 y su línea de «Fuera de alcance» sobre la Papelera.
- Sustituye la línea de «Fuera de alcance» de la spec 011 sobre la Papelera.
- Cambia el caso límite de la spec 009 «Eliminar una nota con cambios sin guardar», porque ya no hay confirmación al eliminar.

## Usuarios / actores

- **Usuario que elimina una nota por error:** quiere deshacerlo enseguida.
- **Usuario que se arrepiente días después:** quiere recuperarla desde la papelera.
- **Usuario que quiere liberar espacio o borrar algo para siempre:** quiere eliminarla definitivamente.

## Historias de usuario

- **H1.** Como usuario, quiero deshacer una eliminación al momento, para corregir un toque equivocado sin buscar la nota.
- **H2.** Como usuario, quiero restaurar una nota eliminada, para recuperarla tal como estaba.
- **H3.** Como usuario, quiero eliminar notas definitivamente, una a una o todas a la vez, para que no queden en el dispositivo.
- **H4.** Como usuario, quiero elegir cuánto tiempo se guardan las notas en la papelera, para decidir entre poder recuperar más y acumular menos.

## Requisitos funcionales (criterios de aceptación en EARS)

### Mover a la papelera
- **RF-1:** CUANDO el usuario toque «Eliminar nota» en el editor, EL SISTEMA moverá la nota a la papelera sin pedir confirmación.
- **RF-2:** CUANDO el sistema mueva una nota a la papelera, EL SISTEMA cerrará el editor y volverá a Mis notas.
- **RF-3:** CUANDO el sistema mueva una nota a la papelera, EL SISTEMA mostrará en Mis notas el aviso «Nota movida a la papelera» con la acción «Deshacer».
- **RF-4:** CUANDO el usuario toque «Deshacer», EL SISTEMA devolverá la nota a Mis notas con el mismo título, contenido, imágenes, color, categoría, estado de fijada y fecha de última edición que tenía.
- **RF-5:** SI no se puede mover la nota a la papelera, ENTONCES EL SISTEMA mostrará en el editor el aviso actual «No pudimos eliminar la nota. Inténtalo de nuevo.» y la nota seguirá abierta.
- **RF-6:** EL SISTEMA guardará la fecha y hora en que cada nota entra en la papelera.
- **RF-7:** EL SISTEMA no mostrará las notas de la papelera en Mis notas, en la búsqueda, en los filtros, en el contador de notas ni en la pantalla de exportar.
- **RF-8:** SI la nota tenía cambios sin guardar con el guardado automático apagado, ENTONCES EL SISTEMA moverá a la papelera la última versión guardada y descartará esos cambios.

### Entrar en la papelera
- **RF-9:** EL SISTEMA mostrará habilitada la fila Papelera de Ajustes.
- **RF-10:** EL SISTEMA mostrará como descripción de la fila Papelera el número de notas de la papelera («N notas», «1 nota» con una sola), en lugar de «Próximamente».
- **RF-11:** MIENTRAS la papelera esté vacía, EL SISTEMA mostrará «Vacía» como descripción de la fila Papelera.
- **RF-12:** CUANDO el usuario toque la fila Papelera, EL SISTEMA abrirá la pantalla Papelera.
- **RF-13:** CUANDO el usuario toque la flecha de volver o use Atrás del sistema en la papelera, EL SISTEMA volverá a Ajustes.

### Pantalla Papelera
- **RF-14:** EL SISTEMA listará en la papelera las notas eliminadas, de la más reciente a la más antigua según la fecha en que entraron en la papelera.
- **RF-15:** EL SISTEMA mostrará cada nota de la papelera con su título (o «Sin título»), un extracto del contenido y su color, como en Mis notas.
- **RF-16:** MIENTRAS el plazo no sea «Nunca», EL SISTEMA mostrará en cada nota los días que faltan para su eliminación definitiva («Se eliminará en N días», «Se eliminará mañana» con 1, «Se eliminará hoy» con 0).
- **RF-17:** EL SISTEMA no mostrará las notas de la papelera como fijadas, aunque lo estuvieran.
- **RF-18:** SI la papelera está vacía, ENTONCES EL SISTEMA mostrará el aviso «La papelera está vacía».
- **RF-19:** SI no se pueden leer las notas de la papelera, ENTONCES EL SISTEMA mostrará el aviso «No se pudo cargar la papelera» en lugar de la lista.
- **RF-20:** CUANDO el usuario toque una nota de la papelera, EL SISTEMA abrirá una hoja inferior con las acciones «Restaurar» y «Eliminar definitivamente».
- **RF-21:** EL SISTEMA no permitirá abrir ni editar una nota de la papelera.

### Restaurar
- **RF-22:** CUANDO el usuario toque «Restaurar», EL SISTEMA devolverá la nota a Mis notas con el mismo título, contenido, imágenes, color, categoría, estado de fijada y fecha de última edición que tenía.
- **RF-23:** CUANDO el sistema restaure una nota, EL SISTEMA mostrará el aviso «Nota restaurada».
- **RF-24:** SI no se puede restaurar la nota, ENTONCES EL SISTEMA mostrará el aviso «No se pudo restaurar la nota» y la nota seguirá en la papelera.

### Eliminar definitivamente
- **RF-25:** CUANDO el usuario toque «Eliminar definitivamente», EL SISTEMA pedirá confirmación con «¿Eliminar definitivamente?» y «No podrás recuperarla.».
- **RF-26:** CUANDO el usuario confirme, EL SISTEMA borrará la nota y sus imágenes del dispositivo.
- **RF-27:** SI el usuario cancela la confirmación, ENTONCES EL SISTEMA dejará la nota en la papelera.
- **RF-28:** SI no se puede eliminar la nota, ENTONCES EL SISTEMA mostrará el aviso «No se pudo eliminar la nota» y la nota seguirá en la papelera.

### Vaciar la papelera
- **RF-29:** MIENTRAS la papelera tenga al menos una nota, EL SISTEMA mostrará la acción «Vaciar papelera».
- **RF-30:** MIENTRAS la papelera esté vacía, EL SISTEMA no mostrará la acción «Vaciar papelera».
- **RF-31:** CUANDO el usuario toque «Vaciar papelera», EL SISTEMA pedirá confirmación con «¿Vaciar la papelera?» y «Se eliminarán definitivamente N notas.» («1 nota» con una sola).
- **RF-32:** CUANDO el usuario confirme, EL SISTEMA borrará todas las notas de la papelera y sus imágenes del dispositivo.
- **RF-33:** SI el usuario cancela la confirmación, ENTONCES EL SISTEMA no borrará ninguna nota.
- **RF-34:** SI no se puede vaciar la papelera, ENTONCES EL SISTEMA mostrará el aviso «No se pudo vaciar la papelera».
- **RF-35:** SI no se puede vaciar la papelera, ENTONCES EL SISTEMA mantendrá en la papelera las notas que no se borraron.

### Plazo de la papelera
- **RF-36:** EL SISTEMA mostrará en la barra superior de la papelera el botón «Vaciar automáticamente», también cuando la papelera esté vacía.
- **RF-37:** EL SISTEMA ofrecerá los plazos «7 días», «30 días», «90 días» y «Nunca».
- **RF-38:** EL SISTEMA usará «30 días» como plazo mientras el usuario no elija otro.
- **RF-39:** CUANDO el usuario toque el botón «Vaciar automáticamente», EL SISTEMA abrirá una hoja inferior con los cuatro plazos y el vigente marcado.
- **RF-40:** CUANDO el usuario elija un plazo igual o más largo que el vigente, EL SISTEMA lo aplicará de inmediato.
- **RF-41:** CUANDO el usuario elija un plazo más corto y ninguna nota lleve en la papelera más tiempo que ese plazo, EL SISTEMA lo aplicará de inmediato.
- **RF-42:** CUANDO el usuario elija un plazo más corto y haya notas que lleven en la papelera más tiempo que ese plazo, EL SISTEMA pedirá confirmación con «N notas llevan más de P en la papelera y se eliminarán definitivamente.» («1 nota lleva» con una sola).
- **RF-43:** CUANDO el usuario confirme, EL SISTEMA aplicará el plazo y borrará definitivamente esas notas y sus imágenes.
- **RF-44:** SI el usuario cancela la confirmación, ENTONCES EL SISTEMA mantendrá el plazo anterior sin borrar ninguna nota.
- **RF-45:** EL SISTEMA aplicará el plazo vigente a todas las notas de la papelera, contando desde la fecha en que entró cada una.
- **RF-46:** SI no se puede guardar el plazo, ENTONCES EL SISTEMA mostrará el aviso «No se pudo guardar el plazo» y mantendrá el anterior.
- **RF-47:** EL SISTEMA conservará el plazo elegido al cerrar y volver a abrir la app.

### Borrado automático
- **RF-48:** SI una nota lleva en la papelera más tiempo que el plazo vigente, ENTONCES EL SISTEMA no la mostrará en la papelera.
- **RF-49:** SI una nota lleva en la papelera más tiempo que el plazo vigente, ENTONCES EL SISTEMA la borrará con sus imágenes a más tardar la siguiente vez que se abra la app.
- **RF-50:** MIENTRAS el plazo sea «Nunca», EL SISTEMA no borrará automáticamente ninguna nota.

## Requisitos no funcionales

- **Datos del usuario:** el cambio de esquema que haga falta sube la versión de la base, exporta el esquema e incluye una migración probada (constitución, principio 5). Las notas existentes quedan fuera de la papelera.
- **Aspecto:** sin diseño en Figma. La papelera usa la barra superior de Ajustes y las tarjetas de Mis notas; la hoja del plazo y la de acciones usan el estilo de la hoja de Tema; los diálogos usan el estilo del diálogo actual de eliminar nota. Se revisa contigo en la demo, en ambos temas.
- **Accesibilidad:** cada nota de la papelera, cada acción y cada opción del plazo tiene una etiqueta accesible.
- **Idioma:** todos los textos en español, en `strings.xml`.

## Casos límite

- Eliminar varias notas seguidas: cada aviso sustituye al anterior y «Deshacer» actúa solo sobre la última.
- El aviso con «Deshacer» desaparece sin tocarlo: la nota se queda en la papelera.
- Plazo «Nunca» y después 7 días: se aplica RF-42 a las notas que lleven más de 7 días.
- Alargar el plazo: las notas que siguen en la papelera cuentan con el nuevo plazo desde que entraron (RF-45).
- Cambiar la hora del dispositivo hacia atrás: los días que faltan nunca superan el plazo vigente.
- Nota fijada en la papelera: se muestra sin fijar (RF-17) y vuelve fijada al restaurarla (RF-22).
- Nota con imágenes: las imágenes se conservan en la papelera y se borran solo al eliminarla definitivamente.
- Restaurar una nota cuyo color ya no existe: no aplica, los colores no cambian.
- Girar la pantalla con la hoja o el diálogo abiertos: se mantienen.
- Notas eliminadas antes de esta spec: se borraron para siempre y no aparecen en la papelera.

## Fuera de alcance

- Abrir o leer una nota de la papelera (sin vista de solo lectura).
- Seleccionar varias notas de la papelera para restaurarlas o eliminarlas a la vez.
- Buscar o filtrar dentro de la papelera.
- Eliminar notas desde Mis notas (por ejemplo, deslizando o con pulsación larga); solo se elimina desde el editor.
- Elegir el plazo desde Ajustes; solo se elige dentro de la papelera.
- Plazos distintos de 7, 30 y 90 días y «Nunca».
- Avisar antes de que una nota se borre automáticamente.
- Borrado automático en segundo plano con la app cerrada.
- Ayuda y comentarios, que sigue atenuada.

## Criterios de finalización

- Todos los RF con prueba en verde (unitarias e instrumentadas) y `./gradlew test` pasando.
- Migración de la base probada con notas existentes.
- Demo manual en el emulador, en ambos temas:
  - eliminar una nota y deshacer;
  - restaurar, eliminar definitivamente y vaciar;
  - cambiar el plazo, incluido acortarlo con notas afectadas;
  - borrado automático de una nota vencida al abrir la app.
- La pantalla Papelera, sin diseño, se revisa contigo en la demo.
- Specs 006, 009 y 011 actualizadas para que no contradigan esta.

## Dudas abiertas

Ninguna.
