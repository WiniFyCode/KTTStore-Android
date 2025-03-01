const Promotion = require('../models/Promotion');
const Product = require('../models/Product');

class PromotionController {
    // Helper function để lấy ID tiếp theo
    static async getNextPromotionID() {
        try {
            const lastPromotion = await Promotion.findOne({}, { promotionID: 1 })
                .sort({ promotionID: -1 });

            if (!lastPromotion) {
                return "1"; // Bắt đầu từ 1 nếu chưa có promotion nào
            }

            // Tăng số lên 1
            const nextID = parseInt(lastPromotion.promotionID) + 1;
            return nextID.toString();
        } catch (error) {
            console.error('Error getting next promotion ID:', error);
            throw error;
        }
    }

    // Helper function để kiểm tra khung giờ Flash Sale
    static isInFlashSaleTimeRange() {
        const now = new Date();
        const currentHour = now.getHours();
        return (currentHour >= 12 && currentHour < 14) ||
            (currentHour >= 20 && currentHour < 22);
    }

    // Lấy chi tiết một promotion
    async getPromotionById(req, res) {
        try {
            const { promotionID } = req.params;

            const promotion = await Promotion.findOne({ promotionID })
                .populate('products', 'productID name price')
                .populate('createdBy', 'userID fullName');

            if (!promotion) {
                return res.status(404).json({
                    success: false,
                    message: 'Không tìm thấy promotion'
                });
            }

            return res.status(200).json({
                success: true,
                data: promotion
            });
        } catch (error) {
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message
            });
        }
    }

    // Lấy các promotion đang active
    async getActivePromotions(req, res) {
        try {
            const currentDate = new Date();

            const activePromotions = await Promotion.find({
                status: 'active',
                startDate: { $lte: currentDate },
                endDate: { $gte: currentDate }
            })
                .populate('products', 'productID name price')
                .populate('createdBy', 'userID fullName email');

            return res.status(200).json({
                success: true,
                data: activePromotions
            });
        } catch (error) {
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message
            });
        }
    }

    // Lấy promotion áp dụng cho một sản phẩm
    async getPromotionsForProduct(req, res) {
        try {
            const { productId } = req.params;
            const currentDate = new Date();

            const promotions = await Promotion.find({
                status: 'active',
                startDate: { $lte: currentDate },
                endDate: { $gte: currentDate },
                $or: [
                    { products: productId },
                    {
                        categories: {
                            $in: await Product.findById(productId).select('category').then(product => product.category)
                        }
                    }
                ]
            });

            return res.status(200).json({
                success: true,
                data: promotions
            });
        } catch (error) {
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message
            });
        }
    }

    // Lấy Flash Sale đang active
    async getActiveFlashSale(req, res) {
        try {
            // Kiểm tra có trong khung giờ Flash Sale không
            if (!PromotionController.isInFlashSaleTimeRange()) {
                return res.status(200).json({
                    success: true,
                    isActive: false,
                    message: 'Không trong khung giờ Flash Sale',
                    nextSessions: [
                        { start: '12:00', end: '14:00' },
                        { start: '20:00', end: '22:00' }
                    ]
                });
            }

            const currentDate = new Date();

            // Tìm Flash Sale đang active
            const flashSale = await Promotion.findOne({
                type: 'flash-sale',
                status: 'active',
                startDate: { $lte: currentDate },
                endDate: { $gte: currentDate }
            })
                .populate('products', 'productID name price thumbnail stock')
                .populate('createdBy', 'userID fullName');

            if (!flashSale) {
                return res.status(200).json({
                    success: true,
                    isActive: false,
                    message: 'Không có Flash Sale nào đang diễn ra'
                });
            }

            // Tính thời gian kết thúc của khung giờ hiện tại
            const currentHour = currentDate.getHours();
            const endHour = currentHour >= 12 && currentHour < 14 ? 14 : 22;
            const endTime = new Date(currentDate.setHours(endHour, 0, 0, 0));

            return res.status(200).json({
                success: true,
                isActive: true,
                data: {
                    ...flashSale.toObject(),
                    endTime
                }
            });
        } catch (error) {
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message
            });
        }
    }

    // Lấy Flash Sale sắp diễn ra
    async getUpcomingFlashSale(req, res) {
        try {
            const currentDate = new Date();

            // Tìm Flash Sale sắp diễn ra
            const upcomingFlashSale = await Promotion.findOne({
                type: 'flash-sale',
                status: 'active',
                startDate: { $gt: currentDate }
            })
                .sort({ startDate: 1 })
                .populate('products', 'productID name price thumbnail')
                .populate('createdBy', 'userID fullName');

            if (!upcomingFlashSale) {
                return res.status(200).json({
                    success: true,
                    message: 'Không có Flash Sale nào sắp diễn ra'
                });
            }

            return res.status(200).json({
                success: true,
                data: upcomingFlashSale
            });
        } catch (error) {
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message
            });
        }
    }

    // Lấy sản phẩm trong Flash Sale
    async getFlashSaleProducts(req, res) {
        try {
            // Kiểm tra có trong khung giờ Flash Sale không
            if (!PromotionController.isInFlashSaleTimeRange()) {
                return res.status(200).json({
                    success: true,
                    isActive: false,
                    message: 'Không trong khung giờ Flash Sale'
                });
            }

            const currentDate = new Date();

            // Tìm Flash Sale đang active
            const flashSale = await Promotion.findOne({
                type: 'flash-sale',
                status: 'active',
                startDate: { $lte: currentDate },
                endDate: { $gte: currentDate }
            });

            if (!flashSale) {
                return res.status(200).json({
                    success: true,
                    isActive: false,
                    message: 'Không có Flash Sale nào đang diễn ra'
                });
            }

            // Lấy thông tin chi tiết của các sản phẩm
            const products = await Product.find({
                _id: { $in: flashSale.products }
            })
                .select('productID name price thumbnail stock');

            // Thêm thông tin giảm giá vào mỗi sản phẩm
            const productsWithDiscount = products.map(product => {
                const price = parseFloat(product.price.replaceAll('.', ''));
                const discountAmount = (price * flashSale.discountPercent) / 100;
                const discountedPrice = price - discountAmount;

                return {
                    ...product.toObject(),
                    discountedPrice: Math.round(discountedPrice)
                        .toString()
                        .replace(/\B(?=(\d{3})+(?!\d))/g, '.'),
                    discountPercent: flashSale.discountPercent,
                    soldQuantity: Math.floor(Math.random() * 50 + 50), // Giả lập số lượng đã bán
                    totalQuantity: 100 // Giả lập tổng số lượng
                };
            });

            return res.status(200).json({
                success: true,
                isActive: true,
                data: {
                    flashSale,
                    products: productsWithDiscount
                }
            });
        } catch (error) {
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message
            });
        }
    }
    
    //!ADMIN
    // Lấy danh sách tất cả promotion và thống kê
    async getAllPromotions(req, res) {
        try {
            const currentDate = new Date();
            
            // Lấy tất cả promotions
            const promotions = await Promotion.find()
                .populate('products', 'productID name price')
                .populate('createdBy', 'userID fullName email');

            // Tính toán thống kê
            const stats = {
                totalPromotions: promotions.length,
                activePromotions: promotions.filter(promo => 
                    promo.status === 'active' && 
                    new Date(promo.startDate) <= currentDate && 
                    new Date(promo.endDate) >= currentDate
                ).length,
                upcomingPromotions: promotions.filter(promo =>
                    promo.status === 'active' && 
                    new Date(promo.startDate) > currentDate
                ).length,
                endedPromotions: promotions.filter(promo =>
                    new Date(promo.endDate) < currentDate
                ).length
            };

            return res.status(200).json({
                promotions: promotions,
                stats,
            });
        } catch (error) {
            return res.status(500).json({
                message: 'Lỗi server',
                error: error.message
            });
        }
    }

    //!ADMIN
    // Tạo mới promotion
    async createPromotion(req, res) {
        try {
            console.log('Creating promotion with data:', req.body);
            const {
                name,
                description,
                discountPercent,
                startDate,
                endDate,
                status,
                products,
                categories,
                type = 'normal' // Mặc định là normal
            } = req.body;

            // Validate required fields
            if (!name || !description || !discountPercent || !startDate || !endDate) {
                console.error('Missing required fields');
                return res.status(400).json({
                    success: false,
                    message: 'Thiếu thông tin bắt buộc'
                });
            }

            // Kiểm tra ngày bắt đầu và kết thúc
            if (new Date(startDate) >= new Date(endDate)) {
                console.error('Invalid dates:', { startDate, endDate });
                return res.status(400).json({
                    success: false,
                    message: 'Ngày kết thúc phải sau ngày bắt đầu'
                });
            }

            // Lấy ID tiếp theo
            console.log('Getting next promotion ID...');
            const nextID = await PromotionController.getNextPromotionID();
            console.log('Next promotion ID:', nextID);

            // Tạo promotion mới
            const promotion = new Promotion({
                promotionID: nextID,
                name,
                description,
                discountPercent,
                startDate,
                endDate,
                status: status || 'active',
                products: products || [],
                categories: categories || [],
                type,
                createdBy: req.user._id
            });

            console.log('Saving promotion:', promotion);
            await promotion.save();
            console.log('Promotion saved successfully');

            return res.status(201).json({
                success: true,
                message: 'Tạo promotion thành công',
                data: promotion
            });
        } catch (error) {
            console.error('Error in createPromotion:', error);
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message,
                stack: error.stack
            });
        }
    }

    //!ADMIN
     // Cập nhật promotion
     async updatePromotion(req, res) {
        try {
            const { promotionID } = req.params;
            const updateData = req.body;

            // Kiểm tra ngày nếu có cập nhật
            if (updateData.startDate && updateData.endDate) {
                if (new Date(updateData.startDate) >= new Date(updateData.endDate)) {
                    return res.status(400).json({
                        success: false,
                        message: 'Ngày kết thúc phải sau ngày bắt đầu'
                    });
                }
            }

            const promotion = await Promotion.findOneAndUpdate(
                { promotionID },
                updateData,
                { new: true }
            );

            if (!promotion) {
                return res.status(404).json({
                    success: false,
                    message: 'Không tìm thấy promotion'
                });
            }

            return res.status(200).json({
                success: true,
                message: 'Cập nhật promotion thành công',
                data: promotion
            });
        } catch (error) {
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message
            });
        }
    }

    //!ADMIN
    // Xóa promotion
    async deletePromotion(req, res) {
        try {
            const { promotionID } = req.params;

            const promotion = await Promotion.findOneAndDelete({ promotionID });

            if (!promotion) {
                return res.status(404).json({
                    success: false,
                    message: 'Không tìm thấy promotion'
                });
            }

            return res.status(200).json({
                success: true,
                message: 'Xóa promotion thành công'
            });
        } catch (error) {
            return res.status(500).json({
                success: false,
                message: 'Lỗi server',
                error: error.message
            });
        }
    }

    //!ADMIN
    // Thêm hàm xử lý toggle status
    async toggleStatus(req, res) {
        try {
            const { id } = req.params;

            // Kiểm tra promotion có tồn tại không
            const promotion = await Promotion.findOne({ promotionID: id });
            if (!promotion) {
                return res.status(404).json({ message: 'Không tìm thấy khuyến mãi' });
            }

            // Tự động chuyển đổi trạng thái
            promotion.status = promotion.status === 'active' ? 'inactive' : 'active';
            await promotion.save();

            res.status(200).json({ 
                success: true,
                message: `Đã ${promotion.status === 'active' ? 'kích hoạt' : 'vô hiệu hóa'} khuyến mãi`,
                promotion 
            });
        } catch (error) {
            console.error('Error toggling promotion status:', error);
            res.status(500).json({ 
                success: false,
                message: 'Lỗi khi cập nhật trạng thái khuyến mãi',
                error: error.message 
            });
        }
    };
}

module.exports = new PromotionController(); 