/**
 * staff.js
 * Staff console scripts for GoLanka Courier frontend
 *
 * Assumptions:
 *  - jQuery is loaded
 *  - SweetAlert2 is loaded as `Swal`
 *  - Global API helper `GoLankaApi` is available with:
 *      - apiJson(path, { method, body, params }) -> Promise resolving parsed JSON or rejecting { status, body/message }
 *      - getUser() -> parsed JWT subject / user info
 *      - isAuthenticated() -> boolean
 *
 * Features:
 *  - List & search parcels
 *  - View parcel details in modal
 *  - Change parcel status (Hold, In Transit, Delivered, Cancelled)
 *  - Assign parcel to driver
 *  - Bulk operations (export CSV, bulk status)
 *  - Polling for new parcels / updates
 *  - Simple UI helpers (toasts, loading)
 *
 * Adjust selectors, endpoint paths and payloads to match your backend.
 */

(function ($, Api, Swal) {

    console.log('staff.js is loading...');

  'use strict';

  if (!$) throw new Error('jQuery required');
  if (!Api) throw new Error('GoLankaApi required');

  // Config
  const POLL_INTERVAL_MS = 20000; // 20s
  let pollTimer = null;

  // --- UI helpers ---
  function toast({ title = '', icon = 'info', timer = 2500 } = {}) {
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon,
      title,
      showConfirmButton: false,
      timer
    });
  }

  function setSubmitting($el, submitting, text = 'Processing...') {
    if (!$el || !$el.length) return;
    if (submitting) {
      $el.data('orig-html', $el.html());
      $el.prop('disabled', true).html(`<span class="loader inline-block w-4 h-4 animate-spin mr-2"></span> ${text}`);
    } else {
      $el.prop('disabled', false).html($el.data('orig-html') || $el.html());
    }
  }

  async function handleError($context, err) {
    console.error('API error', err);
    if (!err) {
      Swal.fire('Error', 'Unknown error', 'error');
      return;
    }
    const message = (err.body && err.body.message) || err.message || 'Request failed';
    // show validation errors if present
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

  // --- Data helpers ---
  async function fetchParcels({ page = 0, size = 25, q = '', status = '', sort = 'createdAt,desc' } = {}) {
    // Build query params
    const params = { page, size, q, status, sort };
    try {
      const res = await Api.apiJson('/api/staff/parcels', { method: 'GET', params });
      // expect { items: [], total, page, size }
      return res;
    } catch (err) {
      throw err;
    }
  }

  // --- Renderers ---
  function renderParcelsTable(items) {
    const $tbody = $('#staff-parcels-table tbody');
    if (!$tbody.length) return;
    $tbody.empty();

    if (!items || items.length === 0) {
      $tbody.append('<tr><td colspan="9" class="text-center py-6">No parcels found</td></tr>');
      return;
    }

    items.forEach(p => {
      const created = p.createdAt ? new Date(p.createdAt).toLocaleString() : '-';
      const statusBadge = statusBadgeHtml(p.status);
      const assignee = p.driverName || (p.assignedTo ? p.assignedTo : '-');

      const row = `
        <tr data-id="${p.id}" class="hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
          <td class="px-4 py-2"><input type="checkbox" class="bulk-checkbox" data-id="${p.id}"></td>
          <td class="px-4 py-2">${p.trackingNumber || ('#' + p.id)}</td>
          <td class="px-4 py-2">${p.senderName || '-'}</td>
          <td class="px-4 py-2">${p.recipientFullName || '-'}</td>
          <td class="px-4 py-2">${p.pickupAddress || '-'}</td>
          <td class="px-4 py-2">${p.deliveryAddress || '-'}</td>
          <td class="px-4 py-2">${assignee}</td>
          <td class="px-4 py-2">${statusBadge}</td>
          <td class="px-4 py-2 text-sm text-slate-600">${created}</td>
          <td class="px-4 py-2">
            <button class="view-parcel btn btn-sm" data-id="${p.id}">View</button>
            <button class="assign-parcel btn btn-sm ml-1" data-id="${p.id}">Assign</button>
            <div class="inline-block ml-1 relative">
              <button class="change-status btn btn-sm" data-id="${p.id}" data-status="${p.status}">Status</button>
            </div>
          </td>
        </tr>
      `;
      $tbody.append(row);
    });
  }

  function statusBadgeHtml(status) {
    const s = (status || 'UNKNOWN').toUpperCase();
    const map = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      ACCEPTED: 'bg-blue-100 text-blue-800',
      IN_TRANSIT: 'bg-indigo-100 text-indigo-800',
      PICKED_UP: 'bg-indigo-100 text-indigo-800',
      DELIVERED: 'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800',
      HOLD: 'bg-gray-100 text-gray-800'
    };
    const cls = map[s] || 'bg-slate-100 text-slate-800';
    return `<span class="px-2 py-1 rounded-full text-xs ${cls}">${s.replace('_', ' ')}</span>`;
  }

  // --- Actions ---

  async function openParcelDetails(id) {
    try {
      const p = await Api.apiJson(`/api/staff/parcels/${id}`, { method: 'GET' });
      renderParcelModal(p);
      $('#parcel-details-modal').addClass('open');
    } catch (err) {
      await handleError(null, err);
    }
  }

  function renderParcelModal(p) {
    const $m = $('#parcel-details-modal');
    if (!$m.length) {
      // fallback: plain alert
      Swal.fire({
        title: `Parcel ${p.trackingNumber || p.id}`,
        html: `<pre style="text-align:left">${JSON.stringify(p, null, 2)}</pre>`,
        width: '80%'
      });
      return;
    }

    $m.find('.modal-title').text(p.trackingNumber || ('#' + p.id));
    $m.find('.parcel-sender').text(p.senderName || '-');
    $m.find('.parcel-recipient').text(p.recipientFullName || '-');
    $m.find('.parcel-weight').text(p.weightKg ? `${p.weightKg} kg` : '-');
    $m.find('.parcel-pickup').text(p.pickupAddress || '-');
    $m.find('.parcel-destination').text(p.deliveryAddress || '-');
    $m.find('.parcel-notes').text(p.notes || '-');
    $m.find('.parcel-status').html(statusBadgeHtml(p.status));
    $m.find('.parcel-assignee').text(p.driverName || '-');

    // set data-id on action buttons
    $m.find('[data-action]').each(function () {
      const action = $(this).data('action');
      $(this).data('id', p.id);
    });

    // populate driver dropdown if exists
    const $sel = $m.find('select[name="assignDriver"]');
    if ($sel.length) {
      // fetch drivers and fill (non-blocking)
      fetchDrivers().then(drivers => {
        $sel.empty().append('<option value="">Select driver</option>');
        drivers.forEach(d => $sel.append(`<option value="${d.id}">${d.fullName} (${d.phone || d.email || 'no contact'})</option>`));
      }).catch(e => console.warn('drivers fetch failed', e));
    }
  }

  async function fetchDrivers() {
    try {
      const d = await Api.apiJson('/api/staff/drivers', { method: 'GET' });
      // expect array
      return Array.isArray(d) ? d : (d.items || []);
    } catch (err) {
      throw err;
    }
  }

  async function assignParcelToDriver(parcelId, driverId, $btn) {
    setSubmitting($btn, true);
    try {
      const body = { driverId };
      const res = await Api.apiJson(`/api/staff/parcels/${parcelId}/assign`, { method: 'POST', body });
      toast({ title: res.message || 'Assigned to driver', icon: 'success' });
      await reloadParcels();
      $('#parcel-details-modal').removeClass('open');
    } catch (err) {
      await handleError($('#parcel-details-modal'), err);
    } finally {
      setSubmitting($btn, false);
    }
  }

  async function changeParcelStatus(parcelId, newStatus, $btn) {
    setSubmitting($btn, true, 'Updating...');
    try {
      const body = { status: newStatus };
      const res = await Api.apiJson(`/api/staff/parcels/${parcelId}/status`, { method: 'POST', body });
      toast({ title: res.message || `Status updated to ${newStatus}`, icon: 'success' });
      await reloadParcels();
      $('#parcel-details-modal').removeClass('open');
    } catch (err) {
      await handleError($('#parcel-details-modal'), err);
    } finally {
      setSubmitting($btn, false);
    }
  }

  // Bulk actions
  async function bulkChangeStatus(ids, status) {
    if (!ids || ids.length === 0) {
      toast({ title: 'Select at least one parcel', icon: 'info' });
      return;
    }
    const confirm = await Swal.fire({
      title: `Change status of ${ids.length} parcel(s) to ${status}?`,
      showCancelButton: true
    });
    if (!confirm.isConfirmed) return;

    try {
      const res = await Api.apiJson('/api/staff/parcels/bulk/status', { method: 'POST', body: { ids, status } });
      toast({ title: res.message || 'Bulk status updated', icon: 'success' });
      await reloadParcels();
    } catch (err) {
      await handleError(null, err);
    }
  }

  async function bulkExportCsv(ids) {
    try {
      // If backend supports CSV directly
      const params = ids ? { ids: ids.join(',') } : {};
      const res = await Api.apiJson('/api/staff/parcels/export', { method: 'GET', params });
      // Expect the API to return { csv: '...' } or a URL; fallback to server-provided file link
      if (res.csv) {
        downloadTextFile(res.csv, `parcels-${Date.now()}.csv`, 'text/csv');
        toast({ title: 'CSV downloaded', icon: 'success' });
      } else if (res.url) {
        window.open(res.url, '_blank');
      } else {
        // fallback: try to convert items to CSV if returned items
        if (Array.isArray(res.items)) {
          const csv = jsonArrayToCsv(res.items);
          downloadTextFile(csv, `parcels-${Date.now()}.csv`, 'text/csv');
          toast({ title: 'CSV created', icon: 'success' });
        } else {
          throw new Error('No CSV data returned from server');
        }
      }
    } catch (err) {
      await handleError(null, err);
    }
  }

  // --- Utilities ---
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

  // --- Fetch + render loop ---
  let currentQuery = { page: 0, size: 25, q: '', status: '', sort: 'createdAt,desc' };

  async function reloadParcels() {
    try {
      $('#staff-parcels-loader').show();
      const res = await fetchParcels(currentQuery);
      const items = res.items || res.content || res; // tolerant
      renderParcelsTable(items);
      renderPagination(res);
    } catch (err) {
      await handleError(null, err);
    } finally {
      $('#staff-parcels-loader').hide();
    }
  }

  function renderPagination(res) {
    // Simple pager: expects { total, page, size } or { totalElements, number, size }
    const total = res.total ?? res.totalElements ?? 0;
    const page = res.page ?? res.number ?? 0;
    const size = res.size ?? res.pageSize ?? 25;
    const $pager = $('#staff-parcels-pager');
    if (!$pager.length) return;
    $pager.empty();

    const pages = Math.ceil(total / size) || 1;
    const start = Math.max(0, page - 2);
    const end = Math.min(pages - 1, page + 2);

    const prevDisabled = page <= 0 ? 'disabled' : '';
    $pager.append(`<button class="pager-btn btn btn-sm mr-2" data-page="${page - 1}" ${prevDisabled}>Prev</button>`);
    for (let p = start; p <= end; p++) {
      const active = p === page ? 'btn-active' : '';
      $pager.append(`<button class="pager-btn btn btn-sm mr-1 ${active}" data-page="${p}">${p + 1}</button>`);
    }
    const nextDisabled = page >= (pages - 1) ? 'disabled' : '';
    $pager.append(`<button class="pager-btn btn btn-sm ml-2" data-page="${page + 1}" ${nextDisabled}>Next</button>`);
  }

  // Polling
  function startPolling() {
    stopPolling();
    pollTimer = setInterval(() => {
      if (!Api.isAuthenticated()) return;
      reloadParcels();
    }, POLL_INTERVAL_MS);
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  // --- Events ---
  $(document).on('click', '.view-parcel', function () {
    const id = $(this).data('id');
    if (id) openParcelDetails(id);
  });

  $(document).on('click', '.assign-parcel', function () {
    const id = $(this).data('id');
    const $modal = $('#parcel-details-modal');
    openParcelDetails(id);
    // show modal; details modal will populate driver list
  });

  $(document).on('click', '.change-status', function () {
    const id = $(this).data('id');
    const current = $(this).data('status');
    const choices = ['PENDING', 'HOLD', 'ACCEPTED', 'IN_TRANSIT', 'PICKED_UP', 'DELIVERED', 'CANCELLED'];
    Swal.fire({
      title: 'Change Status',
      input: 'select',
      inputOptions: choices.reduce((o, s) => (o[s] = s.replace('_', ' '), o), {}),
      inputValue: current,
      showCancelButton: true
    }).then(async (r) => {
      if (!r.isConfirmed) return;
      await changeParcelStatus(id, r.value, $(this));
    });
  });

  // Modal actions
  $(document).on('click', '#parcel-details-modal .assign-confirm', function () {
    const $modal = $('#parcel-details-modal');
    const parcelId = $(this).data('id') || $modal.find('.modal-title').data('id');
    const driverId = $modal.find('select[name="assignDriver"]').val();
    if (!driverId) {
      toast({ title: 'Select driver first', icon: 'info' });
      return;
    }
    assignParcelToDriver(parcelId, driverId, $(this));
  });

  $(document).on('click', '#parcel-details-modal .status-confirm', function () {
    const newStatus = $('#parcel-details-modal select[name="status"]').val();
    const parcelId = $(this).data('id') || $('#parcel-details-modal').find('.modal-title').data('id');
    if (!newStatus) {
      toast({ title: 'Select status', icon: 'info' });
      return;
    }
    changeParcelStatus(parcelId, newStatus, $(this));
  });

  // Bulk controls
  $(document).on('click', '#bulk-select-all', function () {
    const checked = $(this).prop('checked');
    $('#staff-parcels-table tbody .bulk-checkbox').prop('checked', checked);
  });

  $(document).on('click', '#bulk-export', async function () {
    const ids = $('#staff-parcels-table tbody .bulk-checkbox:checked').map((i, el) => $(el).data('id')).get();
    await bulkExportCsv(ids.length ? ids : null);
  });

  $(document).on('click', '#bulk-status-change', async function () {
    const ids = $('#staff-parcels-table tbody .bulk-checkbox:checked').map((i, el) => $(el).data('id')).get();
    if (!ids.length) {
      toast({ title: 'Select parcels first', icon: 'info' });
      return;
    }
    const { value } = await Swal.fire({
      title: 'Select status',
      input: 'select',
      inputOptions: {
        PENDING: 'Pending',
        HOLD: 'Hold',
        IN_TRANSIT: 'In Transit',
        DELIVERED: 'Delivered',
        CANCELLED: 'Cancelled'
      },
      showCancelButton: true
    });
    if (!value) return;
    await bulkChangeStatus(ids, value);
  });

  // search & filter
  let searchTimeout = null;
  $(document).on('input', '#staff-search', function () {
    const q = $(this).val();
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentQuery.q = q;
      currentQuery.page = 0;
      reloadParcels();
    }, 350);
  });

  $(document).on('change', '#staff-filter-status', function () {
    currentQuery.status = $(this).val() || '';
    currentQuery.page = 0;
    reloadParcels();
  });

  $(document).on('click', '.pager-btn', function () {
    const page = Number($(this).data('page'));
    if (isNaN(page) || page < 0) return;
    currentQuery.page = page;
    reloadParcels();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  // modal close
  $(document).on('click', '#parcel-details-modal .modal-close, #parcel-details-modal .modal-backdrop', function () {
    $('#parcel-details-modal').removeClass('open');
  });

  // cleanup on unload
  $(window).on('beforeunload', function () {
    stopPolling();
  });

  // --- Init ---
  $(function init() {
    if (!Api.isAuthenticated()) return;
    // wire modal buttons data-id from last opened parcel (optional)
    $('#parcel-details-modal .assign-confirm, #parcel-details-modal .status-confirm').each(function () {
      const $b = $(this);
      $b.data('id', $b.data('id') || null);
    });

    reloadParcels();
    startPolling();
  });

})(window.jQuery, window.GoLankaApi, window.Swal);
