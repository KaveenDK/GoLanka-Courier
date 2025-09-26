(function ($, Api, Swal) {
    console.log('customer.js is loading...');

    'use strict';

    if (!$) throw new Error('jQuery required');
    if (!Api) throw new Error('GoLankaApi (script.js) required');

    const POLL_INTERVAL_MS = 10000;
    const DASHBOARD_REFRESH_INTERVAL = 30000;

    // Enhanced toast function with better UX
    function toast(opts) {
        const title = opts.title || opts.text || '';
        const icon = opts.icon || 'info';
        
        if (window.showToast) {
            window.showToast(title, icon, opts.timer || 4000);
        } else {
            Swal.fire({
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: opts.timer || 3000,
                icon,
                title
            });
        }
    }

    function setSubmitting($btn, submitting) {
        if (!$btn || !$btn.length) return;
        if (submitting) {
            $btn.data('orig-text', $btn.html());
            $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin mr-2"></i> Processing...');
        } else {
            $btn.prop('disabled', false).html($btn.data('orig-text') || $btn.html());
        }
    }

    // Enhanced error handling with better user feedback
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
                $field.addClass('is-invalid border-red-300');
                const $err = $(`<div class="field-error text-sm text-red-600 mt-1 flex items-center">
                    <i class="fas fa-exclamation-circle mr-1"></i>${message}
                </div>`);
                $field.after($err);
            } else {
                toast({ title: message, icon: 'error' });
            }
        });
    }

    async function handleError($form, err) {
        console.error('Error details:', err);
        
        if (!err) {
            toast({ title: 'Unknown error occurred', icon: 'error' });
            return;
        }

        if (err.body && Array.isArray(err.body.validationErrors) && $form) {
            showValidationErrors($form, err.body.validationErrors);
            return;
        }

        const msg = (err.body && err.body.message) || err.message || 'Request failed';
        toast({ title: msg, icon: 'error', timer: 5000 });
    }

    // Enhanced dashboard loading with better data handling
    async function loadEnhancedDashboard() {
        try {
            showLoadingState();
            
            const user = Api.getUser();
            if (user) {
                updateUserInfo(user);
            }

            // Load dashboard data in parallel for better performance
            const [profileData, parcelsData, statsData] = await Promise.allSettled([
                fetchProfileData(),
                fetchParcelsData(),
                fetchDashboardStats()
            ]);

            if (profileData.status === 'fulfilled') {
                updateProfileUI(profileData.value);
            }

            if (parcelsData.status === 'fulfilled') {
                updateParcelsUI(parcelsData.value);
            }

            if (statsData.status === 'fulfilled') {
                updateStatsUI(statsData.value);
            }

            await loadRecentActivity();
            await loadNotifications();
            
            hideLoadingState();
            
        } catch (e) {
            console.error('Failed to initialize enhanced dashboard', e);
            hideLoadingState();
            toast({ title: 'Failed to load dashboard data', icon: 'error' });
        }
    }

    function showLoadingState() {
        $('#ongoing-loading').removeClass('hidden');
        $('#ongoing-deliveries-container .delivery-item').remove();
    }

    function hideLoadingState() {
        $('#ongoing-loading').addClass('hidden');
    }

    function updateUserInfo(user) {
    const name = user.fullName || user.name || user.sub || 'Customer';
    const email = user.email || '';
    
    $('#customer-name, #customerName').text(name);
    $('#user-name').text(name);
    
    // Set avatar initial
    const initial = name.charAt(0).toUpperCase();
    $('#user-avatar, #profileAvatar').text(initial);
    }

    async function fetchProfileData() {
        try {
            return await Api.apiJson('/api/users/me', { method: 'GET' });
        } catch (err) {
            console.warn('Could not fetch profile', err);
            return null;
        }
    }

    function updateProfileUI(profile) {
        if (!profile) return;
        
        const name = profile.fullName || profile.name || '';
        const email = profile.email || '';
        const phone = profile.phone || '';
        const address = profile.address || profile.addressLine || '';
        
        // Update profile card
        $('#profileName').text(name || 'User');
        $('#profileEmail').text(email || '—');
        $('#profilePhone').text(phone || '—');
        $('#profileAddress').text(address || '—');
        
        // Update avatar
        const initial = (name || email || 'U').charAt(0).toUpperCase();
        $('#profileAvatar').text(initial);
    }

    async function fetchParcelsData() {
        try {
            const url = '/api/parcels?limit=20&sort=createdAt:desc';
            const data = await Api.apiJson(url, { method: 'GET' });
            return data.items || data || [];
        } catch (err) {
            console.error('Failed to fetch parcels', err);
            return [];
        }
    }

    async function fetchDashboardStats() {
        try {
            // Try enhanced dashboard endpoint first
            return await Api.apiJson('/api/dashboard/customer', { method: 'GET' });
        } catch (err) {
            // Fallback to calculating stats from parcels
            console.warn('Dashboard endpoint not available, calculating stats from parcels');
            try {
                const parcels = await fetchParcelsData();
                return calculateStatsFromParcels(parcels);
            } catch (fallbackErr) {
                console.error('Failed to calculate fallback stats', fallbackErr);
                return getDefaultStats();
            }
        }
    }

    function calculateStatsFromParcels(parcels) {
        const stats = {
            completed: 0,
            ongoing: 0,
            pending: 0,
            totalSpent: 0
        };

        parcels.forEach(parcel => {
            const status = (parcel.status || '').toUpperCase();
            switch (status) {
                case 'DELIVERED':
                    stats.completed++;
                    break;
                case 'IN_TRANSIT':
                case 'OUT_FOR_DELIVERY':
                case 'PICKED_UP':
                    stats.ongoing++;
                    break;
                case 'CREATED':
                case 'PENDING':
                    stats.pending++;
                    break;
            }
            
            if (parcel.price || parcel.cost) {
                stats.totalSpent += parseFloat(parcel.price || parcel.cost || 0);
            }
        });

        return { metrics: stats, parcels };
    }

    function getDefaultStats() {
        return {
            metrics: {
                completed: 0,
                ongoing: 0,
                pending: 0,
                totalSpent: 0
            },
            parcels: []
        };
    }

    function updateStatsUI(data) {
    const metrics = data?.metrics || data || {};
    
    $('#completed-deliveries, #metricDelivered').text(metrics.completed || metrics.delivered || 0);
    $('#ongoing-count, #metricActive, #ongoing-deliveries-count').text(metrics.ongoing || metrics.active || 0);
    $('#pending-count, #metricPending').text(metrics.pending || 0);
    $('#total-spent, #metricSpent').text(formatCurrency(metrics.totalSpent || metrics.spent || 0));
    
    // Update last updated time
    $('#last-updated, #metricUpdatedActive').text(new Date().toLocaleTimeString());
}

    function updateParcelsUI(parcels) {
        updateOngoingDeliveries(parcels);
        updateRecentDeliveries(parcels);
    }

    function updateOngoingDeliveries(parcels) {
        const $container = $('#ongoing-deliveries-container');
        $container.find('.delivery-item').remove();

        const ongoingParcels = parcels.filter(p => {
            const status = (p.status || '').toUpperCase();
            return ['IN_TRANSIT', 'OUT_FOR_DELIVERY', 'PICKED_UP'].includes(status);
        });

        if (ongoingParcels.length === 0) {
            $container.append(`
                <div class="text-center py-8 text-gray-500 dark:text-gray-400">
                    <i class="fas fa-shipping-fast text-4xl mb-4 opacity-50"></i>
                    <p>No ongoing deliveries</p>
                    <p class="text-sm">Create a new shipment to get started!</p>
                </div>
            `);
            return;
        }

        ongoingParcels.forEach(parcel => {
            const deliveryElement = createDeliveryElement(parcel);
            $container.append(deliveryElement);
        });
    }

    function createDeliveryElement(parcel) {
        const progress = calculateProgress(parcel.status);
        const statusColor = getStatusColor(parcel.status);
        const progressColor = getProgressColor(parcel.status);

        return $(`
            <div class="delivery-item border border-gray-200 dark:border-gray-700 rounded-lg p-4 mb-4 transition-all duration-300 hover:shadow-md" data-id="${parcel.id}">
                <div class="flex justify-between items-start mb-3">
                    <div>
                        <h3 class="font-semibold text-gray-900 dark:text-white">
                            ${parcel.trackingNumber || '#' + parcel.id}
                        </h3>
                        <p class="text-sm text-gray-500 dark:text-gray-400">
                            ${parcel.pickupAddress || 'N/A'} → ${parcel.deliveryAddress || 'N/A'}
                        </p>
                        <p class="text-xs text-gray-400 mt-1">
                            Recipient: ${parcel.recipientFullName || parcel.recipientName || 'N/A'}
                        </p>
                    </div>
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusColor}">
                        ${formatStatus(parcel.status)}
                    </span>
                </div>
                
                <div class="mb-4">
                    <div class="flex justify-between text-sm mb-2">
                        <span class="text-gray-600 dark:text-gray-300">Delivery Progress</span>
                        <span class="font-medium">${progress}%</span>
                    </div>
                    <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                        <div class="progress-bar ${progressColor} h-2 rounded-full transition-all duration-1000" 
                             style="width: ${progress}%"></div>
                    </div>
                </div>

                <div class="flex space-x-2">
                    <button class="flex-1 btn btn-primary btn-sm track-parcel-btn" data-id="${parcel.id}">
                        <i class="fas fa-map-marker-alt mr-2"></i>Track
                    </button>
                    <button class="btn btn-secondary btn-sm contact-support-btn" data-id="${parcel.id}">
                        <i class="fas fa-phone"></i>
                    </button>
                    ${parcel.canCancel ? `<button class="btn btn-danger btn-sm cancel-parcel-btn" data-id="${parcel.id}">
                        <i class="fas fa-times"></i>
                    </button>` : ''}
                </div>
            </div>
        `);
    }

    function updateRecentDeliveries(parcels) {
        const $tbody = $('#recent-deliveries-table, #recentTableBody');
        $tbody.empty();

        const recentParcels = parcels
            .filter(p => p.status && p.status.toUpperCase() === 'DELIVERED')
            .slice(0, 5);

        if (recentParcels.length === 0) {
            $tbody.append(`
                <tr>
                    <td colspan="6" class="px-6 py-8 text-center text-gray-500 dark:text-gray-400">
                        <i class="fas fa-history text-2xl mb-2 opacity-50"></i>
                        <p>No recent deliveries</p>
                    </td>
                </tr>
            `);
            return;
        }

        recentParcels.forEach(parcel => {
            const row = $(`
                <tr class="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors duration-200">
                    <td class="px-6 py-4 table-cell">
                        <span class="font-mono text-sm">${parcel.trackingNumber || '#' + parcel.id}</span>
                    </td>
                    <td class="px-6 py-4 table-cell">
                        <span class="text-sm">${parcel.recipientFullName || parcel.recipientName || 'N/A'}</span>
                    </td>
                    <td class="px-6 py-4 table-cell">
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(parcel.status)}">
                            ${formatStatus(parcel.status)}
                        </span>
                    </td>
                    <td class="px-6 py-4 table-cell">
                        <span class="text-sm text-gray-500">${formatDate(parcel.createdAt)}</span>
                    </td>
                    <td class="px-6 py-4 table-cell">
                        <span class="text-sm">${parcel.serviceType || 'Standard'}</span>
                    </td>
                    <td class="px-6 py-4 table-cell text-right">
                        <div class="flex items-center justify-end space-x-2">
                            <button class="btn btn-xs btn-secondary view-parcel-btn" data-id="${parcel.id}">
                                <i class="fas fa-eye"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `);
            $tbody.append(row);
        });
    }

    async function loadRecentActivity() {
        try {
            const activities = await Api.apiJson('/api/customer/activity?limit=5', { method: 'GET' });
            updateRecentActivity(activities);
        } catch (err) {
            console.warn('Could not load recent activity', err);
            // Generate mock activity for better UX
            generateMockActivity();
        }
    }

    function generateMockActivity() {
        const mockActivities = [
            {
                type: 'delivery',
                title: 'Parcel delivered successfully',
                description: 'Your parcel has been delivered to the recipient',
                timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000) // 2 hours ago
            },
            {
                type: 'update',
                title: 'Parcel out for delivery',
                description: 'Your parcel is now out for delivery',
                timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000) // 4 hours ago
            },
            {
                type: 'info',
                title: 'New shipment created',
                description: 'You created a new shipment',
                timestamp: new Date(Date.now() - 24 * 60 * 60 * 1000) // 1 day ago
            }
        ];
        updateRecentActivity(mockActivities);
    }

    function updateRecentActivity(activities) {
        const $container = $('#recent-activity-container');
        $container.empty();

        if (!activities || activities.length === 0) {
            $container.append(`
                <div class="text-center py-4 text-gray-500 dark:text-gray-400">
                    <i class="fas fa-bell-slash text-xl mb-2 opacity-50"></i>
                    <p class="text-sm">No recent activity</p>
                </div>
            `);
            return;
        }

        activities.forEach(activity => {
            const element = $(`
                <div class="activity-item">
                    <div class="w-8 h-8 rounded-full ${getActivityColor(activity.type)} flex items-center justify-center">
                        <i class="fas ${getActivityIcon(activity.type)} text-xs"></i>
                    </div>
                    <div>
                        <p class="text-sm font-medium">${activity.title || 'Activity'}</p>
                        <p class="text-xs text-gray-500 dark:text-gray-400">${activity.description || ''}</p>
                        <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">${formatTimeAgo(activity.timestamp)}</p>
                    </div>
                </div>
            `);
            $container.append(element);
        });
    }

    async function loadNotifications() {
        try {
            const notifications = await Api.apiJson('/api/notifications?limit=10', { method: 'GET' });
            updateNotifications(notifications);
        } catch (err) {
            console.warn('Could not load notifications', err);
            generateMockNotifications();
        }
    }

    function generateMockNotifications() {
        const mockNotifications = [
            {
                type: 'delivery',
                title: 'Parcel delivered successfully',
                message: 'Your parcel #GL12345 has been delivered',
                timestamp: new Date(Date.now() - 2 * 60 * 1000), // 2 minutes ago
                read: false
            },
            {
                type: 'info',
                title: 'Parcel is out for delivery',
                message: 'Your parcel #GL67890 is out for delivery',
                timestamp: new Date(Date.now() - 60 * 60 * 1000), // 1 hour ago
                read: false
            },
            {
                type: 'warning',
                title: 'Delivery delay',
                message: 'Your parcel #GL24680 may be delayed due to weather',
                timestamp: new Date(Date.now() - 5 * 60 * 60 * 1000), // 5 hours ago
                read: true
            }
        ];
        updateNotifications(mockNotifications);
    }

    function updateNotifications(notifications) {
        const $list = $('#notifications-list, #notifList');
        const $count = $('#notification-count');
        
        $list.empty();

        if (!notifications || notifications.length === 0) {
            $list.append(`
                <div class="notification-item px-4 py-8 text-center text-gray-500 dark:text-gray-400">
                    <i class="fas fa-bell-slash text-2xl mb-2 opacity-50"></i>
                    <p class="text-sm">No notifications</p>
                </div>
            `);
            $count.addClass('hidden');
            return;
        }

        const unreadCount = notifications.filter(n => !n.read).length;
        if (unreadCount > 0) {
            $count.text(unreadCount).removeClass('hidden');
        } else {
            $count.addClass('hidden');
        }

        notifications.forEach(notification => {
            const element = $(`
                <a href="#" class="block px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700 border-b border-gray-100 dark:border-gray-700 ${!notification.read ? 'bg-blue-50 dark:bg-blue-900/20' : ''}">
                    <div class="flex space-x-3">
                        <div class="flex-shrink-0 w-8 h-8 rounded-full ${getNotificationColor(notification.type)} flex items-center justify-center">
                            <i class="fas ${getNotificationIcon(notification.type)} text-xs"></i>
                        </div>
                        <div class="flex-1 min-w-0">
                            <p class="text-sm font-medium ${!notification.read ? 'text-gray-900 dark:text-white' : 'text-gray-700 dark:text-gray-300'}">
                                ${notification.title || 'Notification'}
                            </p>
                            <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
                                ${notification.message || notification.description || ''}
                            </p>
                            <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">
                                ${formatTimeAgo(notification.timestamp || notification.createdAt)}
                            </p>
                        </div>
                        ${!notification.read ? '<div class="w-2 h-2 bg-blue-500 rounded-full"></div>' : ''}
                    </div>
                </a>
            `);
            $list.append(element);
        });
    }

    // Utility functions
    function calculateProgress(status) {
        const statusProgress = {
            'CREATED': 10,
            'PENDING': 10,
            'PICKED_UP': 25,
            'IN_TRANSIT': 60,
            'OUT_FOR_DELIVERY': 85,
            'DELIVERED': 100,
            'CANCELLED': 0
        };
        return statusProgress[(status || '').toUpperCase()] || 0;
    }

    function getStatusColor(status) {
        const s = (status || '').toUpperCase();
        const colors = {
            'CREATED': 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300',
            'PENDING': 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300',
            'PICKED_UP': 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
            'IN_TRANSIT': 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
            'OUT_FOR_DELIVERY': 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300',
            'DELIVERED': 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300',
            'CANCELLED': 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300'
        };
        return colors[s] || 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }

    function getProgressColor(status) {
        const s = (status || '').toUpperCase();
        const colors = {
            'CREATED': 'bg-yellow-500',
            'PENDING': 'bg-yellow-500',
            'PICKED_UP': 'bg-blue-500',
            'IN_TRANSIT': 'bg-blue-500',
            'OUT_FOR_DELIVERY': 'bg-orange-500',
            'DELIVERED': 'bg-green-500',
            'CANCELLED': 'bg-red-500'
        };
        return colors[s] || 'bg-gray-500';
    }

    function getActivityColor(type) {
        const colors = {
            'delivery': 'bg-green-100 dark:bg-green-900/30',
            'update': 'bg-blue-100 dark:bg-blue-900/30',
            'info': 'bg-blue-100 dark:bg-blue-900/30',
            'warning': 'bg-yellow-100 dark:bg-yellow-900/30',
            'error': 'bg-red-100 dark:bg-red-900/30'
        };
        return colors[type] || 'bg-gray-100 dark:bg-gray-900/30';
    }

    function getActivityIcon(type) {
        const icons = {
            'delivery': 'fa-check',
            'update': 'fa-info-circle',
            'info': 'fa-info-circle',
            'warning': 'fa-exclamation-triangle',
            'error': 'fa-exclamation-circle'
        };
        return icons[type] || 'fa-info-circle';
    }

    function getNotificationColor(type) {
        const colors = {
            'success': 'bg-green-100 dark:bg-green-900/30',
            'info': 'bg-blue-100 dark:bg-blue-900/30',
            'warning': 'bg-yellow-100 dark:bg-yellow-900/30',
            'error': 'bg-red-100 dark:bg-red-900/30',
            'delivery': 'bg-green-100 dark:bg-green-900/30'
        };
        return colors[type] || 'bg-blue-100 dark:bg-blue-900/30';
    }

    function getNotificationIcon(type) {
        const icons = {
            'success': 'fa-check-circle',
            'info': 'fa-info-circle',
            'warning': 'fa-exclamation-triangle',
            'error': 'fa-exclamation-circle',
            'delivery': 'fa-shipping-fast'
        };
        return icons[type] || 'fa-bell';
    }

    function formatStatus(status) {
        return (status || 'UNKNOWN').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
    }

    function formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', { 
            month: 'short', 
            day: 'numeric', 
            year: 'numeric' 
        });
    }

    function formatTimeAgo(dateString) {
        if (!dateString) return 'Unknown';
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHrs = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHrs / 24);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins} min ago`;
        if (diffHrs < 24) return `${diffHrs} hr ago`;
        if (diffDays === 1) return 'Yesterday';
        return `${diffDays} days ago`;
    }

    function formatCurrency(amount) {
        return new Intl.NumberFormat('en-LK', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        }).format(amount || 0);
    }

    // Enhanced parcel details modal
    async function openParcelDetails(parcelId) {
        try {
            const parcel = await Api.apiJson(`/api/parcels/${parcelId}`, { method: 'GET' });
            
            const content = `
                <div class="space-y-6">
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="text-sm font-medium text-gray-500 dark:text-gray-400">Tracking Number</label>
                            <p class="font-mono text-lg font-semibold">${parcel.trackingNumber || '#' + parcel.id}</p>
                        </div>
                        <div>
                            <label class="text-sm font-medium text-gray-500 dark:text-gray-400">Status</label>
                            <p><span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(parcel.status)}">${formatStatus(parcel.status)}</span></p>
                        </div>
                        <div>
                            <label class="text-sm font-medium text-gray-500 dark:text-gray-400">Recipient</label>
                            <p class="font-medium">${parcel.recipientFullName || parcel.recipientName || 'N/A'}</p>
                        </div>
                        <div>
                            <label class="text-sm font-medium text-gray-500 dark:text-gray-400">Weight</label>
                            <p class="font-medium">${parcel.weightKg ? parcel.weightKg + ' kg' : 'N/A'}</p>
                        </div>
                    </div>
                    
                    <div class="space-y-3">
                        <div>
                            <label class="text-sm font-medium text-gray-500 dark:text-gray-400">Pickup Address</label>
                            <p class="text-sm">${parcel.pickupAddress || 'N/A'}</p>
                        </div>
                        <div>
                            <label class="text-sm font-medium text-gray-500 dark:text-gray-400">Delivery Address</label>
                            <p class="text-sm">${parcel.deliveryAddress || 'N/A'}</p>
                        </div>
                    </div>

                    <div class="border-t pt-4">
                        <h4 class="text-lg font-semibold mb-3">Delivery Progress</h4>
                        <div class="space-y-3">
                            ${generateTrackingSteps(parcel.status)}
                        </div>
                    </div>
                </div>
            `;

            await Swal.fire({
                title: `Parcel Details`,
                html: content,
                width: '600px',
                showConfirmButton: false,
                showCloseButton: true,
                customClass: {
                    container: 'parcel-modal'
                }
            });

        } catch (err) {
            console.error('Failed to fetch parcel details', err);
            toast({ title: 'Failed to load parcel details', icon: 'error' });
        }
    }

    function generateTrackingSteps(currentStatus) {
        const steps = [
            { status: 'CREATED', label: 'Order Created', icon: 'fa-plus-circle' },
            { status: 'PICKED_UP', label: 'Picked Up', icon: 'fa-truck' },
            { status: 'IN_TRANSIT', label: 'In Transit', icon: 'fa-road' },
            { status: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', icon: 'fa-shipping-fast' },
            { status: 'DELIVERED', label: 'Delivered', icon: 'fa-check-circle' }
        ];

        const currentIndex = steps.findIndex(step => step.status === currentStatus);

        return steps.map((step, index) => {
            const isCompleted = index <= currentIndex;
            const isCurrent = index === currentIndex;
            
            return `
                <div class="flex items-center space-x-3 ${isCompleted ? 'text-green-600' : 'text-gray-400'}">
                    <div class="flex-shrink-0 w-8 h-8 rounded-full ${isCompleted ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'} flex items-center justify-center">
                        <i class="fas ${step.icon} text-sm"></i>
                    </div>
                    <div class="flex-1">
                        <p class="font-medium ${isCurrent ? 'text-primary' : ''}">${step.label}</p>
                        ${isCurrent ? '<p class="text-xs text-gray-500">Current status</p>' : ''}
                    </div>
                    ${isCompleted ? '<i class="fas fa-check text-green-500"></i>' : ''}
                </div>
            `;
        }).join('');
    }

    // Support contact function
    function contactSupport(parcelId) {
        Swal.fire({
            title: 'Contact Support',
            html: `
                <div class="space-y-4">
                    <div class="text-center">
                        <div class="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4">
                            <i class="fas fa-headset text-primary text-2xl"></i>
                        </div>
                        <p class="text-gray-600">Need help with parcel ${parcelId || ''}?</p>
                    </div>
                    <div class="grid grid-cols-3 gap-3">
                        <a href="tel:+94112345678" class="p-3 bg-green-50 hover:bg-green-100 rounded-lg text-center transition-colors">
                            <i class="fas fa-phone text-green-600 text-xl mb-2"></i>
                            <p class="text-xs font-medium">Call</p>
                        </a>
                        <button onclick="startLiveChat()" class="p-3 bg-blue-50 hover:bg-blue-100 rounded-lg text-center transition-colors">
                            <i class="fas fa-comment text-blue-600 text-xl mb-2"></i>
                            <p class="text-xs font-medium">Live Chat</p>
                        </button>
                        <a href="mailto:support@golanka.lk" class="p-3 bg-purple-50 hover:bg-purple-100 rounded-lg text-center transition-colors">
                            <i class="fas fa-envelope text-purple-600 text-xl mb-2"></i>
                            <p class="text-xs font-medium">Email</p>
                        </a>
                    </div>
                </div>
            `,
            showConfirmButton: false,
            showCloseButton: true
        });
    }

    // Cancel parcel function
    async function cancelParcel(parcelId, $btn) {
        const confirm = await Swal.fire({
            title: 'Cancel Parcel?',
            text: 'This action cannot be undone. Are you sure you want to cancel this shipment?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#ef4444',
            confirmButtonText: 'Yes, Cancel It',
            cancelButtonText: 'Keep Parcel'
        });

        if (!confirm.isConfirmed) return;

        setSubmitting($btn, true);
        
        try {
            // Try multiple cancel endpoints for compatibility
            let cancelled = false;
            const endpoints = [
                `/api/parcels/${parcelId}/cancel`,
                `/api/customers/parcels/${parcelId}/cancel`
            ];

            for (const endpoint of endpoints) {
                try {
                    await Api.apiJson(endpoint, { method: 'POST' });
                    cancelled = true;
                    break;
                } catch (err) {
                    if (err.status === 404 || err.status === 405) {
                        continue; // Try next endpoint
                    }
                    throw err; // Rethrow other errors
                }
            }

            if (!cancelled) {
                throw new Error('No cancel endpoint available');
            }

            toast({ title: 'Parcel cancelled successfully', icon: 'success' });
            
            // Refresh dashboard
            await loadEnhancedDashboard();
            
        } catch (err) {
            await handleError(null, err);
        } finally {
            setSubmitting($btn, false);
        }
    }

    // Event handlers
    $(document).on('click', '.track-parcel-btn', function() {
        const id = $(this).data('id');
        if (id) {
            window.location.href = `/pages/customer/track.html?id=${id}`;
        }
    });

    $(document).on('click', '.view-parcel-btn', async function() {
        const id = $(this).data('id');
        if (id) {
            await openParcelDetails(id);
        }
    });

    $(document).on('click', '.cancel-parcel-btn', async function() {
        const id = $(this).data('id');
        const $btn = $(this);
        if (id) {
            await cancelParcel(id, $btn);
        }
    });

    $(document).on('click', '.contact-support-btn', function() {
        const id = $(this).data('id');
        contactSupport(id);
    });

    // Mark all notifications as read
    $(document).on('click', '#mark-all-read', async function() {
        try {
            await Api.apiJson('/api/notifications/mark-all-read', { method: 'POST' });
            $('#notification-count').addClass('hidden');
            toast({ title: 'All notifications marked as read', icon: 'success' });
        } catch (err) {
            console.warn('Could not mark notifications as read', err);
        }
    });

    // Add this to customer.js or dashboard.html
    function loadCustomerName() {
        try {
            console.log('Loading customer name...');
            
            // Method 1: From API
            if (window.GoLankaApi && window.GoLankaApi.isAuthenticated()) {
                const user = window.GoLankaApi.getUser();
                if (user) {
                    updateCustomerDisplay(user);
                    return;
                }
            }
            
            // Method 2: From JWT token
            const token = localStorage.getItem('accessToken');
            if (token) {
                try {
                    const payload = JSON.parse(atob(token.split('.')[1]));
                    updateCustomerDisplay(payload);
                    return;
                } catch (e) {
                    console.error('Error parsing token:', e);
                }
            }
            
            // Method 3: From localStorage backup
            const userName = localStorage.getItem('userName');
            if (userName) {
                updateCustomerDisplay({ name: userName });
            }
            
        } catch (error) {
            console.error('Error loading customer name:', error);
        }
    }

    function updateCustomerDisplay(userData) {
        const customerName = userData.fullName || 
                            userData.name || 
                            userData.firstName ||
                            userData.given_name ||
                            userData.sub || 
                            (userData.email ? userData.email.split('@')[0] : null) ||
                            'Customer';
        
        console.log('Updating customer display with name:', customerName);
        
        // Update all elements
        $('#customer-name').text(customerName);
        $('#user-name').text(customerName);
        
        const initial = customerName.charAt(0).toUpperCase();
        $('#user-avatar').text(initial);
        
        // Store for later use
        localStorage.setItem('userName', customerName);
    }

    // Initialize dashboard when DOM is ready
    $(function() {
        // Only initialize if user is authenticated
        if (!Api.isAuthenticated()) {
            console.log('User not authenticated, redirecting...');
            window.location.href = '/pages/auth/signin.html';
            return;
        }

        // Initialize enhanced dashboard
        loadEnhancedDashboard();

        // Set up auto-refresh
        setInterval(() => {
            console.log('Auto-refreshing dashboard...');
            loadEnhancedDashboard();
        }, DASHBOARD_REFRESH_INTERVAL);
    });

    // Expose functions for backward compatibility
    window.customerDashboard = {
        loadEnhancedDashboard,
        updateStatsUI,
        openParcelDetails,
        contactSupport,
        toast
    };

    // Force update customer name when page loads
    document.addEventListener("DOMContentLoaded", function () {
        
        // Force load customer name
        loadCustomerName();
        
        // Retry after delays in case scripts are still loading
        setTimeout(loadCustomerName, 1000);
        setTimeout(loadCustomerName, 3000);
        setTimeout(loadCustomerName, 5000);
    });

})(window.jQuery, window.GoLankaApi, window.Swal);