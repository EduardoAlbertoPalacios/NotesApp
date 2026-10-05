# MEMORY.md — App de Notas

Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte.

## Estado actual
- v1 en desarrollo: aún sin código. Listos `CLAUDE.md` y las skills `new-feature`, `write-tests` y `room-migration`.
- Alcance de v1: agregar notas de texto, ver el listado, editarlas, eliminarlas y ordenarlas por fecha.
- Datos en Room, solo en el dispositivo.

## Decisiones (y por qué)
- Clean Architecture con módulos `:app`, `:presentation`, `:domain`, `:data` y `:common`: separar responsabilidades y poder probar cada capa por separado.
- MVI en presentación: un solo `UiState` por pantalla hace el estado predecible y fácil de probar.
- Jetpack Compose + Hilt + Room: stack estándar de Android, sin dependencias fuera de lo necesario.
- Solo notas de texto en v1: sin imágenes, etiquetas, búsqueda ni sincronización, para cerrar un flujo completo antes de crecer.
- Pruebas unitarias y de UI desde el inicio: cada caso de uso, ViewModel y pantalla nace con sus pruebas.

## Aprendizajes y errores a evitar
- (vacío por ahora)

## Próximos pasos
- Crear el proyecto con los cinco módulos y el version catalog.
- Definir `Note` en `:domain` y `NoteEntity`, `NoteDao` y `NotesDatabase` (versión 1) en `:data`.
- Decidir el orden por fecha: ¿creación o última modificación?, ¿descendente fijo o lo elige el usuario?
- Pantalla de listado (`/new-feature`) y pantalla de crear/editar nota.
- Eliminar nota, con confirmación o deshacer.