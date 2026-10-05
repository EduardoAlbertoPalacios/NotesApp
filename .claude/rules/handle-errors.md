## Manejo de errores
- Los errores esperables (validación, nota no encontrada) se modelan como resultado en `:domain`, no como excepciones.
- Nunca captures `Exception` de forma genérica ni dejes un `catch` vacío; no atrapes `CancellationException`.
- El ViewModel convierte los errores en estado de UI; el usuario nunca ve un mensaje técnico.