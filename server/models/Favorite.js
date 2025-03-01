const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// Định nghĩa schema cho Favorite
const favoriteSchema = new Schema({
    favoriteID: {
        type: Number,
        unique: true,
        required: true // Thêm required để đảm bảo luôn có giá trị
    },
    userID: {
        type: Number,
        required: true
    },
    productID: {
        type: Number,
        required: true,
        ref: 'Product'
    },
    note: {
        type: String,
        trim: true,
        default: ''
    }
}, {
    timestamps: true, // Tự động thêm createdAt và updatedAt
    collection: 'newfavorites' // Chỉ định rõ tên collection muốn sử dụng
});

// Thêm index cho các trường thường được tìm kiếm
favoriteSchema.index({ userID: 1, productID: 1 }, { unique: true }); // Đảm bảo mỗi user chỉ có thể thêm 1 sản phẩm một lần
favoriteSchema.index({ createdAt: -1 }); // Index cho việc sắp xếp theo thời gian

// Middleware để tự động tạo favoriteID
favoriteSchema.pre('save', async function(next) {
    try {
        if (!this.favoriteID) {
            const lastFavorite = await this.constructor.findOne()
                .sort({ favoriteID: -1 })
                .select('favoriteID');
            this.favoriteID = (lastFavorite?.favoriteID || 0) + 1;
        }
        next();
    } catch (error) {
        next(error);
    }
});

// Virtual populate để lấy thông tin sản phẩm
favoriteSchema.virtual('product', {
    ref: 'Product',
    localField: 'productID',
    foreignField: 'productID',
    justOne: true
});

// Đảm bảo virtuals được bao gồm khi chuyển đổi sang JSON
favoriteSchema.set('toJSON', { virtuals: true });
favoriteSchema.set('toObject', { virtuals: true });

const Favorite = mongoose.model('Favorite', favoriteSchema);

module.exports = Favorite;
