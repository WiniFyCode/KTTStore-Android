const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

// Số lần đăng nhập sai tối đa cho phép
const MAX_LOGIN_ATTEMPTS = 5;

// Định nghĩa schema cho collection 'users'
const UserSchema = new mongoose.Schema({
    userID: {
        type: Number,
        required: true,
        unique: true
    },
    fullname: {
        type: String,
        required: [true, 'Vui lòng nhập họ và tên'],
        trim: true,
        minlength: [2, 'Họ và tên phải có ít nhất 2 ký tự'],
        maxlength: [50, 'Họ và tên không được vượt quá 50 ký tự']
    },
    gender: {
        type: String,
        enum: {
            values: ['male', 'female'],
            message: 'Giới tính không hợp lệ'
        },
        required: [true, 'Vui lòng chọn giới tính']
    },
    email: {
        type: String,
        required: [true, 'Vui lòng nhập email'],
        unique: true,
        trim: true,
        lowercase: true,
    },
    password: {
        type: String,
        required: [true, 'Vui lòng nhập mật khẩu'],
    },
    phone: {
        type: String,
        required: [true, 'Vui lòng nhập số điện thoại'],
        unique: true,
        validate: {
            validator: function(value) {
                return /^(0[3|5|7|8|9])+([0-9]{8})\b/.test(value);
            },
            message: 'Số điện thoại không hợp lệ'
        }
    },
    isDisabled: {
        type: Boolean,
        default: false
    },
    role: {
        type: String,
        enum: {
            values: ['admin', 'customer'],
            message: 'Vai trò không hợp lệ'
        },
        required: [true, 'Vui lòng chọn vai trò']
    },
    lastLogin: {
        type: Date,
        default: null
    },
    resetPasswordToken: String,
    resetPasswordExpires: Date,
    loginAttempts: {
        type: Number,
        default: 0
    },
    lockUntil: {
        type: Date,
        default: null
    }
}, {
    timestamps: true // Tự động thêm createdAt và updatedAt
});

// Thêm index cho các trường thường được tìm kiếm
UserSchema.index({ fullname: 1 });
UserSchema.index({ role: 1 });
UserSchema.index({ isDisabled: 1 });
UserSchema.index({ createdAt: -1 });

// Middleware để hash mật khẩu trước khi lưu
UserSchema.pre('save', async function(next) {
    try {
        if (!this.isModified('password')) return next();
        
        const salt = await bcrypt.genSalt(10);
        this.password = await bcrypt.hash(this.password, salt);
        next();
    } catch (error) {
        next(error);
    }
});

// Method để so sánh mật khẩu
UserSchema.methods.comparePassword = async function(candidatePassword) {
    return bcrypt.compare(candidatePassword, this.password);
};

// Method để tăng số lần đăng nhập thất bại
UserSchema.methods.incLoginAttempts = async function() {
    // Nếu có lockUntil và vẫn còn trong thời gian khóa
    if (this.lockUntil && this.lockUntil > Date.now()) {
        return this.save();
    }
    
    // Tăng số lần thử
    this.loginAttempts++;
    
    // Khóa tài khoản nếu thử quá MAX_LOGIN_ATTEMPTS lần
    if (this.loginAttempts >= MAX_LOGIN_ATTEMPTS) {
        this.lockUntil = new Date(Date.now() + 30*60*1000); // Khóa 30 phút
    }
    
    return this.save();
};

// Method để reset số lần đăng nhập thất bại
UserSchema.methods.resetLoginAttempts = async function() {
    this.loginAttempts = 0;
    this.lockUntil = null;
    this.lastLogin = new Date();
    return this.save();
};

// Virtual field để lấy danh sách địa chỉ
UserSchema.virtual('addresses', {
    ref: 'Address',
    localField: 'userID',
    foreignField: 'userID',
    justOne: false
});

// Virtual field để lấy danh sách đơn hàng
UserSchema.virtual('orders', {
    ref: 'Order',
    localField: 'userID',
    foreignField: 'userID',
    justOne: false
});

// Virtual field để lấy danh sách mã giảm giá
UserSchema.virtual('coupons', {
    ref: 'UserCoupon',
    localField: 'userID',
    foreignField: 'userID',
    justOne: false
});

// Virtual field để lấy danh sách thông báo
UserSchema.virtual('notifications', {
    ref: 'UserNotification',
    localField: 'userID',
    foreignField: 'userID',
    justOne: false
});

// Tạo model từ schema
const User = mongoose.model('User', UserSchema, 'users');

module.exports = User;
