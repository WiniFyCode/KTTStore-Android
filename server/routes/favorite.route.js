const express = require('express');
const router = express.Router();
const FavoriteController = require('../controllers/FavoriteController');
const { authenticateToken } = require('../middlewares/auth.middleware');


// Middleware xác thực cho tất cả routes
router.use(authenticateToken);
//! Android
/**
 * Routes cho Web và Mobile
 * Base URL: /api/favorites
 */

// Lấy danh sách yêu thích của user đã đăng nhập
router.get('/', FavoriteController.getFavorites);

// Thêm sản phẩm vào danh sách yêu thích
router.post('/add', FavoriteController.addToFavorite);

// Xóa sản phẩm khỏi danh sách yêu thích
router.delete('/:id', FavoriteController.removeFavorite);

// Cập nhật ghi chú cho sản phẩm yêu thích
router.put('/:id', FavoriteController.updateFavorite);

// Thêm route mới để kiểm tra sản phẩm đã yêu thích chưa
router.get('/check/:productID', FavoriteController.checkFavorite);

// Xóa sản phẩm khỏi danh sách yêu thích bằng productID
router.delete('/product/:productID', FavoriteController.removeFavoriteByProductId);

module.exports = router;
