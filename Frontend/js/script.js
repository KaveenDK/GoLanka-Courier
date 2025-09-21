/**
 * script.js (improved)
 * Core helpers: apiFetch wrapper, auth helpers, token refresh
 *
 * - Adds richer error reporting (includes response text/html for 4xx/5xx)
 * - Adds optional mock fallback for frontend testing when backend returns 500
 * - Keeps same public API as before
 */

'use strict';

const Api = (function () {
  console.log('script.js (improved) loading...');

  // ---------- Config (override via window.* if you want) ----------
  const API_BASE = (typeof window.API_BASE === 'string' && window.API_BASE) || 'http://localhost:8080';
  const SIGNIN_PAGE = (typeof window.SIGNIN_PAGE === 'string' && window.SIGNIN_PAGE) || '/pages/auth/signin.html';

  // When true, if a server endpoint returns 500 the client will return mock data for a few endpoints.
  // ONLY enable temporarily for frontend dev. Default false.
  const USE_MOCK_ON_500 = !!window.USE_MOCK_ON_500;

  const ACCESS_TOKEN_KEY = 'accessToken';
  const REFRESH_TOKEN_KEY = 'refreshToken';

  // Single-flight refresh promise
  let refreshPromise = null;

  // ---------- Helpers ----------
  function safeJsonParse(text) {
    if (!text || typeof text !== 'string') return null;
    try { return JSON.parse(text); } catch (e) { return null; }
  }

  function makeError(status, body, message, rawText) {
    const err = new Error(message || (body && body.message) || `Request failed (${status})`);
    err.status = status;
    err.body = body || null;
    if (rawText) err.bodyRaw = rawText;
    return err;
  }

  // ---------- Storage ----------
  function getAccessToken() { return localStorage.getItem(ACCESS_TOKEN_KEY); }
  function setAccessToken(token) { if (token) localStorage.setItem(ACCESS_TOKEN_KEY, token); else localStorage.removeItem(ACCESS_TOKEN_KEY); }
  function getRefreshTokenLocal() { return localStorage.getItem(REFRESH_TOKEN_KEY); }
  function setRefreshTokenLocal(token) { if (token) localStorage.setItem(REFRESH_TOKEN_KEY, token); else localStorage.removeItem(REFRESH_TOKEN_KEY); }

  // ---------- JWT decode ----------
  function decodeJwt(token) {
    if (!token) return null;
    try {
      const parts = token.split('.');
      if (parts.length < 2) return null;
      const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(atob(payload).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
      return JSON.parse(json);
    } catch (e) {
      console.warn('JWT decode failed', e);
      return null;
    }
  }

  // ---------- UI helpers ----------
  function toast(opts = {}) {
    if (window.Swal) {
      return Swal.fire(Object.assign({
        toast: !!opts.toast,
        position: opts.position || 'top-end',
        showConfirmButton: !!opts.showConfirmButton || false,
        timer: opts.timer || (opts.toast ? 2000 : undefined),
        timerProgressBar: !!opts.timer,
      }, { icon: opts.icon, title: opts.title, text: opts.text }));
    } else {
      console.log('[toast]', opts.icon || '', opts.title || opts.text || '');
      return Promise.resolve();
    }
  }

  function swalError(title = 'Error', text = '') {
    if (window.Swal) return Swal.fire({ icon: 'error', title, text });
    alert(title + (text ? '\n\n' + text : ''));
    return Promise.resolve();
  }

  // ---------- Auth helpers ----------
  function isAuthenticated() {
    const token = getAccessToken();
    if (!token) return false;
    const payload = decodeJwt(token);
    if (!payload || !payload.exp) return true;
    const exp = payload.exp;
    const expMs = exp > 1e12 ? exp : exp * 1000;
    return Date.now() < expMs;
  }

  function getUser() {
    const token = getAccessToken();
    const payload = decodeJwt(token);
    if (!payload) return null;
    return {
      email: payload.sub || payload.email,
      name: payload.name || payload.fullName || null,
      roles: payload.roles || payload.authorities || []
    };
  }

  function getRoles() {
    const u = getUser();
    return u ? (Array.isArray(u.roles) ? u.roles : [u.roles]) : [];
  }

  function applyTokenResponse(json) {
    if (!json) return;
    if (json.accessToken) setAccessToken(json.accessToken);
    if (json.refreshToken) setRefreshTokenLocal(json.refreshToken);
  }

  // ---------- Single-flight refresh ----------
  async function refreshAccessToken() {
    if (refreshPromise) return refreshPromise;
    refreshPromise = (async () => {
      try {
        const url = `${API_BASE}/api/auth/refresh`;
        const resp = await fetch(url, {
          method: 'POST',
          credentials: 'include',
          headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
          body: JSON.stringify({})
        });

        const text = await resp.text().catch(() => '');
        const json = safeJsonParse(text);

        if (!resp.ok) {
          setAccessToken(null);
          setRefreshTokenLocal(null);
          throw makeError(resp.status, json || text, `Refresh failed (${resp.status})`, text);
        }

        applyTokenResponse(json);
        return json;
      } finally {
        refreshPromise = null;
      }
    })();

    return refreshPromise;
  }

  // ---------- apiFetch wrapper (detailed error info) ----------
  async function apiFetch(path, opts = {}) {
    const url = path.startsWith('http://') || path.startsWith('https://') ? path : `${API_BASE}${path}`;
    const options = Object.assign({ method: 'GET', credentials: 'include', headers: {} }, opts);

    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
      options.headers['Content-Type'] = options.headers['Content-Type'] || 'application/json';
      options.body = JSON.stringify(options.body);
    }

    const token = getAccessToken();
    if (token) {
      options.headers = options.headers || {};
      options.headers['Authorization'] = `Bearer ${token}`;
    }

    let resp;
    try {
      resp = await fetch(url, options);
    } catch (networkErr) {
      console.error('Network error while fetching', url, networkErr);
      throw makeError(0, null, `Network error: ${networkErr.message || networkErr}`);
    }

    // Handle 401 -> try refresh once
    if (resp.status === 401) {
      try {
        await refreshAccessToken();
        const newToken = getAccessToken();
        if (newToken) options.headers['Authorization'] = `Bearer ${newToken}`;
        else delete options.headers['Authorization'];
        resp = await fetch(url, options);
      } catch (e) {
        console.warn('Token refresh failed', e);
        setAccessToken(null);
        setRefreshTokenLocal(null);
        try { await fetch(`${API_BASE}/api/auth/logout`, { method: 'POST', credentials: 'include' }); } catch (ignore) {}
        throw makeError(401, null, 'Unauthenticated');
      }
    }

    return resp;
  }

  // ---------- parse as JSON and throw on non-ok but include raw body ---------- 
  async function apiJson(path, opts = {}) {
    const resp = await apiFetch(path, opts);
    const text = await resp.text().catch(() => '');
    const json = safeJsonParse(text);

    if (!resp.ok) {
      // attach raw HTML/text to error to help debug 500s
      const contentType = resp.headers.get('content-type') || '';
      let body = json;
      if (!body && text && contentType.indexOf('html') !== -1) {
        body = { html: text.slice(0, 2000) }; // keep truncated html
      }
      // If backend returned a standard error body but not JSON, include text in bodyRaw
      throw makeError(resp.status, body || null, json?.message || resp.statusText || `Request failed (${resp.status})`, text);
    }
    return json;
  }

  async function apiPostJson(path, body) { return apiJson(path, { method: 'POST', body }); }

  // ---------- Auth operations ----------
  async function login(email, password) {
    if (!email || !password) throw new Error('email and password required');

    const url = `${API_BASE}/api/auth/login`;
    let resp;
    try {
      resp = await fetch(url, { method: 'POST', credentials: 'include', headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) });
    } catch (networkErr) {
      throw makeError(0, null, 'Network error: ' + (networkErr.message || networkErr));
    }

    const text = await resp.text().catch(() => '');
    const json = safeJsonParse(text);

    if (!resp.ok) throw makeError(resp.status, json || text, json?.message || `Login failed (${resp.status})`, text);

    applyTokenResponse(json);

    if (!getAccessToken()) {
      try { await refreshAccessToken(); } catch (e) { console.warn('Refresh after login did not produce token', e); }
    }

    return json;
  }

  async function signupCustomer(registerDto) {
    if (!registerDto || typeof registerDto !== 'object') throw new Error('registerDto required');
    return apiJson('/api/auth/signup/customer', { method: 'POST', body: registerDto });
  }

  async function logout(redirectTo) {
    const target = redirectTo || SIGNIN_PAGE;
    try {
      await fetch(`${API_BASE}/api/auth/logout`, { method: 'POST', credentials: 'include', headers: { 'Accept': 'application/json' } });
    } catch (e) {
      console.warn('Logout request failed (continuing)', e);
    } finally {
      setAccessToken(null);
      setRefreshTokenLocal(null);
      try { window.location.replace(target); } catch (e) { window.location.href = target; }
    }
  }

  async function requestPasswordReset(email) {
    if (!email) throw new Error('email required');
    return apiJson('/api/auth/forgot-password', { method: 'POST', body: { email } });
  }

  async function resetPassword(token, newPassword, confirmPassword) {
    if (!token || !newPassword || !confirmPassword) throw new Error('token and passwords required');
    return apiJson('/api/auth/reset-password', { method: 'POST', body: { token, newPassword, confirmPassword } });
  }

  // ---------- optional mock responses for dev ----------
  function mockResponseFor(path) {
    // Provide very small mock payloads. Enable only during frontend dev.
    if (!USE_MOCK_ON_500) return null;
    if (path.startsWith('/api/dashboard/customer')) {
      return {
        profile: { name: 'Demo User', email: 'demo@golanka.test', phone: '+94-77-xxxxxxx', address: 'Colombo, Sri Lanka' },
        metrics: { active: 2, delivered: 12, pending: 1, spent: 12345 },
        notifications: [{ title: 'Welcome to GoLanka', timestamp: Date.now() }]
      };
    }
    if (path.startsWith('/api/parcels')) {
      return [
        { id: 'p1', trackingNumber: 'GL-0001', recipientName: 'Bob', status: 'IN_TRANSIT', createdAt: new Date().toISOString(), serviceType: 'standard' },
        { id: 'p2', trackingNumber: 'GL-0002', recipientName: 'Charlie', status: 'DELIVERED', createdAt: new Date().toISOString(), serviceType: 'express' }
      ];
    }
    if (path.startsWith('/api/notifications')) {
      return [{ title: 'Demo notification', timestamp: Date.now(), message: 'This is a mock notification' }];
    }
    return null;
  }

  // ---------- Public API ----------
  return {
    apiFetch,
    apiJson,
    apiPostJson,
    login,
    logout,
    signupCustomer,
    requestPasswordReset,
    resetPassword,
    getAccessToken,
    setAccessToken,
    getRefreshTokenLocal,
    setRefreshTokenLocal,
    isAuthenticated,
    getUser,
    getRoles,
    toast,
    swalError,
    swalSuccess: (t, s) => { if (window.Swal) return Swal.fire({ icon: 'success', title: t, text: s }); else return Promise.resolve(); },
    _config: { API_BASE, SIGNIN_PAGE, USE_MOCK_ON_500 },
    // helper used by page code: if server returns 500 and USE_MOCK_ON_500 is true, fallback calls can use this function
    _mockResponseFor: mockResponseFor
  };
})();

window.GoLankaApi = Api;
