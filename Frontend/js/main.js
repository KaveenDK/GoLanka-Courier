// main.js (fragment loading disabled — paste entire file)
'use strict';

(function (window, document) {
  const $ = window.jQuery || null;

  if (!$) {
    console.warn('jQuery not detected. main.js will run but jQuery-based conveniences will be skipped.');
  }

  const MAIN = {
    fragmentCache: {},
    themeKey: 'theme',
    tokenKeys: { access: 'accessToken', refresh: 'refreshToken' },

    _fragmentCandidates: function () {
      const baseOverride = (window.COMPONENTS_BASE && String(window.COMPONENTS_BASE)) || null;
      const candidates = [];
      if (baseOverride) {
        const b = baseOverride.endsWith('/') ? baseOverride.slice(0, -1) : baseOverride;
        candidates.push(`${b}/components`);
      }
      candidates.push('/components');
      candidates.push('components');
      candidates.push('../components');
      candidates.push('../../components');
      candidates.push('./components');
      return candidates;
    },

    // NOTE: Fragment loading intentionally disabled — pages will include header/footer manually.
    // This function used to attempt remote loads; now it is a no-op to avoid fetch errors.
    loadFragments: async function () {
      console.info('Fragment loading disabled by configuration. Please include header/footer HTML manually in each page.');
      return Promise.resolve();
    },

    // (Other fragment helpers are kept as helpers but are not called because loadFragments is noop.)
    loadFragmentSmart: async function (name, targetSelector) {
      throw new Error('Fragment loader disabled. Please embed component HTML directly in the page.');
    },
    loadFragment: async function () {
      throw new Error('Fragment loader disabled. Please embed component HTML directly in the page.');
    },
    _injectFragment: async function () {
      throw new Error('Fragment loader disabled. Please embed component HTML directly in the page.');
    },
    _runScriptNode: function () {
      // No-op stub if fragment loader ever gets called mistakenly
      return Promise.resolve();
    },

    // ---------------- Theme helpers ----------------
    initTheme: function () {
      const stored = localStorage.getItem(this.themeKey);
      const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
      const initial = stored || (prefersDark ? 'dark' : 'light');
      this.applyTheme(initial);

      document.addEventListener('click', ev => {
        const t = ev.target && ev.target.closest ? ev.target.closest('#theme-toggle') : null;
        if (t) {
          ev.preventDefault();
          const isDark = document.documentElement.classList.contains('dark');
          this.setTheme(isDark ? 'light' : 'dark');
        }
      });

      if (window.matchMedia) {
        const mq = window.matchMedia('(prefers-color-scheme: dark)');
        mq.addEventListener?.('change', e => {
          if (!localStorage.getItem(this.themeKey)) {
            this.applyTheme(e.matches ? 'dark' : 'light');
          }
        });
      }
    },

    applyTheme: function (mode) {
      if (mode === 'dark') document.documentElement.classList.add('dark');
      else document.documentElement.classList.remove('dark');
      const btn = document.getElementById('theme-toggle');
      if (btn) btn.setAttribute('aria-pressed', String(mode === 'dark'));
    },

    setTheme: function (mode) {
      localStorage.setItem(this.themeKey, mode);
      this.applyTheme(mode);
      this.showToast({ title: `Switched to ${mode} mode`, toast: true, timer: 1200 });
    },

    // ---------------- Token helpers ----------------
    getAccessToken: function () {
      return localStorage.getItem(this.tokenKeys.access) || null;
    },
    setAccessToken: function (token) {
      if (token) localStorage.setItem(this.tokenKeys.access, token);
      else localStorage.removeItem(this.tokenKeys.access);
    },
    clearTokens: function () {
      localStorage.removeItem(this.tokenKeys.access);
      localStorage.removeItem(this.tokenKeys.refresh);
    },

    // ---------------- JWT helpers ----------------
    decodeJwtPayload: function (token) {
      if (!token) return null;
      try {
        const parts = token.split('.');
        if (parts.length < 2) return null;
        const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const json = decodeURIComponent(atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
        return JSON.parse(json);
      } catch (e) {
        return null;
      }
    },

    getRolesFromToken: function () {
      const payload = this.decodeJwtPayload(this.getAccessToken());
      if (!payload) return [];
      const roles = payload.roles || payload.role || payload.authorities || [];
      return Array.isArray(roles) ? roles : [roles];
    },

    applyRoleGuards: function () {
      const roles = this.getRolesFromToken();
      document.querySelectorAll('[data-role]').forEach(el => {
        const req = (el.getAttribute('data-role') || '').split(',').map(s => s.trim()).filter(Boolean);
        if (req.length === 0) return;
        const allowed = req.some(r => roles.includes(r));
        el.classList.toggle('hidden', !allowed);
      });
    },

    // ---------------- UI wiring ----------------
    attachUIHandlers: function () {
      document.addEventListener('click', ev => {
        const t = ev.target && ev.target.closest ? ev.target.closest('#mobile-toggle') : null;
        if (!t) return;
        ev.preventDefault();
        const expanded = t.getAttribute('aria-expanded') === 'true';
        t.setAttribute('aria-expanded', String(!expanded));
        const nav = document.getElementById('mobile-nav');
        if (nav) nav.classList.toggle('hidden');
      });

      document.addEventListener('click', ev => {
        const btn = ev.target && ev.target.closest ? ev.target.closest('#user-menu-btn') : null;
        if (btn) {
          ev.stopPropagation();
          const menu = document.getElementById('user-menu');
          if (menu) menu.classList.toggle('hidden');
        } else {
          const menu = document.getElementById('user-menu');
          if (menu) menu.classList.add('hidden');
        }
      });

      document.addEventListener('click', ev => {
        const b = ev.target && ev.target.closest ? ev.target.closest('#signout-btn') : null;
        if (!b) return;
        ev.preventDefault();
        this.clearTokens();
        this.showToast({ icon: 'success', title: 'Signed out', toast: true, timer: 900 });
        setTimeout(() => { window.location.href = '/pages/auth/signin.html'; }, 900);
      });

      this.populateUserInfo();

      document.querySelectorAll('a[href^="#"]').forEach(a => {
        a.addEventListener('click', ev => {
          const href = a.getAttribute('href');
          if (!href || href.length < 2) return;
          const id = href.slice(1);
          const el = document.getElementById(id);
          if (el) {
            el.tabIndex = -1;
            el.focus({ preventScroll: true });
          }
        });
      });

      document.addEventListener('keydown', ev => {
        if ((ev.ctrlKey || ev.metaKey) && ev.key === 't') {
          ev.preventDefault();
          const isDark = document.documentElement.classList.contains('dark');
          this.setTheme(isDark ? 'light' : 'dark');
        }
      });
    },

    populateUserInfo: function () {
      const payload = this.decodeJwtPayload(this.getAccessToken());
      if (!payload) return;
      const email = payload.sub || payload.email;
      const name = payload.name || payload.fullName || payload.sub;
      if (email) document.querySelectorAll('[data-user="email"]').forEach(el => el.textContent = email);
      if (name) document.querySelectorAll('[data-user="name"]').forEach(el => el.textContent = name);
    },

    showToast: function (opts = {}) {
      if (window.Swal) {
        const conf = {
          toast: !!opts.toast,
          position: opts.position || 'top-end',
          showConfirmButton: !!opts.showConfirmButton || false,
          timer: opts.timer || (opts.toast ? 2000 : undefined),
          timerProgressBar: !!opts.timer,
          icon: opts.icon || undefined,
          title: opts.title || undefined,
          text: opts.text || undefined
        };
        return Swal.fire(conf);
      } else {
        if (opts.title) console.log('[toast]', opts.icon || '', opts.title, opts.text || '');
        return Promise.resolve();
      }
    },

    init: async function () {
      try {
        // fragment loading intentionally disabled; pages must include header/footer manually
        await this.loadFragments();

        if (typeof window.initHeaderBehavior === 'function') {
          try { window.initHeaderBehavior(); } catch (e) { console.warn('initHeaderBehavior threw', e); }
        }

        this.initTheme();
        this.attachUIHandlers();
        this.applyRoleGuards();

        console.info('GoLankaMain initialized successfully (fragments disabled)');
      } catch (err) {
        console.error('GoLankaMain init error:', err);
        if (window.Swal) {
          Swal.fire({ icon: 'error', title: 'Init error', text: String(err) });
        }
      }
    }
  };

  window.GoLankaMain = MAIN;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => MAIN.init());
  } else {
    MAIN.init();
  }
})(window, document);
