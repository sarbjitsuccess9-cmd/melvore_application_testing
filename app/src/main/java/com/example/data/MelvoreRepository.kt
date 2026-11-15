package com.example.data

import android.content.Context
import com.example.model.AboutSection
import com.example.model.Address
import com.example.model.BlogPost
import com.example.model.BrandItem
import com.example.model.CartItem
import com.example.model.CartSummary
import com.example.model.Category
import com.example.model.CollectionItem
import com.example.model.CouponInfo
import com.example.model.DesignBundle
import com.example.model.FaqItem
import com.example.model.LocalPreferences
import com.example.model.Order
import com.example.model.Product
import com.example.model.RazorpayCheckoutPayload
import com.example.model.SupportTicket
import com.example.model.TrendingTag
import com.example.model.User
import com.example.network.AddressDeleteRequest
import com.example.network.AddressSaveRequest
import com.example.network.ApiResult
import com.example.network.CartAddRequest
import com.example.network.CartRemoveRequest
import com.example.network.CartUpdateRequest
import com.example.network.ContactRequest
import com.example.network.CouponValidateRequest
import com.example.network.DtoMappers
import com.example.network.DtoMappers.toDomain
import com.example.network.DtoMappers.toPayload
import com.example.network.ForgotPasswordRequest
import com.example.network.LoginRequest
import com.example.network.MelvoreApiClient
import com.example.network.OrderCancelRequest
import com.example.network.OrderCreateRequest
import com.example.network.OtpRequest
import com.example.network.PaymentFailRequest
import com.example.network.PaymentInitiateRequest
import com.example.network.PaymentVerifyRequest
import com.example.network.ProfileUpdateRequest
import com.example.network.RegisterRequest
import com.example.network.SocialAuthRequest
import com.example.network.SupportCreateRequest
import com.example.network.SupportReplyRequest
import com.example.network.WishlistMutateRequest

data class CouponEvalResult(
    val valid: Boolean,
    val discountAmount: Double = 0.0,
    val code: String? = null,
    val summary: CartSummary? = null,
    val message: String = ""
)

data class DesignHubData(
    val design: DesignBundle,
    val categories: List<Category>,
    val brands: List<BrandItem>,
    val collections: List<CollectionItem>,
    val trendingTags: List<TrendingTag>,
    val faqs: List<FaqItem>
)

data class CategoriesBundle(
    val categories: List<Category>,
    val brands: List<BrandItem>,
    val collections: List<CollectionItem>
)

data class CartData(
    val items: List<CartItem>,
    val count: Int,
    val summary: CartSummary
)

data class ProfileData(
    val user: User,
    val addresses: List<Address>,
    val cartCount: Int,
    val wishlistCount: Int,
    val pendingOrdersCount: Int
)

data class WishlistData(
    val productIds: List<Int>,
    val products: List<Product>
)

sealed interface RepoResult<out T> {
    data class Success<T>(val data: T, val message: String = "") : RepoResult<T>
    data class Error(
        val message: String,
        val errorCode: String? = null,
        val httpStatus: Int? = null
    ) : RepoResult<Nothing>
}

/**
 * Production repository backed exclusively by the Hostinger PHP/MySQL API
 * (https://app.melvore.co.in) and Android Keystore session storage.
 */
class MelvoreRepository(
    val sessionStore: SecureSessionStore,
    val apiClient: MelvoreApiClient = MelvoreApiClient(sessionStore)
) {
    constructor(context: Context) : this(SecureSessionStore(context.applicationContext))

    fun hasAuthToken(): Boolean = !sessionStore.getAuthToken().isNullOrBlank()
    fun clearAuthToken() = sessionStore.clearAuthToken()
    fun getPreferences(): LocalPreferences = sessionStore.getPreferences()
    fun savePreferences(prefs: LocalPreferences) = sessionStore.savePreferences(prefs)
    fun getApiBaseUrl(): String = sessionStore.getApiBaseUrl().trimEnd('/')
    fun setApiBaseUrl(url: String): String = sessionStore.setApiBaseUrl(url)
    fun getPendingPaymentOrderId(): Int? = sessionStore.getPendingPaymentOrderId()
    fun setPendingPaymentOrderId(orderId: Int?) = sessionStore.setPendingPaymentOrderId(orderId)

    private inline fun <T, R> ApiResult<T>.mapResult(
        defaultError: String,
        transform: (T) -> R
    ): RepoResult<R> {
        return when (this) {
            is ApiResult.Success -> RepoResult.Success(transform(data))
            is ApiResult.Error -> RepoResult.Error(
                message = message.ifBlank { defaultError },
                errorCode = errorCode,
                httpStatus = httpStatus
            )
        }
    }

    suspend fun checkHealth(): RepoResult<Boolean> {
        return when (val res = apiClient.safeCall { it.getHealth() }) {
            is ApiResult.Success -> {
                if (res.data.success) RepoResult.Success(true, res.data.status)
                else RepoResult.Error("Melvore API health check returned unsuccessful status.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun fetchDesignHub(): RepoResult<DesignHubData> {
        return when (val res = apiClient.safeCall { it.getDesignBundle("all") }) {
            is ApiResult.Success -> {
                val bundle = res.data
                val activeBrands = bundle.brands
                    .map { it.toDomain() }
                    .filter { it.isActive && it.name.isNotBlank() }
                    .sortedWith(compareBy<BrandItem> { it.displayOrder }.thenBy { it.name.lowercase() })
                RepoResult.Success(
                    DesignHubData(
                        design = bundle.toDomain(),
                        categories = bundle.categories.map { it.toDomain() },
                        brands = activeBrands,
                        collections = bundle.collections.map { it.toDomain() },
                        trendingTags = bundle.trendingTags.map { it.toDomain() },
                        faqs = bundle.faqs.map { it.toDomain() }
                    )
                )
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun fetchProducts(
        page: Int = 1,
        limit: Int = 50,
        category: String? = null,
        brandId: Int? = null,
        search: String? = null,
        sort: String? = null,
        featured: Int? = null,
        onSale: Int? = null
    ): RepoResult<List<Product>> {
        val cleanCat = category?.trim()?.takeIf { it.isNotEmpty() && it != "all" }
        val cleanBrandId = brandId?.takeIf { it > 0 }
        val cleanSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val cleanSort = sort?.trim()?.takeIf { it.isNotEmpty() && it != "featured" }
        return when (val res = apiClient.safeCall {
            it.getProducts(
                page = page,
                limit = limit,
                category = cleanCat,
                brandId = cleanBrandId,
                search = cleanSearch,
                sort = cleanSort,
                featured = featured,
                onSale = onSale
            )
        }) {
            is ApiResult.Success -> RepoResult.Success(res.data.products.map { it.toDomain() })
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun fetchProductDetails(productId: Int): RepoResult<Product> {
        return when (val res = apiClient.safeCall { it.getProductDetails(productId) }) {
            is ApiResult.Success -> {
                val dto = res.data.product
                if (dto != null) RepoResult.Success(dto.toDomain())
                else RepoResult.Error("Product details not found.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun fetchCategories(): RepoResult<CategoriesBundle> {
        return apiClient.safeCall { it.getCategories() }.mapResult("Failed to load categories") {
            val activeBrands = it.brands
                .map { b -> b.toDomain() }
                .filter { b -> b.isActive && b.name.isNotBlank() }
                .sortedWith(compareBy<BrandItem> { b -> b.displayOrder }.thenBy { b -> b.name.lowercase() })
            CategoriesBundle(
                categories = it.categories.map { c -> c.toDomain() },
                brands = activeBrands,
                collections = it.collections.map { col -> col.toDomain() }
            )
        }
    }

    // ==================== AUTH & PROFILE ====================

    suspend fun login(email: String, password: String): RepoResult<User> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            return RepoResult.Error("Email and password are required.")
        }
        return when (val res = apiClient.safeCall { it.login(LoginRequest(cleanEmail, password)) }) {
            is ApiResult.Success -> {
                val body = res.data
                val token = body.token
                val user = body.user?.toDomain()
                if (!body.success || token.isNullOrBlank() || user == null) {
                    RepoResult.Error(body.message ?: "Login failed.", body.errorCode)
                } else {
                    sessionStore.saveAuthToken(token)
                    RepoResult.Success(user, body.message ?: "Welcome back, ${user.name.substringBefore(" ")}!")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun register(name: String, email: String, phone: String, password: String): RepoResult<User> {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isEmpty() || cleanEmail.isEmpty() || password.isEmpty()) {
            return RepoResult.Error("Name, email, and password are required.")
        }
        if (password.length < 6) {
            return RepoResult.Error("Password must be at least 6 characters.")
        }
        return when (val res = apiClient.safeCall {
            it.register(RegisterRequest(cleanName, cleanEmail, cleanPhone, password))
        }) {
            is ApiResult.Success -> {
                val body = res.data
                val token = body.token
                val user = body.user?.toDomain()
                if (!body.success || token.isNullOrBlank() || user == null) {
                    RepoResult.Error(body.message ?: "Registration failed.", body.errorCode)
                } else {
                    sessionStore.saveAuthToken(token)
                    RepoResult.Success(user, body.message ?: "Account created! Welcome to Melvore.")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun socialGoogleLogin(idToken: String): RepoResult<User> {
        if (idToken.isBlank()) return RepoResult.Error("Missing Google ID token.")
        return when (val res = apiClient.safeCall {
            it.socialAuth(SocialAuthRequest(provider = "google", idToken = idToken))
        }) {
            is ApiResult.Success -> {
                val body = res.data
                val token = body.token
                val user = body.user?.toDomain()
                if (!body.success || token.isNullOrBlank() || user == null) {
                    RepoResult.Error(body.message ?: "Google sign-in failed.", body.errorCode)
                } else {
                    sessionStore.saveAuthToken(token)
                    RepoResult.Success(user, body.message ?: "Signed in with Google as ${user.name}")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun sendEmailOtp(email: String): RepoResult<String> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) return RepoResult.Error("Email is required.")
        return when (val res = apiClient.safeCall { it.otpAuth(OtpRequest(action = "send", email = cleanEmail)) }) {
            is ApiResult.Success -> {
                if (res.data.success) RepoResult.Success(res.data.message ?: "OTP sent to $cleanEmail")
                else RepoResult.Error(res.data.message ?: "Failed to send OTP.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun verifyEmailOtp(email: String, otp: String): RepoResult<User> {
        val cleanEmail = email.trim()
        val cleanOtp = otp.trim()
        if (cleanEmail.isEmpty() || cleanOtp.isEmpty()) return RepoResult.Error("Email and OTP are required.")
        return when (val res = apiClient.safeCall {
            it.otpAuth(OtpRequest(action = "verify", email = cleanEmail, otp = cleanOtp))
        }) {
            is ApiResult.Success -> {
                val body = res.data
                val token = body.token
                val user = body.user?.toDomain()
                if (!body.success || token.isNullOrBlank() || user == null) {
                    RepoResult.Error(body.message ?: "OTP verification failed.", body.errorCode)
                } else {
                    sessionStore.saveAuthToken(token)
                    RepoResult.Success(user, body.message ?: "Signed in via OTP")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun requestPasswordReset(email: String): RepoResult<String> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) return RepoResult.Error("Email is required.")
        return when (val res = apiClient.safeCall {
            it.forgotPassword(ForgotPasswordRequest(action = "request", email = cleanEmail))
        }) {
            is ApiResult.Success -> {
                if (res.data.success) RepoResult.Success(res.data.message ?: "Reset OTP sent to $cleanEmail")
                else RepoResult.Error(res.data.message ?: "Could not request password reset.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): RepoResult<String> {
        val cleanEmail = email.trim()
        val cleanOtp = otp.trim()
        if (cleanEmail.isEmpty() || cleanOtp.isEmpty() || newPassword.length < 6) {
            return RepoResult.Error("Email, OTP, and a new password (min 6 characters) are required.")
        }
        return when (val res = apiClient.safeCall {
            it.forgotPassword(
                ForgotPasswordRequest(
                    action = "verify",
                    email = cleanEmail,
                    otp = cleanOtp,
                    newPassword = newPassword,
                    password = newPassword
                )
            )
        }) {
            is ApiResult.Success -> {
                if (res.data.success) RepoResult.Success(res.data.message ?: "Password reset successful.")
                else RepoResult.Error(res.data.message ?: "Password reset failed.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun logout() {
        if (hasAuthToken()) {
            apiClient.safeCall { it.logout() }
        }
        sessionStore.clearAuthToken()
        sessionStore.setPendingPaymentOrderId(null)
    }

    suspend fun fetchProfile(): RepoResult<ProfileData> {
        return when (val res = apiClient.safeCall { it.getProfile() }) {
            is ApiResult.Success -> {
                val u = res.data.user?.toDomain()
                if (u == null) {
                    RepoResult.Error(res.data.message ?: "Failed to load profile.")
                } else {
                    RepoResult.Success(
                        ProfileData(
                            user = u,
                            addresses = res.data.addresses.map { it.toDomain() },
                            cartCount = res.data.counts.cart,
                            wishlistCount = res.data.counts.wishlist,
                            pendingOrdersCount = res.data.counts.pendingOrders
                        )
                    )
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun updateProfile(
        name: String,
        mobilePrimary: String,
        mobileAlternate: String? = null
    ): RepoResult<User> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return RepoResult.Error("Name cannot be empty.")
        return when (val res = apiClient.safeCall {
            it.updateProfile(
                ProfileUpdateRequest(
                    name = cleanName,
                    mobilePrimary = mobilePrimary.trim(),
                    mobileAlternate = mobileAlternate?.trim()?.takeIf { alt -> alt.isNotEmpty() }
                )
            )
        }) {
            is ApiResult.Success -> {
                val u = res.data.user?.toDomain()
                if (u != null) RepoResult.Success(u, res.data.message ?: "Profile updated")
                else RepoResult.Error(res.data.message ?: "Failed to update profile.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    // ==================== CART & COUPONS ====================

    suspend fun fetchCart(): RepoResult<CartData> {
        return apiClient.safeCall { it.getCart() }.mapResult("Failed to load cart") {
            CartData(
                items = it.items.map { item -> item.toDomain() },
                count = it.count,
                summary = it.summary.toDomain()
            )
        }
    }

    suspend fun addToCart(
        productId: Int,
        variationId: Int? = null,
        quantity: Int = 1,
        color: String? = null,
        size: String? = null
    ): RepoResult<CartData> {
        return when (val res = apiClient.safeCall {
            it.addToCart(
                CartAddRequest(
                    productId = productId,
                    variationId = variationId,
                    quantity = quantity.coerceAtLeast(1),
                    color = color?.takeIf { c -> c.isNotBlank() },
                    size = size?.takeIf { s -> s.isNotBlank() }
                )
            )
        }) {
            is ApiResult.Success -> RepoResult.Success(
                CartData(
                    items = res.data.items.map { it.toDomain() },
                    count = res.data.count,
                    summary = res.data.summary.toDomain()
                ),
                res.data.message ?: "Added to cart"
            )
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun updateCartQty(cartId: Int, delta: Int): RepoResult<CartData> {
        return apiClient.safeCall {
            it.updateCart(CartUpdateRequest(cartId = cartId, delta = delta))
        }.mapResult("Failed to update cart item") {
            CartData(
                items = it.items.map { item -> item.toDomain() },
                count = it.count,
                summary = it.summary.toDomain()
            )
        }
    }

    suspend fun removeCartItem(cartId: Int): RepoResult<CartData> {
        return when (val res = apiClient.safeCall { it.removeFromCart(CartRemoveRequest(cartId)) }) {
            is ApiResult.Success -> RepoResult.Success(
                CartData(
                    items = res.data.items.map { it.toDomain() },
                    count = res.data.count,
                    summary = res.data.summary.toDomain()
                ),
                res.data.message ?: "Item removed from cart"
            )
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun fetchActiveCoupons(): RepoResult<List<CouponInfo>> {
        return apiClient.safeCall { it.listActiveCoupons() }.mapResult("Failed to load coupons") {
            it.coupons.map { c -> c.toDomain() }
        }
    }

    suspend fun validateCoupon(rawCode: String, paymentMethod: String = "online"): CouponEvalResult {
        val code = rawCode.trim()
        if (code.isEmpty()) {
            return CouponEvalResult(valid = true, discountAmount = 0.0, code = null)
        }
        return when (val res = apiClient.safeCall {
            it.validateCoupon(CouponValidateRequest(couponCode = code, paymentMethod = paymentMethod))
        }) {
            is ApiResult.Success -> {
                val body = res.data
                CouponEvalResult(
                    valid = body.valid,
                    discountAmount = body.discountAmount,
                    code = body.code,
                    summary = body.summary?.toDomain(),
                    message = body.message ?: if (body.valid) "Coupon applied" else "Invalid coupon code"
                )
            }
            is ApiResult.Error -> CouponEvalResult(
                valid = false,
                discountAmount = 0.0,
                code = null,
                message = res.message
            )
        }
    }

    // ==================== WISHLIST ====================

    suspend fun fetchWishlist(): RepoResult<WishlistData> {
        return apiClient.safeCall { it.getWishlist() }.mapResult("Failed to load wishlist") { resp ->
            val mappedProducts = resp.items.map { item ->
                Product(
                    id = item.productId,
                    sku = item.sku,
                    name = item.name,
                    slug = item.slug,
                    category = item.categoryName ?: "Apparel",
                    regularPrice = item.regularPrice,
                    salePrice = item.salePrice,
                    imageUrl = DtoMappers.resolveMediaUrl(item.primaryImage),
                    badge = item.badge,
                    rating = item.ratingAverage,
                    stockQuantity = item.stockQuantity,
                    inStock = item.inStock
                )
            }
            WishlistData(
                productIds = resp.productIds.ifEmpty { mappedProducts.map { it.id } },
                products = mappedProducts
            )
        }
    }

    suspend fun addToWishlist(productId: Int): RepoResult<WishlistData> {
        return when (val res = apiClient.safeCall { it.addToWishlist(WishlistMutateRequest(productId)) }) {
            is ApiResult.Success -> {
                val mapped = res.data.items.map { item ->
                    Product(
                        id = item.productId,
                        sku = item.sku,
                        name = item.name,
                        slug = item.slug,
                        category = item.categoryName ?: "Apparel",
                        regularPrice = item.regularPrice,
                        salePrice = item.salePrice,
                        imageUrl = DtoMappers.resolveMediaUrl(item.primaryImage),
                        badge = item.badge,
                        rating = item.ratingAverage,
                        stockQuantity = item.stockQuantity,
                        inStock = item.inStock
                    )
                }
                RepoResult.Success(
                    WishlistData(res.data.productIds.ifEmpty { mapped.map { it.id } }, mapped),
                    res.data.message ?: "Saved to wishlist"
                )
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun removeFromWishlist(productId: Int): RepoResult<WishlistData> {
        return when (val res = apiClient.safeCall { it.removeFromWishlist(WishlistMutateRequest(productId)) }) {
            is ApiResult.Success -> {
                val mapped = res.data.items.map { item ->
                    Product(
                        id = item.productId,
                        sku = item.sku,
                        name = item.name,
                        slug = item.slug,
                        category = item.categoryName ?: "Apparel",
                        regularPrice = item.regularPrice,
                        salePrice = item.salePrice,
                        imageUrl = DtoMappers.resolveMediaUrl(item.primaryImage),
                        badge = item.badge,
                        rating = item.ratingAverage,
                        stockQuantity = item.stockQuantity,
                        inStock = item.inStock
                    )
                }
                RepoResult.Success(
                    WishlistData(res.data.productIds.ifEmpty { mapped.map { it.id } }, mapped),
                    res.data.message ?: "Removed from wishlist"
                )
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    // ==================== ADDRESSES ====================

    suspend fun fetchAddresses(): RepoResult<List<Address>> {
        return apiClient.safeCall { it.getAddresses() }.mapResult("Failed to load addresses") {
            it.addresses.map { a -> a.toDomain() }
        }
    }

    suspend fun saveAddress(address: Address): RepoResult<Address> {
        if (address.fullName.isBlank() || address.phone.isBlank() || address.street.isBlank() ||
            address.city.isBlank() || address.state.isBlank() || address.pincode.isBlank()
        ) {
            return RepoResult.Error("Please fill all required address fields.")
        }
        val req = AddressSaveRequest(
            addressId = address.id.takeIf { it > 0 },
            fullName = address.fullName.trim(),
            mobile = address.phone.trim(),
            addressLine1 = address.street.trim(),
            addressLine2 = address.addressLine2.trim(),
            city = address.city.trim(),
            state = address.state.trim(),
            pincode = address.pincode.trim(),
            country = address.country.ifBlank { "India" },
            addressType = address.label.lowercase().let {
                if (it in setOf("home", "work", "other")) it else "home"
            },
            isDefault = address.isDefault
        )
        return when (val res = apiClient.safeCall { it.saveAddress(req) }) {
            is ApiResult.Success -> {
                val saved = res.data.address?.toDomain()
                if (saved != null) RepoResult.Success(saved, res.data.message ?: "Address saved")
                else RepoResult.Error(res.data.message ?: "Failed to save address.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun deleteAddress(addressId: Int): RepoResult<String> {
        return when (val res = apiClient.safeCall { it.deleteAddress(AddressDeleteRequest(addressId)) }) {
            is ApiResult.Success -> {
                if (res.data.success) RepoResult.Success(res.data.message ?: "Address deleted")
                else RepoResult.Error(res.data.message ?: "Failed to delete address.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    // ==================== ORDERS & RAZORPAY PAYMENTS ====================

    suspend fun createOrder(
        shippingAddressId: Int,
        paymentMethod: String,
        couponCode: String,
        notes: String = "",
        idempotencyKey: String
    ): RepoResult<Order> {
        val normalizedMethod = if (paymentMethod.equals("cod", ignoreCase = true)) "cod" else "online"
        return when (val res = apiClient.safeCall {
            it.createOrder(
                idempotencyKey = idempotencyKey,
                body = OrderCreateRequest(
                    shippingAddressId = shippingAddressId,
                    paymentMethod = normalizedMethod,
                    couponCode = couponCode.trim(),
                    notes = notes.trim()
                )
            )
        }) {
            is ApiResult.Success -> {
                val created = res.data.order?.toDomain()
                if (res.data.success && created != null) {
                    if (normalizedMethod == "online") {
                        sessionStore.setPendingPaymentOrderId(created.id)
                    }
                    RepoResult.Success(created, res.data.message ?: "Order placed successfully")
                } else {
                    RepoResult.Error(res.data.message ?: "Failed to create order.")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun fetchOrderHistory(page: Int = 1, limit: Int = 30): RepoResult<List<Order>> {
        return apiClient.safeCall { it.getOrderHistory(page, limit) }.mapResult("Failed to load orders") {
            it.orders.map { o -> o.toDomain() }
        }
    }

    suspend fun fetchOrderDetails(orderId: Int): RepoResult<Order> {
        return when (val res = apiClient.safeCall { it.getOrderDetails(orderId) }) {
            is ApiResult.Success -> {
                val ord = res.data.order?.toDomain()
                if (ord != null) RepoResult.Success(ord)
                else RepoResult.Error("Order not found.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun trackOrder(
        orderNumber: String,
        pincode: String? = null,
        mobile: String? = null
    ): RepoResult<Order> {
        val clean = orderNumber.trim().removePrefix("#")
        if (clean.isEmpty()) return RepoResult.Error("Enter an order number (e.g. MLV-...).")
        return when (val res = apiClient.safeCall {
            it.trackOrder(
                orderNumber = clean,
                pincode = pincode?.trim()?.takeIf { p -> p.isNotEmpty() },
                mobile = mobile?.trim()?.takeIf { m -> m.isNotEmpty() }
            )
        }) {
            is ApiResult.Success -> {
                val trk = res.data.tracking
                if (res.data.success && trk != null) {
                    val details = if (hasAuthToken() && trk.orderId > 0) {
                        (apiClient.safeCall { it.getOrderDetails(trk.orderId) } as? ApiResult.Success)
                            ?.data?.order?.toDomain()
                    } else null
                    RepoResult.Success(trk.toDomain(details))
                } else {
                    RepoResult.Error("No order found matching #$clean.")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun cancelOrder(orderId: Int, reason: String = "Cancelled by customer in Android app"): RepoResult<String> {
        return when (val res = apiClient.safeCall { it.cancelOrder(OrderCancelRequest(orderId, reason)) }) {
            is ApiResult.Success -> {
                if (res.data.success) {
                    if (sessionStore.getPendingPaymentOrderId() == orderId) {
                        sessionStore.setPendingPaymentOrderId(null)
                    }
                    RepoResult.Success(res.data.message ?: "Order cancelled")
                } else {
                    RepoResult.Error(res.data.message ?: "Failed to cancel order.")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun initiateRazorpayPayment(orderId: Int): RepoResult<RazorpayCheckoutPayload> {
        return when (val res = apiClient.safeCall {
            it.initiatePayment(PaymentInitiateRequest(orderId = orderId, gateway = "razorpay"))
        }) {
            is ApiResult.Success -> {
                if (res.data.success && res.data.keyId.isNotBlank() && res.data.razorpayOrderId.isNotBlank()) {
                    sessionStore.setPendingPaymentOrderId(orderId)
                    RepoResult.Success(res.data.toPayload())
                } else {
                    RepoResult.Error("Unable to initialize Razorpay checkout for this order.")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun verifyRazorpayPayment(
        orderId: Int,
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String
    ): RepoResult<String> {
        return when (val res = apiClient.safeCall {
            it.verifyPayment(
                PaymentVerifyRequest(
                    orderId = orderId,
                    gateway = "razorpay",
                    razorpayOrderId = razorpayOrderId,
                    razorpayPaymentId = razorpayPaymentId,
                    razorpaySignature = razorpaySignature
                )
            )
        }) {
            is ApiResult.Success -> {
                if (res.data.success) {
                    if (sessionStore.getPendingPaymentOrderId() == orderId) {
                        sessionStore.setPendingPaymentOrderId(null)
                    }
                    RepoResult.Success(res.data.message ?: "Payment verified for #${res.data.orderNumber}")
                } else {
                    RepoResult.Error(res.data.message ?: "Payment verification failed.")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun failOrCancelPayment(
        orderId: Int,
        cancelOrder: Boolean,
        reason: String? = null
    ): RepoResult<String> {
        val action = if (cancelOrder) "cancel_order" else "fail"
        return when (val res = apiClient.safeCall {
            it.failPayment(PaymentFailRequest(orderId = orderId, action = action, reason = reason))
        }) {
            is ApiResult.Success -> {
                if (cancelOrder && sessionStore.getPendingPaymentOrderId() == orderId) {
                    sessionStore.setPendingPaymentOrderId(null)
                }
                RepoResult.Success(
                    res.data.message ?: if (cancelOrder) "Unpaid order cancelled." else "Payment left pending for retry."
                )
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    // ==================== CMS, BLOG, SUPPORT & CONTACT ====================

    suspend fun fetchBlogs(): RepoResult<List<BlogPost>> {
        return apiClient.safeCall { it.getBlogList() }.mapResult("Failed to load journal") {
            it.posts.map { p -> p.toDomain() }
        }
    }

    suspend fun fetchBlogDetails(postId: Int): RepoResult<BlogPost> {
        return when (val res = apiClient.safeCall { it.getBlogDetails(postId) }) {
            is ApiResult.Success -> {
                val p = res.data.post?.toDomain()
                if (p != null) RepoResult.Success(p) else RepoResult.Error("Article not found.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun fetchFaqs(): RepoResult<List<FaqItem>> {
        return apiClient.safeCall { it.getFaqs() }.mapResult("Failed to load FAQs") {
            it.faqs.map { f -> f.toDomain() }
        }
    }

    suspend fun fetchAboutSections(): RepoResult<List<AboutSection>> {
        return apiClient.safeCall { it.getCmsPage("about") }.mapResult("Failed to load About page") {
            it.aboutContent.map { s -> s.toDomain() }
        }
    }

    suspend fun fetchSupportTickets(): RepoResult<List<SupportTicket>> {
        return apiClient.safeCall { it.getSupportTickets() }.mapResult("Failed to load support tickets") {
            it.tickets.map { t -> t.toDomain() }
        }
    }

    suspend fun createSupportTicket(
        subject: String,
        orderId: Int?,
        category: String,
        priority: String,
        message: String
    ): RepoResult<SupportTicket> {
        if (subject.isBlank() || message.isBlank()) {
            return RepoResult.Error("Subject and message are required.")
        }
        val cleanPriority = priority.lowercase().let {
            when (it) {
                "normal", "medium" -> "medium"
                "low" -> "low"
                "high" -> "high"
                "urgent" -> "urgent"
                else -> "medium"
            }
        }
        return when (val res = apiClient.safeCall {
            it.createSupportTicket(
                SupportCreateRequest(
                    subject = subject.trim(),
                    orderId = orderId,
                    category = category.ifBlank { "general" },
                    priority = cleanPriority,
                    message = message.trim()
                )
            )
        }) {
            is ApiResult.Success -> {
                val ticket = res.data.ticket?.toDomain()
                if (res.data.success && ticket != null) {
                    RepoResult.Success(ticket, res.data.message ?: "Support ticket created")
                } else {
                    RepoResult.Error(res.data.message ?: "Failed to create ticket.")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun replySupportTicket(ticketId: Int, message: String): RepoResult<String> {
        val text = message.trim()
        if (text.isEmpty()) return RepoResult.Error("Message cannot be empty.")
        return when (val res = apiClient.safeCall {
            it.replySupportTicket(SupportReplyRequest(ticketId = ticketId, message = text))
        }) {
            is ApiResult.Success -> {
                if (res.data.success) RepoResult.Success(res.data.message ?: "Reply sent")
                else RepoResult.Error(res.data.message ?: "Failed to send reply.")
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }

    suspend fun submitContact(
        name: String,
        email: String,
        phone: String,
        subject: String,
        message: String
    ): RepoResult<String> {
        if (name.isBlank() || email.isBlank() || message.isBlank()) {
            return RepoResult.Error("Name, email, and message are required.")
        }
        return when (val res = apiClient.safeCall {
            it.submitContact(
                ContactRequest(
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    subject = subject.trim().ifEmpty { "General Inquiry" },
                    message = message.trim()
                )
            )
        }) {
            is ApiResult.Success -> {
                if (res.data.success) {
                    RepoResult.Success(
                        res.data.message ?: "Thank you! Our team will respond shortly."
                    )
                } else {
                    RepoResult.Error(res.data.message ?: "Failed to submit inquiry.")
                }
            }
            is ApiResult.Error -> RepoResult.Error(res.message, res.errorCode, res.httpStatus)
        }
    }
}
