const express = require('express');
const router = express.Router();
const CouponController = require('../controllers/CouponController');
const { authenticateToken, isAdmin } = require('../middlewares/auth.middleware');

//!ADMIN
router.get('/admin/coupons', authenticateToken, isAdmin, CouponController.getCouponsChoADMIN); // Lấy tất cả thông tin mã giảm giá
router.post('/admin/coupons/create', authenticateToken, isAdmin, CouponController.createCoupon); // Tạo mã giảm giá mới
router.put('/admin/coupons/update/:id', authenticateToken, isAdmin, CouponController.updateCoupon); // Cập nhật mã giảm giá
router.delete('/admin/coupons/delete/:id', authenticateToken, isAdmin, CouponController.deleteCoupon); // Xóa mã giảm giá
router.patch('/admin/coupons/toggle/:id', authenticateToken, isAdmin, CouponController.toggleCouponStatus); // Vô hiệu hóa/Kích hoạt mã giảm giá
//!ADMIN CALL THÊM /api/categories

// Routes cho người dùng
router.get('/available', authenticateToken, CouponController.getAvailableCoupons); // Lấy danh sách mã có thể sử dụng
router.post('/apply', authenticateToken, CouponController.applyCoupon); // Áp dụng mã giảm giá
router.get('/history', authenticateToken, CouponController.getCouponHistory); // Lấy lịch sử sử dụng mã

module.exports = router;
