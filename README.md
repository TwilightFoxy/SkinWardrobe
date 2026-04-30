# Skin Wardrobe

Skin Wardrobe is a NeoForge 26.1 mod that lets players change Minecraft skins without reconnecting. It can run server-side through commands, and when installed on the client too it adds an in-game wardrobe GUI with a skin carousel and 3D previews.

## English

### Features

- Change your skin from a command using a supported URL.
- Save named skins in a server-side wardrobe.
- Reuse, delete, list, reset, and inspect your current wardrobe skin.
- Optional client GUI opened with `O`.
- 3D skin preview carousel in the GUI.
- Local PNG skin library on the client.
- URL skins applied through the GUI are downloaded into the local library.
- `Classic` and `Slim` model choice in the GUI, remembered locally.
- Dedicated server compatible: client-only GUI code is only loaded on the client.

### Supported Skin Sources

The final image must be a valid Minecraft PNG skin sized `64x64`.

Supported URL forms:

```text
https://example.com/skin.png
https://disk.yandex.ru/i/...
https://ely.by/skins/s3512393
https://namemc.com/skin/6e8a6d2c80f9e54b
https://ru.namemc.com/skin/6e8a6d2c80f9e54b
```

Notes:

- Direct PNG links must point to the PNG file itself.
- Yandex Disk links must be public image/file links.
- Ely.by skin pages are resolved to their stored PNG skin.
- NameMC skin pages are resolved through NameMC's skin CDN.
- Screenshots, preview renders, avatars, and non-`64x64` images are rejected.

### Client GUI

Install the mod on the client and press `O` in-game to open the wardrobe.

The GUI lets you:

- paste a skin URL;
- choose `Classic` or `Slim`;
- apply a skin immediately;
- save a URL skin to the server wardrobe;
- browse local and saved skins in one carousel;
- preview skins as 3D player models;
- delete saved server wardrobe entries;
- reset to the default skin.

The GUI tries to open on the currently active skin. If the active skin also exists as a local downloaded PNG, the local PNG is shown instead of a duplicate saved entry.

### Local Skin Folder

Put local PNG skins here:

```text
.minecraft/skinwardrobe
```

When a URL skin is applied through the GUI, its PNG is downloaded into this same folder. Old files in `.minecraft/skinwardrobe/skins` are still scanned for compatibility, but the current folder is `.minecraft/skinwardrobe`.

Client settings are stored in:

```text
.minecraft/skinwardrobe/settings.json
```

### Commands

```text
/skinwardrobe seturl <url> [classic|slim]
/skinwardrobe saveurl <name> <url> [classic|slim]
/skinwardrobe use <name>
/skinwardrobe delete <name>
/skinwardrobe list
/skinwardrobe reset
/skinwardrobe current
```

Command details:

- `seturl` applies a supported URL without saving it as a named wardrobe entry.
- `saveurl` applies and saves a supported URL under a name.
- `use` applies one of your saved wardrobe entries.
- `delete` removes one of your saved wardrobe entries.
- `list` shows your saved entries.
- `reset` restores the default skin.
- `current` shows the currently active Skin Wardrobe skin.

Players can only manage their own wardrobe. Admin commands for other players are not implemented yet.

### Server Data

Server wardrobe data is stored per world:

```text
<world>/skinwardrobe/wardrobes.json
```

The server stores signed texture data, active skin information, saved entries, source metadata, model type, and timestamps.

### Requirements and Limitations

- Minecraft / NeoForge target: `26.1`.
- Java target: `25`.
- Skin signing uses the public MineSkin API.
- Applying a new skin depends on MineSkin being available and not rate-limited.
- The mod validates PNG size before sending it to MineSkin.
- Skin changes are broadcast to other players without reconnecting.
- Some clients or networks may have trouble downloading Mojang texture URLs; the optional client mod includes a small HTTPS fix for `textures.minecraft.net`.

## Русский

### Возможности

- Смена скина командой по поддерживаемой ссылке.
- Сохранение именных скинов в серверном гардеробе.
- Повторное применение, удаление, список, сброс и проверка текущего скина.
- Опциональное клиентское GUI по клавише `O`.
- Карусель с 3D-предпросмотром скинов.
- Локальная библиотека PNG-скинов на клиенте.
- Скины, примененные по ссылке через GUI, скачиваются в локальную библиотеку.
- Выбор модели `Classic` или `Slim` в GUI, настройка запоминается локально.
- Совместимость с dedicated server: клиентский GUI-код грузится только на клиенте.

### Поддерживаемые источники

Итоговая картинка должна быть валидным PNG-скином Minecraft размером `64x64`.

Поддерживаемые виды ссылок:

```text
https://example.com/skin.png
https://disk.yandex.ru/i/...
https://ely.by/skins/s3512393
https://namemc.com/skin/6e8a6d2c80f9e54b
https://ru.namemc.com/skin/6e8a6d2c80f9e54b
```

Пояснения:

- Прямая PNG-ссылка должна вести именно на PNG-файл.
- Ссылка Яндекс.Диска должна быть публичной ссылкой на картинку/файл.
- Страницы Ely.by автоматически преобразуются в PNG-скин из хранилища Ely.by.
- Страницы NameMC автоматически преобразуются в PNG через CDN NameMC.
- Скриншоты, рендеры предпросмотра, аватарки и картинки не `64x64` отклоняются.

### Клиентское GUI

Установи мод на клиент и нажми `O` в игре, чтобы открыть гардероб.

В GUI можно:

- вставить ссылку на скин;
- выбрать `Classic` или `Slim`;
- сразу применить скин;
- сохранить скин по ссылке в серверный гардероб;
- смотреть локальные и сохраненные скины в одной карусели;
- видеть 3D-предпросмотр модели игрока;
- удалять сохраненные записи серверного гардероба;
- сбрасывать скин на стандартный.

GUI старается открываться на текущем активном скине. Если активный скин уже скачан как локальный PNG, показывается локальный PNG, а не дубль сохраненной записи.

### Папка локальных скинов

Клади локальные PNG-скины сюда:

```text
.minecraft/skinwardrobe
```

Если применить скин по ссылке через GUI, PNG тоже скачивается в эту папку. Старые файлы из `.minecraft/skinwardrobe/skins` пока тоже читаются для совместимости, но актуальная папка: `.minecraft/skinwardrobe`.

Клиентские настройки хранятся здесь:

```text
.minecraft/skinwardrobe/settings.json
```

### Команды

```text
/skinwardrobe seturl <url> [classic|slim]
/skinwardrobe saveurl <name> <url> [classic|slim]
/skinwardrobe use <name>
/skinwardrobe delete <name>
/skinwardrobe list
/skinwardrobe reset
/skinwardrobe current
```

Пояснения к командам:

- `seturl` применяет поддерживаемую ссылку без сохранения именной записи.
- `saveurl` применяет и сохраняет поддерживаемую ссылку под выбранным именем.
- `use` применяет один из твоих сохраненных скинов.
- `delete` удаляет один из твоих сохраненных скинов.
- `list` показывает список сохраненных скинов.
- `reset` возвращает стандартный скин.
- `current` показывает текущий активный скин Skin Wardrobe.

Игроки управляют только своим гардеробом. Админские команды для чужих скинов пока не реализованы.

### Серверные данные

Данные гардероба хранятся отдельно для каждого мира:

```text
<world>/skinwardrobe/wardrobes.json
```

Сервер хранит подписанные texture-данные, активный скин, сохраненные записи, источник, тип модели и временные метки.

### Требования и ограничения

- Целевая версия Minecraft / NeoForge: `26.1`.
- Целевая Java: `25`.
- Подпись скинов идет через публичный API MineSkin.
- Применение нового скина зависит от доступности MineSkin и его лимитов.
- Мод проверяет размер PNG перед отправкой в MineSkin.
- Смена скина рассылается другим игрокам без перезахода.
- У некоторых клиентов или сетей бывают проблемы со скачиванием URL Mojang textures; опциональный клиентский мод включает небольшую HTTPS-правку для `textures.minecraft.net`.

## Versioning

- Major work or new features bump the second number, for example `0.2.0` -> `0.3.0`.
- Bug fixes and small changes bump the third number, for example `0.2.0` -> `0.2.1`.
- Versions can go past `0.10.0`, `0.20.0`, and so on.
- `1.0.0` is reserved until the maintainer explicitly asks for it.
