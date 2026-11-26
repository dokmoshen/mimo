# Откат визуального и анимационного обновления

Обновление **1.13.0** можно полностью откатить к стабильной версии **1.12.2**.

## Что сохранено

- Коммит: `69334c4` — `Match menu panel width to quick capture panel`.
- Ветка: `rollback/pre-visual-overhaul`.
- Тег: `pre-visual-overhaul`.
- Ветка и тег отправлены на GitHub.
- Подписанный APK 1.12.2: `docs/download/mimo-release.apk` в этом коммите.
- SHA-256 APK 1.12.2: `614a38ae5a8614c7e9fcf0641c55a7c68283c8a4d3b53641f53ed59901fca9a0`.

## Полный откат кода

Вариант 1 — переключиться на резервную ветку:

```bash
git fetch origin
git checkout rollback/pre-visual-overhaul
```

Вариант 2 — отменить изменения в текущей ветке:

```bash
git reset --hard pre-visual-overhaul
```

Вариант 3 — восстановить только APK на сайте:

```bash
git checkout pre-visual-overhaul -- docs/download/mimo-release.apk
```

После отката собрать release-сборку и подписать её ключом из `keys/`:

```bash
./gradlew :app:testDebugUnitTest :app:lintVitalRelease :app:assembleRelease
```

## Важно

- Папка `keys/` не хранится в git. Без неё восстановить обновления поверх установленной версии нельзя.
- Перед откатом сохраните текущие заметки пользователя: при downgrade APK данные приложения не удаляются, но сделать экспорт всё равно безопаснее.
- Если новая версия уже опубликована, откат APK на сайте делается отдельным коммитом с восстановленным файлом и новым релизом.
