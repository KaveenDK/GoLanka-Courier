(function ($, Api, Swal) {

    console.log('notifications.js is loading...');

  'use strict';
  if (!$) throw new Error('jQuery required');
  if (!Api) throw new Error('GoLankaApi required');

  // CONFIG
  const POLL_INTERVAL_MS = 15000; // 15s between polls when authenticated
  const TOAST_DURATION_MS = 5000; // toast auto close
  const MAX_LIST_ITEMS = 25;      // how many notifications to show in dropdown
  const NOTIF_ENDPOINT = '/api/notifications'; // GET returns list, POST/PUT used to mark read

  // Internal state
  let pollTimer = null;
  let lastSeenNotifId = null;     // can be used to fetch only new notifications if backend supports sinceId
  let unseenCache = [];           // newly fetched not-yet-read notifications
  let subscribers = [];           // functions to call on new notification

  // -------------------------
  // UI helpers
  // -------------------------
  function toast({ title = '', text = '', icon = 'info', onClick = null, timer = TOAST_DURATION_MS } = {}) {
    const toastInstance = Swal.fire({
      toast: true,
      position: 'top-end',
      showConfirmButton: false,
      timer,
      timerProgressBar: true,
      icon,
      title,
      text,
      didOpen: (toastEl) => {
        // attach pointer cursor if onClick provided
        if (onClick) {
          toastEl.style.cursor = 'pointer';
          toastEl.addEventListener('click', onClick);
        }
      }
    });
    return toastInstance;
  }

  function updateBadge(count) {
    const $badge = $('#notif-badge');
    if (!$badge.length) return;
    if (count > 0) {
      $badge.removeClass('hidden').text(count > 99 ? '99+' : String(count));
    } else {
      $badge.addClass('hidden').text('');
    }
  }

  function renderDropdown(list) {
    const $dd = $('#notif-dropdown');
    if (!$dd.length) return;
    $dd.empty();

    if (!list || list.length === 0) {
      $dd.append(`<div class="p-4 text-sm text-slate-500">No notifications</div>`);
      updateBadge(0);
      return;
    }

    list.slice(0, MAX_LIST_ITEMS).forEach(n => {
      const timeText = n.createdAt ? timeAgo(new Date(n.createdAt)) : '';
      const unreadClass = n.read ? 'opacity-70' : 'bg-slate-50 dark:bg-slate-800';
      const item = $(`
        <div class="notif-item flex items-start gap-3 p-3 border-b last:border-b-0 ${unreadClass}" data-id="${n.id}">
          <div class="w-10 flex-shrink-0">
            <div class="h-10 w-10 rounded-full flex items-center justify-center bg-slate-100 text-slate-700 font-medium">${(n.title || '').slice(0,1)}</div>
          </div>
          <div class="flex-1 text-sm">
            <div class="font-medium truncate">${escapeHtml(n.title || 'Notification')}</div>
            <div class="text-slate-600 text-xs truncate">${escapeHtml(n.body || '')}</div>
            <div class="text-xs text-slate-400 mt-1">${timeText}</div>
          </div>
          <div class="flex flex-col items-end ml-3">
            <button class="mark-read text-xs underline mb-2" data-id="${n.id}">${n.read ? 'Read' : 'Mark read'}</button>
            <button class="view-notif text-xs text-slate-500" data-id="${n.id}">Open</button>
          </div>
        </div>
      `);
      $dd.append(item);
    });

    // footer with actions
    $dd.append(`
      <div class="p-2 border-t mt-2 flex items-center justify-between">
        <button id="notif-mark-all-read" class="btn btn-sm">Mark all read</button>
        <a href="/notifications" class="text-sm underline">View all</a>
      </div>
    `);

    // update badge count
    const unreadCount = list.filter(x => !x.read).length;
    updateBadge(unreadCount);
  }

  function escapeHtml(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"']/g, function (m) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
  }

  function timeAgo(d) {
    if (!d) return '';
    const sec = Math.floor((Date.now() - +d) / 1000);
    if (sec < 60) return `${sec}s`;
    if (sec < 3600) return `${Math.floor(sec / 60)}m`;
    if (sec < 86400) return `${Math.floor(sec / 3600)}h`;
    return `${Math.floor(sec / 86400)}d`;
  }

  // -------------------------
  // API helpers
  // -------------------------
  async function fetchNotifications({ limit = 10, onlyUnread = false } = {}) {
    // Try to use sinceId if supported by backend
    const params = { limit };
    if (onlyUnread) params.unreadOnly = true;
    if (lastSeenNotifId) params.sinceId = lastSeenNotifId;
    try {
      const res = await Api.apiJson(NOTIF_ENDPOINT, { method: 'GET', params });
      // Expect an array or { items: [], total: n }
      if (Array.isArray(res)) return res;
      if (res.items) return res.items;
      return [];
    } catch (err) {
      // bubble up
      throw err;
    }
  }

  async function markNotificationRead(id) {
    if (!id) return;
    try {
      await Api.apiJson(`${NOTIF_ENDPOINT}/${id}/read`, { method: 'POST' });
      return true;
    } catch (err) {
      throw err;
    }
  }

  async function markAllNotificationsRead() {
    try {
      await Api.apiJson(`${NOTIF_ENDPOINT}/read-all`, { method: 'POST' });
      return true;
    } catch (err) {
      throw err;
    }
  }

  // -------------------------
  // Notification handling logic
  // -------------------------
  async function poll() {
    if (!Api.isAuthenticated()) {
      stopPolling();
      return;
    }
    try {
      const items = await fetchNotifications({ limit: MAX_LIST_ITEMS, onlyUnread: false });
      // calculate newly unseen (by id)
      const newItems = items.filter(it => !unseenCache.some(u => u.id === it.id));
      // update lastSeenNotifId (assume sorted desc)
      if (items.length > 0) lastSeenNotifId = items[0].id;
      unseenCache = items; // keep full cache (backend should provide ordering)

      // render dropdown UI
      renderDropdown(items);

      // show toasts for newly arrived unread items
      newItems.reverse().forEach(n => {
        if (!n.read) {
          // show toast with click behavior: open dropdown and mark read on click if possible
          toast({
            title: n.title || 'New notification',
            text: n.body || '',
            icon: 'info',
            timer: TOAST_DURATION_MS,
            onClick: () => {
              // open dropdown: user should wire dropdown opening; we just scroll to item and focus
              $('#notif-dropdown').closest('.dropdown').addClass('open');
              // small highlight and auto-mark read
              const $el = $(`#notif-dropdown [data-id="${n.id}"]`);
              if ($el.length) {
                $el.addClass('ring ring-2 ring-indigo-300');
                setTimeout(() => $el.removeClass('ring ring-2 ring-indigo-300'), 2000);
              }
              // mark read
              markNotificationRead(n.id).catch(() => {/* ignore */});
            }
          });
        }
      });

      // notify subscribers (if any)
      if (newItems.length > 0) {
        subscribers.forEach(fn => {
          try { fn(newItems); } catch (e) { console.warn('notif subscriber failed', e); }
        });
      }
    } catch (err) {
      // ignore transient errors; optionally show small toast for persistent errors
      console.warn('Notification poll error', err);
    }
  }

  // -------------------------
  // Poll control
  // -------------------------
  function startPolling() {
    if (pollTimer) return;
    // immediate run
    poll();
    pollTimer = setInterval(poll, POLL_INTERVAL_MS);
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  // -------------------------
  // DOM interactions
  // -------------------------
  // Clicking "mark read" in dropdown
  $(document).on('click', '#notif-dropdown .mark-read', async function (e) {
    e.preventDefault();
    const id = $(this).data('id');
    const $item = $(this).closest('.notif-item');
    try {
      await markNotificationRead(id);
      $item.addClass('opacity-70').find('.mark-read').text('Read');
      // update internal cache
      unseenCache = unseenCache.map(n => n.id === id ? { ...n, read: true } : n);
      updateBadge(unseenCache.filter(x => !x.read).length);
    } catch (err) {
      await handleError(null, err);
    }
  });

  // Clicking "view" opens link or triggers handler
  $(document).on('click', '#notif-dropdown .view-notif', function (e) {
    e.preventDefault();
    const id = $(this).data('id');
    const notification = unseenCache.find(n => n.id === id);
    if (!notification) return;
    // If notification has a URL, navigate; otherwise open /notifications page
    if (notification.url) {
      window.location.href = notification.url;
    } else {
      // fallback: open notifications page
      window.location.href = '/notifications';
    }
  });

  // Mark all read button
  $(document).on('click', '#notif-mark-all-read', async function (e) {
    e.preventDefault();
    const $btn = $(this);
    $btn.prop('disabled', true).text('Marking...');
    try {
      await markAllNotificationsRead();
      unseenCache = unseenCache.map(n => ({ ...n, read: true }));
      renderDropdown(unseenCache);
      updateBadge(0);
      toast({ title: 'All notifications marked read', icon: 'success' });
    } catch (err) {
      await handleError(null, err);
    } finally {
      $btn.prop('disabled', false).text('Mark all read');
    }
  });

  // Small error handler for DOM events
  async function handleError($context, err) {
    console.error('Notifications error', err);
    const message = (err && err.body && err.body.message) || (err && err.message) || 'Request failed';
    try {
      await Swal.fire('Error', message, 'error');
    } catch (e) {
      // ignore
    }
  }

  // -------------------------
  // Public API
  // -------------------------
  const publicApi = {
    start: startPolling,
    stop: stopPolling,
    fetchNow: poll,
    subscribe(fn) {
      if (typeof fn === 'function') subscribers.push(fn);
      return () => { subscribers = subscribers.filter(f => f !== fn); };
    },
    getCache() { return [...unseenCache]; },
    markRead: markNotificationRead,
    markAllRead: markAllNotificationsRead
  };

  // Expose globally
  window.GoLankaNotifications = publicApi;

  // -------------------------
  // Auto-init on DOM ready
  // -------------------------
  $(function () {
    // If user is authenticated, start polling and render initial dropdown
    if (Api.isAuthenticated && Api.isAuthenticated()) {
      startPolling();
    }

    // Optional: wire a click on common bell icon to toggle dropdown
    $(document).on('click', '[data-toggle="notif-dropdown"]', function (e) {
      e.preventDefault();
      const $root = $(this).closest('.nav-item');
      // Toggle class 'open' on wrapper dropdown element
      $root.toggleClass('open');
      // If opening, and no items loaded, fetch quickly
      if ($root.hasClass('open')) {
        poll();
      }
    });

    // Close dropdown when clicking outside
    $(document).on('click', function (e) {
      if ($(e.target).closest('.notif-dropdown, [data-toggle="notif-dropdown"]').length === 0) {
        $('.notif-dropdown').removeClass('open');
      }
    });
  });

})(window.jQuery, window.GoLankaApi, window.Swal);
