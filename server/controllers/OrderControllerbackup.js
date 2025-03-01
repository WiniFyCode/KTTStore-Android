const Order = require('../models/Order');
const OrderDetail = require('../models/OrderDetail');
const Cart = require('../models/Cart');
const ProductSizeStock = require('../models/ProductSizeStock');
const UserCoupon = require('../models/UserCoupon');
const Product = require('../models/Product');
const ProductColor = require('../models/ProductColor');
const { getImageLink } = require('../middlewares/ImagesCloudinary_Controller');
const nodemailer = require('nodemailer');

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

            // Lấy thông tin sản phẩm cho từng SKU
            const detailsWithProducts = await Promise.all(
                orderDetails.map(async (detail) => {
                    const stockItem = await ProductSizeStock.findOne({ SKU: detail.SKU });
                    if (!stockItem) return null;

                    // Parse SKU để lấy productID
                    const [productID] = stockItem.SKU.split('_');

                    // Lấy thông tin sản phẩm và màu sắc
                    const [product, color] = await Promise.all([
                        Product.findOne({ productID: Number(productID) })
                            .populate('targetInfo')
                            .populate('categoryInfo'),
                        ProductColor.findOne({ 
                            productID: Number(productID), 
                            colorID: Number(stockItem.colorID) 
                        })
                    ]);

                    return {
                        orderDetailID: detail.orderDetailID,
                        quantity: detail.quantity,
                        SKU: detail.SKU,
                        size: stockItem.size,
                        stock: stockItem.stock,
                        product: product ? {
                            ...product.toObject(),
                            color: color || null
                        } : null
                    };
                })
            );

            // Lọc bỏ các null values nếu có
            const validDetails = detailsWithProducts.filter(detail => detail !== null);

            res.json({
                ...order.toObject(),
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

    // Tạo đơn hàng mới từ giỏ hàng
    async createOrder(req, res) {
        try {
            const userID = req.user.userID;
            const { fullname, phone, address, userCouponsID } = req.body;

            // Log thông tin request
            console.log('===== CREATE ORDER INFO =====');
            console.log('User ID:', userID);
            console.log('Shipping Info:', { fullname, phone, address });

            // Lấy các items trong giỏ hàng
            const cartItems = await Cart.find({ userID });
            console.log('Cart Items:', cartItems);

            if (cartItems.length === 0) {
                return res.status(400).json({ message: 'Giỏ hàng trống' });
            }

            // Tính tổng tiền và kiểm tra tồn khoy
            let totalPrice = 0;
            const orderItems = [];
            
            for (const item of cartItems) {
                const [productID] = item.SKU.split('_');
                
                console.log('\n----- Product Details -----');
                console.log('SKU:', item.SKU);
                console.log('Product ID:', productID);
                
                const [product, stockItem] = await Promise.all([
                    Product.findOne({ productID: Number(productID) })
                        .populate('promotionInfo'),
                    ProductSizeStock.findOne({ SKU: item.SKU })
                ]);

                console.log('Original Price:', product.price);
                console.log('Promotion Info:', product.promotionInfo);

                if (!stockItem || !product) {
                    return res.status(404).json({ 
                        message: `Sản phẩm không tồn tại hoặc đã hết hàng` 
                    });
                }

                if (stockItem.stock < item.quantity) {
                    return res.status(400).json({ 
                        message: `Sản phẩm ${product.name} không đủ số lượng` 
                    });
                }

                // Tính giá sau khuyến mãi (nếu có)
                let finalPrice = product.price;
                if (product.promotionInfo && product.promotionInfo.isActive) {
                    const now = new Date();
                    if (now >= product.promotionInfo.startDate && now <= product.promotionInfo.endDate) {
                        finalPrice = product.price * (1 - product.promotionInfo.discountPercent / 100);
                        console.log('Discount Percent:', product.promotionInfo.discountPercent + '%');
                    }
                }

                const itemPrice = finalPrice * item.quantity;
                totalPrice += itemPrice;

                console.log('Final Price:', finalPrice);
                console.log('Quantity:', item.quantity);
                console.log('Item Total Price:', itemPrice);

                orderItems.push({
                    SKU: item.SKU,
                    quantity: item.quantity,
                    price: finalPrice
                });
            }

            console.log('\n===== Order Summary =====');
            console.log('Order Items:', orderItems);
            console.log('Total Price:', totalPrice);
            console.log('User Coupon ID:', userCouponsID);

            // Xử lý mã giảm giá nếu có
            let paymentPrice = totalPrice;
            let appliedCoupon = null;

            if (userCouponsID) {
                const coupon = await UserCoupon.findOne({ 
                    userCouponsID, 
                    userID, 
                    isUsed: false 
                }).populate('couponInfo');

                if (!coupon) {
                    return res.status(400).json({ 
                        message: 'Mã giảm giá không hợp lệ' 
                    });
                }

                if (coupon.couponInfo.minOrderValue > totalPrice) {
                    return res.status(400).json({ 
                        message: `Đơn hàng phải từ ${coupon.couponInfo.minOrderValue}đ để sử dụng mã giảm giá` 
                    });
                }

                const discountAmount = Math.min(
                    coupon.couponInfo.maxDiscountValue,
                    totalPrice * (coupon.couponInfo.discountPercent / 100)
                );

                paymentPrice = totalPrice - discountAmount;
                appliedCoupon = coupon;
            }

            // Tạo đơn hàng mới
            const lastOrder = await Order.findOne().sort({ orderID: -1 });
            const orderID = lastOrder ? lastOrder.orderID + 1 : 1;

            const order = new Order({
                orderID,
                userID,
                fullname,
                phone,
                address,
                totalPrice: Math.round(totalPrice), // Làm tròn để tránh số thập phân
                userCouponsID: appliedCoupon ? appliedCoupon.userCouponsID : null,
                paymentPrice: Math.round(paymentPrice) // Làm tròn để tránh số thập phân
            });

            // Tạo chi tiết đơn hàng
            const lastOrderDetail = await OrderDetail.findOne().sort({ orderDetailID: -1 });
            let nextOrderDetailID = lastOrderDetail ? lastOrderDetail.orderDetailID + 1 : 1;

            const orderDetails = await Promise.all(orderItems.map(async (item) => {
                const orderDetail = new OrderDetail({
                    orderDetailID: nextOrderDetailID++,
                    orderID,
                    SKU: item.SKU,
                    quantity: item.quantity,
                    price: item.price
                });
                return orderDetail;
            }));

            // Lưu đơn hàng và chi tiết đơn hàng
            await order.save();
            await OrderDetail.insertMany(orderDetails);

            // Cập nhật trạng thái mã giảm giá
            if (appliedCoupon) {
                await UserCoupon.updateOne(
                    { userCouponsID: appliedCoupon.userCouponsID, userID },
                    { isUsed: true, usedAt: new Date() }
                );
            }

            // Cập nhật số lượng tồn kho và xóa giỏ hàng
            await Promise.all([
                ...orderItems.map(item => 
                    ProductSizeStock.updateOne(
                        { SKU: item.SKU },
                        { $inc: { stock: -item.quantity } }
                    )
                ),
                Cart.deleteMany({ userID })
            ]);

            // Log kết quả cuối cùng
            console.log('\n===== Final Order =====');
            console.log('Order ID:', orderID);
            console.log('Total Price:', totalPrice);
            console.log('Payment Price:', paymentPrice);

            res.status(201).json({
                message: 'Tạo đơn hàng thành công',
                order: {
                    ...order.toObject(),
                    totalPrice: order.totalPrice,
                    paymentPrice: order.paymentPrice
                }
            });
        } catch (error) {
            console.error('Error in createOrder:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi tạo đơn hàng',
                error: error.message
            });
        }
    }

    // Hủy đơn hàng
    async cancelOrder(req, res) {
        try {
            const userID = req.user.userID;
            const { id } = req.params;

            const order = await Order.findOne({ orderID: id, userID });
            if (!order) {
                return res.status(404).json({ 
                    message: 'Không tìm thấy đơn hàng' 
                });
            }

            // Kiểm tra trạng thái đơn hàng
            if (!['pending', 'confirmed'].includes(order.orderStatus)) {
                return res.status(400).json({ 
                    message: 'Chỉ có thể hủy đơn hàng ở trạng thái chờ xử lý hoặc đã xác nhận' 
                });
            }

            // Hoàn lại số lượng tồn kho
            const orderDetails = await OrderDetail.find({ orderID: id });
            await Promise.all(orderDetails.map(detail => 
                ProductSizeStock.updateOne(
                    { SKU: detail.SKU },
                    { $inc: { stock: detail.quantity } }
                )
            ));

            // Hoàn lại mã giảm giá nếu có
            if (order.userCouponsID) {
                await UserCoupon.updateOne(
                    { 
                        userCouponsID: order.userCouponsID, 
                        userID 
                    },
                    { 
                        isUsed: false, 
                        usedAt: null 
                    }
                );
            }

            // Cập nhật trạng thái đơn hàng
            order.orderStatus = 'cancelled';
            order.cancelledAt = new Date();
            await order.save();

            res.json({
                message: 'Hủy đơn hàng thành công',
                order: {
                    ...order.toObject(),
                    totalPrice: order.totalPrice,
                    paymentPrice: order.paymentPrice
                }
            });
        } catch (error) {
            console.error('Error in cancelOrder:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi hủy đơn hàng',
                error: error.message
            });
        }
    }


    // ADMIN: Lấy tất cả đơn hàng
    async getAllOrders(req, res) {
        try {
            const { page = 1, limit = 10, status, search } = req.query;

            // Tạo filter dựa trên status và search nếu có
            const filter = {};
            if (status) {
                filter.orderStatus = status;
            }
            if (search) {
                filter.$or = [
                    { fullname: { $regex: search, $options: 'i' } },
                    { phone: { $regex: search, $options: 'i' } },
                    { address: { $regex: search, $options: 'i' } }
                ];
            }

            // Lấy danh sách đơn hàng với phân trang
            const orders = await Order.find(filter)
                .sort('-createdAt')
                .skip((page - 1) * limit)
                .limit(limit)
                .populate('orderDetails')
                .populate('userInfo', 'username email');

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

    //!Toàn thêm
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

    //!Toàn thêm
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

    //!Toàn thêm
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

    // Hàm gửi email thông báo
    async sendPaymentNotification(orderDetails, userEmail) {
        // Tạo transporter với email của hệ thống
        const transporter = nodemailer.createTransport({
            service: 'gmail',
            auth: {
                user: process.env.EMAIL_USER, // Email của hệ thống
                pass: process.env.EMAIL_PASS // App password của email hệ thống
            }
        });

        // Email cho admin
        const adminMailOptions = {
            from: `KTT Store <${process.env.EMAIL_USER}>`,
            to: process.env.ADMIN_EMAIL,
            subject: `[ADMIN] Xác nhận thanh toán đơn hàng #${orderDetails.orderID}`,
            html: `
                <h2>Thông báo thanh toán đơn hàng</h2>
                <p>Đơn hàng #${orderDetails.orderID} đã được thanh toán</p>
                <p><strong>Khách hàng:</strong> ${orderDetails.fullname}</p>
                <p><strong>Email:</strong> ${userEmail}</p>
                <p><strong>Số điện thoại:</strong> ${orderDetails.phone}</p>
                <p><strong>Địa chỉ:</strong> ${orderDetails.address}</p>
                <p><strong>Phương thức thanh toán:</strong> ${orderDetails.paymentMethod === 'banking' ? 'Chuyển khoản ngân hàng' : 'Tiền mặt'}</p>
                <p><strong>Tổng tiền:</strong> ${orderDetails.totalAmount.toLocaleString('vi-VN')}đ</p>
                <p><strong>Thời gian thanh toán:</strong> ${new Date().toLocaleString('vi-VN')}</p>
            `
        };

        // Email cho khách hàng
        const customerMailOptions = {
            from: `KTT Store <${process.env.EMAIL_USER}>`,
            to: userEmail, // Email của khách hàng
            subject: `Xác nhận thanh toán đơn hàng #${orderDetails.orderID}`,
            html: `
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #2563eb;">Cảm ơn bạn đã mua hàng!</h2>
                    <p>Xin chào ${orderDetails.fullname},</p>
                    <p>Chúng tôi đã nhận được thanh toán của bạn cho đơn hàng #${orderDetails.orderID}.</p>
                    
                    <div style="background-color: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="margin-top: 0;">Chi tiết đơn hàng:</h3>
                        <p><strong>Mã đơn hàng:</strong> #${orderDetails.orderID}</p>
                        <p><strong>Ngày đặt hàng:</strong> ${new Date(orderDetails.createdAt).toLocaleString('vi-VN')}</p>
                        <p><strong>Phương thức thanh toán:</strong> ${orderDetails.paymentMethod === 'banking' ? 'Chuyển khoản ngân hàng' : 'Tiền mặt'}</p>
                        <p><strong>Tổng tiền:</strong> ${orderDetails.totalAmount.toLocaleString('vi-VN')}đ</p>
                    </div>

                    <div style="background-color: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="margin-top: 0;">Địa chỉ giao hàng:</h3>
                        <p><strong>Người nhận:</strong> ${orderDetails.fullname}</p>
                        <p><strong>Số điện thoại:</strong> ${orderDetails.phone}</p>
                        <p><strong>Địa chỉ:</strong> ${orderDetails.address}</p>
                    </div>

                    <p>Chúng tôi sẽ sớm giao hàng đến bạn. Bạn có thể theo dõi đơn hàng tại <a href="${process.env.CLIENT_URL}/orders" style="color: #2563eb;">đây</a>.</p>
                    
                    <p style="margin-top: 30px;">Mọi thắc mắc xin vui lòng liên hệ:</p>
                    <p>Email: ${process.env.EMAIL_USER}</p>
                    <p>Hotline: ${process.env.HOTLINE || '0123456789'}</p>
                    
                    <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb;">
                        <p style="color: #6b7280; font-size: 0.875rem;">Email này được gửi tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
            `
        };

        // Gửi email
        await Promise.all([
            transporter.sendMail(adminMailOptions),
            transporter.sendMail(customerMailOptions)
        ]);
    }

    // Hàm xác nhận thanh toán
    async confirmPayment(req, res) {
        try {
            const { orderID } = req.params;
            const order = await Order.findOne({ orderID: orderID })
                .populate('userInfo', 'email'); // Lấy thêm email của user

            if (!order) {
                return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
            }

            // Cập nhật trạng thái thanh toán
            order.isPaid = true;
            order.paidAt = new Date();
            await order.save();

            // Gửi email thông báo cho admin và khách hàng
            await this.sendPaymentNotification(order, order.userInfo.email);

            res.status(200).json({ 
                message: 'Xác nhận thanh toán thành công',
                order: order 
            });
        } catch (error) {
            console.error('Error confirming payment:', error);
            res.status(500).json({ message: 'Lỗi khi xác nhận thanh toán' });
        }
    }
}

module.exports = new OrderController();
