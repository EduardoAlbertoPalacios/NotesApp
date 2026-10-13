---
description: Crea una rama, hace commit, push y abre un PR hacia main
argument-hint: [descripción breve del cambio]
---

Crea un pull request con mis cambios actuales. Contexto opcional: $ARGUMENTS

1. Revisa `git status` y `git diff` para entender los cambios.
2. Si estoy en `main`, crea una rama con formato `tipo/descripcion-corta`
   (feat, fix, refactor, chore) según los cambios.
3. Haz commit con conventional commits. No incluyas archivos que
   parezcan secretos o configuración local.
4. Haz push de la rama con `-u origin`.
5. Abre el PR hacia `main` usando el MCP de GitHub, con:
    - Título en formato conventional commits
    - Descripción con: qué cambia, por qué, y cómo probarlo
6. Devuélveme el link del PR.

Si no hay cambios que commitear, dímelo y no hagas nada más.