const Favorite = require('../models/Favorite');
const Product = require('../models/Product');
const { getImageLink } = require('../middlewares/ImagesCloudinary_Controller');

class FavoriteController {
    // Thêm sản phẩm vào danh sách yêu thích
    async addToFavorite(req, res) {
        try {
            const userID = req.user.userID;
            const { productID, note } = req.body;

            // Kiểm tra xem sản phẩm đã tồn tại trong danh sách yêu thích chưa
            const existingFavorite = await Favorite.findOne({
                userID: Number(userID),
                productID: Number(productID)
            });

            if (existingFavorite) {
                return res.status(400).json({
                    success: false,
                    message: 'Sản phẩm đã tồn tại trong danh sách yêu thích'
                });
            }

            // Tìm favoriteID lớn nhất hiện tại
            const lastFavorite = await Favorite.findOne().sort({ favoriteID: -1 });
            const nextFavoriteID = (lastFavorite?.favoriteID || 0) + 1;

            // Tạo mới favorite với favoriteID
            const newFavorite = new Favorite({
                favoriteID: nextFavoriteID,
                userID: Number(userID),
                productID: Number(productID),
                note: note || ''
            });

            await newFavorite.save();

            res.status(201).json({
                success: true,
                message: 'Đã thêm vào danh sách yêu thích'
            });
        } catch (error) {
            console.error('Lỗi khi thêm vào yêu thích:', error);
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi thêm vào yêu thích'
            });
        }
    }

    // Lấy danh sách sản phẩm yêu thích của người dùng
    async getFavorites(req, res) {
        try {
            const userID = req.user.userID;
            
            const favorites = await Favorite.aggregate([
                { 
                    $match: { 
                        userID: Number(userID) 
                    } 
                },
                // Join với Product và lấy thông tin category
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
                        as: 'productInfo'
                    }
                },
                { $unwind: '$productInfo' },

                // Join với Promotion
                {
                    $lookup: {
                        from: 'promotions',
                        let: { 
                            productId: '$productInfo._id',
                            categoryName: '$productInfo.categoryInfo.name'
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
                            { $sort: { discountPercent: -1 } },
                            { $limit: 1 }
                        ],
                        as: 'promotionInfo'
                    }
                },
                { $unwind: { path: '$promotionInfo', preserveNullAndEmptyArrays: true } },

                // Format dữ liệu trả về
                {
                    $project: {
                        _id: 1,
                        favoriteID: 1,
                        productID: '$productInfo.productID',
                        name: '$productInfo.name',
                        price: '$productInfo.price',
                        thumbnail: '$productInfo.thumbnail',
                        note: 1,
                        promotion: {
                            $cond: {
                                if: '$promotionInfo',
                                then: {
                                    discountPercent: '$promotionInfo.discountPercent',
                                    endDate: '$promotionInfo.endDate',
                                    finalPrice: {
                                        $round: [
                                            {
                                                $multiply: [
                                                    { $toInt: '$productInfo.price' },
                                                    { $subtract: [1, { $divide: ['$promotionInfo.discountPercent', 100] }] }
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
                },
                {
                    $sort: { 
                        createdAt: -1 
                    }
                }
            ]);

            // Xử lý thumbnail và lọc các item không hợp lệ
            const processedItems = await Promise.all(favorites.map(async item => {
                if (!item.name) return null;
                
                // Xử lý thumbnail tương tự như trong getCart
                if (item.thumbnail) {
                    item.thumbnail = await getImageLink(item.thumbnail);
                }
                return item;
            }));

            // Lọc bỏ các item null
            const validItems = processedItems.filter(item => item !== null);

            res.json({
                message: 'Lấy danh sách yêu thích thành công',
                items: validItems,
                itemCount: validItems.length
            });

        } catch (error) {
            console.error('Lỗi khi lấy danh sách yêu thích:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy danh sách yêu thích',
                error: error.message
            });
        }
    }

    // Xóa sản phẩm khỏi danh sách yêu thích
    async removeFavorite(req, res) {
        try {
            const userID = req.user.userID;
            const { id } = req.params;

            const deletedFavorite = await Favorite.findOneAndDelete({
                favoriteID: Number(id),
                userID: Number(userID)
            });

            if (!deletedFavorite) {
                return res.status(404).json({ success: false });
            }

            res.json({ success: true });
        } catch (error) {
            console.error('Lỗi khi xóa khỏi yêu thích:', error);
            res.status(500).json({ success: false });
        }
    }

    // Cập nhật ghi chú cho sản phẩm yêu thích
    async updateFavorite(req, res) {
        try {
            const userID = req.user.userID;
            const { id } = req.params;
            const { note } = req.body;

            const updatedFavorite = await Favorite.findOneAndUpdate(
                { favoriteID: Number(id), userID: Number(userID) },
                { $set: { note } },
                { new: true }
            );

            if (!updatedFavorite) {
                return res.status(404).json({ success: false });
            }

            res.json({ success: true });
        } catch (error) {
            console.error('Lỗi khi cập nhật ghi chú:', error);
            res.status(500).json({ success: false });
        }
    }

    // Kiểm tra sản phẩm đã được yêu thích chưa
    async checkFavorite(req, res) {
        try {
            const userID = req.user.userID;
            const { productID } = req.params;

            const favorite = await Favorite.findOne({
                userID: Number(userID),
                productID: Number(productID)
            });

            res.json({
                success: true,
                isFavorited: !!favorite
            });

        } catch (error) {
            console.error('Lỗi khi kiểm tra yêu thích:', error);
            res.status(500).json({ success: false });
        }
    }

    // Xóa sản phẩm khỏi danh sách yêu thích bằng productID
    async removeFavoriteByProductId(req, res) {
        try {
            const userID = req.user.userID;
            const { productID } = req.params;

            const deletedFavorite = await Favorite.findOneAndDelete({
                productID: Number(productID),
                userID: Number(userID)
            });

            if (!deletedFavorite) {
                return res.status(404).json({ success: false });
            }

            res.json({ success: true });
        } catch (error) {
            console.error('Lỗi khi xóa khỏi yêu thích:', error);
            res.status(500).json({ success: false });
        }
    }
}

module.exports = new FavoriteController();
