# Spec 015 — Animación entre pantallas

> **Estado:** implementada · **Fecha:** 2026-11-20 · **Diseño:** sin frame en Figma; el comportamiento lo define esta spec.

## Contexto y objetivo

Al cambiar de pantalla, la app hace un fundido cruzado de unos 700 ms que no indica si se avanza o se vuelve, y que se nota lento. Además, en Android 14 o superior, el gesto de atrás no deja ver adónde se vuelve antes de soltar.

**Objetivo:** que cada cambio de pantalla se deslice en horizontal, en un sentido para avanzar y en el contrario para volver, y que en Android 14 o superior el gesto de atrás muestre la pantalla anterior mientras se arrastra.

**Relación con otras specs:** no cambia qué pantalla se abre ni cuándo. Las salidas que ya piden confirmación o guardan al salir (specs 009 y 012) se siguen comportando igual; esta spec solo define cómo se ve el cambio.

## Usuarios / actores

- **Usuario que navega con toques:** abre notas, Ajustes y sus pantallas, y vuelve con la flecha.
- **Usuario que vuelve con el gesto o el botón Atrás del sistema**, en cualquier versión de Android.
- **Usuario con las animaciones del sistema reducidas o quitadas**, por accesibilidad o desde las opciones de desarrollador.

## Historias de usuario

- **H1.** Como usuario, quiero ver si avanzo o vuelvo al cambiar de pantalla, para no perderme dentro de la app.
- **H2.** Como usuario de Android 14 o superior, quiero ver la pantalla anterior mientras arrastro el gesto de atrás, para decidir si vuelvo antes de soltar.
- **H3.** Como usuario sensible al movimiento, quiero que la app respete mi ajuste de animaciones, para no ver movimientos que me molestan.

## Requisitos funcionales (criterios de aceptación en EARS)

En esta spec, **avanzar** es abrir una pantalla nueva:
- de Mis notas al editor (nota existente o nueva);
- de Mis notas a Ajustes;
- de Ajustes a Exportar notas, a la Papelera o a Ayuda y comentarios.

**Volver** es cerrar la pantalla actual y regresar a la anterior, con la flecha de volver, con Atrás del sistema, o automáticamente (por ejemplo, al mover una nota a la papelera desde el editor o al terminar de exportar).

### Avanzar
- **RF-1:** CUANDO el usuario avance a una pantalla, EL SISTEMA deslizará la pantalla nueva desde el borde derecho hasta ocupar toda la pantalla.
- **RF-2:** CUANDO el usuario avance a una pantalla, EL SISTEMA desplazará la pantalla anterior hacia la izquierda un tercio de su ancho, por debajo de la nueva.

### Volver
- **RF-3:** CUANDO el usuario vuelva a la pantalla anterior, EL SISTEMA deslizará la pantalla actual hacia el borde derecho hasta que salga por completo.
- **RF-4:** CUANDO el usuario vuelva a la pantalla anterior, EL SISTEMA desplazará la pantalla anterior desde un tercio de su ancho a la izquierda hasta su posición, por debajo de la que sale.
- **RF-5:** CUANDO la app vuelva automáticamente a la pantalla anterior, EL SISTEMA usará la misma animación que al volver con la flecha.

### Duración
- **RF-6:** MIENTRAS la escala de duración de animación del sistema sea 1×, EL SISTEMA completará cada animación de avanzar o volver en 300 ms.
- **RF-7:** MIENTRAS la escala de duración de animación del sistema sea distinta de 1×, EL SISTEMA multiplicará la duración de la animación por esa escala.
- **RF-8:** MIENTRAS las animaciones del sistema estén desactivadas («Quitar animaciones» o escala 0), EL SISTEMA cambiará de pantalla sin animación.

### Gesto de atrás predictivo (Android 14 o superior)
- **RF-9:** MIENTRAS el usuario arrastre el gesto de atrás en Android 14 o superior, EL SISTEMA desplazará la pantalla actual hacia la derecha en proporción al avance del gesto.
- **RF-10:** MIENTRAS el usuario arrastre el gesto de atrás en Android 14 o superior, EL SISTEMA mostrará la pantalla anterior acercándose a su posición en proporción al avance del gesto.
- **RF-11:** CUANDO el usuario suelte el gesto de atrás después de completarlo, EL SISTEMA terminará la animación de volver desde el punto en que quedó.
- **RF-12:** CUANDO el usuario cancele el gesto de atrás, EL SISTEMA devolverá la pantalla actual a su posición sin volver.
- **RF-13:** SI el usuario cancela el gesto de atrás, ENTONCES EL SISTEMA conservará la pantalla actual tal como estaba, con su texto, su selección y su posición de desplazamiento.
- **RF-14:** MIENTRAS volver desde la pantalla actual requiera una confirmación o termine un trabajo en curso (por ejemplo, el diálogo «¿Guardar los cambios?» del editor con el guardado automático apagado, guardar la nota, copiar una imagen o generar la exportación), EL SISTEMA no mostrará la vista previa de la pantalla anterior durante el gesto.
- **RF-15:** MIENTRAS volver desde la pantalla actual requiera una confirmación, EL SISTEMA mostrará esa confirmación al completar el gesto, como hoy.
- **RF-16:** SI tras completar el gesto la pantalla actual no puede cerrarse (por ejemplo, falla el guardado al salir del editor), ENTONCES EL SISTEMA dejará la pantalla actual en su posición y mostrará el aviso de error que ya existe.
- **RF-17:** MIENTRAS el dispositivo use una versión anterior a Android 14, EL SISTEMA reproducirá la animación completa de volver al soltar el gesto o pulsar Atrás.

### Toques durante la animación
- **RF-18:** MIENTRAS una animación de avanzar o volver esté en curso, EL SISTEMA ignorará los toques sobre la pantalla que sale.
- **RF-19:** SI el usuario toca dos veces seguidas un elemento que avanza (por ejemplo, una nota), ENTONCES EL SISTEMA abrirá una sola pantalla.
- **RF-20:** CUANDO el usuario pulse Atrás durante una animación de avanzar, EL SISTEMA volverá a la pantalla de partida con la animación de volver.
- **RF-21:** SI el usuario pulsa Atrás varias veces durante una animación de volver, ENTONCES EL SISTEMA retrocederá una pantalla por pulsación, sin saltarse ninguna ni cerrar la app antes de llegar a Mis notas.

### Sin cambios
- **RF-22:** CUANDO la app se abra, EL SISTEMA mostrará Mis notas sin animación de deslizamiento.
- **RF-23:** EL SISTEMA abrirá y cerrará las hojas inferiores, los diálogos y los avisos con la animación que tienen hoy.

## Requisitos no funcionales

- **Fluidez:** en el emulador de referencia (API 36) y en un teléfono real, la animación no muestra saltos visibles a simple vista, con notas que tienen imágenes.
- **Accesibilidad:** el lector de pantalla anuncia la pantalla nueva igual que hoy; la animación no cambia el orden ni el foco de lectura.
- **Temas:** la animación se ve igual en tema claro y oscuro, sin destellos de otro color entre pantallas.

## Casos límite

- **CL-1.** Girar el dispositivo durante la animación: la app termina en la pantalla de destino, sin duplicarla ni quedarse a medias.
- **CL-2.** Gesto de atrás en Mis notas (sin pantalla anterior): la app se cierra como hoy, con la animación del sistema (fuera de esta spec).
- **CL-3.** Volver automáticamente justo mientras termina otra animación (por ejemplo, la exportación acaba al instante): se ve una sola animación de volver hacia Ajustes.
- **CL-4.** Editor con guardado automático justo después de escribir (menos de medio segundo, cambios aún sin guardar): el gesto no muestra vista previa; al completarlo se guarda y se sale como hoy (RF-14, RF-16). Con todo guardado, el gesto muestra la vista previa y, al cancelarlo, el editor sigue igual (RF-12, RF-13).
- **CL-5.** Editor con el teclado abierto: durante el gesto, el teclado no impide ver el deslizamiento; al cancelar, el cursor sigue donde estaba.
- **CL-6.** Escala de animación a 10× (opciones de desarrollador): la animación dura 3 s y los toques en la pantalla que sale siguen ignorados (RF-7, RF-18).

## Fuera de alcance

- Animaciones distintas por pantalla (el editor creciendo desde la tarjeta, deslizamientos verticales, elementos compartidos entre pantallas).
- Sombra u oscurecimiento sobre la pantalla de debajo durante la animación.
- Animar las hojas inferiores, los diálogos, los avisos o los cambios dentro de una misma pantalla (filtros, búsqueda, vista de lista o cuadrícula).
- La animación del sistema al cerrar la app desde Mis notas o al volver al escritorio.
- Invertir el sentido para idiomas que se escriben de derecha a izquierda: la app solo está en español.
- Ajustes propios de la app para cambiar o quitar la animación; se usa el ajuste del sistema.

## Criterios de finalización

- Todos los RF verificables por pruebas con pruebas en verde: de UI para avanzar, volver, la duración con la escala del sistema, los toques ignorados y Atrás durante la animación.
- La app compila y todas las pruebas pasan.
- Demo manual en el emulador, en ambos temas:
  - avanzar y volver entre todas las pantallas, con la flecha y con Atrás;
  - volver automáticamente al mover una nota a la papelera y al exportar;
  - gesto predictivo en API 36, completado y cancelado, también en el editor con el guardado automático apagado y cambios sin guardar;
  - Atrás en API 25 (sin gesto predictivo);
  - «Quitar animaciones» activado y escala 10×;
  - dos toques rápidos sobre una nota y giro durante la animación.
- Revisas conmigo la animación en el emulador.

## Dudas abiertas

Ninguna.
