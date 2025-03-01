const Order = require('../models/Order');
const OrderDetail = require('../models/OrderDetail');
const Cart = require('../models/Cart');
const ProductSizeStock = require('../models/ProductSizeStock');
const UserCoupon = require('../models/UserCoupon');
const Product = require('../models/Product');
const ProductColor = require('../models/ProductColor');
const Coupon = require('../models/Coupon');
const Promotion = require('../models/Promotion');
const Category = require('../models/Category');

class OrderController {
    // Lấy danh sách đơn hàng của user
    async getOrders(req, res) {
        try {
            const userID = req.user.userID;
            const { page = 1, limit = 10, status } = req.query;

            // Tạo filter dựa trên status nếu có
            const filter = { userID };
            if (status) {
                filter.orderStatus = status;
            }

            // Lấy danh sách đơn hàng với phân trang
            const orders = await Order.find(filter)
                .sort('-createdAt')
                .skip((page - 1) * limit)
                .limit(limit)
                .populate('orderDetails');

            // Đếm tổng số đơn hàng
            const total = await Order.countDocuments(filter);

            res.json({
                orders,
                total,
                totalPages: Math.ceil(total / limit),
                currentPage: page
            });
        } catch (error) {
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy danh sách đơn hàng',
                error: error.message
            });
        }
    }

    // Lấy chi tiết đơn hàng
    async getOrderById(req, res) {
        try {
            const userID = req.user.userID;
            const { id } = req.params;

            // Lấy order và order details
            const order = await Order.findOne({ orderID: id, userID });
            if (!order) {
                return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
            }

            // Lấy chi tiết đơn hàng
            const orderDetails = await OrderDetail.find({ orderID: id });

            // Biến để tính tổng giá sản phẩm
            let totalProductPrice = 0;

            // Lấy thông tin sản phẩm cho từng SKU
            const detailsWithProducts = await Promise.all(
                orderDetails.map(async (detail) => {
                    const stockItem = await ProductSizeStock.findOne({ SKU: detail.SKU });
                    if (!stockItem) return null;

                    // Parse SKU để lấy productID và colorID
                    const [productID, colorID] = stockItem.SKU.split('_');

                    // Lấy thông tin sản phẩm và màu sắc
                    const [product, color] = await Promise.all([
                        Product.findOne({ productID: Number(productID) }),
                        ProductColor.findOne({
                            productID: Number(productID),
                            colorID: Number(colorID)
                        })
                    ]);

                    // Chuyển đổi giá từ chuỗi "623.000" thành số 623000
                    const priceAsNumber = product ? Number(product.price.replace(/\./g, '')) : 0;

                    // Chỉ lấy các thuộc tính cần thiết
                    const productInfo = product ? {
                        productID: product.productID,
                        name: product.name,
                        price: priceAsNumber, // Sử dụng giá đã chuyển đổi
                        colorName: color ? color.colorName : null,
                        image: color && color.images && color.images.length > 0 ? color.images[0] : null
                    } : null;

                    // Cộng dồn vào tổng giá sản phẩm
                    if (productInfo) {
                        totalProductPrice += productInfo.price * detail.quantity;
                    }

                    return {
                        orderDetailID: detail.orderDetailID,
                        quantity: detail.quantity,
                        SKU: detail.SKU,
                        size: stockItem.size,
                        stock: stockItem.stock,
                        product: productInfo
                    };
                })
            );

            // Lọc bỏ các null values nếu có
            const validDetails = detailsWithProducts.filter(detail => detail !== null);

            res.json({
                ...order.toObject(),
                totalProductPrice, // Thêm totalProductPrice ở cấp cao nhất
                orderDetails: validDetails
            });
        } catch (error) {
            console.error('Error in getOrderById:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy chi tiết đơn hàng',
                error: error.message
            });
        }
    }

    // Xử lý mã giảm giá
    static async validateAndApplyCoupon(userID, userCouponsID, totalPrice, items) {
        if (!userCouponsID) return { finalPaymentPrice: totalPrice, appliedCoupon: null };

        const userCoupon = await UserCoupon.findOne({ 
            userCouponsID,
            userID,
            status: 'active',
            isExpired: false,
            usageLeft: { $gt: 0 }
        }).populate('couponInfo');

        if (!userCoupon) {
            throw new Error('Mã giảm giá không hợp lệ hoặc đã hết lượt sử dụng');
        }

        const coupon = userCoupon.couponInfo;
        const now = new Date();

        // Kiểm tra các điều kiện của coupon
        if (!coupon.isActive) {
            throw new Error('Mã giảm giá không còn hoạt động');
        }

        if (now < new Date(coupon.startDate) || now > new Date(coupon.endDate)) {
            throw new Error('Mã giảm giá không trong thời gian sử dụng');
        }

        if (now > new Date(userCoupon.expiryDate)) {
            throw new Error('Mã giảm giá đã hết hạn');
        }

        // Log thông tin coupon
        console.log('Coupon info:', {
            couponID: coupon.couponID,
            appliedCategories: coupon.appliedCategories
        });

        // Tính tổng số lượng và giá trị các sản phẩm được áp dụng
        let applicableTotal = 0;
        let totalQuantity = 0;
        let nonApplicableTotal = 0;  // Thêm biến này để tính tổng tiền sản phẩm không được áp dụng

        // Lấy danh sách categoryID của các sản phẩm
        const productPromises = items.map(async item => {
            const productID = parseInt(item.SKU.split('_')[0]);
            const product = await Product.findOne({ productID });
            
            console.log('Product found:', {
                SKU: item.SKU,
                productID,
                product: product ? {
                    categoryID: product.categoryID,
                    name: product.name
                } : null
            });

            if (!product) {
                console.log(`Product not found for SKU: ${item.SKU}`);
                return null;
            }

            const categoryID = typeof product.categoryID === 'string' 
                ? parseInt(product.categoryID) 
                : product.categoryID;

            console.log('Processed item:', {
                SKU: item.SKU,
                categoryID,
                price: item.price,
                quantity: item.quantity,
                subtotal: item.price * item.quantity
            });

            return {
                ...item,
                categoryID,
                subtotal: item.price * item.quantity
            };
        });

        const itemsWithCategory = (await Promise.all(productPromises))
            .filter(item => item !== null);

        console.log('Items with category:', itemsWithCategory);

        const appliedCategories = coupon.appliedCategories.map(cat => 
            typeof cat === 'string' ? parseInt(cat) : cat
        );
        console.log('Normalized applied categories:', appliedCategories);

        itemsWithCategory.forEach(item => {
            console.log('Checking item for discount:', {
                SKU: item.SKU,
                categoryID: item.categoryID,
                isIncluded: appliedCategories.includes(item.categoryID),
                subtotal: item.subtotal
            });
            
            if (appliedCategories.includes(item.categoryID)) {
                applicableTotal += item.subtotal;
                totalQuantity += item.quantity;
                console.log('Added to applicable total:', {
                    currentTotal: applicableTotal,
                    currentQuantity: totalQuantity
                });
            } else {
                nonApplicableTotal += item.subtotal;
                console.log('Added to non-applicable total:', {
                    currentNonApplicableTotal: nonApplicableTotal
                });
            }
        });

        console.log('Final calculation:', {
            applicableTotal,
            nonApplicableTotal,
            totalQuantity,
            minOrderValue: coupon.minOrderValue,
            minimumQuantity: coupon.minimumQuantity
        });

        // Kiểm tra điều kiện áp dụng
        if (applicableTotal === 0) {
            throw new Error('Không có sản phẩm nào trong đơn hàng thuộc danh mục được áp dụng mã giảm giá này');
        }

        if (applicableTotal < coupon.minOrderValue) {
            throw new Error(`Tổng giá trị các sản phẩm được áp dụng cần tối thiểu ${coupon.minOrderValue.toLocaleString('vi-VN')}đ để sử dụng mã giảm giá này`);
        }

        if (totalQuantity < coupon.minimumQuantity) {
            throw new Error(`Cần tối thiểu ${coupon.minimumQuantity} sản phẩm thuộc danh mục áp dụng để sử dụng mã giảm giá này`);
        }

        // Tính giảm giá chỉ trên những sản phẩm được áp dụng
        let discountAmount;
        if (coupon.discountType === 'percentage') {
            discountAmount = (applicableTotal * coupon.discountValue) / 100;
            discountAmount = Math.min(discountAmount, coupon.maxDiscountAmount);
        } else { // fixed
            discountAmount = Math.min(coupon.discountValue, applicableTotal);
        }

        // Tính giá cuối cùng: tổng tiền các sản phẩm không được giảm + (tổng tiền các sản phẩm được giảm - số tiền giảm)
        const finalPaymentPrice = nonApplicableTotal + (applicableTotal - discountAmount);

        return {
            finalPaymentPrice,
            appliedCoupon: userCoupon,
            discountAmount,
            applicableTotal,
            nonApplicableTotal
        };
    }

    // Tạo đơn hàng mới từ giỏ hàng
    async createOrder(req, res) {
        try {
            console.log('Bắt đầu tạo đơn hàng mới');
            const userID = req.user.userID;
            console.log('UserID:', userID);
            
            const {
                fullname,
                phone,
                email,
                address,
                note,
                paymentMethod,
                selectedBank,
                bankAccountNumber,
                items,
                userCouponsID
            } = req.body;

            console.log('Thông tin đơn hàng:', {
                fullname,
                phone,
                email,
                address,
                paymentMethod,
                itemsCount: items?.length,
                userCouponsID
            });

            // Validate required fields
            if (!items || !Array.isArray(items) || items.length === 0) {
                console.log('Lỗi: Không có sản phẩm trong đơn hàng');
                return res.status(400).json({ message: 'Vui lòng chọn sản phẩm để thanh toán' });
            }

            // Kiểm tra tồn kho và lấy giá sản phẩm
            let totalPrice = 0;
            const itemsWithPrice = [];

            console.log('Bắt đầu kiểm tra tồn kho và tính giá cho từng sản phẩm');
            for (const item of items) {
                console.log('Kiểm tra sản phẩm SKU:', item.SKU);
                
                // Tách thông tin từ SKU (format: productID_colorID_size_stockID)
                const [productID] = item.SKU.split('_');
                
                const stockItem = await ProductSizeStock.findOne({ SKU: item.SKU });
                console.log('Thông tin tồn kho:', stockItem);
                
                if (!stockItem) {
                    console.log('Lỗi: Không tìm thấy SKU', item.SKU);
                    return res.status(404).json({ 
                        message: `Sản phẩm với SKU ${item.SKU} không tồn tại` 
                    });
                }

                if (stockItem.stock < item.quantity) {
                    const product = await Product.findOne({ productID: Number(productID) });
                    console.log('Lỗi: Không đủ số lượng cho sản phẩm', {
                        productName: product?.name,
                        requested: item.quantity,
                        available: stockItem.stock
                    });
                    
                    return res.status(400).json({ 
                        message: `Sản phẩm ${product ? product.name : item.SKU} không đủ số lượng` 
                    });
                }

                // Lấy thông tin sản phẩm và category
                const product = await Product.findOne({ productID: Number(productID) });
                
                if (!product) {
                    console.log('Lỗi: Không tìm thấy thông tin sản phẩm cho SKU', item.SKU);
                    return res.status(404).json({ 
                        message: `Không tìm thấy thông tin sản phẩm với SKU ${item.SKU}` 
                    });
                }

                // Bước 2: Sử dụng categoryID để lấy category name
                const category = await Category.findOne({ categoryID: product.categoryID });

                if (!category) {
                    console.log('Lỗi: Không tìm thấy thông tin danh mục cho categoryID', product.categoryID);
                    return res.status(404).json({ 
                        message: `Không tìm thấy thông tin danh mục của sản phẩm` 
                    });
                }

                // Bước 3: Kiểm tra khuyến mãi với category name
                console.log('Bắt đầu kiểm tra khuyến mãi cho SKU:', item.SKU);
                const promotion = await Promotion.findOne({
                    $or: [
                        { products: product._id },
                        { categories: category.name }, // Sử dụng category.name
                        { SKUs: item.SKU }
                    ],
                    startDate: { $lte: new Date() },
                    endDate: { $gte: new Date() },
                    status: 'active'
                }).sort({ discountPercent: -1 });

                console.log('Kết quả tìm khuyến mãi:', {
                    SKU: item.SKU,
                    productID: Number(productID),
                    categoryID: product.categoryID,
                    categoryName: category.name,
                    foundPromotion: promotion ? {
                        _id: promotion._id,
                        name: promotion.name,
                        discountPercent: promotion.discountPercent,
                        startDate: promotion.startDate,
                        endDate: promotion.endDate,
                        products: promotion.products,
                        categories: promotion.categories,
                        SKUs: promotion.SKUs
                    } : 'Không tìm thấy khuyến mãi'
                });

                // Tính giá sau khuyến mãi
                const basePrice = parseInt(product.price.replace(/\./g, '')); // Xóa dấu chấm trước khi parse
                let finalPrice = basePrice;
                
                console.log('Thông tin giá gốc:', {
                    SKU: item.SKU,
                    productName: product.name,
                    basePrice: basePrice, // 623000
                    originalPrice: product.price // '623.000' - giá gốc từ DB
                });

                if (promotion) {
                    // Tính toán giá sau khuyến mãi
                    finalPrice = Math.round(basePrice * (1 - promotion.discountPercent / 100));
                    console.log('Chi tiết tính giá khuyến mãi:', {
                        SKU: item.SKU,
                        basePrice: basePrice, // 623000
                        discountPercent: promotion.discountPercent, // 30
                        calculation: `${basePrice} * (1 - ${promotion.discountPercent}/100)`,
                        finalPrice: finalPrice, // 436100
                        totalDiscount: basePrice - finalPrice // 186900
                    });
                } else {
                    console.log('Không có khuyến mãi áp dụng, giữ nguyên giá gốc');
                }

                const itemTotal = finalPrice * item.quantity;
                totalPrice += itemTotal;
                
                console.log('Tổng tiền cho sản phẩm:', {
                    SKU: item.SKU,
                    quantity: item.quantity,
                    pricePerUnit: finalPrice, // 436100
                    itemTotal: itemTotal // 436100 * quantity
                });
                
                itemsWithPrice.push({
                    ...item,
                    price: finalPrice,
                    originalPrice: basePrice,
                    promotion: promotion ? {
                        discountPercent: promotion.discountPercent,
                        endDate: promotion.endDate
                    } : null
                });
            }

            console.log('Tổng giá trị đơn hàng trước khi áp dụng mã giảm giá:', totalPrice);
            let finalPaymentPrice = totalPrice;

            // Xử lý mã giảm giá
            if (userCouponsID) {
                console.log('Bắt đầu xử lý mã giảm giá:', userCouponsID);
                try {
                    const discountResult = await OrderController.validateAndApplyCoupon(
                        userID,
                        userCouponsID,
                        totalPrice,
                        itemsWithPrice
                    );
                    finalPaymentPrice = discountResult.finalPaymentPrice;
                    console.log('Kết quả áp dụng mã giảm giá:', {
                        originalPrice: totalPrice,
                        finalPrice: finalPaymentPrice,
                        discount: totalPrice - finalPaymentPrice
                    });
                } catch (error) {
                    console.log('Lỗi khi áp dụng mã giảm giá:', error.message);
                    return res.status(400).json({ message: error.message });
                }
            }

            // Validate finalPaymentPrice
            if (finalPaymentPrice < 0) {
                return res.status(400).json({
                    message: 'Giá thanh toán không thể âm'
                });
            }

            // Tạo đơn hàng mới
            const lastOrder = await Order.findOne().sort({ orderID: -1 });
            const orderID = lastOrder ? lastOrder.orderID + 1 : 1;

            const order = new Order({
                orderID,
                userID,
                fullname,
                phone,
                email,
                address,
                note: note || '',
                paymentMethod: paymentMethod || 'COD',
                selectedBank: selectedBank || '',
                bankAccountNumber: bankAccountNumber || '',
                totalPrice,
                paymentPrice: finalPaymentPrice,
                userCouponsID,
                orderStatus: 'pending',
                shippingStatus: 'preparing',
                isPayed: paymentMethod === 'COD' ? false : true,
                createdAt: new Date(),
                updatedAt: new Date()
            });

            await order.save();

            // Tạo chi tiết đơn hàng
            try {
                // Lấy orderDetailID cuối cùng
                const lastOrderDetail = await OrderDetail.findOne().sort({ orderDetailID: -1 });
                let nextOrderDetailID = lastOrderDetail ? lastOrderDetail.orderDetailID + 1 : 1;

                // Tạo danh sách chi tiết đơn hàng với giá đã tính
                const orderDetails = itemsWithPrice.map(item => ({
                    orderDetailID: nextOrderDetailID++,
                    orderID,
                    SKU: item.SKU,
                    quantity: item.quantity,
                    price: item.price,
                    createdAt: new Date(),
                    updatedAt: new Date()
                }));

                // Lưu chi tiết đơn hàng
                await OrderDetail.insertMany(orderDetails);

                // Cập nhật số lượng tồn kho
                for (const item of items) {
                    await ProductSizeStock.updateOne(
                        { SKU: item.SKU },
                        { $inc: { stock: -item.quantity } }
                    );
                }

                // Cập nhật trạng thái mã giảm giá nếu có
                if (userCouponsID) {
                    await UserCoupon.updateOne(
                        { userCouponsID },
                        {
                            $inc: { usageLeft: -1 },
                            $set: {
                                isExpired: false,
                                status: 'active'
                            }
                        }
                    );
                }

                // Xóa các sản phẩm đã đặt khỏi giỏ hàng
                const skuList = items.map(item => item.SKU);
                await Cart.deleteMany({
                    userID: userID,
                    SKU: { $in: skuList }
                });

                console.log('Hoàn thành tạo đơn hàng:', {
                    orderID: order.orderID,
                    totalPrice: order.totalPrice,
                    finalPaymentPrice: order.paymentPrice
                });

                res.status(201).json({
                    message: 'Tạo đơn hàng thành công',
                    order
                });

            } catch (error) {
                console.error('Error creating order details:', error);
                // Xóa đơn hàng nếu tạo chi tiết thất bại
                await Order.deleteOne({ orderID });
                res.status(500).json({
                    message: 'Có lỗi xảy ra khi tạo chi tiết đơn hàng',
                    error: error.message
                });
            }
        } catch (error) {
            console.error('Lỗi nghiêm trọng trong createOrder:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi tạo đơn hàng',
                error: error.message
            });
        }
    }

    // Hủy đơn hàng
    async cancelOrder(req, res) {
        try {
            const { id } = req.params;
            const userID = req.user.userID;

            // Tìm đơn hàng
            const order = await Order.findOne({ orderID: id, userID });
            
            if (!order) {
                return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
            }

            // Kiểm tra điều kiện hủy đơn
            if (order.orderStatus !== 'pending' && order.orderStatus !== 'completed') {
                return res.status(400).json({ 
                    message: 'Không thể hủy đơn hàng ở trạng thái này' 
                });
            }

            // Cập nhật trạng thái đơn hàng
            order.orderStatus = 'cancelled';
            order.updatedAt = new Date();
            await order.save();

            // Hoàn trả số lượng vào kho
            const orderDetails = await OrderDetail.find({ orderID: id });
            for (const detail of orderDetails) {
                await ProductSizeStock.updateOne(
                    { SKU: detail.SKU },
                    { $inc: { stock: detail.quantity } }
                );
            }

            res.json({
                message: 'Hủy đơn hàng thành công',
                order
            });
        } catch (error) {
            console.error('Error in cancelOrder:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi hủy đơn hàng',
                error: error.message
            });
        }
    }
    //!ADMIN
    // ADMIN: Lấy tất cả đơn hàng
    async getAllOrdersChoADMIN(req, res) {
        try {
            // Lấy tất cả đơn hàng
            const orders = await Order.find()
                .select('orderID userID fullname phone address totalPrice userCouponsID paymentPrice orderStatus shippingStatus isPayed createdAt updatedAt')
                .lean();


            // Tính toán thống kê
            const stats = {
                totalOrders: orders.length,
                totalRevenue: orders.reduce((sum, order) => sum + order.paymentPrice, 0),
                totalPaidOrders: orders.filter(order => order.isPayed).length,
                totalUnpaidOrders: orders.filter(order => !order.isPayed).length,
            };

            res.json({
                orders,
                stats
            });
        } catch (error) {
            console.log(error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy danh sách đơn hàng',
                error: error.message
            });
        }
    }
    
    //! HÀM XEM CHI TIẾT ORDER TRONG ORDERDETAILCONTROLLER

    //!ADMIN
    // ADMIN: Cập nhật trạng thái đơn hàng
    async updateOrderStatus(req, res) {
        try {
            const { id } = req.params;
            const { orderStatus, shippingStatus, isPayed } = req.body;

            console.log('Request params:', { id });
            console.log('Request body:', { orderStatus, shippingStatus, isPayed });

            // Kiểm tra đơn hàng tồn tại
            let order = await Order.findOne({ orderID: id });
            if (!order) {
                return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
            }

            console.log('Trạng thái hiện tại:', {
                orderStatus: order.orderStatus,
                shippingStatus: order.shippingStatus,
                isPayed: order.isPayed
            });

            // Danh sách trạng thái hợp lệ
            const validOrderStatuses = ['pending', 'confirmed', 'processing', 'completed', 'cancelled', 'refunded'];
            const validShippingStatuses = ['preparing', 'shipping', 'delivered', 'returned', 'cancelled'];

            // Kiểm tra và cập nhật orderStatus
            if (orderStatus && !validOrderStatuses.includes(orderStatus)) {
                return res.status(400).json({
                    message: 'Trạng thái đơn hàng không hợp lệ',
                    validStatuses: validOrderStatuses
                });
            }

            // Cập nhật trực tiếp bằng findOneAndUpdate
            const updateData = {};
            if (orderStatus) updateData.orderStatus = orderStatus;
            if (shippingStatus) updateData.shippingStatus = shippingStatus;
            if (typeof isPayed === 'boolean') updateData.isPayed = isPayed;

            const updatedOrder = await Order.findOneAndUpdate(
                { orderID: id },
                { $set: updateData },
                { new: true }
            );

            console.log('Trạng thái sau khi cập nhật:', {
                orderStatus: updatedOrder.orderStatus,
                shippingStatus: updatedOrder.shippingStatus,
                isPayed: updatedOrder.isPayed
            });

            res.json({
                message: 'Cập nhật trạng thái đơn hàng thành công',
                order: updatedOrder
            });
        } catch (error) {
            console.error('Lỗi khi cập nhật trạng thái đơn hàng:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi cập nhật trạng thái đơn hàng',
                error: error.message
            });
        }
    }

    //!ADMIN
    // ADMIN: Xoá đơn hàng
    async deleteOrder(req, res) {
        try {
            const { id } = req.params;
            await Order.deleteOne({ orderID: id });
            res.json({ message: 'Xoá đơn hàng thành công' });
        } catch (error) {
            res.status(500).json({ message: 'Có lỗi xảy ra khi xoá đơn hàng', error: error.message });
        }
    }

}

module.exports = new OrderController();
