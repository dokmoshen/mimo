# MimoPromo — Remotion

Промо-ролик Mimo 9:16, 1080×1920, 30 fps, 675 кадров.

- Композиция: `MimoPromo` в `src/Root.tsx`.
- Сцены и 2.5D-камера: `src/Promo.tsx`.
- Шрифты и ассеты: `public/`.
- Официальные Remotion Skills: `.agents/skills/`.

## Preview

```bash
npm run dev
```

## Render

```bash
npx remotion render MimoPromo out/mimo.mp4 --codec=h264 --audio-codec=aac --crf=18
```

Если загрузка Chrome Headless Shell блокируется сетью, скачайте её с `storage.googleapis.com/chrome-for-testing-public` и передайте `--browser-executable=/path/to/chrome-headless-shell`.
