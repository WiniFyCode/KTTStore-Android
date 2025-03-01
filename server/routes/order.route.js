const express = require('express');
const router = express.Router();
const OrderController = require('../controllers/OrderController');
const { authenticateToken, isAdmin } = require('../middlewares/auth.middleware');


router.get('/my-orders', authenticateToken, OrderController.getOrders);
router.get('/my-orders/:id', authenticateToken, OrderController.getOrderById);
router.post('/create', authenticateToken, OrderController.createOrder);
router.put('/:id/cancel', authenticateToken, OrderController.cancelOrder);

//!ADMIN
router.get('/admin/orders', authenticateToken, isAdmin, OrderController.getAllOrdersChoADMIN); // Lấy tất cả đơn hàng
//!Xem chi tiết đơn hàng trong OrderDetailController
router.patch('/admin/orders/update/:id', authenticateToken, isAdmin, OrderController.updateOrderStatus); // Cập nhật trạng thái đơn hàng
router.delete('/admin/orders/delete/:id', authenticateToken, isAdmin, OrderController.deleteOrder); // Xóa đơn hàng

module.exports = router;
