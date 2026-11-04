# Spec 006 — Pantalla de Ajustes y tema claro/oscuro

> **Estado:** implementada (API 30 pendiente de comprobar) · **Fecha:** 2026-11-02 · **Diseño:** Figma `JyeMmyDAEO1Y52QnwKwgGH`: Ajustes `4:1794` (oscuro) y `40:1211` (claro); en claro, Mis notas `40:956`, editor `40:1037` y selector de color `40:1115`

## Contexto y objetivo

El botón de ajustes de Mis notas se ve, pero no lleva a ninguna parte. Además, la app solo se puede usar en oscuro, aunque el diseño ya define una versión clara, y quien prefiere leer y escribir sobre fondo claro no tiene esa opción.

**Objetivo:** que el usuario pueda abrir la pantalla de Ajustes del diseño y elegir entre tema claro y oscuro, y que su elección se aplique a toda la app y se conserve. El resto de las opciones de Ajustes se muestran deshabilitadas hasta tener su propia spec.

## Usuarios / actores

- **Usuario de la app:** quiere usar la app con el tema que le resulte más cómodo.

## Historias de usuario

- **H1.** Como usuario, quiero abrir Ajustes desde Mis notas, para ver las opciones de la app.
- **H2.** Como usuario, quiero elegir entre tema claro y oscuro, para usar la app con el que me resulte más cómodo.
- **H3.** Como usuario, quiero que la app recuerde el tema elegido, para no tener que elegirlo cada vez que la abro.
- **H4.** Como usuario, quiero distinguir qué opciones todavía no están disponibles, para no tocar algo que no hará nada.

## Requisitos funcionales (criterios de aceptación en EARS)

### Navegación
- **RF-1:** CUANDO el usuario toque el botón de ajustes en Mis notas, EL SISTEMA abrirá la pantalla de Ajustes.
- **RF-2:** CUANDO el usuario toque la flecha de volver en Ajustes, EL SISTEMA regresará a Mis notas.
- **RF-3:** CUANDO el usuario use el gesto o el botón Atrás del sistema en Ajustes, EL SISTEMA regresará a Mis notas.

### Contenido de la pantalla
- **RF-4:** EL SISTEMA mostrará en Ajustes el aviso «Tus ideas, solo tuyas» con el texto «Tus notas se guardan en este dispositivo.».
- **RF-5:** EL SISTEMA mostrará en la sección «Apariencia» las filas Tema, Color predeterminado y Vista de notas, en ese orden.
- **RF-6:** EL SISTEMA mostrará en la sección «Tus notas» las filas Guardado automático, Exportar notas y Papelera, en ese orden.
- **RF-7:** EL SISTEMA mostrará en la sección «Acerca de» la fila Ayuda y comentarios.
- **RF-8:** EL SISTEMA mostrará al final de la pantalla el nombre de la app y su versión instalada.

### Opciones deshabilitadas
- **RF-9:** EL SISTEMA mostrará atenuadas las filas Vista de notas, Guardado automático, Exportar notas, Papelera y Ayuda y comentarios (la fila Color predeterminado funciona desde la spec 007).
- **RF-10:** CUANDO el usuario toque una fila atenuada, EL SISTEMA no realizará ninguna acción.
- **RF-11:** EL SISTEMA mostrará en la fila Color predeterminado el nombre del color vigente (sustituido por la spec 007, RF-3).
- **RF-12:** EL SISTEMA mostrará en la fila Vista de notas el valor «Cuadrícula».
- **RF-13:** EL SISTEMA mostrará activado el interruptor de Guardado automático.
- **RF-14:** EL SISTEMA mostrará «Próximamente» como descripción de la fila Papelera.
- **RF-15:** EL SISTEMA no mostrará ningún valor en la fila Papelera.

### Elegir el tema
- **RF-16:** MIENTRAS Ajustes esté abierta, EL SISTEMA mostrará en la fila Tema el nombre del tema activo.
- **RF-17:** CUANDO el usuario toque la fila Tema, EL SISTEMA abrirá una hoja inferior con las opciones Claro y Oscuro.
- **RF-18:** MIENTRAS la hoja de tema esté abierta, EL SISTEMA marcará como seleccionada la opción del tema activo.
- **RF-19:** CUANDO el usuario toque una opción de la hoja, EL SISTEMA aplicará ese tema de inmediato.
- **RF-20:** CUANDO el usuario toque una opción de la hoja, EL SISTEMA cerrará la hoja.
- **RF-21:** CUANDO el usuario cierre la hoja sin tocar ninguna opción, EL SISTEMA mantendrá el tema activo.

### Conservar el tema
- **RF-22:** CUANDO el usuario elija un tema, EL SISTEMA lo guardará en el dispositivo.
- **RF-23:** CUANDO la app se abra con un tema guardado, EL SISTEMA aplicará ese tema.
- **RF-24:** SI no hay ningún tema guardado, ENTONCES EL SISTEMA aplicará el tema Oscuro.
- **RF-25:** SI el tema guardado no se puede leer, ENTONCES EL SISTEMA aplicará el tema Oscuro.
- **RF-26:** EL SISTEMA aplicará el tema elegido sin tener en cuenta el modo claro u oscuro del teléfono.

### Aplicación del tema
- **RF-27:** MIENTRAS el tema Claro esté activo, EL SISTEMA mostrará Mis notas según el frame `40:956`.
- **RF-28:** MIENTRAS el tema Claro esté activo, EL SISTEMA mostrará Ajustes según el frame `40:1211`.
- **RF-29:** MIENTRAS el tema Claro esté activo, EL SISTEMA mostrará el selector de color según el frame `40:1115`.
- **RF-30:** MIENTRAS el tema Claro esté activo, EL SISTEMA mostrará los menús, diálogos, avisos y hojas sin frame propio con la misma paleta clara.
- **RF-31:** MIENTRAS el tema Oscuro esté activo, EL SISTEMA mostrará las pantallas existentes como hasta ahora.
- **RF-32:** EL SISTEMA mostrará los mismos ocho colores de nota en ambos temas.
- **RF-33:** DONDE la nota tenga un color distinto de «Sin color», EL SISTEMA pintará el fondo del editor con ese color en ambos temas (spec 003, RF-24).
- **RF-34:** DONDE la nota tenga «Sin color» y el tema Claro esté activo, EL SISTEMA mostrará el editor con el fondo y los textos del frame `40:1037`.
- **RF-35:** DONDE la nota tenga «Sin color» y el tema Claro esté activo, EL SISTEMA mostrará su tarjeta con una superficie clara y texto oscuro.
- **RF-36:** MIENTRAS el tema Claro esté activo, EL SISTEMA mostrará oscuros los íconos de las barras de estado y de navegación.
- **RF-37:** MIENTRAS el tema Oscuro esté activo, EL SISTEMA mostrará claros los íconos de las barras de estado y de navegación.

### Fallo al guardar y apertura de la app
- **RF-38:** SI el tema elegido no se puede guardar, ENTONCES EL SISTEMA lo mantendrá aplicado hasta que se cierre la app.
- **RF-39:** SI el tema elegido no se puede guardar, ENTONCES EL SISTEMA mostrará el aviso «No se pudo guardar el tema».
- **RF-40:** CUANDO la app se abra con el tema Claro guardado, EL SISTEMA mostrará en claro la ventana inicial previa a la primera pantalla.

## Requisitos no funcionales

- **RNF-1. Aspecto.** Ajustes sigue los frames `4:1794` y `40:1211`, salvo las filas atenuadas y la Papelera (RF-9, RF-14 y RF-15). La hoja de tema no tiene frame propio: sigue el estilo del selector de color.
- **RNF-2. Contraste.** Los textos de las pantallas en tema Claro cumplen un contraste mínimo de 4,5:1 respecto a su fondo; las filas atenuadas quedan exentas, por estar deshabilitadas.
- **RNF-3. Accesibilidad.** El lector de pantalla anuncia las filas atenuadas como deshabilitadas, la opción seleccionada de la hoja de tema como seleccionada y la flecha de volver con su nombre. Las zonas táctiles de la fila Tema, las opciones de la hoja y la flecha miden al menos 44 × 44 dp.
- **RNF-4. Datos.** La elección del tema se guarda solo en el dispositivo y cambiar de tema no modifica ninguna nota.
- **RNF-5. Idioma.** Los textos nuevos están en español, en `strings.xml`.

## Casos límite

- **CL-1.** Primera apertura tras actualizar la app: se ve en Oscuro, igual que antes de actualizar (RF-24).
- **CL-2.** Elegir el tema que ya está activo: la hoja se cierra y no cambia nada en pantalla.
- **CL-3.** Girar la pantalla con la hoja de tema abierta: la hoja sigue abierta y el tema no cambia.
- **CL-4.** Cambiar de tema y volver a Mis notas: la lista ya se ve con el tema elegido, sin reiniciar la app.
- **CL-5.** Cambiar de tema con una búsqueda o un filtro activos en Mis notas: al volver, siguen activos.
- **CL-6.** Tema Claro con el teléfono en modo oscuro, o tema Oscuro con el teléfono en modo claro: se mantiene el tema elegido en la app, incluso si el teléfono cambia de modo con la app abierta (RF-26).
- **CL-7.** Cerrar la app por completo después de elegir un tema y volver a abrirla: se abre con ese tema (RF-23).
- **CL-8.** Nota con color en tema Claro: el editor y la tarjeta usan el color de la nota con texto oscuro, igual que en Oscuro. El frame `40:1037` muestra una nota Cielo con fondo claro, pero se mantiene el comportamiento de la spec 003 (RF-33).
- **CL-9.** La versión instalada no coincide con el «1.0.0» del diseño: se muestra la versión instalada (RF-8).
- **CL-10.** Tocar el interruptor de Guardado automático: no cambia y el editor sigue guardando solo (RF-10, RF-13).

## Fuera de alcance

- Hacer funcionar Vista de notas, Guardado automático, Exportar notas, Papelera y Ayuda y comentarios; cada una necesitará su propia spec.
- Una opción para seguir el modo claro u oscuro del teléfono.
- Colores dinámicos del sistema y temas distintos de Claro y Oscuro.
- Cambiar los colores de las notas o agregar colores nuevos.
- Rediseñar pantallas existentes, más allá de su paleta en tema Claro.
- Mostrar el botón Compartir que aparece en el frame `40:1037`; sigue oculto hasta tener esa función.
- Sincronizar el tema entre dispositivos.

## Criterios de finalización

- Todos los requisitos RF-1 a RF-40 y los casos límite CL-1 a CL-10 tienen pruebas que los verifican y pasan; lo que dependa de la apariencia se comprueba a mano frente a Figma.
- `./gradlew test` y las pruebas instrumentadas pasan, según la constitución del proyecto.
- Se comprueba a mano en el emulador:
  - abrir Ajustes desde Mis notas y volver con la flecha y con Atrás;
  - tocar cada fila atenuada y comprobar que no ocurre nada;
  - cambiar a Claro y revisar Mis notas, el editor con y sin color, el selector de color, los menús y los diálogos;
  - cerrar y reabrir la app para comprobar que conserva el tema y que, en Claro, no hay un destello oscuro al abrirla;
  - instalar esta versión sobre la anterior y comprobar que abre en Oscuro con las notas intactas.

## Dudas abiertas

Ninguna.
