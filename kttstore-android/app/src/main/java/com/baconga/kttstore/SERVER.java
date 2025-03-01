package com.baconga.kttstore;

public class SERVER {
    //Nha
    public static String BASE_URL = "https://3368-103-249-21-213.ngrok-free.app";

//    ADDRESS
    public static String addresses = BASE_URL + "/api/address";
    public static String get_addresses = addresses + "/";
    public static String get_address_by_id = addresses + "/:id";
    public static String add_address = addresses + "/";
    public static String update_address = addresses + "/:id";
    public static String delete_address = addresses + "/:id";
    public static String set_default_address = addresses + "/:id/default";

//    AUTH
    public static String auth = BASE_URL + "/api/auth";
    public static String register = auth + "/register";
    public static String login = auth + "/login";
    public static String forgot_password = auth + "/forgot-password";
    public static String reset_password = auth + "/reset-password";
    public static String verify_token = auth + "/verify-token";

//    CART
    public static String cart = BASE_URL + "/api/cart";
    public static String get_cart = cart + "/";
    public static String add_to_cart = cart + "/add";
    public static String update_cart = cart + "/:id";
    public static String delete_cart = cart + "/:id";
    public static String clear_cart = cart + "/";

//    CATEGORY
    public static String categories = BASE_URL + "/api/categories";
    public static String get_categories = categories + "/";
    public static String get_category_by_id = categories + "/:id";

//    COUPON
    public static String coupons = BASE_URL + "/api/coupon";
    public static String get_available_coupons = coupons + "/available";
    public static String apply_coupon = coupons + "/apply";
    public static String get_coupon_history = coupons + "/history";

//    !FAVORITE
    public static String favorites = BASE_URL + "/api/favorite";
    public static String get_favorites = favorites + "/";
    public static String add_to_favorites = favorites + "/add";
    public static String update_favorite = favorites + "/:id";
    public static String delete_favorite = favorites + "/:id";
    public static String check_favorite = favorites + "/check/:productID";
    public static String remove_favorite_by_product_id = favorites + "/product/:productID";

//    NOTIFICATION
    public static String notifications = BASE_URL + "/api/notification";
    public static String get_notifications = notifications + "/";
    public static String get_unread_count = notifications + "/unread/count";
    public static String mark_as_read = notifications + "/read/:id";
    public static String mark_all_as_read = notifications + "/read-all";

//    ORDER
    public static String order = BASE_URL + "/api/order";
    public static String get_orders = order + "/my-orders";
    public static String get_order_by_id = order + "/my-orders/:orderId";
    public static String create_order = order + "/create";
    public static String cancel_order = order + "/cancel/:id";

//    ORDER DETAIL
    public static String order_detail = BASE_URL + "/api/order-detail";
    public static String get_order_details = order_detail + "/order/:orderID";
    public static String get_order_detail_by_id = order_detail + "/order/:orderID/detail/:id";

//    PRODUCT
    public static String products = BASE_URL + "/api/products";
    public static String get_products = products + "/";
    public static String get_products_basic_info = products + "/basic";
    public static String get_products_by_gender = products + "/gender";
    public static String get_product_by_id = products + "/:id";

//    PRODUCT COLOR
    public static String product_color = BASE_URL + "/api/product-color";
    public static String get_product_colors = product_color + "/product/:productID";
    public static String get_color_by_id = product_color + "/:id";

//    PRODUCT SIZE STOCK
    public static String product_size_stock = BASE_URL + "/api/product-size-stock";
    public static String get_stock_by_sku = product_size_stock + "/sku/:SKU";
    public static String get_stock_by_color = product_size_stock + "/color/:colorID";

//    PROMOTION
    public static String promotions = BASE_URL + "/api/promotions";
    public static String get_active_flash_sale = promotions + "/flash-sale/active";
    public static String get_upcoming_flash_sale = promotions + "/flash-sale/upcoming";
    public static String get_flash_sale_products = promotions + "/flash-sale/products";
    public static String get_active_promotions = promotions + "/active";
    public static String get_promotion_by_id = promotions + "/:promotionID";
    public static String get_promotions_for_product = promotions + "/product/:productId";

//    REVIEW
    public static String reviews = BASE_URL + "/api/reviews";
    public static String get_reviews_by_product = reviews + "/product/:productID";
    public static String create_review = reviews + "/";
    public static String update_review = reviews + "/:reviewID";
    public static String delete_review = reviews + "/:reviewID";
    public static String get_reviews_by_user = reviews + "/user";

//    TARGET
    public static String targets = BASE_URL + "/api/target";
    public static String get_targets = targets + "/";
    public static String get_target_by_id = targets + "/:id";

//    USER COUPON
    public static String user_coupon = BASE_URL + "/api/user-coupon";
    public static String get_user_coupons = user_coupon + "/my-coupons";
    public static String get_user_coupon_by_id = user_coupon + "/my-coupons/:id";
    public static String use_user_coupon = user_coupon + "/apply";
    public static String get_user_coupon_available = user_coupon + "/available";

//    USER NOTIFICATION
    public static String user_notification = BASE_URL + "/api/user-notification";
    public static String get_user_notifications = user_notification + "/";
    public static String mark_user_notification_as_read = user_notification + "/:userNotificationID/read";
    public static String mark_all_user_notification_as_read = user_notification + "/read-all";
    public static String get_unread_user_notification_count = user_notification + "/unread/count";

//    USER
    public static String user = BASE_URL + "/api/user";
    public static String get_profile = user + "/profile";
    public static String update_profile = user + "/profile";
    public static String change_password = user + "/change-password";
}
