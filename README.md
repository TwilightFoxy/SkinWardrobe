# Skin Wardrobe

Skin Wardrobe is a NeoForge 26.1 mod for changing player skins on a server. It works server-side through commands and, when installed on the client too, adds an in-game wardrobe screen.

## Commands

- `/skinwardrobe seturl <url> [classic|slim]` applies a direct PNG skin URL.
- `/skinwardrobe saveurl <name> <url> [classic|slim]` signs, applies, and saves a URL skin.
- `/skinwardrobe use <name>` applies a saved skin.
- `/skinwardrobe delete <name>` removes a saved skin.
- `/skinwardrobe list` lists saved skins.
- `/skinwardrobe reset` restores the default textures.
- `/skinwardrobe current` shows the active wardrobe skin.

Only PNG skins sized `64x64` or `64x32` are accepted.

## Client Wardrobe

Press `O` to open the wardrobe screen. Local PNG files are loaded from:

```text
.minecraft/skinwardrobe/skins
```

Local skins are signed through MineSkin and then sent to the server as signed texture data.

## Notes

Skin signing uses the public MineSkin API, so applying new skins depends on that service being available and not rate-limited.

## Versioning

- Major work or new features bump the second number, for example `0.2.0` -> `0.3.0`.
- Bug fixes and small changes bump the third number, for example `0.2.0` -> `0.2.1`.
- Versions can go past `0.10.0`, `0.20.0`, and so on.
- `1.0.0` is reserved until the maintainer explicitly asks for it.
