(function ($, Api, Swal) {

    console.log('admin.js is loading...');

  'use strict';

  if (!$) throw new Error('jQuery required');
  if (!Api) throw new Error('GoLankaApi required');

  // Config
  const STATS_POLL_MS = 30000; // 30s
  let statsTimer = null;

  // -------------------------
  // UI helpers
  // -------------------------
  function toast({ title = '', icon = 'info', timer = 2400 } = {}) {
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon,
      title,
      showConfirmButton: false,
      timer
    });
  }

  function setSubmitting($btn, submitting, text = 'Processing...') {
    if (!$btn || !$btn.length) return;
    if (submitting) {
      $btn.data('orig-html', $btn.html());
      $btn.prop('disabled', true).html(`<span class="loader inline-block w-4 h-4 animate-spin mr-2"></span>${text}`);
    } else {
      $btn.prop('disabled', false).html($btn.data('orig-html') || $btn.html());
    }
  }

  async function handleError($context, err) {
    console.error('Admin API error', err);
    if (!err) {
      Swal.fire('Error', 'Unknown error', 'error');
      return;
    }
    const message = (err.body && err.body.message) || err.message || 'Request failed';
    // show validation error fields if present
    if (err.body && Array.isArray(err.body.validationErrors) && $context) {
      $context.find('.field-error').remove();
      $context.find('.is-invalid').removeClass('is-invalid');
      err.body.validationErrors.forEach(e => {
        const $f = $context.find(`[name="${e.field}"]`);
        if ($f.length) {
          $f.addClass('is-invalid').after(`<div class="field-error text-sm text-red-600 mt-1">${e.message}</div>`);
        } else {
          toast({ title: e.message, icon: 'error' });
        }
      });
      return;
    }
    Swal.fire('Error', message, 'error');
  }

  // -------------------------
  // Admin: Stats
  // -------------------------
  async function fetchStats() {
    try {
      const res = await Api.apiJson('/api/admin/stats', { method: 'GET' });
      return res;
    } catch (err) {
      throw err;
    }
  }

  function renderStats(s) {
    if (!s) return;
    $('#stat-users-count').text(s.users ?? '-');
    $('#stat-parcels-count').text(s.parcels ?? '-');
    $('#stat-revenue').text(s.revenue ? formatCurrency(s.revenue) : '-');
    $('#stat-drivers').text(s.drivers ?? '-');
    $('#stat-pending-parcels').text(s.pendingParcels ?? '-');
  }

  function formatCurrency(val) {
    // Sri Lanka LKR formatting (simple)
    try {
      return new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR' }).format(val);
    } catch (e) {
      return `LKR ${val}`;
    }
  }

  function startStatsPolling() {
    stopStatsPolling();
    statsTimer = setInterval(async () => {
      if (!Api.isAuthenticated()) return;
      try {
        const s = await fetchStats();
        renderStats(s);
      } catch (e) {
        console.warn('Stats poll failed', e);
      }
    }, STATS_POLL_MS);
  }

  function stopStatsPolling() {
    if (statsTimer) {
      clearInterval(statsTimer);
      statsTimer = null;
    }
  }

  // -------------------------
  // Admin: Users management
  // -------------------------
  const userQuery = { page: 0, size: 20, q: '', sort: 'createdAt,desc' };

  async function fetchUsers({ page = 0, size = 20, q = '', sort = 'createdAt,desc' } = {}) {
    const params = { page, size, q, sort };
    try {
      const res = await Api.apiJson('/api/admin/users', { method: 'GET', params });
      return res;
    } catch (err) {
      throw err;
    }
  }

  function renderUsersTable(data) {
    const items = data.items || data.content || data || [];
    const $tbody = $('#admin-users-table tbody');
    $tbody.empty();
    if (!items || items.length === 0) {
      $tbody.append('<tr><td colspan="8" class="text-center py-6">No users found</td></tr>');
      return;
    }
    items.forEach(u => {
      const created = u.createdAt ? new Date(u.createdAt).toLocaleString() : '-';
      const enabled = u.enabled ? '<span class="text-green-600 text-sm">Active</span>' : '<span class="text-red-600 text-sm">Disabled</span>';
      const roles = (u.roles || []).map(r => `<span class="inline-block px-2 py-0.5 text-xs rounded bg-slate-100 mr-1">${r.replace('ROLE_', '')}</span>`).join(' ');
      const row = `
        <tr data-id="${u.id}" class="hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
          <td class="px-3 py-2">${u.id}</td>
          <td class="px-3 py-2">${escapeHtml(u.fullName || u.email)}</td>
          <td class="px-3 py-2">${escapeHtml(u.email)}</td>
          <td class="px-3 py-2">${escapeHtml(u.phone || '-')}</td>
          <td class="px-3 py-2">${roles}</td>
          <td class="px-3 py-2">${enabled}</td>
          <td class="px-3 py-2 text-sm text-slate-600">${created}</td>
          <td class="px-3 py-2">
            <button class="btn btn-sm view-user" data-id="${u.id}">View</button>
            <button class="btn btn-sm ml-1 edit-user" data-id="${u.id}">Edit</button>
            <button class="btn btn-sm ml-1 toggle-user" data-id="${u.id}" data-enabled="${u.enabled ? '1' : '0'}">${u.enabled ? 'Disable' : 'Enable'}</button>
            <button class="btn btn-sm ml-1 reset-password" data-id="${u.id}">Reset PW</button>
          </td>
        </tr>
      `;
      $tbody.append(row);
    });
    renderUsersPager(data);
  }

  function renderUsersPager(data) {
    const total = data.total ?? data.totalElements ?? 0;
    const page = data.page ?? data.number ?? 0;
    const size = data.size ?? data.pageSize ?? 20;
    const $pager = $('#admin-users-pager');
    if (!$pager.length) return;
    $pager.empty();
    const pages = Math.max(1, Math.ceil(total / size));
    const start = Math.max(0, page - 2);
    const end = Math.min(pages - 1, page + 2);
    $pager.append(`<button class="pager-btn btn btn-sm mr-2" data-page="${page - 1}" ${page <= 0 ? 'disabled' : ''}>Prev</button>`);
    for (let p = start; p <= end; p++) {
      $pager.append(`<button class="pager-btn btn btn-sm mr-1 ${p === page ? 'btn-active' : ''}" data-page="${p}">${p + 1}</button>`);
    }
    $pager.append(`<button class="pager-btn btn btn-sm ml-2" data-page="${page + 1}" ${page >= pages - 1 ? 'disabled' : ''}>Next</button>`);
  }

  function escapeHtml(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"']/g, function (m) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
  }

  async function loadUsers() {
    try {
      $('#admin-users-loader').show();
      const res = await fetchUsers(userQuery);
      renderUsersTable(res);
    } catch (err) {
      await handleError(null, err);
    } finally {
      $('#admin-users-loader').hide();
    }
  }

  // Create user
  $(document).on('submit', '#admin-user-create-form', async function (e) {
    e.preventDefault();
    const $form = $(this);
    $form.find('.field-error').remove();
    const payload = {
      fullName: $form.find('[name="fullName"]').val(),
      email: $form.find('[name="email"]').val(),
      phone: $form.find('[name="phone"]').val(),
      password: $form.find('[name="password"]').val(),
      roles: $form.find('[name="roles"]').val() || []
    };
    const $btn = $form.find('button[type="submit"]');
    setSubmitting($btn, true, 'Creating...');
    try {
      const resp = await Api.apiJson('/api/admin/users', { method: 'POST', body: payload });
      toast({ title: resp.message || 'User created', icon: 'success' });
      $form[0].reset();
      loadUsers();
      $('#admin-user-create-modal').removeClass('open');
    } catch (err) {
      await handleError($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // View user
  $(document).on('click', '.view-user', async function () {
    const id = $(this).data('id');
    if (!id) return;
    try {
      const u = await Api.apiJson(`/api/admin/users/${id}`, { method: 'GET' });
      populateUserViewModal(u);
      $('#admin-user-view-modal').addClass('open');
    } catch (err) {
      await handleError(null, err);
    }
  });

  function populateUserViewModal(u) {
    const $m = $('#admin-user-view-modal');
    $m.find('.user-id').text(u.id);
    $m.find('.user-fullname').text(u.fullName || '-');
    $m.find('.user-email').text(u.email || '-');
    $m.find('.user-phone').text(u.phone || '-');
    $m.find('.user-created').text(u.createdAt ? new Date(u.createdAt).toLocaleString() : '-');
    $m.find('.user-enabled').text(u.enabled ? 'Active' : 'Disabled');
    $m.find('.user-roles').html((u.roles || []).map(r => `<span class="inline-block px-2 py-0.5 text-xs rounded bg-slate-100 mr-1">${r}</span>`).join(' '));
  }

  // Edit user - open form
  $(document).on('click', '.edit-user', async function () {
    const id = $(this).data('id');
    if (!id) return;
    try {
      const u = await Api.apiJson(`/api/admin/users/${id}`, { method: 'GET' });
      const $form = $('#admin-user-edit-form');
      $form.find('[name="id"]').val(u.id);
      $form.find('[name="fullName"]').val(u.fullName);
      $form.find('[name="email"]').val(u.email);
      $form.find('[name="phone"]').val(u.phone);
      // roles may be checkboxes or multiselect; support multi-select
      if ($form.find('[name="roles"]').length) {
        $form.find('[name="roles"]').val(u.roles || []);
      }
      $('#admin-user-edit-modal').addClass('open');
    } catch (err) {
      await handleError(null, err);
    }
  });

  // Submit edit
  $(document).on('submit', '#admin-user-edit-form', async function (e) {
    e.preventDefault();
    const $form = $(this);
    $form.find('.field-error').remove();
    const id = $form.find('[name="id"]').val();
    if (!id) return;
    const payload = {
      fullName: $form.find('[name="fullName"]').val(),
      phone: $form.find('[name="phone"]').val(),
      roles: $form.find('[name="roles"]').val() || []
    };
    const $btn = $form.find('button[type="submit"]');
    setSubmitting($btn, true, 'Saving...');
    try {
      const res = await Api.apiJson(`/api/admin/users/${id}`, { method: 'PUT', body: payload });
      toast({ title: res.message || 'User updated', icon: 'success' });
      $('#admin-user-edit-modal').removeClass('open');
      loadUsers();
    } catch (err) {
      await handleError($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // Toggle enable / disable
  $(document).on('click', '.toggle-user', async function () {
    const id = $(this).data('id');
    const currentlyEnabled = $(this).data('enabled') === '1';
    const action = currentlyEnabled ? 'Disable' : 'Enable';
    const confirm = await Swal.fire({
      title: `${action} user?`,
      text: `This will ${action.toLowerCase()} the user account.`,
      showCancelButton: true,
      icon: 'warning'
    });
    if (!confirm.isConfirmed) return;
    const $btn = $(this);
    setSubmitting($btn, true, `${action}...`);
    try {
      const res = await Api.apiJson(`/api/admin/users/${id}/enabled`, { method: 'POST', body: { enabled: !currentlyEnabled } });
      toast({ title: res.message || `${action}d`, icon: 'success' });
      loadUsers();
    } catch (err) {
      await handleError(null, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // Reset password for a user
  $(document).on('click', '.reset-password', async function () {
    const id = $(this).data('id');
    if (!id) return;
    const { value: password } = await Swal.fire({
      title: 'Reset password',
      input: 'password',
      inputLabel: 'Enter new password for the user',
      inputPlaceholder: 'New password',
      showCancelButton: true,
      inputAttributes: { autocapitalize: 'off' }
    });
    if (!password) return;
    try {
      const res = await Api.apiJson(`/api/admin/users/${id}/reset-password`, { method: 'POST', body: { password } });
      toast({ title: res.message || 'Password reset', icon: 'success' });
    } catch (err) {
      await handleError(null, err);
    }
  });

  // Export users CSV
  $(document).on('click', '#admin-export-users', async function () {
    try {
      const res = await Api.apiJson('/api/admin/users/export', { method: 'GET' });
      if (res.csv) {
        downloadTextFile(res.csv, `users-${Date.now()}.csv`, 'text/csv');
        toast({ title: 'CSV downloaded', icon: 'success' });
      } else if (res.url) {
        window.open(res.url, '_blank');
      } else if (Array.isArray(res.items)) {
        const csv = jsonArrayToCsv(res.items);
        downloadTextFile(csv, `users-${Date.now()}.csv`, 'text/csv');
        toast({ title: 'CSV created', icon: 'success' });
      } else {
        throw new Error('No CSV data');
      }
    } catch (err) {
      await handleError(null, err);
    }
  });

  // Utilities for CSV/download
  function jsonArrayToCsv(items) {
    if (!items || items.length === 0) return '';
    const keys = Object.keys(items[0]);
    const escape = s => `"${String(s || '').replace(/"/g, '""')}"`;
    const header = keys.join(',');
    const rows = items.map(it => keys.map(k => escape(it[k])).join(','));
    return [header, ...rows].join('\n');
  }

  function downloadTextFile(text, filename, mime = 'text/plain') {
    const blob = new Blob([text], { type: mime });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  // -------------------------
  // Admin: Roles & Drivers quick actions
  // -------------------------
  async function fetchRoles() {
    try {
      const res = await Api.apiJson('/api/admin/roles', { method: 'GET' });
      return res;
    } catch (err) {
      throw err;
    }
  }

  async function populateRolesSelector($select) {
    try {
      const roles = await fetchRoles();
      $select.empty();
      (roles || []).forEach(r => $select.append(`<option value="${r}">${r.replace('ROLE_', '')}</option>`));
    } catch (err) {
      console.warn('Could not fetch roles', err);
    }
  }

  $(document).on('click', '#admin-open-create-user', function () {
    const $modal = $('#admin-user-create-modal');
    populateRolesSelector($modal.find('[name="roles"]'));
    $modal.addClass('open');
  });

  // -------------------------
  // Generic search & pager
  // -------------------------
  let searchTimeout = null;
  $(document).on('input', '#admin-users-search', function () {
    const q = $(this).val();
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      userQuery.q = q;
      userQuery.page = 0;
      loadUsers();
    }, 350);
  });

  $(document).on('click', '#admin-users-pager .pager-btn', function () {
    const page = Number($(this).data('page'));
    if (isNaN(page) || page < 0) return;
    userQuery.page = page;
    loadUsers();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  // Close modals
  $(document).on('click', '.modal-close, .modal-backdrop', function () {
    $(this).closest('.modal').removeClass('open');
  });

  // -------------------------
  // Init
  // -------------------------
  $(function init() {
    if (!Api.isAuthenticated()) return;

    // Load initial dashboard stats and start polling
    fetchStats()
      .then(renderStats)
      .catch(e => console.warn('Initial stats fetch failed', e));
    startStatsPolling();

    // Load initial users list
    loadUsers();

    // Wire up role selectors in create/edit forms
    populateRolesSelector($('#admin-user-create-modal').find('[name="roles"]'));
    populateRolesSelector($('#admin-user-edit-modal').find('[name="roles"]'));
  });

  // Cleanup
  $(window).on('beforeunload', function () {
    stopStatsPolling();
  });

})(window.jQuery, window.GoLankaApi, window.Swal);
