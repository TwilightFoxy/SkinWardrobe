# Skin Wardrobe

Skin Wardrobe is a NeoForge 26.1 mod for changing player skins on a server. It works server-side through commands, and when installed on the client too it adds an in-game wardrobe screen with skin previews.

## English

### What links work?

Skin Wardrobe accepts direct Minecraft skin PNG files sized `64x64`. Public Yandex Disk image links, Ely.by skin pages, and NameMC skin pages are supported too, for example links like:

```text
https://disk.yandex.ru/i/...
https://ely.by/skins/s3512393
https://namemc.com/skin/6e8a6d2c80f9e54b
```

The final image must still be a valid Minecraft skin PNG. If the image is a screenshot, preview image, or a non-`64x64` picture, the mod will reject it.

### Local skins

With the client mod installed, put PNG skin files here:

```text
.minecraft/skinwardrobe
```

URL skins applied through the GUI are also downloaded into this folder, so local files and downloaded skins appear in the same carousel.

### Commands

- `/skinwardrobe seturl <url> [classic|slim]` applies a skin URL.
- `/skinwardrobe saveurl <name> <url> [classic|slim]` signs, applies, and saves a URL skin.
- `/skinwardrobe use <name>` applies a saved skin.
- `/skinwardrobe delete <name>` removes a saved skin.
- `/skinwardrobe list` lists saved skins.
- `/skinwardrobe reset` restores the default textures.
- `/skinwardrobe current` shows the active wardrobe skin.

Skin signing uses the public MineSkin API, so applying new skins depends on that service being available and not rate-limited.

## Русский

### Какие ссылки подходят?

Skin Wardrobe принимает ссылки на PNG-скины Minecraft размером `64x64`. Публичные ссылки Яндекс.Диска, страницы скинов Ely.by и NameMC тоже поддерживаются, например:

```text
https://disk.yandex.ru/i/...
https://ely.by/skins/s3512393
https://namemc.com/skin/6e8a6d2c80f9e54b
```

Итоговая картинка должна быть валидным PNG-скином Minecraft. Если это скриншот, превью или картинка не `64x64`, мод ее отклонит.

### Локальные скины

Если мод установлен на клиенте, закидывай PNG-скины сюда:

```text
.minecraft/skinwardrobe
```

Скины, примененные по ссылке через GUI, тоже скачиваются в эту папку. Поэтому вручную добавленные и скачанные скины отображаются в одной карусели.

### Команды

- `/skinwardrobe seturl <url> [classic|slim]` применяет скин по ссылке.
- `/skinwardrobe saveurl <name> <url> [classic|slim]` подписывает, применяет и сохраняет скин по ссылке.
- `/skinwardrobe use <name>` применяет сохраненный скин.
- `/skinwardrobe delete <name>` удаляет сохраненный скин.
- `/skinwardrobe list` показывает список сохраненных скинов.
- `/skinwardrobe reset` возвращает стандартный скин.
- `/skinwardrobe current` показывает текущий активный скин.

Подпись скинов идет через публичный API MineSkin, поэтому применение новых скинов зависит от доступности MineSkin и его лимитов.

## Versioning

- Major work or new features bump the second number, for example `0.2.0` -> `0.3.0`.
- Bug fixes and small changes bump the third number, for example `0.2.0` -> `0.2.1`.
- Versions can go past `0.10.0`, `0.20.0`, and so on.
- `1.0.0` is reserved until the maintainer explicitly asks for it.
