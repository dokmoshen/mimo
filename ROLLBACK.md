# Откат обновлений Mimo

## 1.14.0 → 1.13.0 (motion pass)

Точка отката перед анимационным обновлением:

- Коммит: `0606322` — `Rebuild promo with Remotion 2.5D composition` (стабильная 1.13.0).
- Ветка: `rollback/pre-motion-pass`.
- Тег: `pre-motion-pass`.
- Ветка и тег отправлены на GitHub.
- APK 1.13.0: `docs/download/mimo-release.apk` в этом коммите.

Полный откат:

```bash
git fetch origin
git checkout rollback/pre-motion-pass
```

Или отменить изменения в текущей ветке:

```bash
git reset --hard pre-motion-pass
```

Восстановить только APK на сайте:

```bash
git checkout pre-motion-pass -- docs/download/mimo-release.apk
```

## 1.13.0 → 1.12.2 (visual pass)

Более глубокая точка отката перед визуальным перезапуском:

- Коммит: `69334c4` — `Match menu panel width to quick capture panel`.
- Ветка: `rollback/pre-visual-overhaul`.
- Тег: `pre-visual-overhaul`.
- APK 1.12.2, SHA-256: `614a38ae5a8614c7e9fcf0641c55a7c68283c8a4d3b53641f53ed59901fca9a0`.

```bash
git checkout rollback/pre-visual-overhaul
```

## После отката

Собрать release-сборку и подписать её ключом из `keys/`:

```bash
./gradlew :app:testDebugUnitTest :app:lintVitalRelease :app:assembleRelease
```

## Важно

- Папка `keys/` не хранится в git. Без неё восстановить обновления поверх установленной версии нельзя.
- Перед откатом сохраните заметки пользователя: при downgrade APK данные не удаляются, но экспорт всё равно безопаснее.
- Если новая версия уже опубликована, откат APK на сайте делается отдельным коммитом с восстановленным файлом и новым релизом.
