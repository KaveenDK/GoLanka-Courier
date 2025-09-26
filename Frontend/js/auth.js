(function ($, Api, Swal) {
  'use strict';

  console.log('Auth.js loaded');

  // --- Small compatibility guards ---
  if (typeof $ === 'undefined') {
    console.warn('jQuery not detected. This file expects jQuery but will still attempt to work.');
  }
  // Api is optional; fallback functions will be used.
  if (!Api) {
    console.warn('GoLankaApi not detected. Falling back to direct fetch for API calls.');
  }

  // ---------- Helpers ----------
  function jq(selectorVariants) {
    // Accept array or single selector; try each and return first found jQuery element (or null)
    const list = Array.isArray(selectorVariants) ? selectorVariants : [selectorVariants];
    for (const s of list) {
      try {
        const $el = $ ? $(s) : document.querySelector(s) ? { length: 1, 0: document.querySelector(s), get: (i) => document.querySelector(s) } : null;
        if ($el && $el.length) return $el;
      } catch (e) { /* ignore */ }
    }
    return $ ? $() : null;
  }

  function setSubmitting($btn, submitting) {
    if (!$btn || !$btn.length) return;
    if (submitting) {
      $btn.data('orig-text', $btn.html());
      $btn.prop('disabled', true);
      // nice spinner if Tailwind loader exists; fallback to "Please wait..."
      $btn.html('<span class="inline-block w-4 h-4 animate-spin mr-2 border-2 border-current rounded-full"></span> Please wait...');
    } else {
      const orig = $btn.data('orig-text');
      if (orig) $btn.html(orig);
      $btn.prop('disabled', false);
    }
  }

  function clearFieldErrors($form) {
    if (!$form || !$form.length) return;
    $form.find('.field-error').remove();
    $form.find('.is-invalid').removeClass('is-invalid');
  }

  function showValidationErrors($form, errors) {
    if (!$form || !$form.length) return;
    if (!Array.isArray(errors)) return;
    // clear first
    clearFieldErrors($form);
    errors.forEach(err => {
      const name = String(err.field || '').trim();
      const msg = err.message || String(err || 'Invalid');
      let $field = $form.find(`[name="${name}"]`);
      if (!$field || !$field.length) $field = $form.find(`#${name}`);
      if ($field && $field.length) {
        $field.addClass('is-invalid');
        const $err = $(`<div class="field-error text-sm text-red-600 mt-1">${msg}</div>`);
        $field.after($err);
      } else {
        // fallback: top-level toast
        if (Api && typeof Api.toast === 'function') Api.toast({ title: msg, icon: 'error' });
        else if (Swal) Swal.fire('Error', msg, 'error');
        else console.warn('Validation:', msg);
      }
    });
  }

  async function handleErrorResponse($form, err) {
    // Normalized error shapes:
    // - Error instance with message
    // - object with { status, body } where body may contain validationErrors or message
    // - plain object
    if (!err) {
      if (Swal) await Swal.fire('Error', 'Unknown error occurred', 'error');
      else console.error('Unknown error occurred');
      return;
    }

    // If err.body.validationErrors -> show inline
    const body = err.body || (err.response && err.response.body) || null;

    if (body && Array.isArray(body.validationErrors) && $form) {
      showValidationErrors($form, body.validationErrors);
      return;
    }

    // If body.message
    const message = (body && body.message) || err.message || err.msg || JSON.stringify(err);

    if (Swal) {
      await Swal.fire('Error', message, 'error');
    } else {
      console.error('Error:', message);
    }
  }

  // ---------- Fallback HTTP helpers (used when Api is not present) ----------
  function normalizeBase() {
    return (window.API_BASE && String(window.API_BASE).replace(/\/+$/, '')) || '';
  }

  async function postJsonFallback(path, body) {
    const base = normalizeBase();
    const url = path.startsWith('http') ? path : (path.startsWith('/') ? base + path : base + '/' + path);
    let resp;
    try {
      resp = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: body ? JSON.stringify(body) : undefined
      });
    } catch (networkErr) {
      throw { status: 0, body: null, message: networkErr.message || 'Network error' };
    }

    const text = await resp.text().catch(() => '');
    let json = null;
    try { json = text ? JSON.parse(text) : null; } catch (e) { json = null; }

    if (!resp.ok) throw { status: resp.status, body: json || text };
    return json || { success: true };
  }

  // ---------- API wrapper helpers (prefer Api when available) ----------
  async function apiSignupCustomer(payload) {
    if (Api && typeof Api.signupCustomer === 'function') return Api.signupCustomer(payload);
    return postJsonFallback('/api/auth/signup/customer', payload);
  }
  async function apiLogin(email, password) {
    if (Api && typeof Api.login === 'function') return Api.login(email, password);
    return postJsonFallback('/api/auth/login', { email, password });
  }
  async function apiVerifyEmail(email, code) {
    if (Api && typeof Api.apiJson === 'function') return Api.apiJson('/api/auth/verify-email', { method: 'POST', body: { email, code } });
    return postJsonFallback('/api/auth/verify-email', { email, code });
  }
  async function apiRequestPasswordReset(email) {
    if (Api && typeof Api.requestPasswordReset === 'function') return Api.requestPasswordReset(email);
    return postJsonFallback('/api/auth/forgot-password', { email });
  }
  async function apiResetPassword(token, newPassword, confirmPassword) {
    if (Api && typeof Api.resetPassword === 'function') return Api.resetPassword(token, newPassword, confirmPassword);
    return postJsonFallback('/api/auth/reset-password', { token, newPassword, confirmPassword });
  }

  // ---------- Form wiring ----------
  // selectors used (try multiple variants)
  const forms = {
    signup: ['#signup-form', '#signupForm', 'form[name="signup"]'],
    signin: ['#signin-form', '#signinForm', 'form[name="signin"]'],
    verify: ['#verify-email-form', '#verifyEmailForm', 'form[name="verify-email"]'],
    forgot: ['#forgot-password-form', '#forgotForm', 'form[name="forgot-password"]'],
    reset: ['#reset-password-form', '#resetPasswordForm', 'form[name="reset-password"]']
  };

  // Helper to bind safely only if form exists
  function bindIfPresent(name, handler) {
    const $f = jq(forms[name]);
    if (!$f || !$f.length) return null;
    try {
      $f.on('submit', handler);
      return $f;
    } catch (e) {
      // older non-jQuery fallback
      try {
        const el = document.querySelector(forms[name][0]);
        if (el) el.addEventListener('submit', function (ev) { ev.preventDefault(); handler.call(el, ev); });
      } catch (ee) { /* ignore */ }
    }
    return $f;
  }

  // ---------- Signup ----------
  bindIfPresent('signup', async function (ev) {
    try {
      ev.preventDefault();
    } catch (e) {}
    const $form = $(this);
    const $btn = $form.find('button[type="submit"]').first();
    clearFieldErrors($form);

    // build payload - adjust names to your backend
    const payload = {
      email: $.trim($form.find('[name="email"]').val() || ''),
      password: $form.find('[name="password"]').val() || '',
      confirmPassword: $form.find('[name="confirmPassword"]').val() || '',
      fullName: $form.find('[name="fullName"]').val() || '',
      phone: $form.find('[name="phone"]').val() || '',
      address: $form.find('[name="address"]').val() || $form.find('[name="addressLine"]').val() || '',
      addressLine: $form.find('[name="addressLine"]').val() || '',
      city: $form.find('[name="city"]').val() || '',
      province: $form.find('[name="province"]').val() || '',
      country: $form.find('[name="country"]').val() || '',
      postalCode: $form.find('[name="postalCode"]').val() || ''
    };

    // quick client-side validation
    const required = ['email', 'password', 'confirmPassword', 'fullName'];
    const errors = [];
    required.forEach(k => { if (!payload[k]) errors.push({ field: k, message: 'Required' }); });
    if (payload.password && payload.confirmPassword && payload.password !== payload.confirmPassword) {
      errors.push({ field: 'confirmPassword', message: 'Passwords do not match' });
    }
    if (errors.length) {
      showValidationErrors($form, errors);
      return;
    }

    setSubmitting($btn, true);
    try {
      const resp = await apiSignupCustomer(payload);
      // success - prefill verify and redirect
      if (sessionStorage) sessionStorage.setItem('preVerifyEmail', payload.email || '');
      if (Swal) {
        await Swal.fire({ icon: 'success', title: 'Registration successful', text: resp?.message || 'Check your email for a verification code.' });
      }
      // redirect to verify page if exists
      const verifyPath = '/pages/verify-email.html';
      window.location.href = verifyPath;
    } catch (err) {
      await handleErrorResponse($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // ---------- Verify Email ----------
  bindIfPresent('verify', async function (ev) {
    try { ev.preventDefault(); } catch (e) {}
    const $form = $(this);
    const $btn = $form.find('button[type="submit"]').first();

    clearFieldErrors($form);
    const email = $.trim($form.find('[name="email"]').val() || sessionStorage.getItem('preVerifyEmail') || '');
    const code = $.trim($form.find('[name="code"]').val() || '');

    if (!email || !code) {
      showValidationErrors($form, [{ field: !email ? 'email' : 'code', message: 'Required' }]);
      return;
    }

    setSubmitting($btn, true);
    try {
      const resp = await apiVerifyEmail(email, code);
      if (Swal) await Swal.fire({ icon: 'success', title: 'Verified', text: resp?.message || 'Email verified' });
      window.location.href = '/pages/signin.html';
    } catch (err) {
      await handleErrorResponse($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // Prefill verify email if set in sessionStorage
  (function prefillVerify() {
    const $v = jq(forms.verify);
    if ($v && $v.length && sessionStorage) {
      const pre = sessionStorage.getItem('preVerifyEmail');
      if (pre) $v.find('[name="email"]').val(pre);
    }
  })();

  // ---------- Signin ----------
  bindIfPresent('signin', async function (ev) {
    try { ev.preventDefault(); } catch (e) {}
    const $form = $(this);
    const $btn = $form.find('button[type="submit"]').first();
    clearFieldErrors($form);

    const email = $.trim($form.find('[name="email"]').val() || '');
    const password = $form.find('[name="password"]').val() || '';

    if (!email || !password) {
      showValidationErrors($form, [{ field: !email ? 'email' : 'password', message: 'Required' }]);
      return;
    }

    setSubmitting($btn, true);
    try {
      const resp = await apiLogin(email, password);
      // ensure Api stored tokens if present; otherwise store accessToken to expected key
      if (resp && resp.accessToken) {
        if (Api && typeof Api.setAccessToken === 'function') {
          Api.setAccessToken(resp.accessToken);
          if (resp.refreshToken && typeof Api.setRefreshTokenLocal === 'function') Api.setRefreshTokenLocal(resp.refreshToken);
        } else {
          localStorage.setItem('accessToken', resp.accessToken);
          if (resp.refreshToken) localStorage.setItem('refreshToken', resp.refreshToken);
        }
      }
      // show success
      if (Swal) await Swal.fire({ icon: 'success', title: 'Signed in', text: 'Redirecting...' });

      // determine roles: prefer Api.getUser() if available
      let roles = [];
      try {
        if (Api && typeof Api.getUser === 'function') {
          const u = Api.getUser(); roles = u && u.roles ? (Array.isArray(u.roles) ? u.roles : [u.roles]) : [];
        } else if (resp && resp.roles) {
          roles = Array.isArray(resp.roles) ? resp.roles : [resp.roles];
        } else if (resp && resp.accessToken) {
          // try decode token locally
          try {
            const t = resp.accessToken.split('.')[1] || '';
            const json = JSON.parse(decodeURIComponent(atob(t.replace(/-/g,'+').replace(/_/g,'/')).split('').map(c=> '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')));
            roles = json.roles || json.authorities || json.role || [];
            if (!Array.isArray(roles) && roles) roles = [roles];
          } catch (e) { /* ignore */ }
        }
      } catch (e) {
        roles = [];
      }

      // simple role-based redirect
      if (roles.includes('ROLE_ADMIN')) {
        window.location.href = '/pages/admin/dashboard.html';
      } else if (roles.includes('ROLE_DRIVER')) {
        window.location.href = '/pages/driver/dashboard.html';
      } else if (roles.includes('ROLE_STAFF')) {
        window.location.href = '/pages/staff/dashboard.html';
      } else {
        window.location.href = '/pages/customer/dashboard.html';
      }
    } catch (err) {
      await handleErrorResponse($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // ---------- Forgot Password ----------
  bindIfPresent('forgot', async function (ev) {
    try { ev.preventDefault(); } catch (e) {}
    const $form = $(this);
    const $btn = $form.find('button[type="submit"]').first();
    clearFieldErrors($form);

    const email = $.trim($form.find('[name="email"]').val() || '');
    if (!email) {
      showValidationErrors($form, [{ field: 'email', message: 'Email is required' }]);
      return;
    }

    setSubmitting($btn, true);
    try {
      const resp = await apiRequestPasswordReset(email);
      if (Swal) await Swal.fire({ icon: 'success', title: 'If that email exists', text: resp?.message || 'If an account exists, instructions have been sent.' });
      // Optionally redirect to reset page with instruction
      window.location.href = '/pages/auth/reset-password.html';
    } catch (err) {
      await handleErrorResponse($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // ---------- Reset Password ----------
  bindIfPresent('reset', async function (ev) {
    try { ev.preventDefault(); } catch (e) {}
    const $form = $(this);
    const $btn = $form.find('button[type="submit"]').first();
    clearFieldErrors($form);

    // token may be prefilled from query string or input
    const token = $.trim($form.find('[name="token"]').val() || '');
    const newPassword = $form.find('[name="newPassword"]').val() || '';
    const confirmPassword = $form.find('[name="confirmPassword"]').val() || '';

    const errs = [];
    if (!token) errs.push({ field: 'token', message: 'Token is required' });
    if (!newPassword || newPassword.length < 8) errs.push({ field: 'newPassword', message: 'Password must be at least 8 characters' });
    if (newPassword !== confirmPassword) errs.push({ field: 'confirmPassword', message: 'Passwords do not match' });

    if (errs.length) {
      showValidationErrors($form, errs);
      return;
    }

    setSubmitting($btn, true);
    try {
      const resp = await apiResetPassword(token, newPassword, confirmPassword);
      if (Swal) await Swal.fire({ icon: 'success', title: 'Password changed', text: resp?.message || 'You may now sign in.' });
      window.location.href = '/pages/auth/signin.html';
    } catch (err) {
      await handleErrorResponse($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // ---------- Prefill helpers (verify/reset) ----------
  (function prefillFromSessionAndQuery() {
    // verify form: prefill email from sessionStorage
    try {
      const $verify = jq(forms.verify);
      if ($verify && $verify.length && sessionStorage) {
        const pre = sessionStorage.getItem('preVerifyEmail');
        if (pre) $verify.find('[name="email"]').val(pre);
      }
    } catch (e) { /* ignore */ }

    // reset form: prefill token from query string
    try {
      const params = new URLSearchParams(window.location.search);
      const tok = params.get('token');
      const $reset = jq(forms.reset);
      if (tok && $reset && $reset.length) $reset.find('[name="token"]').val(tok);
    } catch (e) { /* ignore */ }
  })();

  // ---------- Small UX helpers ----------
  // remove inline error on input change
  $(document).on('input change', 'input,textarea,select', function () {
    const $t = $(this);
    $t.removeClass('is-invalid');
    if ($t.next && $t.next('.field-error').length) $t.next('.field-error').remove();
  });

  // Auto-redirect away from signin/signup if already authenticated (best-effort)
  (function autoRedirectAuth() {
    try {
      if (Api && typeof Api.isAuthenticated === 'function' && Api.isAuthenticated()) {
        const path = window.location.pathname || '';
        const loginPages = ['/pages/auth/signin.html', '/pages/auth/signup.html', '/signin.html', '/signup.html'];
        if (loginPages.some(p => path.endsWith(p))) {
          window.location.href = '/pages/customer/dashboard.html';
        }
      }
    } catch (e) { /* ignore */ }
  })();

})(window.jQuery, window.GoLankaApi, window.Swal);
