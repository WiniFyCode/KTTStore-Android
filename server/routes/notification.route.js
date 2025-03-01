const express = require('express');
const router = express.Router();
const NotificationController = require('../controllers/NotificationController');
const { authenticateToken, isAdmin } = require('../middlewares/auth.middleware');

// Tất cả routes đều yêu cầu đăng nhập
router.use(authenticateToken);

// Routes cho user (đặt trước routes admin để tránh conflict)
router.get('/', NotificationController.getUserNotifications); // Lấy thông báo của user
router.get('/unread/count', NotificationController.getUnreadCount); // Lấy số lượng thông báo chưa đọc
router.put('/read/:id', NotificationController.markAsRead); // Đánh dấu đã đọc
router.put('/read-all', NotificationController.markAllAsRead); // Đánh dấu tất cả đã đọc

// Routes cho admin (đặt sau routes user để tránh conflict)
router.get('/all', authenticateToken, isAdmin, NotificationController.getAllNotifications); // Lấy tất cả thông báo

//!ADMIN
router.get('/admin/notifications', authenticateToken, isAdmin, NotificationController.getNotficationChoADMIN); // Lấy tất cả thông báo cho admin
router.post('/admin/notifications/create', authenticateToken, isAdmin, NotificationController.createNotification); // Tạo thông báo mới
router.put('/admin/notifications/update/:id', authenticateToken, isAdmin, NotificationController.updateNotification); // Cập nhật thông báo
router.delete('/admin/notifications/delete/:id', authenticateToken, isAdmin, NotificationController.deleteNotification); // Xóa thông báo
//!ADMIN CALL THÊM /api/users

module.exports = router;
