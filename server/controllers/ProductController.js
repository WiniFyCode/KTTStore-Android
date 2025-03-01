const Product = require('../models/Product');
const Category = require('../models/Category');
const Target = require('../models/Target');
const ProductColor = require('../models/ProductColor');
const ProductSizeStock = require('../models/ProductSizeStock');
const Promotion = require('../models/Promotion'); // Thêm dòng này
const { getImageLink, uploadImage } = require('../middlewares/ImagesCloudinary_Controller');
const { GoogleGenerativeAI } = require("@google/generative-ai");
const trainingData = require('../data/trainingData');

// Khởi tạo Gemini AI
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-1.5-pro" });

class ProductController {
    //! Android
    // Lấy danh sách sản phẩm với phân trang và lọc
    async getProducts(req, res) {
        try {
            // Bước 1: Lấy và validate query params
            const {
                page = 1,
                limit = 6,
                sort = '-createdAt',
                categoryID,
                targetID,
                minPrice,
                maxPrice,
                search,
                isActivated,
                hasPromotion
            } = req.query;

            // Bước 2: Xây dựng pipeline cho aggregation
            const pipeline = [];

            // Bước 2.1: Match stage đầu tiên (lọc cơ bản)
            const matchStage = {};

            // Xử lý trạng thái active
            if (typeof isActivated !== 'undefined') {
                matchStage.isActivated = isActivated === 'true';
            } else {
                matchStage.isActivated = true;
            }

            // Xử lý targetID
            if (targetID) {
                matchStage.targetID = parseInt(targetID);
            }

            // Xử lý categoryID
            if (categoryID && categoryID !== 'Tất cả') {
                matchStage.categoryID = parseInt(categoryID);
            }

            // Xử lý khoảng giá
            if (minPrice || maxPrice) {
                matchStage.price = {};
                if (minPrice) matchStage.price.$gte = parseInt(minPrice);
                if (maxPrice) matchStage.price.$lte = parseInt(maxPrice);
            }

            // Xử lý tìm kiếm
            if (search) {
                // Chuẩn hóa chuỗi tìm kiếm
                const normalizedSearch = search.trim()                    // Xóa khoảng trắng đầu/cuối
                    .replace(/\s+/g, ' ')                                // Thay thế nhiều khoảng trắng thành một khoảng trắng
                    .split(' ')                                          // Tách thành mảng các từ
                    .filter(word => word.length > 0)                     // Lọc bỏ chuỗi rỗng
                    .map(word => `(?=.*${word})`)                       // Tạo positive lookahead cho mỗi từ
                    .join('');                                          // Nối lại thành một chuỗi

                // Tạo regex pattern với các điều kiện:
                // - i: case insensitive
                // - Tìm kiếm mọi từ trong tên sản phẩm (không phân biệt thứ tự)
                matchStage.name = new RegExp(normalizedSearch, 'i');
            }

            pipeline.push({ $match: matchStage });

            // Bước 2.2: Lookup stages
            pipeline.push(
                {
                    $lookup: {
                        from: 'targets',
                        localField: 'targetID',
                        foreignField: 'targetID',
                        as: 'targetInfo'
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
                    $lookup: {
                        from: 'product_colors',
                        localField: 'productID',
                        foreignField: 'productID',
                        as: 'colors'
                    }
                }
            );

            // Bước 2.3: Xử lý colors và sizes
            pipeline.push(
                {
                    $unwind: {
                        path: '$colors',
                        preserveNullAndEmptyArrays: true
                    }
                },
                {
                    $lookup: {
                        from: 'product_sizes_stocks',
                        localField: 'colors.colorID',
                        foreignField: 'colorID',
                        as: 'sizes'
                    }
                },
                {
                    $unwind: {
                        path: '$sizes',
                        preserveNullAndEmptyArrays: true
                    }
                },
                {
                    $group: {
                        _id: '$_id',
                        productID: { $first: '$productID' },
                        name: { $first: '$name' },
                        price: { $first: '$price' },
                        thumbnail: { $first: '$thumbnail' },
                        isActivated: { $first: '$isActivated' },
                        categoryInfo: { $first: '$categoryInfo' },
                        targetInfo: { $first: '$targetInfo' },
                        totalStock: { $sum: '$sizes.stock' },
                        colors: {
                            $push: {
                                $cond: {
                                    if: { $ne: ['$colors', null] },
                                    then: {
                                        colorID: '$colors.colorID',
                                        colorName: '$colors.colorName',
                                        images: '$colors.images',
                                        sizes: '$sizes'
                                    },
                                    else: null
                                }
                            }
                        }
                    }
                },
                {
                    $project: {
                        _id: 1,
                        productID: 1,
                        name: 1,
                        price: 1,
                        thumbnail: 1,
                        isActivated: 1,
                        categoryInfo: { $arrayElemAt: ['$categoryInfo', 0] },
                        targetInfo: { $arrayElemAt: ['$targetInfo', 0] },
                        totalStock: 1,
                        colors: {
                            $filter: {
                                input: '$colors',
                                as: 'color',
                                cond: { $ne: ['$$color', null] }
                            }
                        }
                    }
                }
            );

            // Bước 2.4: Lookup promotion
            pipeline.push({
                $lookup: {
                    from: 'promotions',
                    let: { 
                        productId: '$_id', 
                        categoryName: '$categoryInfo.name' 
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
                    as: 'activePromotion'
                }
            });

            // Thêm match stage để lọc sản phẩm có khuyến mãi nếu hasPromotion=true
            if (hasPromotion === 'true') {
                pipeline.push({
                    $match: {
                        'activePromotion': { $ne: [] } // Chỉ lấy sản phẩm có khuyến mãi
                    }
                });
            }

            // Bước 2.5: Project stage cuối cùng
            pipeline.push({
                $project: {
                    productID: 1,
                    name: 1,
                    price: 1,
                    thumbnail: 1,
                    isActivated: 1,
                    totalStock: 1,
                    inStock: { $gt: ['$totalStock', 0] },
                    category: '$categoryInfo.name',
                    target: '$targetInfo.name',
                    promotion: {
                        $let: {
                            vars: {
                                promo: { $arrayElemAt: ['$activePromotion', 0] }
                            },
                            in: {
                                $cond: {
                                    if: '$$promo',
                                    then: {
                                        discountPercent: '$$promo.discountPercent',
                                        finalPrice: {
                                            $toString: {
                                                $round: [
                                                    {
                                                        $multiply: [
                                                            { $toInt: '$price' },
                                                            { $subtract: [1, { $divide: ['$$promo.discountPercent', 100] }] }
                                                        ]
                                                    },
                                                    0
                                                ]
                                            }
                                        }
                                    },
                                    else: null
                                }
                            }
                        }
                    }
                }
            });

            // Bước 3: Thêm sort stage
            const sortStage = {};
            switch (sort) {
                case 'price-asc': sortStage.price = 1; break;
                case 'price-desc': sortStage.price = -1; break;
                case 'name-asc': sortStage.name = 1; break;
                case 'name-desc': sortStage.name = -1; break;
                case 'stock-asc': sortStage.totalStock = 1; break;
                case 'stock-desc': sortStage.totalStock = -1; break;
                case 'newest': sortStage.createdAt = -1; break;
                default: sortStage.createdAt = -1;
            }
            pipeline.push({ $sort: sortStage });

            // Bước 4: Thực hiện aggregation
            const [results] = await Promise.all([
                Product.aggregate(pipeline),
                Product.createIndexes([
                    { key: { name: 1 } },
                    { key: { price: 1 } },
                    { key: { categoryID: 1 } },
                    { key: { targetID: 1 } },
                    { key: { isActivated: 1 } }
                ])
            ]);

            // Bước 5: Xử lý phân trang
            const total = results.length;
            const totalPages = Math.ceil(total / limit);
            const startIndex = (page - 1) * limit;
            const endIndex = startIndex + parseInt(limit);
            const paginatedResults = results.slice(startIndex, endIndex);

            // Bước 6: Xử lý cloudinary cho thumbnails
            const productsWithCloudinary = await Promise.all(
                paginatedResults.map(async (product) => ({
                    ...product,
                    thumbnail: await getImageLink(product.thumbnail)
                }))
            );

            res.json({
                products: productsWithCloudinary,
                totalPages,
                currentPage: parseInt(page),
                limit: parseInt(limit)

            });

        } catch (error) {
            console.error('Error in getProducts:', error);
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi lấy danh sách sản phẩm',
                error: error.message
            });
        }
    }

    // Lấy thông tin cơ bản của tất cả sản phẩm (không phân trang)
    async getAllProductsBasicInfo(req, res) {
        try {
            // Lấy tất cả sản phẩm đang hoạt động
            const products = await Product.find({ isActivated: true })
                .populate('targetInfo', 'name')
                .populate('categoryInfo', 'name');

            // Lấy thông tin về màu sắc và kích thước cho từng sản phẩm
            const productsWithDetails = await Promise.all(products.map(async (product) => {
                const colors = await ProductColor.find({ productID: product.productID });

                // Tính tổng số lượng tồn kho cho tất cả màu và size
                let totalStock = 0;
                for (const color of colors) {
                    const sizes = await ProductSizeStock.find({ colorID: color.colorID });
                    totalStock += sizes.reduce((sum, size) => sum + size.stock, 0);
                }

                return {
                    _id: product._id,
                    productID: product.productID,
                    name: product.name,
                    price: product.price,
                    category: product.categoryInfo?.name,
                    target: product.targetInfo?.name,
                    thumbnail: product.thumbnail ? await getImageLink(product.thumbnail) : null,
                    colorCount: colors.length,
                    totalStock,
                    inStock: totalStock > 0
                };
            }));

            res.json({
                success: true,
                products: productsWithDetails
            });
        } catch (error) {
            console.error('Error in getAllProductsBasicInfo:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy thông tin sản phẩm',
                error: error.message
            });
        }
    }
    
    //! Android
    // Lấy chi tiết sản phẩm theo ID
    async getProductById(req, res) {
        try {
            const { id } = req.params;

            // Lấy thông tin cơ bản của sản phẩm, sử dụng productID thay vì _id
            const product = await Product.findOne({ productID: id })
                .populate('targetInfo', 'name')
                .populate('categoryInfo', 'name');

            if (!product) {
                return res.status(404).json({
                    message: 'Không tìm thấy sản phẩm'
                });
            }

            // Lấy tất cả màu của sản phẩm với đầy đủ thông tin
            const colors = await ProductColor.find({ productID: product.productID });

            // Xử lý thumbnail bằng Cloudinary
            const thumbnail = product.thumbnail ? await getImageLink(product.thumbnail) : null;

            // Lấy thông tin size và tồn kho cho từng màu
            const colorsWithSizes = await Promise.all(colors.map(async (color) => {
                const sizes = await ProductSizeStock.find({ colorID: color.colorID });

                // Xử lý hình ảnh từng màu sắc bằng Cloudinary
                const imagesPromises = color.images.map(async img => await getImageLink(img));
                const images = await Promise.all(imagesPromises);

                return {
                    _id: color._id,
                    colorID: color.colorID,
                    productID: color.productID,
                    colorName: color.colorName,
                    images: images || [],
                    sizes: sizes.map(size => ({
                        _id: size._id,
                        size: size.size,
                        stock: size.stock,
                        SKU: size.SKU
                    }))
                };
            }));

            // Lấy promotion đang active cho sản phẩm
            const currentDate = new Date();
            const activePromotion = await Promotion.findOne({
                $or: [
                    { products: product._id },
                    { categories: product.categoryInfo.name }
                ],
                startDate: { $lte: currentDate },
                endDate: { $gte: currentDate },
                status: 'active'
            }).sort({ discountPercent: -1 }); // Lấy promotion có giảm giá cao nhất

            // Tính giá sau khuyến mãi nếu có
            let discountedPrice = null;
            if (activePromotion) {
                const priceNumber = Number(product.price.toString().replace(/\./g, ''));
                const discountedNumber = Math.round(priceNumber * (1 - activePromotion.discountPercent / 100));
                discountedPrice = discountedNumber.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
            }

            // Tạo object chứa thông tin sản phẩm với các trường bổ sung
            const formattedProduct = {
                _id: product._id,
                productID: product.productID,
                name: product.name,
                targetID: product.targetID,
                description: product.description,
                price: product.price,
                categoryID: product.categoryID,
                createdAt: product.createdAt,
                updatedAt: product.updatedAt,
                thumbnail,
                isActivated: product.isActivated,
                colors: colorsWithSizes,
                category: product.categoryInfo?.name,
                target: product.targetInfo?.name,
                totalStock: colorsWithSizes.reduce((total, color) =>
                    total + color.sizes.reduce((sum, size) => sum + size.stock, 0), 0),
                inStock: colorsWithSizes.some(color =>
                    color.sizes.some(size => size.stock > 0)
                ),
                promotion: activePromotion ? {
                    name: activePromotion.name,
                    discountPercent: activePromotion.discountPercent,
                    discountedPrice: discountedPrice,
                    endDate: activePromotion.endDate
                } : null
            };

            res.json({
                success: true,
                product: formattedProduct
            });
        } catch (error) {
            console.error('Error in getProductById:', error);
            res.status(500).json({
                message: 'Có lỗi xảy ra khi lấy chi tiết sản phẩm',
                error: error.message
            });
        }
    }

    // Lấy sản phẩm theo giới tính (Nam/Nữ) với bộ lọc nâng cao
    async getProductsByGender(req, res) {
        try {
            const {
                targetID,
                page = 1,
                limit = 12,
                sort = '-createdAt',
                categories,
                minPrice,
                maxPrice,
                search,
            } = req.query;

            // Xây dựng query cơ bản
            const baseQuery = {
                isActivated: true,
                targetID: parseInt(targetID)
            };

            // Xử lý lọc theo nhiều danh mục
            if (categories && categories !== '') {
                const categoryNames = categories.split(',');
                const categoryDocs = await Category.find({
                    name: { $in: categoryNames }
                });

                if (categoryDocs.length > 0) {
                    const categoryIDs = categoryDocs.map(cat => cat.categoryID);
                    baseQuery.categoryID = { $in: categoryIDs };
                }
            }

            // Xử lý lọc theo khoảng giá
            if (minPrice || maxPrice) {
                baseQuery.price = {};
                if (minPrice) baseQuery.price.$gte = parseInt(minPrice);
                if (maxPrice) baseQuery.price.$lte = parseInt(maxPrice);
            }

            // Xử lý tìm kiếm theo tên
            if (search) {
                baseQuery.$text = { $search: search };
            }

            // Thực hiện query với phân trang
            const products = await Product.find(baseQuery)
                .sort(sort)
                .skip((page - 1) * limit)
                .limit(parseInt(limit))
                .populate('categoryInfo')
                .populate('targetInfo');

            // Lấy ngày hiện tại để kiểm tra khuyến mãi
            const currentDate = new Date();

            // Xử lý thông tin chi tiết cho từng sản phẩm
            const enhancedProducts = await Promise.all(products.map(async (product) => {
                // Lấy thông tin màu sắc và kích thước
                const colors = await ProductColor.find({ productID: product.productID });
                const colorsWithSizes = await Promise.all(colors.map(async (color) => {
                    const sizes = await ProductSizeStock.find({ colorID: color.colorID });

                    // Xử lý images cho từng màu sắc sử dụng cloudinary
                    const imagesPromises = color.images.map(async img => await getImageLink(img));
                    const images = await Promise.all(imagesPromises);

                    return {
                        ...color.toObject(),
                        images: images || [],
                        sizes
                    };
                }));

                // Tính tổng tồn kho
                const totalStock = colorsWithSizes.reduce((total, color) => (
                    total + color.sizes.reduce((sum, size) => sum + size.stock, 0)
                ), 0);

                // Lấy thông tin khuyến mãi
                const activePromotion = await Promotion.findOne({
                    $or: [
                        { products: product._id },
                        { categories: product.categoryInfo?.name }
                    ],
                    startDate: { $lte: currentDate },
                    endDate: { $gte: currentDate },
                    status: 'active'
                }).sort({ discountPercent: -1 });

                // Tính giá sau khuyến mãi
                let promotionInfo = null;
                if (activePromotion) {
                    const priceNumber = parseInt(product.price.toString().replace(/\./g, ''));
                    const discountedPrice = Math.round(priceNumber * (1 - activePromotion.discountPercent / 100));
                    promotionInfo = {
                        discountPercent: activePromotion.discountPercent,
                        discountedPrice: discountedPrice.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".")
                    };
                }

                return {
                    _id: product._id,
                    productID: product.productID,
                    name: product.name,
                    price: product.price,
                    thumbnail: await getImageLink(product.thumbnail),
                    category: product.categoryInfo?.name,
                    target: product.targetInfo?.name,
                    colors: colorsWithSizes,
                    totalStock,
                    inStock: totalStock > 0,
                    promotion: promotionInfo
                };
            }));

            // Tính toán phân trang
            const total = await Product.countDocuments(baseQuery);
            const totalPages = Math.ceil(total / limit);

            res.json({
                success: true,
                data: {
                    products: enhancedProducts,
                    pagination: {
                        total,
                        totalPages,
                        currentPage: parseInt(page),
                        pageSize: parseInt(limit)
                    }
                }
            });

        } catch (error) {
            console.error('Error in getProductsByGender:', error);
            res.status(500).json({
                success: false,
                message: 'Internal server error',
                error: error.message
            });
        }
    }

    // Thêm method mới để xử lý AI chat
    async getAIProductRecommendation(req, res) {
        try {
            const { query } = req.body;

            // Lấy danh sách sản phẩm đang giảm giá
            const productsWithPromotion = await Product.find({
                isActivated: true,
                'promotion': { $exists: true, $ne: null }
            })
                .populate('targetInfo')
                .populate('categoryInfo')
                .limit(10);

            // Lấy danh sách sản phẩm mới nhất
            const newProducts = await Product.find({ isActivated: true })
                .sort('-createdAt')
                .populate('targetInfo')
                .populate('categoryInfo')
                .limit(5);

            // Lấy danh sách sản phẩm bán chạy (giả sử có trường soldCount)
            const bestSellers = await Product.find({ isActivated: true })
                .sort('-soldCount')
                .populate('targetInfo')
                .populate('categoryInfo')
                .limit(5);

            // Tạo context cho AI từ thông tin sản phẩm
            const context = {
                promotions: productsWithPromotion.map(product => ({
                    id: product.productID,
                    name: product.name,
                    originalPrice: product.price,
                    discountedPrice: product.promotion?.discountedPrice,
                    discountPercent: product.promotion?.discountPercent,
                    category: product.categoryInfo?.name,
                    target: product.targetInfo?.name
                })),
                newArrivals: newProducts.map(product => ({
                    id: product.productID,
                    name: product.name,
                    price: product.price,
                    category: product.categoryInfo?.name,
                    target: product.targetInfo?.name
                })),
                bestSellers: bestSellers.map(product => ({
                    id: product.productID,
                    name: product.name,
                    price: product.price,
                    category: product.categoryInfo?.name,
                    target: product.targetInfo?.name,
                    soldCount: product.soldCount
                }))
            };

            // Tạo prompt cho AI
            const prompt = `
            Bạn là trợ lý AI của cửa hàng thời trang. Dựa trên dữ liệu sau:

            THÔNG TIN SẢN PHẨM ĐANG GIẢM GIÁ:
            ${JSON.stringify(context.promotions, null, 2)}

            THÔNG TIN SẢN PHẨM MỚI:
            ${JSON.stringify(context.newArrivals, null, 2)}

            THÔNG TIN SẢN PHẨM BÁN CHẠY:
            ${JSON.stringify(context.bestSellers, null, 2)}

            Hãy trả lời câu hỏi của khách hàng: "${query}"

            Yêu cầu:
            1. Trả lời bằng tiếng Việt, thân thiện
            2. Nếu khách hỏi về sản phẩm giảm giá:
               - Liệt kê các sản phẩm đang giảm giá
               - Nêu rõ phần trăm giảm và giá sau giảm
               - Đề xuất sản phẩm có mức giảm tốt nhất
            3. Nếu khách hỏi về sản phẩm mới:
               - Giới thiệu các sản phẩm mới nhất
               - Nhấn mạnh tính thời trang và xu hướng
            4. Nếu khách hỏi về sản phẩm bán chạy:
               - Giới thiệu các sản phẩm bán chạy nhất
               - Nêu rõ lý do sản phẩm được ưa chuộng
            5. Nếu không có thông tin, gợi ý khách liên hệ hotline
            6. Giới hạn câu trả lời trong 200 từ

            Format câu trả lời:
            - Lời chào
            - Nội dung trả lời
            - Đề xuất sản phẩm cụ thể
            - Lời kết
            `;

            // Gọi API Gemini
            const result = await model.generateContent(prompt);
            const response = result.response.text();

            res.json({
                success: true,
                message: 'AI đã trả lời thành công',
                response: response
            });

        } catch (error) {
            console.error('Error in AI recommendation:', error);
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi xử lý AI',
                error: error.message
            });
        }
    }

    // Thêm method để lấy gợi ý phối đồ
    async getAIOutfitSuggestion(req, res) {
        try {
            const { productID } = req.params;

            // Lấy thông tin sản phẩm
            const product = await Product.findOne({ productID })
                .populate('targetInfo')
                .populate('categoryInfo');

            if (!product) {
                return res.status(404).json({
                    message: 'Không tìm thấy sản phẩm'
                });
            }

            // Tạo prompt cho AI
            const prompt = `
            Bạn là chuyên gia thời trang. Hãy gợi ý cách phối đồ với sản phẩm sau:
            Tên: ${product.name}
            Loại: ${product.categoryInfo?.name}
            Đối tượng: ${product.targetInfo?.name}
            Mô tả: ${product.description}

            Yêu cầu:
            1. Đề xuất 3 cách phối đồ khác nhau
            2. Mỗi cách phối cần có:
               - Các món đồ kết hợp
               - Phụ kiện phù hợp
               - Dịp phù hợp để mặc
            3. Thêm lời khuyên về màu sắc
            4. Viết bằng tiếng Việt, dễ hiểu
            `;

            // Gọi API Gemini
            const result = await model.generateContent(prompt);
            const response = result.response.text();

            res.json({
                success: true,
                message: 'Đã tạo gợi ý phối đồ thành công',
                response: response
            });

        } catch (error) {
            console.error('Error in outfit suggestion:', error);
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi tạo gợi ý phối đồ',
                error: error.message
            });
        }
    }

    // Thêm method để train AI
    async trainAI(req, res) {
        try {
            const prompt = `
            Tôi sẽ cung cấp cho bạn dữ liệu training về shop thời trang. 
            Hãy học và ghi nhớ những thông tin này để tư vấn cho khách hàng:

            ${JSON.stringify(trainingData, null, 2)}

            Yêu cầu:
            1. Học cách trả lời các câu hỏi về size, chất liệu, phối đồ và bảo quản
            2. Trả lời thân thiện, dễ hiểu
            3. Đưa ra gợi ý cụ thể dựa trên dữ liệu sản phẩm của shop
            4. Nếu không chắc chắn, gợi ý khách liên hệ trực tiếp
            `;

            const result = await model.generateContent(prompt);
            const response = result.response.text();

            res.json({
                success: true,
                message: 'AI đã được train thành công',
                response: response
            });

        } catch (error) {
            console.error('Error training AI:', error);
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi train AI',
                error: error.message
            });
        }
    }

    // Thêm method để cập nhật dữ liệu training
    async updateTrainingData(req, res) {
        try {
            const { category, data } = req.body;

            if (!category || !data) {
                return res.status(400).json({
                    message: 'Thiếu thông tin category hoặc data'
                });
            }

            // Cập nhật dữ liệu training
            trainingData[category].push(...data);

            // Train lại AI với dữ liệu mới
            await this.trainAI(req, res);

        } catch (error) {
            console.error('Error updating training data:', error);
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi cập nhật dữ liệu training',
                error: error.message
            });
        }
    }

    // Thêm method mới để lấy sản phẩm theo category
    async getProductsByCategory(req, res) {
        try {
            const { categoryID } = req.params;
            const { page = 1, limit = 12, sort = '-createdAt' } = req.query;

            // Kiểm tra category tồn tại
            const category = await Category.findOne({ categoryID: parseInt(categoryID) });
            if (!category) {
                return res.status(404).json({
                    success: false,
                    message: 'Không tìm thấy danh mục'
                });
            }

            // Xây dựng query cơ bản
            const query = {
                categoryID: parseInt(categoryID),
                isActivated: true
            };

            // Xử lý sắp xếp
            let sortOptions = {};
            switch (sort) {
                case 'price-asc':
                    sortOptions.price = 1;
                    break;
                case 'price-desc':
                    sortOptions.price = -1;
                    break;
                case 'name-asc':
                    sortOptions.name = 1;
                    break;
                case 'name-desc':
                    sortOptions.name = -1;
                    break;
                case 'newest':
                    sortOptions.createdAt = -1;
                    break;
                case 'oldest':
                    sortOptions.createdAt = 1;
                    break;
                default:
                    sortOptions.createdAt = -1;
            }

            // Thực hiện query với phân trang
            const products = await Product.find(query)
                .sort(sortOptions)
                .skip((page - 1) * limit)
                .limit(parseInt(limit))
                .populate('targetInfo', 'name')
                .populate('categoryInfo', 'name');

            // Lấy ngày hiện tại để kiểm tra khuyến mãi
            const currentDate = new Date();

            // Xử lý thông tin chi tiết cho từng sản phẩm
            const enhancedProducts = await Promise.all(products.map(async (product) => {
                // Lấy thông tin màu sắc và kích thước
                const colors = await ProductColor.find({ productID: product.productID });
                const colorsWithSizes = await Promise.all(colors.map(async (color) => {
                    const sizes = await ProductSizeStock.find({ colorID: color.colorID });

                    // Xử lý images cho từng màu sắc
                    const imagesPromises = color.images.map(async img => await getImageLink(img));
                    const images = await Promise.all(imagesPromises);

                    return {
                        colorID: color.colorID,
                        colorName: color.colorName,
                        images: images || [],
                        sizes: sizes.map(size => ({
                            size: size.size,
                            stock: size.stock
                        }))
                    };
                }));

                // Tính tổng tồn kho
                const totalStock = colorsWithSizes.reduce((total, color) =>
                    total + color.sizes.reduce((sum, size) => sum + size.stock, 0), 0);

                // Tìm khuyến mãi đang áp dụng
                const activePromotion = await Promotion.findOne({
                    $or: [
                        { products: product._id },
                        { categories: product.categoryInfo.name }
                    ],
                    startDate: { $lte: currentDate },
                    endDate: { $gte: currentDate },
                    status: 'active'
                }).sort({ discountPercent: -1 });

                // Tính giá sau khuyến mãi
                let promotionDetails = null;
                if (activePromotion) {
                    const priceNumber = parseInt(product.price.replace(/\./g, ''));
                    const discountedValue = Math.round(priceNumber * (1 - activePromotion.discountPercent / 100));
                    promotionDetails = {
                        name: activePromotion.name,
                        discountPercent: activePromotion.discountPercent,
                        discountedPrice: discountedValue.toLocaleString('vi-VN'),
                        endDate: activePromotion.endDate
                    };
                }

                return {
                    productID: product.productID,
                    name: product.name,
                    price: product.price,
                    description: product.description,
                    thumbnail: await getImageLink(product.thumbnail),
                    category: product.categoryInfo.name,
                    target: product.targetInfo.name,
                    colors: colorsWithSizes,
                    totalStock,
                    inStock: totalStock > 0,
                    promotion: promotionDetails
                };
            }));

            // Đếm tổng số sản phẩm
            const total = await Product.countDocuments(query);

            // Thống kê bổ sung cho category
            const stats = {
                totalProducts: total,
                inStockProducts: enhancedProducts.filter(p => p.inStock).length,
                outOfStockProducts: enhancedProducts.filter(p => !p.inStock).length,
                productsOnPromotion: enhancedProducts.filter(p => p.promotion).length,
                averagePrice: enhancedProducts.length > 0
                    ? Math.round(enhancedProducts.reduce((sum, p) =>
                        sum + parseInt(p.price.replace(/\./g, '')), 0) / enhancedProducts.length)
                    : 0
            };

            res.json({
                success: true,
                category: {
                    id: category.categoryID,
                    name: category.name,
                    description: category.description,
                    imageURL: await getImageLink(category.imageURL)
                },
                products: enhancedProducts,
                stats,
                pagination: {
                    total,
                    totalPages: Math.ceil(total / limit),
                    currentPage: parseInt(page),
                    limit: parseInt(limit)
                }
            });

        } catch (error) {
            console.error('Error in getProductsByCategory:', error);
            res.status(500).json({
                success: false,
                message: 'Có lỗi xảy ra khi lấy danh sách sản phẩm theo danh mục',
                error: error.message
            });
        }
    }

    // Thêm method để lấy tất cả sản phẩm được nhóm theo danh mục
    async getAllProductsByCategories(req, res) {
        try {
            // Lấy tất cả danh mục
            const categories = await Category.find().sort({ categoryID: 1 });

            // Lấy ngày hiện tại để kiểm tra khuyến mãi
            const currentDate = new Date();

            // Xử lý từng danh mục và sản phẩm của nó
            const categoriesWithProducts = await Promise.all(
                categories.map(async (category) => {
                // Lấy sản phẩm theo danh mục
                const products = await Product.find({
                    categoryID: category.categoryID,
                        isActivated: true,
                })
                        .populate("targetInfo", "name")
                    .sort({ createdAt: -1 }); // Sắp xếp theo thời gian tạo mới nhất

                // Xử lý chi tiết cho từng sản phẩm
                    const enhancedProducts = await Promise.all(
                        products.map(async (product) => {
                    // Lấy thông tin màu sắc và kích thước
                            const colors = await ProductColor.find({
                                productID: product.productID,
                            });
                            const colorsWithSizes = await Promise.all(
                                colors.map(async (color) => {
                                    const sizes = await ProductSizeStock.find({
                                        colorID: color.colorID,
                                    });
                        return {
                            colorID: color.colorID,
                            colorName: color.colorName,
                                        sizes: sizes.map((size) => ({
                                size: size.size,
                                            stock: size.stock,
                                        })),
                        };
                                })
                            );

                    // Tính tổng tồn kho
                            const totalStock = colorsWithSizes.reduce(
                                (total, color) =>
                                    total +
                                    color.sizes.reduce((sum, size) => sum + size.stock, 0),
                                0
                            );

                    // Tìm khuyến mãi đang áp dụng
                    const activePromotion = await Promotion.findOne({
                                $or: [{ products: product._id }, { categories: category.name }],
                        startDate: { $lte: currentDate },
                        endDate: { $gte: currentDate },
                                status: "active",
                    }).sort({ discountPercent: -1 });

                    // Tính giá sau khuyến mãi
                    let promotionDetails = null;
                    if (activePromotion) {
                                const priceNumber = parseInt(product.price.replace(/\./g, ""));
                                const discountedValue = Math.round(
                                    priceNumber * (1 - activePromotion.discountPercent / 100)
                                );
                        promotionDetails = {
                            name: activePromotion.name,
                            discountPercent: activePromotion.discountPercent,
                                    discountedPrice: discountedValue.toLocaleString("vi-VN"),
                                    endDate: activePromotion.endDate,
                        };
                    }

                    return {
                        productID: product.productID,
                        name: product.name,
                        price: product.price,
                        thumbnail: await getImageLink(product.thumbnail),
                        target: product.targetInfo.name,
                        totalStock,
                        inStock: totalStock > 0,
                                promotion: promotionDetails,
                    };
                        })
                    );

                // Thống kê cho danh mục
                const categoryStats = {
                    totalProducts: enhancedProducts.length,
                        inStockProducts: enhancedProducts.filter((p) => p.inStock).length,
                        outOfStockProducts: enhancedProducts.filter((p) => !p.inStock)
                            .length,
                        productsOnPromotion: enhancedProducts.filter((p) => p.promotion)
                            .length,
                };

                return {
                    categoryID: category.categoryID,
                    name: category.name,
                    description: category.description,
                    imageURL: await getImageLink(category.imageURL),
                    stats: categoryStats,
                        products: enhancedProducts,
                };
                })
            );

            res.json({
                success: true,
                categories: categoriesWithProducts,
            });
        } catch (error) {
            console.error("Error in getAllProductsByCategories:", error);
            res.status(500).json({
                success: false,
                message: "Có lỗi xảy ra khi lấy danh sách sản phẩm theo danh mục",
                error: error.message,
            });
        }
    }

    //!ADMIN
    // Lấy danh sách sản phẩm cho ADMIN bao gồm
    // "product" + "stats : tổng sp , sp nam , sp nữ"
    async getProductsChoADMIN(req, res) {
        try {
            // Sử dụng aggregation để lấy và chuyển đổi dữ liệu trực tiếp
            const products = await Product.aggregate([
                {
                    $lookup: {
                        from: "categories",
                        localField: "categoryID",
                        foreignField: "categoryID",
                        as: "category",
                    },
                },
                {
                    $lookup: {
                        from: "targets",
                        localField: "targetID",
                        foreignField: "targetID",
                        as: "target",
                    },
                },
                {
                    $project: {
                        _id: 1,
                        productID: 1,
                        name: 1,
                        price: 1,
                        createdAt: 1,
                        thumbnail: 1,
                        inStock: 1,
                        isActivated: 1,
                        category: { $arrayElemAt: ["$category.name", 0] },
                        target: { $arrayElemAt: ["$target.name", 0] },
                        description: 1,
                    },
                },
            ]);

            // Xử lý thumbnail với Cloudinary
            const productsWithCloudinary = await Promise.all(
                products.map(async (product) => ({
                ...product,
                    thumbnail: await getImageLink(product.thumbnail),
                }))
            );

            // Tính toán thống kê
            const stats = {
                totalMaleProducts: products.filter((p) => p.target === "Nam").length,
                totalFemaleProducts: products.filter((p) => p.target === "Nữ").length,
                totalDeactivatedProducts: products.filter((p) => !p.isActivated).length,
                total: products.length,
            };

            res.json({
                products: productsWithCloudinary,
                stats,
            });
        } catch (error) {
            console.log(error);
            res.status(500).json({
                message: "Có lỗi xảy ra khi lấy danh sách sản phẩm",
                error: error.message,
            });
        }
    }

    //!ADMIN
    // Lấy chi tiết sản phẩm theo ID có cloudinary
    async getProductByIdChoADMIN(req, res) {
        try {
            const { id } = req.params;

            // Lấy thông tin cơ bản của sản phẩm, sử dụng productID thay vì _id
            const product = await Product.findOne({ productID: id })
                .populate("targetInfo", "name")
                .populate("categoryInfo", "name");

            if (!product) {
                return res.status(404).json({
                    message: "Không tìm thấy sản phẩm",
                });
            }

            // Lấy tất cả màu của sản phẩm
            const colors = await ProductColor.find({ productID: product.productID });

            // Lấy thông tin size và tồn kho cho từng màu
            const colorsWithSizes = await Promise.all(
                colors.map(async (color) => {
                    const sizes = await ProductSizeStock.find({
                        colorID: color.colorID,
                    }).select("size stock SKU");

                // Xử lý hình ảnh cho từng màu sắc
                    const imagesPromises = color.images.map(
                        async (img) => await getImageLink(img)
                    );
                const images = await Promise.all(imagesPromises);

                return {
                    colorID: color.colorID,
                    colorName: color.colorName,
                    images: images || [],
                        sizes: sizes.map((size) => ({
                        size: size.size,
                            stock: size.stock,
                            SKU: size.SKU,
                        })),
                };
                })
            );

            // Lấy promotion đang active cho sản phẩm
            const currentDate = new Date();
            const activePromotion = await Promotion.findOne({
                $or: [
                    { products: product._id },
                    { categories: product.categoryInfo.name },
                ],
                startDate: { $lte: currentDate },
                endDate: { $gte: currentDate },
                status: "active",
            }).sort({ discountPercent: -1 }); // Lấy promotion có giảm giá cao nhất

            // Tính giá sau khuyến mãi nếu có
            let discountedPrice = null;
            if (activePromotion) {
                // Chuyển đổi giá từ string sang number, loại bỏ dấu chấm
                const priceNumber = Number(product.price.replace(/\./g, ""));
                // Tính toán giá sau khuyến mãi
                const discountedNumber = Math.round(
                    priceNumber * (1 - activePromotion.discountPercent / 100)
                );
                // Chuyển đổi lại thành định dạng VN
                discountedPrice = discountedNumber
                    .toString()
                    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
            }

            // Format lại dữ liệu trước khi gửi về client
            const formattedProduct = {
                _id: product._id,
                productID: product.productID,
                name: product.name,
                description: product.description,
                price: product.price,
                category: product.categoryInfo?.name,
                target: product.targetInfo?.name,
                thumbnail: await getImageLink(product.thumbnail),
                colors: colorsWithSizes,
                promotion: activePromotion
                    ? {
                    name: activePromotion.name,
                    description: activePromotion.description,
                    discountPercent: activePromotion.discountPercent,
                    discountedPrice: discountedPrice,
                        endDate: activePromotion.endDate,
                    }
                    : null,
                // Tính toán các thông tin bổ sung
                totalStock: colorsWithSizes.reduce(
                    (total, color) =>
                        total + color.sizes.reduce((sum, size) => sum + size.stock, 0),
                    0
                ),
                availableSizes: [
                    ...new Set(
                        colorsWithSizes.flatMap((color) =>
                            color.sizes.map((size) => size.size)
                        )
                    ),
                ].sort(),
                availableColors: colorsWithSizes.map((color) => color.colorName),
            };

            res.json({
                success: true,
                product: formattedProduct,
            });
        } catch (error) {
            console.error("Error in getProductById:", error);
            res.status(500).json({
                message: "Có lỗi xảy ra khi lấy chi tiết sản phẩm",
                error: error.message,
            });
        }
    }

    //!ADMIN
    // Cập nhật sản phẩm
    async updateProduct(req, res) {
        try {
            const { id } = req.params;
            const updateData = req.body;
            const thumbnailFile = req.files?.thumbnail;

            // Kiểm tra sản phẩm tồn tại
            const product = await Product.findOne({ productID: id });
            if (!product) {
                return res.status(404).json({ message: "Không tìm thấy sản phẩm" });
            }

            // Nếu cập nhật target hoặc category, kiểm tra tồn tại
            if (updateData.targetID || updateData.categoryID) {
                const [target, category] = await Promise.all([
                    updateData.targetID
                        ? Target.findOne({ targetID: updateData.targetID })
                        : Promise.resolve(true),
                    updateData.categoryID
                        ? Category.findOne({ categoryID: updateData.categoryID })
                        : Promise.resolve(true),
                ]);

                if (!target || !category) {
                    return res.status(400).json({
                        message: "Target hoặc Category không tồn tại",
                    });
                }
            }

            // Xử lý upload thumbnail mới nếu có
            if (thumbnailFile) {
                const thumbnailResult = await uploadImage(thumbnailFile);
                if (!thumbnailResult.success) {
                    return res.status(400).json({
                        message: "Lỗi khi upload ảnh thumbnail",
                    });
                }
                updateData.thumbnail = thumbnailResult.publicId;
            }

            // Chỉ cập nhật các thông tin chung của sản phẩm
            const allowedUpdates = {
                name: updateData.name,
                description: updateData.description,
                price: updateData.price,
                targetID: updateData.targetID,
                categoryID: updateData.categoryID,
                isActivated: updateData.isActivated,
                thumbnail: updateData.thumbnail, // Thêm thumbnail vào danh sách cập nhật
            };

            // Lọc bỏ các giá trị undefined
            Object.keys(allowedUpdates).forEach(
                (key) => allowedUpdates[key] === undefined && delete allowedUpdates[key]
            );

            // Cập nhật thông tin sản phẩm
            Object.assign(product, allowedUpdates);
            await product.save();

            // Lấy sản phẩm đã cập nhật với đầy đủ thông tin
            const updatedProduct = await Product.findOne({ productID: id })
                .populate("targetInfo", "name")
                .populate("categoryInfo", "name");

            // Xử lý thumbnail URL trước khi trả về
            const productWithThumbnail = {
                ...updatedProduct.toObject(),
                thumbnail: await getImageLink(updatedProduct.thumbnail),
            };

            res.json({
                message: "Cập nhật sản phẩm thành công",
                product: productWithThumbnail,
            });
        } catch (error) {
            res.status(500).json({
                message: "Có lỗi xảy ra khi cập nhật sản phẩm",
                error: error.message,
            });
        }
    }

    //!ADMIN
    // Tạo sản phẩm mới
    async createProduct(req, res) {
        try {
            console.log("=== DEBUG CREATE PRODUCT ===");
            console.log("Request body:", req.body);

            const {
                name,
                price,
                description,
                thumbnail,
                categoryID,
                targetID,
                colors,
            } = req.body;

            // Kiểm tra dữ liệu đầu vào
            if (
                !name ||
                !price ||
                !description ||
                !thumbnail ||
                !categoryID ||
                !targetID
            ) {
                return res.status(400).json({
                    message: "Vui lòng điền đầy đủ thông tin sản phẩm",
                });
            }

            // Kiểm tra target và category tồn tại
            const [target, category] = await Promise.all([
                Target.findOne({ targetID: targetID }),
                Category.findOne({ categoryID: categoryID }),
            ]);

            if (!target || !category) {
                return res.status(400).json({
                    message: "Target hoặc Category không tồn tại",
                });
            }

            // Tạo productID mới
            const lastProduct = await Product.findOne().sort({ productID: -1 });
            const newProductID = lastProduct ? lastProduct.productID + 1 : 1;

            // Tạo sản phẩm mới
            const newProduct = new Product({
                productID: newProductID,
                name,
                price: Number(price),
                description,
                thumbnail,
                categoryID: category.categoryID,
                targetID: target.targetID,
                isActivated: true,
            });

            // Lưu sản phẩm
            const savedProduct = await newProduct.save();
            console.log("Saved product:", savedProduct);

            // Xử lý màu sắc và size nếu có
            if (colors && colors.length > 0) {
                // Tạo colorID mới
                const lastColor = await ProductColor.findOne().sort({ colorID: -1 });
                let nextColorID = lastColor ? lastColor.colorID + 1 : 1;

                // Tìm sizeStockID cuối cùng
                const lastSizeStock = await ProductSizeStock.findOne().sort({
                    sizeStockID: -1,
                });
                let nextSizeStockID = lastSizeStock ? lastSizeStock.sizeStockID + 1 : 1;

                for (const color of colors) {
                    // Tạo màu mới
                    const newColor = new ProductColor({
                        colorID: nextColorID,
                        productID: newProductID,
                        colorName: color.colorName,
                        images: color.images,
                    });
                    const savedColor = await newColor.save();

                    // Tạo size stocks cho màu này
                    if (color.sizes && color.sizes.length > 0) {
                        const sizeStocks = color.sizes.map((size) => {
                            const sizeStockID = nextSizeStockID++;
                            return {
                                sizeStockID,
                                SKU: `${newProductID}_${nextColorID}_${size.size}_${sizeStockID}`,
                                colorID: savedColor.colorID,
                                size: size.size,
                                stock: size.stock,
                            };
                        });

                        await ProductSizeStock.insertMany(sizeStocks);
                    }

                    nextColorID++;
                }
            }

            // Lấy sản phẩm đã tạo với đầy đủ thông tin
            const createdProduct = await Product.findOne({ productID: newProductID })
                .populate("targetInfo", "name")
                .populate("categoryInfo", "name");

            // Xử lý thumbnail URL trước khi trả về
            const productWithThumbnail = {
                ...createdProduct.toObject(),
                thumbnail: await getImageLink(createdProduct.thumbnail),
            };

            console.log("=== END DEBUG ===");

            res.status(201).json({
                message: "Thêm sản phẩm mới thành công",
                product: productWithThumbnail,
            });
        } catch (error) {
            console.error("Error in createProduct:", error);
            res.status(500).json({
                message: "Có lỗi xảy ra khi thêm sản phẩm mới",
                error: error.message,
            });
        }
    }

    //!Toàn thêm
    // Xóa sản phẩm
    async deleteProduct(req, res) {
        try {
            const { id } = req.params;

            // Tìm sản phẩm
            const product = await Product.findOne({ productID: id });
            if (!product) {
                return res.status(404).json({ message: "Không tìm thấy sản phẩm" });
            }

            // Tìm tất cả colorID của sản phẩm trước khi xóa
            const colors = await ProductColor.find({ productID: id });
            const colorIDs = colors.map((color) => color.colorID);

            // Xóa tất cả size-stock liên quan đến các màu
            await ProductSizeStock.deleteMany({ colorID: { $in: colorIDs } });

            // Xóa tất cả màu sắc liên quan
            await ProductColor.deleteMany({ productID: id });

            // Xóa sản phẩm chính
            await Product.deleteOne({ productID: id });

            res.json({
                message: "Đã xóa hoàn toàn sản phẩm và dữ liệu liên quan",
            });
        } catch (error) {
            console.error("Error in deleteProduct:", error);
            res.status(500).json({
                message: "Có lỗi xảy ra khi xóa sản phẩm",
                error: error.message,
            });
        }
    }

    //!ADMIN
    // Kích hoạt/Vô hiệu hóa sản phẩm
    async toggleProductStatus(req, res) {
        try {
            const { id } = req.params;

            // Tìm sản phẩm
            const product = await Product.findOne({ productID: id });
            if (!product) {
                return res.status(404).json({ message: "Không tìm thấy sản phẩm" });
            }

            // Đảo ngược trạng thái isActivated
            product.isActivated = !product.isActivated;
            await product.save();

            res.json({
                message: `Đã ${product.isActivated ? "kích hoạt" : "vô hiệu hóa"
                    } sản phẩm thành công`,
                isActivated: product.isActivated,
            });
        } catch (error) {
            console.error("Error in toggleProductStatus:", error);
            res.status(500).json({
                message: "Có lỗi xảy ra khi thay đổi trạng thái sản phẩm",
                error: error.message,
            });
        }
    }
}

module.exports = new ProductController();