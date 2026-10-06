## Compose
- Todo composable público recibe `modifier: Modifier = Modifier` como primer parámetro opcional. Excepción: los composables de tema (como `NotesAppTheme`), que solo proveen valores y no dibujan layout.
- Las listas (`LazyColumn`) usan `key` estable, con el id de la nota.
- Colores, tipografías y espaciados salen del tema; nada de valores escritos a mano en las pantallas.
- Todo elemento interactivo tiene `contentDescription` o etiqueta accesible.