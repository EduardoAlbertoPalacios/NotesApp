## Compose
- Todo composable público recibe `modifier: Modifier = Modifier` como primer parámetro opcional.
- Las listas (`LazyColumn`) usan `key` estable, con el id de la nota.
- Colores, tipografías y espaciados salen del tema; nada de valores escritos a mano en las pantallas.
- Todo elemento interactivo tiene `contentDescription` o etiqueta accesible.