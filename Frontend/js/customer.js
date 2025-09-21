/**
 * customer.js
 * Customer dashboard + create-shipment logic for GoLanka Courier frontend
 *
 * Requirements:
 *  - jQuery
 *  - SweetAlert2 (Swal)
 *  - GoLankaApi (script.js) with apiJson, getUser, isAuthenticated helpers
 *
 * Expected DOM elements / selectors (adjust to match your HTML):
 *  - Dashboard:
 *    - #customer-name
 *    - #customer-email
 *    - #parcel-list tbody
 *    - #parcel-pagination (optional)
 *  - Create shipment form: #create-parcel-form
 *    fields: name (recipientFullName), recipientEmail, recipientPhone,
 *            pickupAddress, deliveryAddress, weightKg, lengthCm, widthCm, heightCm,
 *            parcelType, valueLkr (declared value)
 *    - button[type=submit] inside the form
 *    - #quote-result .price, .eta, .currency
 *  - Quote form (optional separate): #quote-form
 *  - Parcel details modal container: #parcel-details-modal (fill content)
 *
 * Add / modify selectors if your markup differs.
 */

(function ($, Api, Swal) {

  console.log('customer.js is loading...');

  'use strict';

  if (!$) throw new Error('jQuery required');
  if (!Api) throw new Error('GoLankaApi (script.js) required');

  const POLL_INTERVAL_MS = 10000; // poll tracking every 10s while modal open

  // small helper to show toast via SweetAlert2
  function toast(opts) {
    const title = opts.title || opts.text || '';
    const icon = opts.icon || 'info';
    Swal.fire({
      toast: true,
      position: 'top-end',
      showConfirmButton: false,
      timer: opts.timer || 3000,
      icon,
      title
    });
  }

  function setSubmitting($btn, submitting) {
    if (!$btn || !$btn.length) return;
    if (submitting) {
      $btn.data('orig-text', $btn.html());
      $btn.prop('disabled', true).html('<span class="loader inline-block w-4 h-4 animate-spin mr-2"></span> Processing...');
    } else {
      $btn.prop('disabled', false).html($btn.data('orig-text') || $btn.html());
    }
  }

  // Show server-side validation errors (same format used earlier)
  function showValidationErrors($form, errors) {
    $form.find('.field-error').remove();
    $form.find('.is-invalid').removeClass('is-invalid');

    if (!Array.isArray(errors)) return;
    errors.forEach(err => {
      const name = err.field;
      const message = err.message || 'Invalid';
      let $field = $form.find(`[name="${name}"]`);
      if (!$field.length) $field = $form.find(`#${name}`);
      if ($field.length) {
        $field.addClass('is-invalid');
        const $err = $(`<div class="field-error text-sm text-red-600 mt-1">${message}</div>`);
        $field.after($err);
      } else {
        toast({ title: message, icon: 'error' });
      }
    });
  }

  // Generic error handler
  async function handleError($form, err) {
    if (!err) {
      Swal.fire('Error', 'Unknown error', 'error');
      return;
    }
    if (err.body && Array.isArray(err.body.validationErrors) && $form) {
      showValidationErrors($form, err.body.validationErrors);
      return;
    }
    const msg = (err.body && err.body.message) || err.message || JSON.stringify(err);
    Swal.fire('Error', msg, 'error');
  }

  // --- Dashboard init ---
  async function loadCustomerDashboard() {
    try {
      const user = Api.getUser();
      if (user) {
        $('#customer-name').text(user.fullName || user.sub || 'Customer');
        $('#customer-email').text(user.email || '');
      }

      await fetchAndRenderParcels();
      await fetchProfileToUI();
    } catch (e) {
      console.error('Failed initialize dashboard', e);
    }
  }

  // fetch profile and put into profile panel inputs (if any)
  async function fetchProfileToUI() {
    try {
      const profile = await Api.apiJson('/api/users/me', { method: 'GET' });
      // populate profile display fields if present
      if ($('#profile-fullName').length) $('#profile-fullName').val(profile.fullName || '');
      if ($('#profile-phone').length) $('#profile-phone').val(profile.phone || '');
      if ($('#profile-city').length) $('#profile-city').val(profile.city || '');
      // ... populate other fields as needed
    } catch (err) {
      console.warn('Could not fetch profile', err);
    }
  }

  // --- Parcels listing & rendering ---
  async function fetchAndRenderParcels(query = {}) {
    // query may include page, size, status, search
    try {
      // build query string
      const qs = new URLSearchParams(query).toString();
      const url = '/api/parcels' + (qs ? `?${qs}` : '');
      const data = await Api.apiJson(url, { method: 'GET' });

      renderParcelsTable(data.items || data || []);
      // optional: render pagination from data.meta
    } catch (err) {
      console.error('Failed to fetch parcels', err);
      toast({ title: 'Failed to load parcels', icon: 'error' });
    }
  }

  function renderParcelsTable(items) {
    const $tbody = $('#parcel-list tbody');
    if (!$tbody.length) return;
    $tbody.empty();

    if (!items || items.length === 0) {
      $tbody.append('<tr><td colspan="6" class="text-center py-4">No parcels found.</td></tr>');
      return;
    }

    items.forEach(p => {
      const statusBadge = renderStatusBadge(p.status);
      const created = p.createdAt ? new Date(p.createdAt).toLocaleString() : '-';
      const row = `
        <tr data-id="${p.id}" class="hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
          <td class="px-4 py-2">${p.trackingNumber || ('#' + p.id)}</td>
          <td class="px-4 py-2">${p.recipientFullName || p.recipientName || '-'}</td>
          <td class="px-4 py-2">${p.pickupAddress || '-'}</td>
          <td class="px-4 py-2">${p.deliveryAddress || '-'}</td>
          <td class="px-4 py-2">${statusBadge}</td>
          <td class="px-4 py-2 text-sm text-slate-600">${created}</td>
          <td class="px-4 py-2">
            <button class="view-parcel btn btn-sm" data-id="${p.id}">View</button>
            ${p.canCancel ? `<button class="cancel-parcel btn btn-sm ml-2 text-red-600" data-id="${p.id}">Cancel</button>` : ''}
          </td>
        </tr>
      `;
      $tbody.append(row);
    });
  }

  function renderStatusBadge(status) {
    // example statuses: CREATED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    const s = (status || '').toUpperCase();
    let cls = 'px-2 py-1 rounded text-sm';
    let label = s || 'UNKNOWN';
    switch (s) {
      case 'CREATED': cls += ' bg-yellow-100 text-yellow-800'; break;
      case 'PICKED_UP': cls += ' bg-indigo-100 text-indigo-800'; break;
      case 'IN_TRANSIT': cls += ' bg-blue-100 text-blue-800'; break;
      case 'OUT_FOR_DELIVERY': cls += ' bg-orange-100 text-orange-800'; break;
      case 'DELIVERED': cls += ' bg-green-100 text-green-800'; break;
      case 'CANCELLED': cls += ' bg-red-100 text-red-800'; break;
      default: cls += ' bg-slate-100 text-slate-800';
    }
    return `<span class="${cls}">${label.replaceAll('_', ' ')}</span>`;
  }

  // --- View single parcel (details + polling tracking updates) ---
  let parcelPollTimer = null;
  let currentViewedParcelId = null;

  async function openParcelDetails(parcelId) {
    try {
      const p = await Api.apiJson(`/api/parcels/${parcelId}`, { method: 'GET' });
      renderParcelModal(p);
      // start polling for live updates
      currentViewedParcelId = parcelId;
      startParcelPolling(parcelId);
      $('#parcel-details-modal').addClass('open'); // or show modal depending on your HTML
    } catch (err) {
      console.error('Failed fetch parcel', err);
      toast({ title: 'Failed to load parcel details', icon: 'error' });
    }
  }

  async function renderParcelModal(p) {
    const $m = $('#parcel-details-modal');
    if (!$m.length) {
      // fallback: show details via swal
      Swal.fire({
        title: `Parcel ${p.trackingNumber || p.id}`,
        html: `<pre style="text-align:left">${JSON.stringify(p, null, 2)}</pre>`,
        width: '80%',
      });
      return;
    }
    $m.find('.modal-title').text(p.trackingNumber || ('Parcel #' + p.id));
    $m.find('.parcel-status').html(renderStatusBadge(p.status));
    $m.find('.parcel-recipient').text(p.recipientFullName || '-');
    $m.find('.parcel-pickup').text(p.pickupAddress || '-');
    $m.find('.parcel-delivery').text(p.deliveryAddress || '-');
    $m.find('.parcel-weight').text((p.weightKg ? p.weightKg + ' kg' : '-'));
    $m.find('.parcel-dimensions').text(`${p.lengthCm || '-'} x ${p.widthCm || '-'} x ${p.heightCm || '-'}`);
    $m.find('.parcel-updated-at').text(p.updatedAt ? new Date(p.updatedAt).toLocaleString() : '-');

    // render status history if present
    const $hist = $m.find('.parcel-history').empty();
    if (Array.isArray(p.history) && p.history.length) {
      p.history.forEach(h => {
        $hist.append(`<div class="py-1 border-b"><strong>${h.status}</strong> — <span class="text-sm">${h.note || ''}</span> <div class="text-xs text-slate-500">${new Date(h.createdAt).toLocaleString()}</div></div>`);
      });
    } else {
      $hist.append('<div class="text-sm text-slate-500">No history available</div>');
    }
  }

  function startParcelPolling(parcelId) {
    stopParcelPolling();
    parcelPollTimer = setInterval(async () => {
      try {
        const track = await Api.apiJson(`/api/parcels/${parcelId}/track`, { method: 'GET' });
        // update minimal UI: status + history
        renderParcelModal(track);
        // also refresh parcel list to show updated status
        await fetchAndRenderParcels();
      } catch (err) {
        // If polling fails, keep trying. Only log.
        console.warn('tracking poll failed', err);
      }
    }, POLL_INTERVAL_MS);
  }

  function stopParcelPolling() {
    if (parcelPollTimer) {
      clearInterval(parcelPollTimer);
      parcelPollTimer = null;
      currentViewedParcelId = null;
    }
  }

  // --- Cancel parcel ---
  async function cancelParcel(parcelId, $btn) {
    const confirm = await Swal.fire({
      title: 'Cancel parcel?',
      text: 'This will cancel the shipment if it has not already been delivered.',
      icon: 'warning',
      showCancelButton: true
    });
    if (!confirm.isConfirmed) return;

    setSubmitting($btn, true);
    try {
      const res = await Api.apiJson(`/api/parcels/${parcelId}/cancel`, { method: 'POST' });
      toast({ title: res.message || 'Parcel cancelled', icon: 'success' });
      await fetchAndRenderParcels();
      if (currentViewedParcelId === parcelId) {
        await openParcelDetails(parcelId); // refresh modal
      }
    } catch (err) {
      await handleError(null, err);
    } finally {
      setSubmitting($btn, false);
    }
  }

  // --- Create shipment / Quote ---
  // Form: #create-parcel-form
  $('#create-parcel-form').on('submit', async function (e) {
    e.preventDefault();
    const $form = $(this);
    const $btn = $form.find('button[type="submit"]');

    // gather fields
    const payload = {
      recipientFullName: $form.find('[name="recipientFullName"]').val(),
      recipientEmail: $form.find('[name="recipientEmail"]').val(),
      recipientPhone: $form.find('[name="recipientPhone"]').val(),
      pickupAddress: $form.find('[name="pickupAddress"]').val(),
      deliveryAddress: $form.find('[name="deliveryAddress"]').val(),
      weightKg: parseFloat($form.find('[name="weightKg"]').val()) || 0,
      lengthCm: parseFloat($form.find('[name="lengthCm"]').val()) || null,
      widthCm: parseFloat($form.find('[name="widthCm"]').val()) || null,
      heightCm: parseFloat($form.find('[name="heightCm"]').val()) || null,
      parcelType: $form.find('[name="parcelType"]').val() || 'PARCEL',
      valueLkr: parseFloat($form.find('[name="valueLkr"]').val()) || 0,
      notes: $form.find('[name="notes"]').val() || ''
    };

    // basic client side validation
    const missing = [];
    if (!payload.recipientFullName) missing.push('recipientFullName');
    if (!payload.recipientPhone) missing.push('recipientPhone');
    if (!payload.pickupAddress) missing.push('pickupAddress');
    if (!payload.deliveryAddress) missing.push('deliveryAddress');
    if (!payload.weightKg || payload.weightKg <= 0) missing.push('weightKg');

    $form.find('.field-error').remove();
    $form.find('.is-invalid').removeClass('is-invalid');

    if (missing.length) {
      missing.forEach(n => {
        const $f = $form.find(`[name="${n}"]`);
        if ($f.length) {
          $f.addClass('is-invalid').after(`<div class="field-error text-sm text-red-600 mt-1">Required</div>`);
        }
      });
      toast({ title: 'Please fill required fields', icon: 'warning' });
      return;
    }

    setSubmitting($btn, true);

    try {
      // optionally first request quote
      const quoteRes = await Api.apiJson('/api/parcels/quote', { method: 'POST', body: payload });
      // show quote and confirm
      const priceText = quoteRes.price ? `${quoteRes.price} ${quoteRes.currency || 'LKR'}` : 'See below';
      const confirm = await Swal.fire({
        title: 'Shipment quote',
        html: `<div class="text-left"><p>Estimated price: <strong>${priceText}</strong></p>
               <p>ETA: <strong>${quoteRes.eta || '—'}</strong></p>
               <p>Service: <strong>${quoteRes.service || 'Standard'}</strong></p></div>`,
        showCancelButton: true,
        confirmButtonText: 'Create shipment',
      });
      if (!confirm.isConfirmed) {
        setSubmitting($btn, false);
        return;
      }

      // create parcel
      const createRes = await Api.apiJson('/api/parcels', { method: 'POST', body: payload });
      toast({ title: createRes.message || 'Shipment created', icon: 'success' });
      $form[0].reset();
      // refresh list
      await fetchAndRenderParcels();
      // optionally open details for new parcel
      if (createRes.id) openParcelDetails(createRes.id);
    } catch (err) {
      await handleError($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // --- Quick quote form (optional) ---
  $('#quote-form').on('submit', async function (e) {
    e.preventDefault();
    const $form = $(this);
    const $btn = $form.find('button[type="submit"]');
    const payload = {
      pickupAddress: $form.find('[name="pickupAddress"]').val(),
      deliveryAddress: $form.find('[name="deliveryAddress"]').val(),
      weightKg: parseFloat($form.find('[name="weightKg"]').val()) || 0,
      parcelType: $form.find('[name="parcelType"]').val() || 'PARCEL'
    };
    setSubmitting($btn, true);
    try {
      const q = await Api.apiJson('/api/parcels/quote', { method: 'POST', body: payload });
      $('#quote-result .price').text(q.price ? `${q.price} ${q.currency || 'LKR'}` : '—');
      $('#quote-result .eta').text(q.eta || '—');
      $('#quote-result').removeClass('hidden');
    } catch (err) {
      await handleError($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // --- Events: delegate actions from table ---
  $(document).on('click', '.view-parcel', function () {
    const id = $(this).data('id');
    if (id) openParcelDetails(id);
  });

  $(document).on('click', '.cancel-parcel', function () {
    const id = $(this).data('id');
    const $btn = $(this);
    if (id) cancelParcel(id, $btn);
  });

  // Close modal handlers (assumes a .close-modal button)
  $(document).on('click', '.modal-close, .modal-backdrop', function () {
    $('#parcel-details-modal').removeClass('open');
    stopParcelPolling();
  });

  // cleanup on page unload
  $(window).on('beforeunload', function () {
    stopParcelPolling();
  });

  // --- Profile update (optional) ---
  $('#profile-form').on('submit', async function (e) {
    e.preventDefault();
    const $form = $(this);
    const $btn = $form.find('button[type="submit"]');

    const body = {
      fullName: $form.find('[name="fullName"]').val(),
      phone: $form.find('[name="phone"]').val(),
      city: $form.find('[name="city"]').val(),
      province: $form.find('[name="province"]').val(),
      postalCode: $form.find('[name="postalCode"]').val(),
      addressLine: $form.find('[name="addressLine"]').val()
    };

    setSubmitting($btn, true);
    try {
      const res = await Api.apiJson('/api/users/me', { method: 'PUT', body });
      toast({ title: res.message || 'Profile updated', icon: 'success' });
    } catch (err) {
      await handleError($form, err);
    } finally {
      setSubmitting($btn, false);
    }
  });

  // --- init when DOM ready ---
  $(function init() {
    // only run if authenticated and on customer pages
    if (!Api.isAuthenticated()) return;

    // Initialize UI components / dashboard
    loadCustomerDashboard();

    // optionally wire quick filters
    $('#parcel-filter-form').on('submit', function (e) {
      e.preventDefault();
      const status = $(this).find('[name="status"]').val();
      fetchAndRenderParcels({ status });
    });

    // auto-refresh parcel list periodically (not as aggressive as modal polling)
    setInterval(() => {
      // do not refresh if modal polling active (modal will refresh)
      if (!parcelPollTimer) fetchAndRenderParcels();
    }, 30000); // 30s
  });

})(window.jQuery, window.GoLankaApi, window.Swal);
