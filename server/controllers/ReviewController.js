const Review = require('../models/Review');
const Product = require('../models/Product');
const User = require('../models/User');
const Order = require('../models/Order');
const OrderDetail = require('../models/OrderDetail');

class ReviewController {
    // Tạo đánh giá mới
    async create(req, res) {
        try {
            const { productID, rating, comment, images } = req.body;
            const userID = req.user.userID; // Lấy từ middleware auth

            console.log('Creating review with data:', { productID, rating, comment, userID });

            // Validate dữ liệu đầu vào
            if (!productID || !rating || !comment) {
                return res.status(400).json({ 
                    message: 'Vui lòng điền đầy đủ thông tin đánh giá' 
                });
            }

            // Kiểm tra sản phẩm tồn tại
            const product = await Product.findOne({ productID: parseInt(productID) });
            if (!product) {
                return res.status(404).json({ message: 'Không tìm thấy sản phẩm' });
            }

            // Kiểm tra user đã mua sản phẩm chưa
            const orders = await Order.find({ 
                userID: parseInt(userID),
                status: 'delivered' // Chỉ tính các đơn hàng đã giao
            });

            const orderIDs = orders.map(order => order.orderID);
            const hasBoughtProduct = await OrderDetail.findOne({
                orderID: { $in: orderIDs },
                productID: parseInt(productID)
            });

            if (!hasBoughtProduct) {
                return res.status(400).json({ 
                    message: 'Bạn chỉ có thể đánh giá sản phẩm đã mua' 
                });
            }

            // Kiểm tra user đã đánh giá sản phẩm này chưa
            const existingReview = await Review.findOne({ 
                userID: parseInt(userID), 
                productID: parseInt(productID) 
            });
            
            if (existingReview) {
                return res.status(400).json({ message: 'Bạn đã đánh giá sản phẩm này rồi' });
            }

            // Tạo reviewID mới
            const lastReview = await Review.findOne().sort({ reviewID: -1 });
            const reviewID = lastReview ? lastReview.reviewID + 1 : 1;

            // Tạo review mới
            const review = await Review.create({
                reviewID,
                userID: parseInt(userID),
                productID: parseInt(productID),
                rating: parseInt(rating),
                comment,
                images: images || [],
                createdAt: new Date()
            });

            // Cập nhật rating trung bình của sản phẩm
            const allReviews = await Review.find({ productID: parseInt(productID) });
            const avgRating = allReviews.reduce((sum, review) => sum + review.rating, 0) / allReviews.length;
            
            await Product.findOneAndUpdate(
                { productID: parseInt(productID) },
                { 
                    rating: avgRating.toFixed(1),
                    totalReviews: allReviews.length
                }
            );

            res.status(201).json({
                message: 'Đánh giá sản phẩm thành công',
                review
            });

        } catch (error) {
            console.error('Error in create review:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi tạo đánh giá',
                error: error.message
            });
        }
    }

    // Cập nhật đánh giá
    async update(req, res) {
        try {
            const { reviewID } = req.params;
            const { rating, comment, images } = req.body;
            const userID = req.user.userID;

            // Kiểm tra review tồn tại và thuộc về user
            const review = await Review.findOne({ reviewID, userID });
            if (!review) {
                return res.status(404).json({ message: 'Không tìm thấy đánh giá' });
            }

            // Cập nhật review
            const updatedReview = await Review.findOneAndUpdate(
                { reviewID },
                {
                    rating,
                    comment,
                    images: images || review.images,
                    updatedAt: new Date()
                },
                { new: true }
            );

            // Cập nhật rating trung bình của sản phẩm
            const allReviews = await Review.find({ productID: review.productID });
            const avgRating = allReviews.reduce((sum, review) => sum + review.rating, 0) / allReviews.length;
            
            await Product.findOneAndUpdate(
                { productID: review.productID },
                { rating: avgRating.toFixed(1) }
            );

            res.json({
                message: 'Cập nhật đánh giá thành công',
                review: updatedReview
            });

        } catch (error) {
            console.error('Error in update review:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi cập nhật đánh giá',
                error: error.message
            });
        }
    }

    // Xóa đánh giá
    async delete(req, res) {
        try {
            const { reviewID } = req.params;
            const userID = req.user.userID;

            // Kiểm tra review tồn tại và thuộc về user
            const review = await Review.findOne({ reviewID, userID });
            if (!review) {
                return res.status(404).json({ message: 'Không tìm thấy đánh giá' });
            }

            // Lưu productID trước khi xóa review
            const productID = review.productID;

            // Xóa review
            await Review.findOneAndDelete({ reviewID });

            // Cập nhật rating trung bình của sản phẩm
            const allReviews = await Review.find({ productID });
            const avgRating = allReviews.length > 0
                ? allReviews.reduce((sum, review) => sum + review.rating, 0) / allReviews.length
                : 0;
            
            await Product.findOneAndUpdate(
                { productID },
                { 
                    rating: avgRating.toFixed(1),
                    totalReviews: allReviews.length
                }
            );

            res.json({ message: 'Xóa đánh giá thành công' });

        } catch (error) {
            console.error('Error in delete review:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi xóa đánh giá',
                error: error.message
            });
        }
    }

    // Lấy đánh giá của sản phẩm
    async getByProduct(req, res) {
        try {
            const { productID } = req.params;
            const { sort = 'newest' } = req.query;

            console.log('Getting reviews for product:', productID);

            // Kiểm tra sản phẩm tồn tại
            const product = await Product.findOne({ productID: parseInt(productID) });
            if (!product) {
                return res.status(404).json({ message: 'Không tìm thấy sản phẩm' });
            }

            // Lấy reviews và populate thông tin user
            const reviews = await Review.find({ productID: parseInt(productID) })
                .sort(sort === 'newest' ? { createdAt: -1 } : { rating: -1 });

            console.log('Found reviews:', reviews);

            // Lấy thông tin user cho mỗi review
            const formattedReviews = await Promise.all(reviews.map(async review => {
                console.log('Processing review:', review);
                console.log('Looking for user with ID:', review.userID);
                
                const user = await User.findOne({ userID: parseInt(review.userID) });
                console.log('Found user:', user);

                return {
                    reviewID: review.reviewID,
                    rating: review.rating,
                    comment: review.comment,
                    createdAt: review.createdAt,
                    userInfo: user ? {
                        userID: user.userID,
                        fullName: user.fullname || 'Người dùng ẩn danh'
                    } : {
                        userID: review.userID,
                        fullName: 'Người dùng ẩn danh'
                    },
                    isCurrentUser: req.user && parseInt(review.userID) === parseInt(req.user.userID)
                };
            }));

            console.log('Formatted reviews:', formattedReviews);

            res.json({
                message: 'Lấy danh sách đánh giá thành công',
                reviews: formattedReviews,
                totalReviews: reviews.length
            });

        } catch (error) {
            console.error('Error in get reviews by product:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy danh sách đánh giá',
                error: error.message
            });
        }
    }

    // Lấy đánh giá của user
    async getByUser(req, res) {
        try {
            const userID = req.user.userID;
            const { page = 1, limit = 10 } = req.query;
            const skip = (parseInt(page) - 1) * parseInt(limit);

            // Lấy danh sách đánh giá
            const reviews = await Review.find({ userID })
                .sort({ createdAt: -1 })
                .skip(skip)
                .limit(parseInt(limit));

            // Lấy thông tin sản phẩm
            const formattedReviews = await Promise.all(reviews.map(async (review) => {
                const product = await Product.findOne({ productID: review.productID });
                return {
                    reviewID: review.reviewID,
                    rating: review.rating,
                    comment: review.comment,
                    createdAt: review.createdAt,
                    productInfo: product ? {
                        name: product.name,
                        image: product.images ? product.images[0] : null,
                        price: product.price
                    } : {
                        name: 'Sản phẩm không còn tồn tại',
                        image: null,
                        price: 0
                    }
                };
            }));

            // Đếm tổng số đánh giá
            const totalReviews = await Review.countDocuments({ userID });

            res.json({
                message: 'Lấy danh sách đánh giá thành công',
                reviews: formattedReviews,
                totalPages: Math.ceil(totalReviews / parseInt(limit)),
                currentPage: parseInt(page),
                totalReviews
            });

        } catch (error) {
            console.error('Error in get reviews by user:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy danh sách đánh giá',
                error: error.message
            });
        }
    }

    // Admin: Lấy tất cả đánh giá
    async getAll(req, res) {
        try {
            const { page = 1, limit = 10, sort = 'newest' } = req.query;
            const skip = (parseInt(page) - 1) * parseInt(limit);

            // Tạo query cơ bản
            const query = Review.find();

            // Thêm sort
            if (sort === 'newest') {
                query.sort({ createdAt: -1 });
            } else {
                query.sort({ rating: -1 });
            }

            // Thực hiện query với phân trang
            const reviews = await query
                .skip(skip)
                .limit(parseInt(limit));

            // Lấy thông tin chi tiết cho mỗi review
            const detailedReviews = await Promise.all(reviews.map(async (review) => {
                const user = await User.findOne({ userID: review.userID });
                const product = await Product.findOne({ productID: review.productID });

                return {
                    reviewID: review.reviewID,
                    rating: review.rating,
                    comment: review.comment,
                    createdAt: review.createdAt,
                    user: user ? {
                        userID: user.userID,
                        fullname: user.fullname,
                        email: user.email
                    } : null,
                    product: product ? {
                        productID: product.productID,
                        name: product.name,
                        thumbnail: product.thumbnail
                    } : null
                };
            }));

            // Đếm tổng số reviews
            const totalReviews = await Review.countDocuments();

            // Tính toán thống kê
            const stats = {
                totalReviews,
                averageRating: await Review.aggregate([
                    {
                        $group: {
                            _id: null,
                            avgRating: { $avg: "$rating" }
                        }
                    }
                ]).then(result => result[0]?.avgRating || 0),
                ratingDistribution: await Review.aggregate([
                    {
                        $group: {
                            _id: "$rating",
                            count: { $sum: 1 }
                        }
                    }
                ]).then(result => {
                    const distribution = {};
                    result.forEach(item => {
                        distribution[item._id] = item.count;
                    });
                    return distribution;
                })
            };

            res.json({
                reviews: detailedReviews,
                stats,
                pagination: {
                    totalReviews,
                    totalPages: Math.ceil(totalReviews / parseInt(limit)),
                    currentPage: parseInt(page),
                    limit: parseInt(limit)
                }
            });

        } catch (error) {
            console.error('Error in get all reviews:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy danh sách đánh giá',
                error: error.message
            });
        }
    }

    // Admin: Xóa đánh giá
    async adminDelete(req, res) {
        try {
            const { reviewID } = req.params;

            // Kiểm tra review tồn tại
            const review = await Review.findOne({ reviewID });
            if (!review) {
                return res.status(404).json({ message: 'Không tìm thấy đánh giá' });
            }

            // Lưu productID trước khi xóa review
            const productID = review.productID;

            // Xóa review
            await Review.findOneAndDelete({ reviewID });

            // Cập nhật rating trung bình của sản phẩm
            const allReviews = await Review.find({ productID });
            const avgRating = allReviews.length > 0
                ? allReviews.reduce((sum, review) => sum + review.rating, 0) / allReviews.length
                : 0;
            
            await Product.findOneAndUpdate(
                { productID },
                { 
                    rating: avgRating.toFixed(1),
                    totalReviews: allReviews.length
                }
            );

            res.json({ message: 'Xóa đánh giá thành công' });

        } catch (error) {
            console.error('Error in admin delete review:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi xóa đánh giá',
                error: error.message
            });
        }
    }
}

module.exports = new ReviewController();
