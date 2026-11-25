(function () {
  'use strict';

  const root = document.documentElement;
  const themeToggle = document.getElementById('themeToggle');
  const metaTheme = document.querySelector('meta[name="theme-color"]');

  function applyTheme(theme) {
    root.dataset.theme = theme;
    if (metaTheme) {
      metaTheme.setAttribute('content', theme === 'dark' ? '#121714' : '#f4eee3');
    }
    if (themeToggle) {
      themeToggle.setAttribute(
        'aria-label',
        theme === 'dark' ? 'Включить светлую тему' : 'Включить тёмную тему'
      );
    }
  }

  applyTheme(root.dataset.theme || 'light');
  themeToggle?.addEventListener('click', () => {
    const next = root.dataset.theme === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    try { localStorage.setItem('mimo-theme', next); } catch (_) {}
  });

  const nav = document.getElementById('nav');
  const onScroll = () => {
    if (!nav) return;
    nav.classList.toggle('scrolled', window.scrollY > 12);
  };
  onScroll();
  window.addEventListener('scroll', onScroll, { passive: true });

  const revealItems = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -30px' }
    );
    revealItems.forEach((item) => observer.observe(item));
  } else {
    revealItems.forEach((item) => item.classList.add('visible'));
  }

  // Interactive morphing dock.
  const surface = document.getElementById('demoSurface');
  const menuBtn = document.getElementById('demoMenuBtn');
  const plusBtn = document.getElementById('demoPlusBtn');
  const layoutBtn = document.getElementById('demoLayoutBtn');
  const demoStage = document.getElementById('demoStage');

  function setMode(mode) {
    if (!surface) return;
    surface.dataset.mode = mode;
    const menuPanel = surface.querySelector('.demo-menu-panel');
    const capturePanel = surface.querySelector('.demo-capture-panel');
    if (menuPanel) menuPanel.setAttribute('aria-hidden', String(mode !== 'menu'));
    if (capturePanel) capturePanel.setAttribute('aria-hidden', String(mode !== 'capture'));
    if (menuBtn) menuBtn.classList.toggle('active', mode === 'menu');
    if (plusBtn) plusBtn.classList.toggle('active', mode === 'capture');
    if (plusBtn) plusBtn.textContent = mode === 'capture' ? '×' : '+';
  }

  menuBtn?.addEventListener('click', (event) => {
    event.stopPropagation();
    setMode(surface?.dataset.mode === 'menu' ? 'dock' : 'menu');
  });
  plusBtn?.addEventListener('click', (event) => {
    event.stopPropagation();
    setMode(surface?.dataset.mode === 'capture' ? 'dock' : 'capture');
  });
  layoutBtn?.addEventListener('click', (event) => {
    event.stopPropagation();
    layoutBtn.classList.toggle('list-mode');
  });
  surface?.querySelectorAll('[data-demo-close]').forEach((button) => {
    button.addEventListener('click', (event) => {
      event.stopPropagation();
      setMode('dock');
    });
  });
  surface?.querySelectorAll('.demo-row:not(.active), .demo-options button').forEach((button) => {
    button.addEventListener('click', () => setMode('dock'));
  });
  demoStage?.addEventListener('click', (event) => {
    if (surface && !surface.contains(event.target)) setMode('dock');
  });

  // Elastic card demo.
  const zone = document.getElementById('elasticZone');
  const card = document.getElementById('elasticCard');
  let drag = null;

  function setCardVars(dx, dy) {
    if (!card) return;
    const max = 58;
    const x = Math.max(-max, Math.min(max, dx * 0.62));
    const y = Math.max(-max, Math.min(max, dy * 0.62));
    const rot = x / 22;
    const sx = 1 + Math.abs(x) / 900;
    const sy = 1 + Math.abs(y) / 900;
    card.style.setProperty('--dx', `${x * 0.55}px`);
    card.style.setProperty('--dy', `${y * 0.55}px`);
    card.style.setProperty('--rot', `${rot}deg`);
    card.style.setProperty('--sx', String(sx));
    card.style.setProperty('--sy', String(sy));
  }

  function resetCard() {
    if (!card) return;
    card.classList.remove('dragging');
    card.style.setProperty('--dx', '0px');
    card.style.setProperty('--dy', '0px');
    card.style.setProperty('--rot', '0deg');
    card.style.setProperty('--sx', '1');
    card.style.setProperty('--sy', '1');
  }

  card?.addEventListener('pointerdown', (event) => {
    if (event.button !== 0 && event.pointerType === 'mouse') return;
    drag = { id: event.pointerId, x: event.clientX, y: event.clientY };
    card.classList.add('dragging');
    card.setPointerCapture?.(event.pointerId);
    if (navigator.vibrate) navigator.vibrate(7);
  });
  card?.addEventListener('pointermove', (event) => {
    if (!drag || drag.id !== event.pointerId) return;
    setCardVars(event.clientX - drag.x, event.clientY - drag.y);
  });
  card?.addEventListener('pointerup', (event) => {
    if (!drag || drag.id !== event.pointerId) return;
    drag = null;
    resetCard();
  });
  card?.addEventListener('pointercancel', () => {
    drag = null;
    resetCard();
  });
  zone?.addEventListener('pointerleave', () => {
    if (drag) {
      drag = null;
      resetCard();
    }
  });

  // Lightbox.
  const lightbox = document.getElementById('lightbox');
  const lightboxImage = document.getElementById('lightboxImage');
  const lightboxCaption = document.getElementById('lightboxCaption');
  const lightboxClose = document.getElementById('lightboxClose');
  let lastLightboxTrigger = null;

  function closeLightbox() {
    if (!lightbox) return;
    lightbox.classList.remove('open');
    lightbox.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('no-scroll');
    if (lastLightboxTrigger) lastLightboxTrigger.focus();
  }

  document.querySelectorAll('.screen-card').forEach((button) => {
    button.addEventListener('click', () => {
      if (!lightbox || !lightboxImage) return;
      lastLightboxTrigger = button;
      lightboxImage.src = button.dataset.full || '';
      lightboxImage.alt = button.querySelector('img')?.alt || '';
      if (lightboxCaption) lightboxCaption.textContent = button.dataset.caption || '';
      lightbox.classList.add('open');
      lightbox.setAttribute('aria-hidden', 'false');
      document.body.classList.add('no-scroll');
      lightboxClose?.focus();
    });
  });
  lightboxClose?.addEventListener('click', closeLightbox);
  lightbox?.addEventListener('click', (event) => {
    if (event.target === lightbox) closeLightbox();
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && lightbox?.classList.contains('open')) {
      closeLightbox();
    }
  });

  // Subtle pointer parallax on the hero phone.
  const hero = document.getElementById('heroVisual');
  const phone = hero?.querySelector('.phone-main');
  if (hero && phone && window.matchMedia('(pointer: fine)').matches) {
    hero.addEventListener('pointermove', (event) => {
      const rect = hero.getBoundingClientRect();
      const x = (event.clientX - rect.left) / rect.width - 0.5;
      const y = (event.clientY - rect.top) / rect.height - 0.5;
      phone.style.transform = `rotate(${2.2 - x * 3}deg) translate3d(${x * 8}px, ${y * 8}px, 0)`;
    });
    hero.addEventListener('pointerleave', () => {
      phone.style.transform = '';
    });
  }
})();
