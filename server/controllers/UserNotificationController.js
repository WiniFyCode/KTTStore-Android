const UserNotification = require('../models/UserNotification');
const Notification = require('../models/Notification');

class UserNotificationController {
    // Lấy tất cả thông báo của user
    async getNotifications(req, res) {
        try {
            const userID = req.user.userID;
            const notifications = await UserNotification.find({ userID })
                .populate('notificationID')
                .sort({ createdAt: -1 }); // Sắp xếp theo thời gian tạo mới nhất

            res.json({
                message: 'Lấy danh sách thông báo thành công',
                notifications
            });
        } catch (error) {
            console.error('Error in getNotifications:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy danh sách thông báo',
                error: error.message
            });
        }
    }

    // Đánh dấu thông báo đã đọc
    async markAsRead(req, res) {
        try {
            const userID = req.user.userID;
            const { userNotificationID } = req.params;

            const notification = await UserNotification.findOne({
                userID,
                userNotificationID: parseInt(userNotificationID)
            });

            if (!notification) {
                return res.status(404).json({
                    message: 'Không tìm thấy thông báo'
                });
            }

            await notification.markAsRead();

            res.json({
                message: 'Đánh dấu thông báo đã đọc thành công'
            });
        } catch (error) {
            console.error('Error in markAsRead:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi đánh dấu thông báo đã đọc',
                error: error.message
            });
        }
    }

    // Đánh dấu tất cả thông báo đã đọc
    async markAllAsRead(req, res) {
        try {
            const userID = req.user.userID;

            await UserNotification.updateMany(
                { userID, isRead: false },
                { isRead: true, readAt: new Date() }
            );

            res.json({
                message: 'Đánh dấu tất cả thông báo đã đọc thành công'
            });
        } catch (error) {
            console.error('Error in markAllAsRead:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi đánh dấu tất cả thông báo đã đọc',
                error: error.message
            });
        }
    }

    // Lấy số lượng thông báo chưa đọc
    async getUnreadCount(req, res) {
        try {
            const userID = req.user.userID;
            const count = await UserNotification.getUnreadCount(userID);

            res.json({
                message: 'Lấy số lượng thông báo chưa đọc thành công',
                count
            });
        } catch (error) {
            console.error('Error in getUnreadCount:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy số lượng thông báo chưa đọc',
                error: error.message
            });
        }
    }
}

module.exports = new UserNotificationController();
