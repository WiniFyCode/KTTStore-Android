const Cart = require('../models/Cart');
const ProductSizeStock = require('../models/ProductSizeStock');
const Product = require('../models/Product');
const ProductColor = require('../models/ProductColor');
const Promotion = require('../models/Promotion');
const { getImageLink } = require('../middlewares/ImagesCloudinary_Controller');

class CartController {
    async getCart(req, res) {
        try {
            const userID = req.user.userID;
    
            // Sử dụng aggregation pipeline để join các collection
            const cartItems = await Cart.aggregate([
                { $match: { userID } },
                
                // Join với ProductSizeStock
                {
                    $lookup: {
                        from: 'product_sizes_stocks',
                        localField: 'SKU',
                        foreignField: 'SKU',
                        as: 'sizeStock'
                    }
                },
                { $unwind: '$sizeStock' },
    
                // Tạo trường productID và colorID từ SKU
                {
                    $addFields: {
                        productID: {
                            $toInt: { $arrayElemAt: [{ $split: ['$SKU', '_'] }, 0] }
                        },
                        colorID: {
                            $toInt: { $arrayElemAt: [{ $split: ['$SKU', '_'] }, 1] }
                        }
                    }
                },
    
                // Join với Product
                {
                    $lookup: {
                        from: 'products',
                        let: { productID: '$productID' },
                        pipeline: [
                            {
                                $match: {
                                    $expr: {
                                        $and: [
                                            { $eq: ['$productID', '$$productID'] },
                                            { $eq: ['$isActivated', true] }
                                        ]
                                    }
                                }
                            },
                            {
                                $lookup: {
                                    from: 'categories',
                                    localField: 'categoryID',
                                    foreignField: 'categoryID',
                                    as: 'categoryInfo'
                                }
                            },
                            { 
                                $project: { 
                                    productID: 1, 
                                    name: 1, 
                                    price: 1, 
                                    thumbnail: 1,
                                    categoryInfo: { $arrayElemAt: ['$categoryInfo', 0] }
                                } 
                            }
                        ],
                        as: 'product'
                    }
                },
                { $unwind: { path: '$product', preserveNullAndEmptyArrays: true } },
    
                // Join với ProductColor
                {
                    $lookup: {
                        from: 'product_colors',
                        let: { productID: '$productID', colorID: '$colorID' },
                        pipeline: [
                            {
                                $match: {
                                    $expr: {
                                        $and: [
                                            { $eq: ['$productID', '$$productID'] },
                                            { $eq: ['$colorID', '$$colorID'] }
                                        ]
                                    }
                                }
                            }
                        ],
                        as: 'color'
                    }
                },
                { $unwind: { path: '$color', preserveNullAndEmptyArrays: true } },
    
                // Join với Promotion
                {
                    $lookup: {
                        from: 'promotions',
                        let: { 
                            productId: '$product._id',
                            categoryName: '$product.categoryInfo.name'
                        },
                        pipeline: [
                            {
                                $match: {
                                    $expr: {
                                        $and: [
                                            {
                                                $or: [
                                                    { $in: ['$$productId', '$products'] },
                                                    { $in: ['$$categoryName', '$categories'] }
                                                ]
                                            },
                                            { $lte: ['$startDate', new Date()] },
                                            { $gte: ['$endDate', new Date()] },
                                            { $eq: ['$status', 'active'] }
                                        ]
                                    }
                                }
                            },
                            {
                                $addFields: {
                                    debug: {
                                        productId: '$$productId',
                                        categoryName: '$$categoryName',
                                        categories: '$categories'
                                    }
                                }
                            },
                            { $sort: { discountPercent: -1 } },
                            { $limit: 1 }
                        ],
                        as: 'promotion'
                    }
                },
                { $unwind: { path: '$promotion', preserveNullAndEmptyArrays: true } },
    
                // Xử lý và định dạng dữ liệu trả về
                {
                    $project: {
                        cartID: 1,
                        SKU: 1,
                        productID: '$product.productID',
                        name: '$product.name',
                        price: '$product.price',
                        thumbnail: '$product.thumbnail',
                        size: '$sizeStock.size',
                        colorName: { $ifNull: ['$color.colorName', 'Mặc định'] },
                        quantity: {
                            $min: ['$quantity', '$sizeStock.stock']
                        },
                        stock: '$sizeStock.stock',
                        promotion: {
                            $cond: {
                                if: '$promotion',
                                then: {
                                    discountPercent: '$promotion.discountPercent',
                                    endDate: '$promotion.endDate',
                                    finalPrice: {
                                        $round: [
                                            {
                                                $multiply: [
                                                    { $toInt: '$product.price' },
                                                    { $subtract: [1, { $divide: ['$promotion.discountPercent', 100] }] }
                                                ]
                                            },
                                            0
                                        ]
                                    }
                                },
                                else: null
                            }
                        }
                    }
                }
            ]);
    
            // Xử lý thumbnail và tính toán totalAmount
            const processedItems = await Promise.all(cartItems.map(async item => {
                if (!item.name) return null;
                
                item.thumbnail = await getImageLink(item.thumbnail);
                // Không thêm subtotal vào item nữa
                return item;
            }));
    
            // Lọc bỏ các item null
            const validItems = processedItems.filter(item => item !== null);
    
            // Tính tổng tiền dựa trên giá sau khuyến mãi
            const totalAmount = validItems.reduce((sum, item) => {
                const itemPrice = item.promotion ? item.promotion.finalPrice : item.price;
                return sum + (itemPrice * item.quantity);
            }, 0);
    
            res.json({
                success: true,
                items: validItems,
                totalAmount,
                itemCount: validItems.length
            });
        } catch (error) {
            console.error('Error in getCart:', error);
            res.status(500).json({
                success: false,
                error: error.message
            });
        }
    }

    // Thêm sản phẩm vào giỏ hàng
    async addToCart(req, res) {
        try {
            const userID = req.user.userID;
            const { SKU, quantity = 1 } = req.body;

            console.log(`SKU: ${SKU}, quantity: ${quantity}`);

            // Kiểm tra sản phẩm tồn tại và còn hàng
            const stockItem = await ProductSizeStock.findOne({ SKU });
            if (!stockItem) {
                return res.status(404).json({ 
                    success: false,
                    message: 'Sản phẩm không tồn tại' 
                });
            }
            console.log(`stock: ${stockItem.stock}`);

            if (stockItem.stock < quantity) {
                return res.status(400).json({ 
                    success: false,
                    message: 'Số lượng sản phẩm trong kho không đủ' 
                });
            }

            // Kiểm tra sản phẩm đã có trong giỏ hàng chưa
            let cartItem = await Cart.findOne({ userID, SKU });

            if (cartItem) {
                // Nếu đã có, cập nhật số lượng
                const newQuantity = cartItem.quantity + quantity;
                console.log(`new quantity: ${newQuantity}`);
                if (newQuantity > stockItem.stock) {
                    return res.status(400).json({ 
                        success: false,
                        message: `Số lượng sản phẩm trong kho không đủ, đã tồn tại ${cartItem.quantity} sản phẩm này trong giỏ hàng`, 
                        
                    });
                }

                cartItem.quantity = newQuantity;
                await cartItem.save();
            } else {
                // Nếu chưa có, tạo mới
                const lastCart = await Cart.findOne().sort({ cartID: -1 });
                const cartID = lastCart ? lastCart.cartID + 1 : 1;

                cartItem = new Cart({
                    cartID,
                    userID,
                    SKU,
                    quantity
                });
                await cartItem.save();
            }

            res.status(201).json({
                success: true,
                message: 'Thêm vào giỏ hàng thành công'
            });
        } catch (error) {
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi thêm vào giỏ hàng',
                error: error.message
            });
        }
    }

    // Cập nhật số lượng sản phẩm trong giỏ
    async updateCartItem(req, res) {
        try {
            const userID = req.user.userID;
            const { id } = req.params;
            const { quantity } = req.body;

            // Kiểm tra item tồn tại trong giỏ
            const cartItem = await Cart.findOne({ cartID: id, userID });
            if (!cartItem) {
                return res.status(404).json({ 
                    success: false,
                    message: 'Không tìm thấy sản phẩm trong giỏ hàng' 
                });
            }

            // Kiểm tra số lượng tồn kho
            const stockItem = await ProductSizeStock.findOne({ SKU: cartItem.SKU });
            if (stockItem.stock < quantity) {
                return res.status(400).json({ 
                    success: false,
                    message: 'Số lượng sản phẩm trong kho không đủ', 
                });
            }

            // Cập nhật số lượng
            cartItem.quantity = quantity;
            await cartItem.save();

            res.json({
                success: true,
                message: 'Cập nhật số lượng thành công',
            });
        } catch (error) {
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi cập nhật số lượng',
                error: error.message
            });
        }
    }

    // Xóa sản phẩm khỏi giỏ hàng
    async removeFromCart(req, res) {
        try {
            const userID = req.user.userID;
            const { id } = req.params;

            const cartItem = await Cart.findOne({ cartID: id, userID });
            if (!cartItem) {
                return res.status(404).json({ 
                    success: false,
                    message: 'Không tìm thấy sản phẩm trong giỏ hàng' 
                });
            }

            await cartItem.deleteOne();

            res.json({ 
                success: true,
                message: 'Xóa sản phẩm khỏi giỏ hàng thành công' 
            });
        } catch (error) {
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi xóa sản phẩm khỏi giỏ hàng',
                error: error.message
            });
        }
    }

    // Xóa toàn bộ giỏ hàng
    async clearCart(req, res) {
        try {
            const userID = req.user.userID;
            
            await Cart.deleteMany({ userID });

            res.json({ message: 'Xóa giỏ hàng thành công' });
        } catch (error) {
            res.status(500).json({
                message: 'Có lỗi xảy ra khi xóa giỏ hàng',
                error: error.message
            });
        }
    }
}

module.exports = new CartController();
