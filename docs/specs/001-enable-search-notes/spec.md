# Spec 001 — Buscar en mis notas

> **Estado:** implementada · **Fecha:** 2026-10-13

## 1. Contexto y objetivo

La pantalla **Mis notas** ya muestra un buscador, pero es solo visual: no hace nada. A medida que el usuario acumula notas, encontrar algo que escribió hace tiempo obliga a recorrer la cuadrícula a ojo o a abrir nota por nota.

**Objetivo:** que el usuario recupere en segundos cualquier nota a partir de lo que recuerda haber escrito, aunque no recuerde el texto exacto, sus mayúsculas o sus acentos.

## 2. Usuarios

- **Usuario de la app con muchas notas:** sabe que escribió algo («la reunión con proveedores», «la receta») pero no dónde ni cuándo.
- **Usuario que organiza por categorías:** separa Trabajo y Personal, pero a veces no recuerda en qué categoría guardó una nota.

## 3. Historias de usuario

- **HU-1.** Como usuario, quiero escribir una palabra en el buscador y ver solo las notas que la contienen, para encontrar rápido lo que busco.
- **HU-2.** Como usuario, quiero que la búsqueda encuentre la nota aunque escriba sin acentos, en otras mayúsculas o con las palabras en otro orden, para no tener que recordar el texto exacto.
- **HU-3.** Como usuario, quiero que la búsqueda incluya todas mis notas sin importar la categoría seleccionada, para encontrar la nota aunque no recuerde dónde la clasifiqué.
- **HU-4.** Como usuario, quiero abrir una nota de los resultados y, al volver, seguir viendo mi búsqueda, para revisar varios resultados sin volver a escribirla.
- **HU-5.** Como usuario, quiero saber cuántas notas coinciden y enterarme claramente cuando ninguna lo hace, para no pensar que la app falló.

## 4. Requisitos funcionales

### RF-1. Búsqueda al escribir
- **CA-1.1.** Cuando el usuario escriba en el buscador, el sistema debe mostrar en la cuadrícula de Mis notas solo las notas que coincidan con el texto, sin pedir confirmación ni abrir otra pantalla.
- **CA-1.2.** Cuando el usuario modifique o borre caracteres del texto buscado, el sistema debe actualizar los resultados para reflejar el texto nuevo.
- **CA-1.3.** Mientras el buscador esté vacío o contenga solo espacios, el sistema debe mostrar las notas como si no hubiera búsqueda.

### RF-2. Campos en los que se busca
- **CA-2.1.** El sistema debe considerar que una nota coincide cuando el texto buscado aparezca en su título o en su contenido.
- **CA-2.2.** El sistema no debe considerar como coincidencia el nombre de la categoría, el color, la fecha ni las imágenes de la nota.

### RF-3. Coincidencia flexible
- **CA-3.1.** El sistema debe ignorar la diferencia entre mayúsculas y minúsculas al comparar (incluidas «Ñ» y «ñ»).
- **CA-3.2.** El sistema debe ignorar las tildes y la diéresis al comparar (por ejemplo, «reunion» coincide con «Reunión» y «pinguino» con «pingüino»).
- **CA-3.3.** El sistema debe tratar la «ñ» como una letra distinta de la «n» (por ejemplo, «ano» no coincide con «año»).
- **CA-3.4.** El sistema debe considerar coincidencia una parte de una palabra (por ejemplo, «reun» coincide con «reunión»).
- **CA-3.5.** Cuando el texto buscado tenga varias palabras, el sistema debe mostrar solo las notas que contengan todas esas palabras, en cualquier orden y repartidas entre título y contenido.
- **CA-3.6.** El sistema debe ignorar los espacios al inicio, al final y repetidos entre palabras.

### RF-4. Búsqueda independiente de la categoría
- **CA-4.1.** Mientras haya texto en el buscador, el sistema debe buscar en todas las notas, sin importar el filtro Todas / Trabajo / Personal seleccionado.
- **CA-4.2.** Mientras haya texto en el buscador, el sistema debe mostrar los chips de categoría como inactivos, sin perder cuál estaba seleccionado.
- **CA-4.3.** Cuando el usuario toque un chip de categoría con una búsqueda activa, el sistema debe vaciar el buscador, seleccionar ese chip y mostrar las notas de esa categoría.
- **CA-4.4.** Cuando el usuario vacíe el buscador, el sistema debe volver a mostrar las notas según el chip que estaba seleccionado.

### RF-5. Orden de los resultados
- **CA-5.1.** El sistema debe ordenar los resultados con el mismo criterio que la lista sin búsqueda: primero las notas fijadas y después por última edición, de la más reciente a la más antigua.

### RF-6. Número de resultados
- **CA-6.1.** Mientras haya texto en el buscador, el sistema debe mostrar en el encabezado el número de notas que coinciden (por ejemplo, «1 resultado», «3 resultados») en lugar del resumen habitual.
- **CA-6.2.** Cuando el usuario vacíe el buscador, el sistema debe volver a mostrar el resumen habitual del encabezado.

### RF-7. Sin resultados
- **CA-7.1.** Si ninguna nota coincide con el texto buscado, entonces el sistema debe mostrar un mensaje que incluya el texto buscado, por ejemplo «No hay notas que coincidan con «texto»».
- **CA-7.2.** Si ninguna nota coincide, entonces el sistema no debe mostrar el mensaje de «sin notas» que aparece cuando el usuario no tiene ninguna nota.

### RF-8. Limpiar la búsqueda
- **CA-8.1.** Mientras haya texto en el buscador, el sistema debe mostrar una acción para limpiarlo.
- **CA-8.2.** Cuando el usuario toque la acción de limpiar, el sistema debe vaciar el buscador y volver a mostrar las notas según el chip que estaba seleccionado.

### RF-9. Conservar la búsqueda
- **CA-9.1.** Cuando el usuario abra una nota desde los resultados y vuelva a Mis notas, el sistema debe conservar el texto buscado.
- **CA-9.2.** Cuando el usuario vuelva tras editar o eliminar una nota, el sistema debe recalcular los resultados con el contenido actual de las notas: una nota editada que deja de coincidir desaparece y una que pasa a coincidir aparece.
- **CA-9.3.** Cuando el sistema cierre la app por su cuenta (por ejemplo, por falta de memoria o un cambio de orientación) y el usuario vuelva a ella, el sistema debe restaurar el texto buscado.
- **CA-9.4.** Cuando el usuario cierre la app y la vuelva a abrir, el sistema debe mostrar Mis notas sin búsqueda activa.

### RF-10. Buscador sin notas
- **CA-10.1.** Mientras el usuario no tenga ninguna nota, el sistema debe mostrar el buscador deshabilitado y el mensaje habitual de lista vacía.
- **CA-10.2.** Cuando el usuario cree su primera nota, el sistema debe habilitar el buscador.

### RF-11. Nota nueva durante una búsqueda
- **CA-11.1.** Cuando el usuario cree una nota nueva con una búsqueda activa, el sistema debe asignarle la categoría del chip que estaba seleccionado antes de buscar, igual que sin búsqueda.

## 5. Requisitos no funcionales

- **RNF-1. Rapidez.** Con hasta 1 000 notas guardadas, los resultados deben verse en menos de 300 ms desde la última tecla pulsada.
- **RNF-2. Fluidez.** Escribir en el buscador no debe congelar la pantalla ni perder caracteres.
- **RNF-3. Privacidad.** La búsqueda ocurre solo en el dispositivo; el texto buscado no se envía a ningún servicio ni se guarda como historial más allá de lo indicado en CA-9.3.
- **RNF-4. Sin conexión.** La búsqueda funciona sin internet.
- **RNF-5. Accesibilidad.** El buscador y la acción de limpiar tienen etiquetas accesibles; el estado inactivo de los chips, el número de resultados y el mensaje de «sin resultados» los anuncian los lectores de pantalla.
- **RNF-6. Idioma.** Todos los textos visibles nuevos (marcador del buscador, número de resultados, mensaje de sin resultados, acción de limpiar) están en español, con singular y plural correctos.

## 6. Casos límite

- **CL-1.** Texto con solo espacios: se comporta como buscador vacío (CA-1.3).
- **CL-2.** Texto con signos, números o emojis: se busca tal cual, sin error.
- **CL-3.** Texto muy largo (por ejemplo, un párrafo pegado): la app no se bloquea; lo normal es que no haya resultados.
- **CL-4.** Nota sin título, solo con contenido (o al revés): puede coincidir por el campo que sí tiene.
- **CL-5.** Nota que solo tiene imágenes: nunca coincide con una búsqueda de texto.
- **CL-6.** El usuario no tiene ninguna nota: buscador deshabilitado y mensaje de lista vacía (RF-10).
- **CL-7.** Búsqueda con «ñ» o «n»: «año» no coincide con «ano», pero «AÑO» sí coincide con «año» (CA-3.1, CA-3.3).
- **CL-8.** Con una búsqueda activa, el usuario elimina la última nota que le queda: se muestra el mensaje de lista vacía, y el buscador se vacía y se deshabilita (RF-10).
- **CL-9.** Con una búsqueda activa, el usuario crea una nota nueva: nace en la categoría del chip seleccionado antes de buscar (RF-11) y, al volver, aparece en los resultados solo si coincide con el texto buscado.

## 7. Fuera de alcance

- Buscar por categoría, color, fecha o dentro de imágenes.
- Combinar la búsqueda con un filtro de categoría.
- Resaltar el texto coincidente dentro de las tarjetas.
- Historial o sugerencias de búsquedas recientes, y recordar la búsqueda después de que el usuario cierre la app.
- Búsqueda tolerante a errores de escritura (por ejemplo, «reunoin» → «reunión»).
- Crear una nota a partir del texto buscado desde el mensaje de sin resultados.
- Una pantalla de búsqueda separada.
- Buscar dentro del editor de una nota.
- Cambiar el orden de los resultados por relevancia.

## 8. Criterios de finalización

- Todos los criterios de aceptación de RF-1 a RF-11 se cumplen y tienen pruebas que los verifican.
- Los casos límite CL-1 a CL-9 tienen pruebas.
- RNF-1 se respalda con el diseño y una comprobación manual con las notas existentes; no se mide con 1 000 notas.
- Las pruebas del proyecto pasan, según la constitución del proyecto.
- Se comprueba a mano en un dispositivo o emulador: buscar, ver el número de resultados, abrir un resultado y volver con la búsqueda conservada, girar la pantalla, tocar un chip durante la búsqueda, limpiarla y ver el mensaje de sin resultados.
- Los textos visibles nuevos están en español y ninguno está escrito directamente en el código.

## 9. Dudas abiertas

Ninguna por ahora.
