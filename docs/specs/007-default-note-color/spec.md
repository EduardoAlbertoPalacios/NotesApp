# Spec 007 — Color predeterminado de las notas nuevas

> **Estado:** implementada · **Fecha:** 2026-11-03 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`: selector de color `4:1710` (oscuro) y `40:1115` (claro); Ajustes `4:1794` (oscuro) y `40:1211` (claro)

## Contexto y objetivo

Las notas nuevas salen siempre en Cielo y la fila «Color predeterminado» de Ajustes está atenuada. Quien prefiere otro color, o una cuadrícula sobria, tiene que cambiarlo a mano en cada nota nueva.

**Objetivo:** que el usuario pueda elegir en Ajustes el color con el que se crean sus notas nuevas, entre las ocho opciones que ya existen.

**Relación con otras specs:** esta spec sustituye el RF-22 de la spec 003 y los RF-9 (solo en lo relativo a esta fila) y RF-11 de la spec 006.

## Usuarios / actores

- **Usuario que suele usar un mismo color:** quiere que sus notas nuevas salgan con él.
- **Usuario que prefiere una cuadrícula sobria:** quiere que sus notas nuevas salgan sin color.

## Historias de usuario

- **H1.** Como usuario, quiero elegir el color de mis notas nuevas, para no cambiarlo en cada una.
- **H2.** Como usuario, quiero ver cómo quedará una nota antes de aplicar el color, para no tener que probar varios.
- **H3.** Como usuario, quiero poder cerrar el selector sin cambiar nada, para no aplicar un color por accidente.
- **H4.** Como usuario, quiero poder cambiar el color de una nota concreta aunque tenga un predeterminado, para seguir distinguiéndola.

## Requisitos funcionales (criterios de aceptación en EARS)

### Fila en Ajustes
- **RF-1:** EL SISTEMA mostrará habilitada la fila Color predeterminado.
- **RF-2:** EL SISTEMA mostrará «Para tus nuevas notas» como descripción de la fila Color predeterminado.
- **RF-3:** MIENTRAS Ajustes esté abierta, EL SISTEMA mostrará en la fila Color predeterminado el nombre del color predeterminado vigente.

### Abrir el selector
- **RF-4:** CUANDO el usuario toque la fila Color predeterminado, EL SISTEMA mostrará una hoja inferior titulada «Color predeterminado» con la descripción «Para tus nuevas notas».
- **RF-5:** EL SISTEMA mostrará en la hoja ocho opciones, en este orden: Sin color, Cielo, Rosa, Vainilla, Lavanda, Menta, Melocotón y Arena.
- **RF-6:** CUANDO se abra la hoja, EL SISTEMA marcará como elegida la opción del color predeterminado vigente.

### Elegir un color
- **RF-7:** CUANDO el usuario toque una opción, EL SISTEMA la marcará como elegida con una marca de verificación y su nombre resaltado.
- **RF-8:** CUANDO el usuario toque una opción, EL SISTEMA quitará la marca de la opción elegida antes.
- **RF-9:** MIENTRAS la hoja esté abierta, EL SISTEMA mostrará una vista previa de la tarjeta con el color elegido.
- **RF-10:** MIENTRAS la hoja esté abierta, EL SISTEMA mostrará «Nota nueva» como título de la vista previa.
- **RF-11:** MIENTRAS la hoja esté abierta, EL SISTEMA mostrará bajo el título de la vista previa el texto «Así se verá en tus notas · <nombre del color elegido>».
- **RF-12:** MIENTRAS la hoja esté abierta, EL SISTEMA mantendrá sin cambios el color predeterminado vigente hasta que el usuario pulse «Aplicar color».

### Aplicar o descartar
- **RF-13:** CUANDO el usuario pulse «Aplicar color», EL SISTEMA establecerá el color elegido como predeterminado.
- **RF-14:** CUANDO el usuario pulse «Aplicar color», EL SISTEMA cerrará la hoja.
- **RF-15:** CUANDO el usuario cierre la hoja sin pulsar «Aplicar color» (con la X, deslizándola hacia abajo, tocando fuera de ella o con el botón Atrás del sistema), EL SISTEMA conservará el color predeterminado vigente.
- **RF-16:** CUANDO el usuario vuelva a abrir la hoja tras cerrarla sin aplicar, EL SISTEMA marcará como elegido el color predeterminado vigente.

### Notas nuevas
- **RF-17:** CUANDO el usuario cree una nota nueva, EL SISTEMA la abrirá con el color predeterminado vigente.
- **RF-18:** CUANDO el usuario aplique otro color a una nota nueva, EL SISTEMA conservará ese color en esa nota.
- **RF-19:** CUANDO el usuario aplique un color a una nota desde el editor, EL SISTEMA mantendrá sin cambios el color predeterminado.
- **RF-20:** CUANDO cambie el color predeterminado, EL SISTEMA mantendrá sin cambios el color de las notas ya guardadas.

### Conservar la elección
- **RF-21:** CUANDO el usuario aplique un color predeterminado, EL SISTEMA lo guardará en el dispositivo.
- **RF-22:** CUANDO la app se abra con un color predeterminado guardado, EL SISTEMA usará ese color como predeterminado.
- **RF-23:** SI no hay ningún color predeterminado guardado, ENTONCES EL SISTEMA usará Cielo como predeterminado.
- **RF-24:** SI el color predeterminado guardado no se puede leer, ENTONCES EL SISTEMA usará Cielo como predeterminado.

### Fallo al guardar
- **RF-25:** SI el color predeterminado elegido no se puede guardar, ENTONCES EL SISTEMA lo mantendrá vigente hasta que se cierre la app.
- **RF-26:** SI el color predeterminado elegido no se puede guardar, ENTONCES EL SISTEMA mostrará el aviso «No se pudo guardar el color predeterminado».

### Accesibilidad
- **RF-27:** EL SISTEMA dará a cada opción de color una etiqueta accesible con el nombre del color.
- **RF-28:** EL SISTEMA anunciará a los lectores de pantalla si cada opción está elegida o no.
- **RF-29:** EL SISTEMA dará al botón de cerrar la hoja una etiqueta accesible.

## Requisitos no funcionales

- **Aspecto:** la hoja, la paleta, la vista previa y el botón siguen los frames `4:1710` (oscuro) y `40:1115` (claro), cambiando solo los textos de RF-4 y RF-10. La fila sigue los frames `4:1794` y `40:1211`.
- **Accesibilidad:** la zona táctil de la fila y la de cada opción de la hoja miden al menos 44 × 44 dp.
- **Datos:** la elección se guarda solo en el dispositivo.
- **Idioma:** todos los textos nuevos en español, en `strings.xml`.

## Casos límite

- **Primera apertura tras actualizar la app:** el predeterminado es Cielo, igual que antes de actualizar (RF-23).
- **«Sin color» como predeterminado:** la fila muestra «Sin color» y las notas nuevas se abren como cualquier nota sin color, en ambos temas (specs 003 y 006).
- **Aplicar el color que ya era predeterminado:** la hoja se cierra y no cambia nada en pantalla.
- **Nota nueva abandonada sin escribir:** no se guarda nada, como hasta ahora (spec 003, RF-19), y el predeterminado no cambia.
- **Cambiar el predeterminado con filtro o búsqueda activos en Mis notas:** al volver, siguen activos y las tarjetas no cambian.
- **Girar la pantalla o pasar a segundo plano con la hoja abierta:** la hoja sigue abierta con la misma opción elegida.
- **Fallo al guardar y cierre de la app:** al reabrirla vuelve el último color guardado, o Cielo si no hay ninguno (RF-22, RF-23).
- **Deshacer en una nota nueva:** no cambia su color, porque deshacer no afecta al color (spec 005).

## Fuera de alcance

- Colores personalizados o distintos de las ocho opciones existentes.
- Aplicar el predeterminado a notas ya guardadas, o cambiar varias notas a la vez.
- Recordar como predeterminado el último color usado en una nota.
- Predeterminados distintos por categoría.
- Hacer funcionar las demás filas atenuadas de Ajustes.
- Sincronizar el predeterminado entre dispositivos.

## Criterios de finalización

- Todos los RF con prueba en verde (unitarias e instrumentadas) y `./gradlew test` pasando.
- Demo manual en el emulador, en ambos temas:
  - Elegir un color, aplicarlo y comprobar la fila y una nota nueva.
  - Cerrar sin aplicar por cada una de las cuatro vías.
  - Usar «Sin color» como predeterminado.
  - Cerrar la app por completo, volver a abrirla y comprobar que el color se conserva.
  - Verificar que las notas guardadas conservan su color.
- La hoja comparada con las capturas de Figma, salvo los textos adaptados.

## Dudas abiertas

Ninguna.
