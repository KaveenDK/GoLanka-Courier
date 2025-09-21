/**
 * driver.js
 * Driver dashboard actions for GoLanka Courier frontend
 *
 * Requirements:
 *  - jQuery
 *  - SweetAlert2 (Swal)
 *  - GoLankaApi (script.js) with apiJson, getUser, isAuthenticated helpers
 *
 * Expected DOM elements / selectors (adjust to match your HTML):
 *  - #driver-name, #driver-email
 *  - #availability-toggle (checkbox/button) to set availability
 *  - #assignments-table tbody
 *  - #assignment-details-modal
 *  - #driver-location-share (button to share current GPS coordinates)
 *  - Buttons inside table rows: .accept-assignment, .decline-assignment, .navigate-pickup, .navigate-destination, .mark-picked, .mark-delivered
 *
 * Endpoints assumed (adjust to your backend):
 *  - GET  /api/driver/assignments                  -> list assigned/pending assignments for this driver
 *  - POST /api/driver/assignments/{id}/accept
 *  - POST /api/driver/assignments/{id}/decline
 *  - POST /api/driver/assignments/{id}/pickup
 *  - POST /api/driver/assignments/{id}/deliver
 *  - GET  /api/driver/assignments/{id}             -> details
 *  - GET  /api/driver/me                            -> driver profile
 *  - POST /api/driver/availability                  -> { available: true/false }
 *
 * Notes:
 *  - This file focuses on UI interactions & calling API wrapper (GoLankaApi.apiJson).
 *  - Add or change selectors to match your HTML markup.
 */

(function ($, Api, Swal) {

    console.log('driver.js is loading...');

  'use strict';

  if (!$) throw new Error('jQuery required');
  if (!Api) throw new Error('GoLankaApi (script.js) required');

  const POLL_INTERVAL_MS = 15000; // poll assignments every 15s
  let pollTimer = null;

  // Small helpers (copied/compatible with customer.js)
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

  async function handleError($form, err) {
    if (!err) {
      Swal.fire('Error', 'Unknown error', 'error');
      return;
    }
    if (err.body && Array.isArray(err.body.validationErrors) && $form) {
      // show validation errors near fields if UI form present (not common for driver actions)
      $form.find('.field-error').remove();
      $form.find('.is-invalid').removeClass('is-invalid');
      err.body.validationErrors.forEach(e => {
        const $f = $form.find(`[name="${e.field}"]`);
        if ($f.length) {
          $f.addClass('is-invalid').after(`<div class="field-error text-sm text-red-600 mt-1">${e.message}</div>`);
        } else {
          toast({ title: e.message, icon: 'error' });
        }
      });
      return;
    }
    const msg = (err.body && err.body.message) || err.message || JSON.stringify(err);
    Swal.fire('Error', msg, 'error');
  }

  // --- Driver dashboard init ---
  async function loadDriverDashboard() {
    try {
      const user = Api.getUser();
      if (user) {
        $('#driver-name').text(user.fullName || user.sub || 'Driver');
        $('#driver-email').text(user.email || '');
      }

      await fetchAndRenderAssignments();
      await fetchDriverProfileAndAvailability();
      startPolling();
    } catch (e) {
      console.error('Failed to initialize driver dashboard', e);
    }
  }

  // fetch driver profile (for availability toggle etc.)
  async function fetchDriverProfileAndAvailability() {
    try {
      const profile = await Api.apiJson('/api/driver/me', { method: 'GET' });
      // if your API returns an `available` flag
      if ($('#availability-toggle').length) {
        const avail = !!profile.available;
        $('#availability-toggle').prop('checked', avail);
        $('#availability-label').text(avail ? 'Available' : 'Unavailable');
      }
      // populate other fields if any
      if ($('#driver-vehicle').length) $('#driver-vehicle').text(profile.vehicle || '-');
    } catch (err) {
      console.warn('Could not fetch driver profile', err);
    }
  }

  // --- Assignments (jobs) list ---
  async function fetchAndRenderAssignments() {
    try {
      const data = await Api.apiJson('/api/driver/assignments', { method: 'GET' });
      // backend may return list or object; normalize
      const items = Array.isArray(data) ? data : (data.items || data.assignments || []);
      renderAssignmentsTable(items);
    } catch (err) {
      console.error('Failed to fetch assignments', err);
      toast({ title: 'Failed to load assignments', icon: 'error' });
    }
  }

  function renderAssignmentsTable(items) {
    const $tbody = $('#assignments-table tbody');
    if (!$tbody.length) return;
    $tbody.empty();

    if (!items || items.length === 0) {
      $tbody.append('<tr><td colspan="7" class="text-center py-6">No assignments available</td></tr>');
      return;
    }

    items.forEach(a => {
      const created = a.createdAt ? new Date(a.createdAt).toLocaleString() : '-';
      const canAccept = a.status === 'PENDING' && !a.assignedTo;
      const canPickup = a.status === 'ASSIGNED' && a.canMarkPicked;
      const canDeliver = a.status === 'PICKED_UP' && a.canMarkDelivered;

      const row = `
        <tr data-id="${a.id}" class="hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
          <td class="px-4 py-2">${a.trackingNumber || ('#' + a.id)}</td>
          <td class="px-4 py-2">${a.customerName || a.recipientFullName || '-'}</td>
          <td class="px-4 py-2">${a.pickupAddress || '-'}</td>
          <td class="px-4 py-2">${a.deliveryAddress || '-'}</td>
          <td class="px-4 py-2">${a.status || '-'}</td>
          <td class="px-4 py-2 text-sm text-slate-600">${created}</td>
          <td class="px-4 py-2">
            ${canAccept ? `<button class="accept-assignment btn btn-sm" data-id="${a.id}">Accept</button>
                           <button class="decline-assignment btn btn-sm ml-2" data-id="${a.id}">Decline</button>` : ''}
            <button class="view-assignment btn btn-sm ml-2" data-id="${a.id}">View</button>
            ${canPickup ? `<button class="mark-picked btn btn-sm ml-2" data-id="${a.id}">Picked</button>` : ''}
            ${canDeliver ? `<button class="mark-delivered btn btn-sm ml-2" data-id="${a.id}">Delivered</button>` : ''}
            <button class="navigate-pickup btn btn-sm ml-2" data-id="${a.id}" data-lat="${a.pickupLat || ''}" data-lng="${a.pickupLng || ''}" data-address="${encodeURIComponent(a.pickupAddress || '')}">Nav Pickup</button>
            <button class="navigate-destination btn btn-sm ml-2" data-id="${a.id}" data-lat="${a.deliveryLat || ''}" data-lng="${a.deliveryLng || ''}" data-address="${encodeURIComponent(a.deliveryAddress || '')}">Nav Dest</button>
          </td>
        </tr>
      `;
      $tbody.append(row);
    });
  }

  // --- Actions: accept / decline / pickup / deliver ---
  async function acceptAssignment(id, $btn) {
    setSubmitting($btn, true);
    try {
      const res = await Api.apiJson(`/api/driver/assignments/${id}/accept`, { method: 'POST' });
      toast({ title: res.message || 'Assignment accepted', icon: 'success' });
      await fetchAndRenderAssignments();
    } catch (err) {
      await handleError(null, err);
    } finally {
      setSubmitting($btn, false);
    }
  }

  async function declineAssignment(id, $btn) {
    const { value } = await Swal.fire({
      title: 'Decline assignment',
      input: 'textarea',
      inputLabel: 'Reason (optional)',
      inputPlaceholder: 'Reason for declining...',
      showCancelButton: true
    });
    if (value === undefined) return; // cancelled

    setSubmitting($btn, true);
    try {
      const body = { reason: value || null };
      const res = await Api.apiJson(`/api/driver/assignments/${id}/decline`, { method: 'POST', body });
      toast({ title: res.message || 'Assignment declined', icon: 'success' });
      await fetchAndRenderAssignments();
    } catch (err) {
      await handleError(null, err);
    } finally {
      setSubmitting($btn, false);
    }
  }

  async function markPicked(id, $btn) {
    setSubmitting($btn, true);
    try {
      const res = await Api.apiJson(`/api/driver/assignments/${id}/pickup`, { method: 'POST' });
      toast({ title: res.message || 'Marked as picked up', icon: 'success' });
      await fetchAndRenderAssignments();
      openAssignmentDetails(id); // refresh modal if open
    } catch (err) {
      await handleError(null, err);
    } finally {
      setSubmitting($btn, false);
    }
  }

  async function markDelivered(id, $btn) {
    // optionally ask for recipient signature or photo
    const confirm = await Swal.fire({
      title: 'Confirm delivery',
      text: 'You can add a note or delivery proof after confirming.',
      showCancelButton: true
    });
    if (!confirm.isConfirmed) return;

    setSubmitting($btn, true);
    try {
      const res = await Api.apiJson(`/api/driver/assignments/${id}/deliver`, { method: 'POST' });
      toast({ title: res.message || 'Marked as delivered', icon: 'success' });
      await fetchAndRenderAssignments();
      openAssignmentDetails(id);
    } catch (err) {
      await handleError(null, err);
    } finally {
      setSubmitting($btn, false);
    }
  }

  // --- View single assignment (details modal) ---
  async function openAssignmentDetails(id) {
    try {
      const a = await Api.apiJson(`/api/driver/assignments/${id}`, { method: 'GET' });
      renderAssignmentModal(a);
      $('#assignment-details-modal').addClass('open');
    } catch (err) {
      console.error('Failed fetch assignment', err);
      toast({ title: 'Failed to load details', icon: 'error' });
    }
  }

  function renderAssignmentModal(a) {
    const $m = $('#assignment-details-modal');
    if (!$m.length) {
      Swal.fire({
        title: `Assignment ${a.trackingNumber || a.id}`,
        html: `<pre style="text-align:left">${JSON.stringify(a, null, 2)}</pre>`,
        width: '80%'
      });
      return;
    }

    $m.find('.modal-title').text(a.trackingNumber || ('#' + a.id));
    $m.find('.assign-status').text(a.status || '-');
    $m.find('.assign-recipient').text(a.recipientFullName || '-');
    $m.find('.assign-pickup').text(a.pickupAddress || '-');
    $m.find('.assign-delivery').text(a.deliveryAddress || '-');
    $m.find('.assign-notes').text(a.notes || '-');
    $m.find('.assign-weight').text(a.weightKg ? `${a.weightKg} kg` : '-');
    $m.find('.assign-updated').text(a.updatedAt ? new Date(a.updatedAt).toLocaleString() : '-');

    // optional: show navigate buttons' hrefs
    const pickupHref = googleMapsLink(a.pickupLat, a.pickupLng, a.pickupAddress);
    const destHref = googleMapsLink(a.deliveryLat, a.deliveryLng, a.deliveryAddress);
    $m.find('.navigate-pickup').attr('href', pickupHref).attr('target', '_blank');
    $m.find('.navigate-destination').attr('href', destHref).attr('target', '_blank');
  }

  // --- Availability toggle ---
  async function setAvailability(available) {
    try {
      const res = await Api.apiJson('/api/driver/availability', { method: 'POST', body: { available } });
      $('#availability-label').text(available ? 'Available' : 'Unavailable');
      toast({ title: res.message || (available ? 'You are now available' : 'You are now unavailable'), icon: 'success' });
    } catch (err) {
      // revert checkbox if failed
      $('#availability-toggle').prop('checked', !available);
      await handleError(null, err);
    }
  }

  // --- Location helpers / navigation ---
  function googleMapsLink(lat, lng, address) {
    if (lat && lng) return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
    if (address) return `https://www.google.com/maps/dir/?api=1&destination=${address}`;
    return 'https://www.google.com/maps';
  }

  // share current GPS location to backend (optional)
  async function shareCurrentLocation() {
    if (!navigator.geolocation) {
      toast({ title: 'Geolocation not supported', icon: 'error' });
      return;
    }
    const $btn = $('#driver-location-share');
    setSubmitting($btn, true);
    navigator.geolocation.getCurrentPosition(async (pos) => {
      try {
        const body = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        const res = await Api.apiJson('/api/driver/location', { method: 'POST', body });
        toast({ title: res.message || 'Location shared', icon: 'success' });
      } catch (err) {
        await handleError(null, err);
      } finally {
        setSubmitting($btn, false);
      }
    }, (err) => {
      setSubmitting($btn, false);
      toast({ title: 'Could not get location', icon: 'error' });
    }, { enableHighAccuracy: true, timeout: 10000 });
  }

  // --- Polling ---
  function startPolling() {
    stopPolling();
    pollTimer = setInterval(() => {
      // if user not on driver pages, skip (optional)
      if (!Api.isAuthenticated()) return;
      fetchAndRenderAssignments();
    }, POLL_INTERVAL_MS);
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  // --- Event bindings ---
  $(document).on('click', '.accept-assignment', function () {
    const id = $(this).data('id');
    if (id) acceptAssignment(id, $(this));
  });

  $(document).on('click', '.decline-assignment', function () {
    const id = $(this).data('id');
    if (id) declineAssignment(id, $(this));
  });

  $(document).on('click', '.mark-picked', function () {
    const id = $(this).data('id');
    if (id) markPicked(id, $(this));
  });

  $(document).on('click', '.mark-delivered', function () {
    const id = $(this).data('id');
    if (id) markDelivered(id, $(this));
  });

  $(document).on('click', '.view-assignment', function () {
    const id = $(this).data('id');
    if (id) openAssignmentDetails(id);
  });

  $(document).on('click', '.navigate-pickup', function (e) {
    // allow default anchor navigation; if a button, open new tab
    const lat = $(this).data('lat');
    const lng = $(this).data('lng');
    const address = decodeURIComponent($(this).data('address') || '');
    const href = googleMapsLink(lat, lng, address);
    window.open(href, '_blank');
  });

  $(document).on('click', '.navigate-destination', function (e) {
    const lat = $(this).data('lat');
    const lng = $(this).data('lng');
    const address = decodeURIComponent($(this).data('address') || '');
    const href = googleMapsLink(lat, lng, address);
    window.open(href, '_blank');
  });

  $('#availability-toggle').on('change', function () {
    const v = $(this).prop('checked');
    setAvailability(v);
  });

  $('#driver-location-share').on('click', function () {
    shareCurrentLocation();
  });

  // Modal close
  $(document).on('click', '#assignment-details-modal .modal-close, #assignment-details-modal .modal-backdrop', function () {
    $('#assignment-details-modal').removeClass('open');
  });

  // Cleanup
  $(window).on('beforeunload', function () {
    stopPolling();
  });

  // Init
  $(function init() {
    if (!Api.isAuthenticated()) return;
    loadDriverDashboard();
  });

})(window.jQuery, window.GoLankaApi, window.Swal);
