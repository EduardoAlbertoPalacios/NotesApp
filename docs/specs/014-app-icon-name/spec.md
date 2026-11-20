# Spec 014 — Icono y nombre de la app

> **Estado:** implementada · **Fecha:** 2026-11-19 · **Diseño:** archivos del icono entregados por el usuario (`notelet-android-icon`: icono adaptable, versión monocroma, SVG maestro y PNG de 512 × 512).

## Contexto y objetivo

La app se instala con el nombre provisional «NotesApp» y con el icono que Android Studio pone por defecto (el robot verde sobre una cuadrícula). Dentro de la app, el pie de Ajustes la llama «Notas». Quien la tiene instalada no la distingue de un proyecto de prueba, y el nombre cambia según dónde se mire.

**Objetivo:** que la app se llame «Notelet» en todas partes y que muestre su icono oficial, una página blanca con marcador sobre fondo rojo, en todas las versiones de Android que admite.

**Relación con otras specs:**
- La spec 006 (RF-8) sigue vigente: el pie de Ajustes muestra el nombre de la app y su versión. Esta spec fija ese nombre en «Notelet».
- La pantalla de carga de Android 12 o superior (con el fondo del tema elegido) no cambia de comportamiento. Solo cambia el icono que muestra.

## Usuarios / actores

- **Usuario que busca la app** en el lanzador, en la lista de apps o en las recientes.
- **Usuario con iconos temáticos activados** (Android 13 o superior).
- **Usuario con Android 7.0 o 7.1**, donde el sistema no usa iconos adaptables.

## Historias de usuario

- **H1.** Como usuario, quiero reconocer la app por su icono y su nombre, para encontrarla rápido entre las demás.
- **H2.** Como usuario, quiero que la app se llame igual dentro y fuera de ella, para no dudar de que es la misma.

## Requisitos funcionales (criterios de aceptación en EARS)

### Nombre
- **RF-1:** EL SISTEMA mostrará «Notelet» como nombre de la app en el lanzador.
- **RF-2:** EL SISTEMA mostrará «Notelet» como nombre de la app en la lista de aplicaciones de los ajustes del sistema.
- **RF-3:** EL SISTEMA mostrará «Notelet» como nombre de la app en la pantalla de apps recientes.
- **RF-4:** EL SISTEMA mostrará en el pie de Ajustes el texto «Notelet · Versión X», con X igual a la versión instalada.

### Icono
- **RF-5:** EL SISTEMA mostrará como icono de la app una página blanca con un marcador oscuro y tres líneas, sobre fondo rojo, idéntica a los archivos entregados.
- **RF-6:** DONDE el lanzador recorte los iconos con una forma propia (círculo, cuadrado redondeado u otra), EL SISTEMA mostrará la página y el marcador completos, sin cortes.
- **RF-7:** DONDE el lanzador pida el icono redondo, EL SISTEMA mostrará el mismo dibujo sobre el mismo fondo rojo.
- **RF-8:** DONDE el usuario tenga activados los iconos temáticos (Android 13 o superior), EL SISTEMA mostrará la versión monocroma del icono, con los colores que aplique el sistema.
- **RF-9:** MIENTRAS el dispositivo use Android 7.0 o 7.1, EL SISTEMA mostrará el icono de Notelet, tanto en su versión cuadrada como en la redonda.
- **RF-10:** CUANDO la app se abra en Android 12 o superior, EL SISTEMA mostrará el icono de Notelet en la pantalla de carga, sobre el fondo del tema elegido.
- **RF-11:** EL SISTEMA no mostrará el icono anterior (el robot de Android Studio) en ninguna versión de Android ni densidad de pantalla.

## Requisitos no funcionales

- **Fidelidad:** el icono es el entregado, sin redibujarlo, sin cambiar colores ni proporciones. En Android 7.0 y 7.1, las imágenes se generan a partir de esos mismos archivos.
- **Nitidez:** el icono se ve nítido en todas las densidades de pantalla (de mdpi a xxxhdpi), sin bordes borrosos ni dentados a simple vista.
- **Idioma:** «Notelet» es un nombre propio y no se traduce. El resto del pie («Versión») sigue en español.
- **Compatibilidad:** instalar esta versión sobre la anterior conserva las notas y los ajustes.

## Casos límite

- **CL-1.** El lanzador sigue mostrando el icono o el nombre anteriores justo después de actualizar, porque los guarda en caché: es un comportamiento del lanzador. Tras reiniciarlo, o el dispositivo, aparecen los nuevos (RF-1, RF-5).
- **CL-2.** Iconos temáticos desactivados en Android 13 o superior: se ve el icono a color (RF-5).
- **CL-3.** Lanzador con forma de recorte muy cerrada (círculo pequeño o gota): la página y el marcador siguen completos (RF-6).
- **CL-4.** Tema oscuro del sistema o de la app: el icono no cambia; la pantalla de carga usa el fondo del tema elegido (RF-10).
- **CL-5.** Letra o pantalla ampliadas en el sistema: el nombre «Notelet» (7 letras) puede cortarlo el lanzador. Depende del lanzador, no de la app.

## Fuera de alcance

- Mostrar el icono dentro de la app (pie de Ajustes, Ayuda u otras pantallas).
- Personalizar la pantalla de carga (otro fondo, otro icono, animación o marca) o añadirla en versiones anteriores a Android 12.
- La ficha de Google Play (icono de 512 × 512, capturas, descripción): llegará cuando la app se publique.
- Cambiar el identificador de la app, los nombres internos del proyecto o del código.
- Cambiar el título «Mis notas» de la pantalla principal o la palabra «notas» en otros textos.
- Cambiar el nombre del archivo de exportación «Notas AAAA-MM-DD.zip» (spec 011).
- Iconos de acceso directo, widgets o notificaciones.

## Criterios de finalización

- Prueba de UI del pie de Ajustes con «Notelet · Versión X» en verde, en ambos temas.
- La app compila y todas las pruebas pasan.
- Demo manual en el emulador:
  - el nombre en el lanzador, la lista de apps y las recientes;
  - el icono con al menos dos formas de recorte;
  - los iconos temáticos activados y desactivados;
  - la pantalla de carga en tema claro y en tema oscuro;
  - el icono en un emulador con Android 7.x, en las versiones cuadrada y redonda.
- Revisas conmigo el icono en el emulador frente a los archivos entregados.

## Dudas abiertas

Ninguna.
