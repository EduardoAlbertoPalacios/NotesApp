# Constitución — App de Notas

1. **Stack mínimo.** Solo Kotlin, Compose, Hilt, Room y Navigation Compose; toda dependencia nueva requiere aprobación explícita y su versión vive en `libs.versions.toml`.
2. **La spec manda.** Ningún cambio de comportamiento entra sin una spec aprobada en `docs/`; si el código se aparta de ella, spec y código se actualizan en el mismo commit.
3. **Lógica fuera de la UI.** Las reglas de negocio viven en `:domain` (Kotlin puro); los composables solo pintan un `UiState` y emiten `Intent`.
4. **Sin pruebas no hay commit.** Todo caso de uso, ViewModel, repositorio y mapper tiene pruebas unitarias, y toda pantalla al menos una de UI; `./gradlew test` debe pasar antes de cada commit.
5. **Los datos del usuario no se pierden.** Todo cambio de esquema de Room sube la versión, exporta el esquema e incluye una migración probada; `fallbackToDestructiveMigration` está prohibido.
6. **Idioma.** Identificadores y commits en inglés; comentarios y documentación en español; los textos visibles van en español en `strings.xml`, nunca en el código.
