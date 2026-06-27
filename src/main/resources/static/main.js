// Handles UI interactions like sidebar toggling, button animations, etc.

document.addEventListener("DOMContentLoaded", () => {
    console.log("Main.js loaded ✅");



    // Example: highlight active link in sidebar
    const links = document.querySelectorAll(".nav-link");
    links.forEach(link => {
        if (link.href === window.location.href) {
            link.classList.add("active");
        }
    });

    // Populate header user profile dynamically
    fetch('/api/users/current')
        .then(r => r.ok ? r.json() : null)
        .then(u => {
            if(!u) return;
            const rolesArr = u.roles || [];
            const roles = rolesArr.join(', ');
            setText('hdrFullName', u.fullName || u.username);
            setText('hdrRoles', roles);
            setText('hdrEmail', u.email || '');
            
            setText('hdrWelcomeMsg', 'Welcome');
            setText('hdrWelcomeMsgMobile', 'Welcome');

            applyRoleVisibility(rolesArr);
            updateProfileImage(u);
        }).catch(() => {});

    function setText(id, value){
        const el = document.getElementById(id);
        if(el) el.textContent = value;
    }
    
    function updateProfileImage(user) {
        const profileContainer = document.getElementById('hdrProfileImageContainer');
        const profileContainerMobile = document.getElementById('hdrProfileImageContainerMobile');
        const initialsEl = document.getElementById('hdrInitials');
        const initialsElMobile = document.getElementById('hdrInitialsMobile');
        
        // Generate initials from full name
        const fullName = user.fullName || user.username || 'User';
        const initials = fullName.split(' ')
            .map(n => n[0])
            .join('')
            .toUpperCase()
            .substring(0, 2);
        
        if (user.profileImagePath && user.profileImagePath.trim() !== '') {
            // User has uploaded a profile image
            const imgUrl = user.profileImagePath;
            
            // Desktop profile image
            if (profileContainer) {
                profileContainer.style.backgroundImage = `url('${imgUrl}')`;
                profileContainer.style.backgroundSize = 'cover';
                profileContainer.style.backgroundPosition = 'center';
                profileContainer.classList.remove('bg-danger');
                if (initialsEl) initialsEl.style.display = 'none';
            }
            
            // Mobile profile image
            if (profileContainerMobile) {
                profileContainerMobile.style.backgroundImage = `url('${imgUrl}')`;
                profileContainerMobile.style.backgroundSize = 'cover';
                profileContainerMobile.style.backgroundPosition = 'center';
                profileContainerMobile.classList.remove('bg-danger');
                if (initialsElMobile) initialsElMobile.style.display = 'none';
            }
        } else {
            // No profile image - show initials
            if (initialsEl) initialsEl.textContent = initials;
            if (initialsElMobile) initialsElMobile.textContent = initials;
        }
    }

    function applyRoleVisibility(userRoles){
        const set = new Set(userRoles);
        document.querySelectorAll('[data-visible-for]')
            .forEach(el => {
                const allowed = (el.getAttribute('data-visible-for') || '')
                    .split(',')
                    .map(s => s.trim())
                    .filter(Boolean);
                const show = allowed.length === 0 || allowed.some(r => set.has(r));
                if (show) {
                    el.style.setProperty('display', 'block', 'important');
                } else {
                    el.style.setProperty('display', 'none', 'important');
                }
            });
    }

    // Load notifications and setup WebSocket
    loadNotifications();
    setupNotificationWebSocket();
});

// Notification functions
async function loadNotifications() {
    try {
        const res = await fetch('/api/notifications/unread');
        if (!res.ok) return;
        const notifications = await res.json();
        
        updateNotificationBadge(notifications.length);
        updateNotificationDropdown(notifications);
    } catch (err) {
        console.error('Failed to load notifications:', err);
    }
}

function updateNotificationBadge(count) {
    const badge = document.getElementById('notifBadge');
    if (badge) {
        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }
}

function updateNotificationDropdown(notifications) {
    const dropdown = document.getElementById('notifDropdown');
    if (!dropdown) return;

    if (notifications.length === 0) {
        dropdown.innerHTML = '<li class="px-3 py-2 text-muted text-center small">No new notifications</li>';
        return;
    }

    dropdown.innerHTML = notifications.slice(0, 5).map(n => `
        <li>
            <a class="dropdown-item py-2 border-bottom" href="/notifications" onclick="markAsRead(${n.id})">
                <div class="d-flex align-items-start">
                    <i class="bi bi-bell-fill text-success me-2 mt-1"></i>
                    <div class="flex-grow-1">
                        <div class="small fw-semibold">${escapeHtml(n.message)}</div>
                        <small class="text-muted">${formatTime(n.sentAt)}</small>
                    </div>
                </div>
            </a>
        </li>
    `).join('');
}

async function markAsRead(id) {
    try {
        await fetch(`/api/notifications/${id}/read`, { method: 'PUT' });
        loadNotifications();
    } catch (err) {
        console.error('Failed to mark as read:', err);
    }
}

function setupNotificationWebSocket() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        console.warn('WebSocket libraries not loaded');
        return;
    }

    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);
    
    stompClient.connect({}, () => {
        stompClient.subscribe('/topic/notifications', (message) => {
            const notification = JSON.parse(message.body);
            console.log('New notification:', notification);
            loadNotifications();
            showToast(notification.message);
        });
    }, (error) => {
        console.error('WebSocket connection error:', error);
    });
}

function showToast(message) {
    // Simple toast notification
    const toast = document.createElement('div');
    toast.className = 'position-fixed top-0 end-0 p-3';
    toast.style.zIndex = '9999';
    toast.innerHTML = `
        <div class="toast show" role="alert">
            <div class="toast-header bg-success text-white">
                <i class="bi bi-bell-fill me-2"></i>
                <strong class="me-auto">New Notification</strong>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${escapeHtml(message)}
            </div>
        </div>
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 5000);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d ago`;
}
