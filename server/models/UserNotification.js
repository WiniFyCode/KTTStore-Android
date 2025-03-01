const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// Định nghĩa schema cho UserNotification
const userNotificationSchema = new Schema({
    userNotificationID: {
        type: Number,
        required: true,
        unique: true
    },
    notificationID: {
        type: Number,
        required: true,
        ref: 'Notification' // Reference đến model Notification
    },
    userID: {
        type: Number,
        required: true,
        ref: 'User' // Reference đến model User
    },
    isRead: {
        type: Boolean,
        default: false
    },
    readAt: {
        type: Date,
        default: null,
        validate: {
            validator: function(value) {
                // readAt chỉ được set khi isRead là true
                return !value || this.isRead;
            },
            message: 'readAt chỉ được set khi thông báo đã được đọc'
        }
    }
}, {
    timestamps: true // Tự động thêm createdAt và updatedAt
});

// Thêm index cho các trường thường được tìm kiếm
userNotificationSchema.index({ userID: 1, notificationID: 1 }, { unique: true }); // Mỗi user chỉ có 1 bản ghi cho mỗi thông báo
userNotificationSchema.index({ userID: 1, isRead: 1 }); // Tìm kiếm thông báo chưa đọc của user
userNotificationSchema.index({ readAt: 1 });

// Middleware để kiểm tra user và notification tồn tại
userNotificationSchema.pre('save', async function(next) {
    try {
        const User = mongoose.model('User');
        const Notification = mongoose.model('Notification');

        const [user, notification] = await Promise.all([
            User.findOne({ userID: this.userID }),
            Notification.findOne({ notificationID: this.notificationID })
        ]);

        if (!user) {
            throw new Error('Người dùng không tồn tại');
        }

        if (!notification) {
            throw new Error('Thông báo không tồn tại');
        }

        // Kiểm tra thời hạn của thông báo
        if (notification.expiresAt < new Date()) {
            throw new Error('Thông báo đã hết hạn');
        }

        next();
    } catch (error) {
        next(error);
    }
});

// Virtual field để lấy thông tin thông báo
userNotificationSchema.virtual('notificationInfo', {
    ref: 'Notification',    // Model tham chiếu
    localField: 'notificationID', // Thuộc tính của model hiện tại
    foreignField: 'notificationID', // Thuộc tính của model tham chiếu
    justOne: true          // Chỉ lấy 1 kết quả
});

// Virtual field để lấy thông tin người dùng
userNotificationSchema.virtual('userInfo', {
    ref: 'User',          // Model tham chiếu
    localField: 'userID', // Thuộc tính của model hiện tại
    foreignField: 'userID', // Thuộc tính của model tham chiếu
    justOne: true        // Chỉ lấy 1 kết quả
});

// Method để đánh dấu đã đọc thông báo
userNotificationSchema.methods.markAsRead = async function() {
    if (!this.isRead) {
        this.isRead = true;
        this.readAt = new Date();
        
        // Cập nhật số lượt đọc trong bảng Notification
        const Notification = mongoose.model('Notification');
        await Notification.updateOne(
            { notificationID: this.notificationID },
            { $inc: { readCount: 1 } }
        );

        return this.save();
    }
    return this;
};

// Static method để lấy số lượng thông báo chưa đọc của user
userNotificationSchema.statics.getUnreadCount = async function(userID) {
    return this.countDocuments({ userID, isRead: false });
};

// Tạo model từ schema
const UserNotification = mongoose.model('UserNotification', userNotificationSchema, 'user_notifications');

module.exports = UserNotification;
