class NotificationManager {
    constructor() {
        this.stompClient = null;
        this.notificationCount = 0;
        this.userRole = document.body.dataset.userRole || 'ADMIN'; // Get from your app
        this.connect();
    }
    
    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            
            // Subscribe to notifications
            this.stompClient.subscribe('/topic/notifications', (notification) => {
                this.handleNotification(JSON.parse(notification.body));
            });
            
            // You might want to subscribe to user-specific topics
            this.stompClient.subscribe(`/topic/notifications/${this.userRole}`, (notification) => {
                this.handleNotification(JSON.parse(notification.body));
            });
        });
    }
    
    handleNotification(notification) {
        // Update UI with new notification
        this.showNotification(notification);
        this.updateNotificationCount();
    }
    
    showNotification(notification) {
        // Create and show notification in UI
        const notificationElement = document.createElement('div');
        notificationElement.className = 'notification alert alert-info';
        notificationElement.innerHTML = `
            <strong>New Alert!</strong>
            <p>${notification.message}</p>
            <small>${new Date(notification.sentAt).toLocaleString()}</small>
        `;
        
        const container = document.getElementById('notification-container');
        if (container) {
            container.prepend(notificationElement);
        }
        
        // Play sound
        this.playNotificationSound();
    }
    
    async markAllAsRead() {
        try {
            const response = await fetch('/api/notifications/mark-all-read', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (response.ok) {
                // Update UI
                document.querySelectorAll('.notification').forEach(n => {
                    n.classList.remove('unread');
                });
                this.updateNotificationCount(0);
                
                // Send WebSocket update
                this.stompClient.send("/app/notifications/read-all", {}, JSON.stringify({
                    action: 'mark_all_read'
                }));
            }
        } catch (error) {
            console.error('Error marking all as read:', error);
        }
    }
    
    async clearAll() {
        try {
            const response = await fetch('/api/notifications/clear-all', {
                method: 'DELETE'
            });
            
            if (response.ok) {
                // Clear UI
                const container = document.getElementById('notification-container');
                if (container) {
                    container.innerHTML = '';
                }
                this.updateNotificationCount(0);
            }
        } catch (error) {
            console.error('Error clearing notifications:', error);
        }
    }
    
    updateNotificationCount(count) {
        if (count !== undefined) {
            this.notificationCount = count;
        } else {
            this.notificationCount++;
        }
        
        // Update badge
        const badge = document.getElementById('notification-badge');
        if (badge) {
            badge.textContent = this.notificationCount;
            badge.style.display = this.notificationCount > 0 ? 'inline' : 'none';
        }
    }
    
    playNotificationSound() {
        const audio = new Audio('/notification.mp3'); // Add sound file
        audio.play().catch(e => console.log('Audio play failed:', e));
    }
}

// Initialize when page loads
document.addEventListener('DOMContentLoaded', () => {
    window.notificationManager = new NotificationManager();
});