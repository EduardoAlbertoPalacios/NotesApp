# Spec 009 — Guardado automático activable

> **Estado:** implementada · **Fecha:** 2026-11-06 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`: Ajustes `4:1794` (oscuro) y `40:1211` (claro), fila «Guardado automático» con su interruptor. El botón Guardar, el estado «Cambios sin guardar» y el diálogo de salida no tienen frame: se diseñan con el estilo actual.

## Contexto y objetivo

El editor guarda siempre solo: mientras se escribe y al salir de la nota. La fila «Guardado automático» de Ajustes muestra un interruptor encendido que no hace nada. Quien quiere probar cambios sin que queden guardados, o decidir cuándo una nota cambia, no tiene forma de hacerlo.

**Objetivo:** que el usuario pueda apagar el guardado automático desde Ajustes y, con él apagado, guardar los cambios de cada nota solo cuando lo pida, sin perder trabajo por salir sin querer.

**Relación con otras specs:** esta spec sustituye los RF-9 (solo en lo relativo a esta fila) y RF-13, y el CL-10 de la spec 006. El comportamiento de guardado descrito en las specs 003, 004 y 005 se mantiene con el guardado automático encendido.

## Usuarios / actores

- **Usuario que prefiere confirmar sus cambios:** quiere decidir cuándo se guarda una nota.
- **Usuario que prueba ideas sobre una nota existente:** quiere poder descartar lo que hizo y volver a la versión anterior.
- **Usuario actual:** espera que todo siga igual si no toca el ajuste.

## Historias de usuario

- **H1.** Como usuario, quiero apagar el guardado automático, para que mis notas solo cambien cuando yo lo decida.
- **H2.** Como usuario, quiero un botón para guardar, para confirmar los cambios cuando estén listos.
- **H3.** Como usuario, quiero que la app me avise si salgo con cambios sin guardar, para no perderlos sin querer.
- **H4.** Como usuario, quiero descartar los cambios de una nota, para volver a su última versión guardada.

## Requisitos funcionales (criterios de aceptación en EARS)

### Ajuste en Ajustes
- **RF-1:** EL SISTEMA mostrará habilitada la fila Guardado automático.
- **RF-2:** MIENTRAS Ajustes esté abierta, EL SISTEMA mostrará el interruptor encendido o apagado según el estado vigente del guardado automático.
- **RF-3:** CUANDO el usuario toque la fila Guardado automático, EL SISTEMA cambiará el estado del guardado automático.
- **RF-4:** CUANDO el usuario cambie el estado del guardado automático, EL SISTEMA lo guardará en el dispositivo.
- **RF-5:** SI no hay ningún estado guardado, ENTONCES EL SISTEMA mantendrá el guardado automático encendido.
- **RF-6:** SI el estado guardado no se puede leer, ENTONCES EL SISTEMA mantendrá el guardado automático encendido.
- **RF-7:** SI el estado elegido no se puede guardar, ENTONCES EL SISTEMA lo mantendrá vigente hasta que se cierre la app.
- **RF-8:** SI el estado elegido no se puede guardar, ENTONCES EL SISTEMA mostrará el aviso «No se pudo guardar el guardado automático».

### Guardado automático encendido
- **RF-9:** MIENTRAS el guardado automático esté encendido, EL SISTEMA guardará las notas igual que antes de esta funcionalidad.
- **RF-10:** MIENTRAS el guardado automático esté encendido, EL SISTEMA no mostrará el botón Guardar en el editor.

### Guardado automático apagado: editor
- **RF-11:** MIENTRAS el guardado automático esté apagado, EL SISTEMA no guardará ningún cambio de una nota hasta que el usuario lo pida.
- **RF-12:** EL SISTEMA aplicará RF-11 a los cambios de título, contenido, formato, tareas, fijación, categoría, color e imágenes.
- **RF-13:** MIENTRAS el guardado automático esté apagado, EL SISTEMA mostrará un botón Guardar en el editor.
- **RF-14:** MIENTRAS la nota abierta sea igual a su última versión guardada, EL SISTEMA mostrará el botón Guardar deshabilitado.
- **RF-15:** MIENTRAS la nota abierta tenga cambios sin guardar, EL SISTEMA mostrará «Cambios sin guardar» en la línea de estado del editor.
- **RF-16:** CUANDO el usuario toque el botón Guardar, EL SISTEMA guardará todos los cambios pendientes de la nota.
- **RF-17:** CUANDO el usuario guarde una nota con cambios, EL SISTEMA actualizará su fecha de última edición.
- **RF-18:** CUANDO el usuario guarde una nota, EL SISTEMA mostrará «Todos los cambios guardados» en la línea de estado.
- **RF-19:** CUANDO el usuario guarde una nota, EL SISTEMA mantendrá el historial de Deshacer.

### Guardado automático apagado: salir del editor
- **RF-20:** CUANDO el usuario intente salir del editor con cambios sin guardar (flecha de volver o botón Atrás del sistema), EL SISTEMA mostrará el diálogo «¿Guardar los cambios?» con las opciones Guardar, Descartar y Cancelar.
- **RF-21:** CUANDO el usuario elija Guardar en el diálogo, EL SISTEMA guardará los cambios pendientes.
- **RF-22:** CUANDO el guardado del diálogo termine bien, EL SISTEMA volverá a Mis notas.
- **RF-23:** CUANDO el usuario elija Descartar en el diálogo, EL SISTEMA volverá a Mis notas sin guardar los cambios pendientes.
- **RF-24:** CUANDO el usuario descarte los cambios, EL SISTEMA dejará la nota igual que en su última versión guardada, incluidas sus imágenes.
- **RF-25:** CUANDO el usuario elija Cancelar o cierre el diálogo sin elegir, EL SISTEMA seguirá en el editor con los cambios pendientes.
- **RF-26:** SI el usuario sale del editor sin cambios sin guardar, ENTONCES EL SISTEMA volverá a Mis notas sin mostrar el diálogo.
- **RF-27:** SI la nota es nueva y no tiene título, contenido ni imágenes, ENTONCES EL SISTEMA no la guardará ni mostrará el diálogo al salir.

### Guardado automático apagado: errores
- **RF-28:** SI el guardado no se puede completar, ENTONCES EL SISTEMA mostrará el aviso «No pudimos guardar los cambios. Inténtalo de nuevo.».
- **RF-29:** SI el guardado no se puede completar, ENTONCES EL SISTEMA mantendrá los cambios como pendientes.
- **RF-30:** SI el guardado elegido en el diálogo no se puede completar, ENTONCES EL SISTEMA seguirá en el editor.

### Guardado automático apagado: segundo plano
- **RF-31:** CUANDO el usuario vuelva a una nota con cambios sin guardar tras pasar la app a segundo plano, EL SISTEMA mostrará la nota con esos cambios.
- **RF-32:** CUANDO el sistema cierre la app en segundo plano con cambios sin guardar y el usuario vuelva a ella, EL SISTEMA mostrará la nota con esos cambios.
- **RF-33:** CUANDO el usuario vuelva a una nota con cambios recuperados, EL SISTEMA los mantendrá sin guardar.
- **RF-34:** CUANDO el usuario cierre la app desde las apps recientes con cambios sin guardar, EL SISTEMA descartará esos cambios.

### Cambiar el ajuste
- **RF-35:** CUANDO el usuario cambie el estado del guardado automático, EL SISTEMA lo aplicará a las notas que se abran después.

### Accesibilidad
- **RF-36:** EL SISTEMA dará al botón Guardar una etiqueta accesible.
- **RF-37:** EL SISTEMA anunciará a los lectores de pantalla si el botón Guardar está deshabilitado.
- **RF-38:** EL SISTEMA anunciará a los lectores de pantalla si el interruptor está encendido o apagado.

## Requisitos no funcionales

- **Aspecto:** la fila y su interruptor encendido siguen los frames `4:1794` y `40:1211`. El interruptor apagado no tiene frame: usa el mismo tamaño, con el círculo a la izquierda y los colores apagados del tema. El botón Guardar va en la barra superior del editor, junto a Fijar, con el estilo de sus botones. El diálogo de salida sigue el estilo del diálogo de eliminar nota. El estado «Cambios sin guardar» ocupa la línea de estado actual.
- **Accesibilidad:** las zonas táctiles de la fila y del botón Guardar miden al menos 44 × 44 dp.
- **Datos:** el estado del ajuste se guarda solo en el dispositivo. Los cambios pendientes recuperados en RF-32 no salen del dispositivo.
- **Idioma:** todos los textos nuevos en español, en `strings.xml`.

## Casos límite

- **Primera apertura tras actualizar la app:** el guardado automático sigue encendido, igual que antes (RF-5).
- **Deshacer hasta la última versión guardada:** la nota deja de tener cambios pendientes y el botón Guardar se deshabilita (RF-14).
- **Cambiar algo y volver a dejarlo como estaba** (por ejemplo, fijar y desfijar): no quedan cambios pendientes.
- **Nota nueva con título o contenido, sin guardar nunca:** al descartar no se crea (RF-23, RF-24).
- **Nota nueva vacía con color aplicado:** al salir no se guarda ni aparece el diálogo (RF-27).
- **Nota nueva solo con imágenes:** tiene cambios pendientes, así que al salir aparece el diálogo (RF-20, RF-27).
- **Agregar imágenes y descartar:** la nota queda sin ellas (RF-24).
- **Quitar una imagen y descartar:** la nota conserva la imagen (RF-24).
- **Eliminar una nota con cambios sin guardar:** se elimina tras la confirmación de siempre, sin el diálogo de guardar.
- **Girar la pantalla con el diálogo abierto:** el diálogo sigue abierto y los cambios siguen pendientes.
- **Guardar dos veces seguidas sin cambios entre medias:** la segunda vez el botón ya está deshabilitado (RF-14).
- **Vaciar el título y el contenido de una nota ya guardada:** no se guarda, igual que con el guardado automático. Al pulsar Guardar no pasa nada visible y los cambios siguen pendientes. Si se elige Guardar en el diálogo, el editor se cierra y la nota conserva su última versión guardada. Se revisará en la spec sobre notas vaciadas.
- **Error al guardar al salir:** el usuario sigue en el editor con sus cambios y puede reintentar o descartar (RF-30).

## Fuera de alcance

- Botón Guardar o diálogo de salida con el guardado automático encendido.
- Cambiar los 500 ms de espera del guardado automático o hacerlo configurable.
- Guardar versiones anteriores de una nota o un historial de guardados.
- Recuperar cambios sin guardar tras cerrar la app desde las apps recientes, reiniciar el teléfono o desinstalar la app.
- Rehacer.
- Atajos de teclado para guardar.
- Avisar en Mis notas de qué notas tienen cambios sin guardar.
- Hacer funcionar las demás filas atenuadas de Ajustes.
- Sincronizar el ajuste entre dispositivos.

## Criterios de finalización

- Todos los RF con prueba en verde (unitarias e instrumentadas) y `./gradlew test` pasando.
- Demo manual en el emulador, en ambos temas:
  - Con el ajuste encendido, comprobar que el editor guarda igual que antes.
  - Apagar el ajuste, editar texto, color, tareas e imágenes, y guardar con el botón.
  - Salir con cambios y probar Guardar, Descartar y Cancelar del diálogo.
  - Pasar la app a segundo plano con cambios y forzar que el sistema la cierre; al volver, los cambios siguen sin guardar.
  - Cerrar la app por completo y reabrirla: el ajuste se conserva.
- El botón, el estado y el diálogo revisados contigo frente a su diseño.

## Dudas abiertas

Ninguna.
