const express = require('express');
const router = express.Router();
const promotionController = require('../controllers/PromotionController');
const { authenticateAdmin, authenticateToken } = require('../middlewares/auth.middleware');

//!ADMIN
router.get('/all', authenticateAdmin, promotionController.getAllPromotions);
router.post('/create', authenticateAdmin, promotionController.createPromotion);
router.put('/update/:promotionID', authenticateAdmin, promotionController.updatePromotion);
router.delete('/delete/:promotionID', authenticateAdmin, promotionController.deletePromotion);
router.patch('/toggle-status/:id', authenticateAdmin, promotionController.toggleStatus);
//!ADMIN CALL THÊM /api/categories
//!ADMIN CALL THÊM /api/products

// Routes cho flash sale
router.get('/flash-sale/active', promotionController.getActiveFlashSale);
router.get('/flash-sale/upcoming', promotionController.getUpcomingFlashSale);
router.get('/flash-sale/products', promotionController.getFlashSaleProducts);

// Routes cho cả admin và customer
router.get('/active', promotionController.getActivePromotions);
router.get('/:promotionID', promotionController.getPromotionById);
router.get('/product/:productId', promotionController.getPromotionsForProduct);

module.exports = router; 