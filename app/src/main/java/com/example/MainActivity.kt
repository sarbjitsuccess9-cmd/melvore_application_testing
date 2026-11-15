package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.model.RazorpayCheckoutPayload
import com.example.ui.components.CartBottomSheet
import com.example.ui.components.MelvoreBottomBar
import com.example.ui.components.MelvoreTopBar
import com.example.ui.components.ProfileMenuBottomSheet
import com.example.ui.components.SearchDiscoveryModal
import com.example.ui.navigation.AboutRoute
import com.example.ui.navigation.AuthRoute
import com.example.ui.navigation.BlogRoute
import com.example.ui.navigation.CategoriesRoute
import com.example.ui.navigation.CheckoutRoute
import com.example.ui.navigation.ContactRoute
import com.example.ui.navigation.FaqRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.OrderTrackingRoute
import com.example.ui.navigation.ProductDetailsRoute
import com.example.ui.navigation.ProfileRoute
import com.example.ui.navigation.ShopRoute
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BlogScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.ContactScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrderTrackingScreen
import com.example.ui.screens.ProductDetailsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.TopEditorialNavigationOverlay
import com.example.ui.theme.MelvoreTheme
import com.example.ui.viewmodel.MelvoreViewModel
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val melvoreViewModel: MelvoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        runCatching { Checkout.preload(applicationContext) }
        setContent {
            MelvoreApp(
                viewModel = melvoreViewModel,
                onLaunchRazorpay = { payload -> launchRazorpayCheckout(payload) }
            )
        }
    }

    private fun launchRazorpayCheckout(payload: RazorpayCheckoutPayload) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(payload.keyId)
            val options = JSONObject().apply {
                put("name", "Melvore")
                put("description", "Order #${payload.orderNumber}")
                put("order_id", payload.razorpayOrderId)
                put("currency", payload.currency.ifBlank { "INR" })
                put("amount", payload.amountPaise)
                put(
                    "prefill",
                    JSONObject().apply {
                        if (payload.prefillName.isNotBlank()) put("name", payload.prefillName)
                        if (payload.prefillEmail.isNotBlank()) put("email", payload.prefillEmail)
                        if (payload.prefillContact.isNotBlank()) put("contact", payload.prefillContact)
                    }
                )
                put("theme", JSONObject().apply { put("color", "#141414") })
            }
            checkout.open(this, options)
        } catch (e: Exception) {
            melvoreViewModel.handleRazorpaySdkFailure(
                orderId = payload.orderId,
                reason = e.message ?: "Unable to launch Razorpay checkout."
            )
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = melvoreViewModel.activeRazorpayOrderId ?: return
        val rzpOrderId = paymentData?.orderId?.takeIf { it.isNotBlank() }
            ?: melvoreViewModel.activeRazorpayGatewayOrderId.orEmpty()
        val rzpPaymentId = paymentData?.paymentId?.takeIf { it.isNotBlank() }
            ?: razorpayPaymentId.orEmpty()
        val rzpSignature = paymentData?.signature.orEmpty()

        if (rzpPaymentId.isBlank() || rzpSignature.isBlank()) {
            melvoreViewModel.handleRazorpaySdkFailure(
                orderId = orderId,
                reason = "Incomplete Razorpay verification signature returned."
            )
            return
        }

        melvoreViewModel.verifyRazorpayPayment(
            orderId = orderId,
            razorpayOrderId = rzpOrderId,
            razorpayPaymentId = rzpPaymentId,
            razorpaySignature = rzpSignature
        )
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val orderId = melvoreViewModel.activeRazorpayOrderId ?: return
        val cleanReason = response?.takeIf { it.isNotBlank() }
            ?: "Razorpay payment was cancelled or failed (code $code)."
        melvoreViewModel.handleRazorpaySdkFailure(orderId = orderId, reason = cleanReason)
    }
}

@Composable
fun MelvoreApp(
    viewModel: MelvoreViewModel = viewModel(),
    onLaunchRazorpay: (RazorpayCheckoutPayload) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCartOpen by viewModel.isCartOpen.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val isProfileSheetOpen by viewModel.isProfileSheetOpen.collectAsStateWithLifecycle()
    val isTopMenuOpen by viewModel.isTopMenuOpen.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    LaunchedEffect(Unit) {
        viewModel.toastMessages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.razorpayLaunchRequests.collect { payload ->
            onLaunchRazorpay(payload)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateAfterOrder.collect { orderNo ->
            navController.navigate(OrderTrackingRoute(orderNo)) {
                popUpTo(HomeRoute)
            }
        }
    }

    val currentSection = when {
        currentDest?.hasRoute<HomeRoute>() == true -> "home"
        currentDest?.hasRoute<ShopRoute>() == true || currentDest?.hasRoute<CategoriesRoute>() == true -> "shop"
        currentDest?.hasRoute<ProfileRoute>() == true -> {
            val tabArg = runCatching { backStackEntry?.toRoute<ProfileRoute>()?.tab }.getOrNull() ?: "overview"
            if (tabArg == "wishlist") "wishlist" else "profile"
        }
        else -> ""
    }

    val isDark = uiState.themeMode == "dark"

    MelvoreTheme(darkTheme = isDark) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                MelvoreTopBar(
                    isDark = isDark,
                    isMenuOpen = isTopMenuOpen,
                    onMenuClick = { viewModel.setTopMenuOpen(!isTopMenuOpen) },
                    onLogoClick = {
                        viewModel.setTopMenuOpen(false)
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    },
                    onTrackClick = {
                        viewModel.setTopMenuOpen(false)
                        val latest = uiState.orders.firstOrNull()?.orderNumber ?: ""
                        navController.navigate(OrderTrackingRoute(latest))
                    },
                    onSearchClick = {
                        viewModel.setTopMenuOpen(false)
                        viewModel.setSearchOpen(true)
                    },
                    onToggleTheme = { viewModel.toggleTheme() }
                )
            },
            bottomBar = {
                MelvoreBottomBar(
                    currentSection = currentSection,
                    user = uiState.user,
                    wishlistCount = uiState.wishlistIds.size,
                    cartCount = uiState.cartCount,
                    onHomeClick = {
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    },
                    onShopClick = { navController.navigate(ShopRoute()) },
                    onMeClick = { viewModel.setProfileSheetOpen(true) },
                    onWishlistClick = {
                        if (uiState.user == null) {
                            viewModel.showToast("Please sign in to view your wishlist")
                            navController.navigate(AuthRoute("login"))
                        } else {
                            navController.navigate(ProfileRoute("wishlist"))
                        }
                    },
                    onCartClick = { viewModel.setCartOpen(true) }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        uiState = uiState,
                        onRetry = { viewModel.refreshAll() },
                        onOpenTopMenu = { viewModel.setTopMenuOpen(true) },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id)) },
                        onQuickAdd = { p -> viewModel.addToCart(p) },
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        },
                        onNavigateShop = { cat, col, q ->
                            navController.navigate(ShopRoute(category = cat, collection = col, query = q))
                        },
                        onNavigateBrand = { bid ->
                            navController.navigate(ShopRoute(brandId = bid))
                        },
                        onNavigateCategories = { navController.navigate(CategoriesRoute) },
                        onNavigateTrack = {
                            val latest = uiState.orders.firstOrNull()?.orderNumber ?: ""
                            navController.navigate(OrderTrackingRoute(latest))
                        },
                        onNavigateAbout = { navController.navigate(AboutRoute) },
                        onNavigateBlog = { navController.navigate(BlogRoute) },
                        onNavigateContact = { navController.navigate(ContactRoute) },
                        onNavigateFaq = { navController.navigate(FaqRoute) }
                    )
                }

                composable<ShopRoute> { entry ->
                    val route = entry.toRoute<ShopRoute>()
                    ShopScreen(
                        uiState = uiState,
                        initialCategory = route.category,
                        initialCollection = route.collection,
                        initialQuery = route.query,
                        initialBrandId = route.brandId,
                        onFetchCatalog = { cat, col, q, sort, bid ->
                            viewModel.loadCatalog(cat, col, q, sort, bid)
                        },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id)) },
                        onQuickAdd = { p -> viewModel.addToCart(p) },
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        }
                    )
                }

                composable<CategoriesRoute> {
                    CategoriesScreen(
                        uiState = uiState,
                        onRetry = { viewModel.refreshAll() },
                        onSelectCategory = { slug -> navController.navigate(ShopRoute(category = slug)) },
                        onSelectCollection = { slug -> navController.navigate(ShopRoute(collection = slug)) }
                    )
                }

                composable<ProductDetailsRoute> { entry ->
                    val route = entry.toRoute<ProductDetailsRoute>()
                    ProductDetailsScreen(
                        productId = route.productId,
                        uiState = uiState,
                        onLoadDetails = { id -> viewModel.loadProductDetails(id) },
                        onBack = { navController.popBackStack() },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id)) },
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        },
                        onAddToCart = { p, color, size, qty -> viewModel.addToCart(p, color, size, qty) },
                        onBuyNow = { p, color, size, qty ->
                            viewModel.addToCart(p, color, size, qty) {
                                navController.navigate(CheckoutRoute)
                            }
                        }
                    )
                }

                composable<CheckoutRoute> {
                    CheckoutScreen(
                        uiState = uiState,
                        onBackToShop = { navController.navigate(ShopRoute()) },
                        onNavigateLogin = { navController.navigate(AuthRoute("login")) },
                        onValidateCoupon = { code, method, onRes ->
                            viewModel.validateCoupon(code, method, onRes)
                        },
                        onPlaceOrder = { addrId, draft, method, coupon, notes ->
                            viewModel.submitCheckoutOrder(addrId, draft, method, coupon, notes)
                        },
                        onRetryRazorpay = { oid -> viewModel.retryRazorpayPayment(oid) },
                        onCancelUnpaidOrder = { oid -> viewModel.cancelUnpaidOrder(oid) }
                    )
                }

                composable<OrderTrackingRoute> { entry ->
                    val route = entry.toRoute<OrderTrackingRoute>()
                    OrderTrackingScreen(
                        initialOrderNumber = route.orderNumber,
                        uiState = uiState,
                        onTrackOrder = { orderNo, pin, mob -> viewModel.trackOrder(orderNo, pin, mob) },
                        onRetryRazorpay = { oid -> viewModel.retryRazorpayPayment(oid) },
                        onCancelUnpaidOrder = { oid -> viewModel.cancelUnpaidOrder(oid) },
                        onNavigateContact = { navController.navigate(ContactRoute) }
                    )
                }

                composable<AuthRoute> { entry ->
                    val route = entry.toRoute<AuthRoute>()
                    AuthScreen(
                        initialTab = route.initialTab,
                        isAuthBusy = uiState.isAuthBusy,
                        onLogin = { e, p, ok -> viewModel.login(e, p, ok) },
                        onRegister = { n, e, ph, p, ok -> viewModel.register(n, e, ph, p, ok) },
                        onGoogleIdToken = { idToken, ok -> viewModel.loginWithGoogleIdToken(idToken, ok) },
                        onSendOtp = { e, sent -> viewModel.sendEmailOtp(e, sent) },
                        onVerifyOtp = { e, otp, ok -> viewModel.verifyEmailOtp(e, otp, ok) },
                        onRequestPasswordReset = { e, sent -> viewModel.requestPasswordReset(e, sent) },
                        onResetPassword = { e, otp, np, ok -> viewModel.resetPassword(e, otp, np, ok) },
                        onAuthSuccess = {
                            navController.navigate(ProfileRoute("overview")) {
                                popUpTo(HomeRoute)
                            }
                        }
                    )
                }

                composable<ProfileRoute> { entry ->
                    val route = entry.toRoute<ProfileRoute>()
                    ProfileScreen(
                        initialTab = route.tab,
                        uiState = uiState,
                        onNavigateAuth = { tab -> navController.navigate(AuthRoute(tab)) },
                        onNavigateShop = { navController.navigate(ShopRoute()) },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id)) },
                        onAddToCart = { p -> viewModel.addToCart(p) },
                        onToggleWishlist = { pid -> viewModel.toggleWishlist(pid) },
                        onSaveProfile = { n, ph, alt -> viewModel.updateProfile(n, ph, alt) },
                        onTrackOrder = { ordNo -> navController.navigate(OrderTrackingRoute(ordNo)) },
                        onCancelOrder = { oid -> viewModel.cancelOrder(oid) },
                        onRetryRazorpay = { oid -> viewModel.retryRazorpayPayment(oid) },
                        onAddAddress = { addr, ok -> viewModel.saveAddress(addr) { if (it != null) ok() } },
                        onSetDefaultAddress = { aid -> viewModel.setDefaultAddress(aid) },
                        onDeleteAddress = { aid -> viewModel.deleteAddress(aid) },
                        onCreateTicket = { sub, ordNo, cat, prio, msg, ok ->
                            viewModel.createTicket(sub, ordNo, cat, prio, msg, ok)
                        },
                        onReplyTicket = { tid, msg, done -> viewModel.replyTicket(tid, msg, done) },
                        onSetTheme = { theme -> viewModel.setTheme(theme) },
                        onUpdatePreferences = { patch -> viewModel.updateLocalPreferences(patch) },
                        onRequestPasswordReset = { e, sent -> viewModel.requestPasswordReset(e, sent) },
                        onResetPassword = { e, otp, np, ok -> viewModel.resetPassword(e, otp, np, ok) },
                        onSaveApiBaseUrl = { url -> viewModel.saveApiBaseUrl(url) },
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(HomeRoute) {
                                popUpTo(HomeRoute) { inclusive = true }
                            }
                        }
                    )
                }

                composable<AboutRoute> {
                    AboutScreen(
                        uiState = uiState,
                        onNavigateShop = { navController.navigate(ShopRoute()) },
                        onNavigateContact = { navController.navigate(ContactRoute) }
                    )
                }

                composable<BlogRoute> {
                    BlogScreen(
                        uiState = uiState,
                        onOpenPost = { id -> viewModel.loadBlogDetails(id) },
                        onClearPost = { viewModel.clearSelectedBlogPost() }
                    )
                }

                composable<FaqRoute> {
                    FaqScreen(
                        uiState = uiState,
                        onNavigateContact = { navController.navigate(ContactRoute) }
                    )
                }

                composable<ContactRoute> {
                    ContactScreen(
                        uiState = uiState,
                        onSubmitContact = { n, e, ph, sub, msg, done ->
                            viewModel.submitContact(n, e, ph, sub, msg, done)
                        }
                    )
                }
            }
        }

        if (isSearchOpen) {
            SearchDiscoveryModal(
                products = uiState.products,
                categories = uiState.categories,
                collections = uiState.collections,
                trendingTags = uiState.trendingTags,
                currency = uiState.currency,
                onClose = { viewModel.setSearchOpen(false) },
                onSelectProduct = { p ->
                    viewModel.setSearchOpen(false)
                    navController.navigate(ProductDetailsRoute(p.id))
                },
                onSelectCategory = { slug ->
                    viewModel.setSearchOpen(false)
                    navController.navigate(ShopRoute(category = slug))
                },
                onSelectCollection = { slug ->
                    viewModel.setSearchOpen(false)
                    navController.navigate(ShopRoute(collection = slug))
                },
                onViewAllResults = { q ->
                    viewModel.setSearchOpen(false)
                    navController.navigate(ShopRoute(query = q))
                }
            )
        }

        if (isCartOpen) {
            CartBottomSheet(
                cartItems = uiState.cartItems,
                cartCount = uiState.cartCount,
                cartSummary = uiState.cartSummary,
                isCartBusy = uiState.isCartBusy,
                currency = uiState.currency,
                onDismiss = { viewModel.setCartOpen(false) },
                onUpdateQty = { cid, delta -> viewModel.updateCartQty(cid, delta) },
                onRemoveItem = { cid -> viewModel.removeCartItem(cid) },
                onBrowseProducts = {
                    viewModel.setCartOpen(false)
                    navController.navigate(ShopRoute())
                },
                onCheckout = {
                    viewModel.setCartOpen(false)
                    navController.navigate(CheckoutRoute)
                }
            )
        }

        if (isProfileSheetOpen) {
            ProfileMenuBottomSheet(
                user = uiState.user,
                isDark = isDark,
                pendingOrdersCount = uiState.pendingOrdersCount,
                wishlistCount = uiState.wishlistIds.size,
                onDismiss = { viewModel.setProfileSheetOpen(false) },
                onNavigateProfileTab = { tab ->
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(ProfileRoute(tab))
                },
                onNavigateAuth = { tab ->
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(AuthRoute(tab))
                },
                onNavigateHome = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(HomeRoute)
                },
                onNavigateShop = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(ShopRoute())
                },
                onNavigateCategories = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(CategoriesRoute)
                },
                onNavigateTrack = {
                    viewModel.setProfileSheetOpen(false)
                    val latest = uiState.orders.firstOrNull()?.orderNumber ?: ""
                    navController.navigate(OrderTrackingRoute(latest))
                },
                onNavigateBlog = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(BlogRoute)
                },
                onNavigateFaq = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(FaqRoute)
                },
                onNavigateContact = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(ContactRoute)
                },
                onToggleTheme = { viewModel.toggleTheme() },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
        }

        TopEditorialNavigationOverlay(
            isOpen = isTopMenuOpen,
            categories = uiState.categories,
            brands = uiState.brands,
            collections = uiState.collections,
            promoBanners = uiState.design.promoBanners,
            trendingTags = uiState.trendingTags,
            blogs = uiState.blogs,
            onDismiss = { viewModel.setTopMenuOpen(false) },
            onNavigateAllShop = { navController.navigate(ShopRoute()) },
            onSelectBrand = { bid -> navController.navigate(ShopRoute(brandId = bid)) },
            onSelectCategory = { slug -> navController.navigate(ShopRoute(category = slug)) },
            onSelectCollection = { slug -> navController.navigate(ShopRoute(collection = slug)) },
            onSelectTagQuery = { tag -> navController.navigate(ShopRoute(query = tag)) },
            onNavigateCategories = { navController.navigate(CategoriesRoute) },
            onNavigateTrack = {
                val latest = uiState.orders.firstOrNull()?.orderNumber ?: ""
                navController.navigate(OrderTrackingRoute(latest))
            },
            onNavigateBlog = { navController.navigate(BlogRoute) },
            onNavigateAbout = { navController.navigate(AboutRoute) },
            onNavigateContact = { navController.navigate(ContactRoute) },
            onNavigateFaq = { navController.navigate(FaqRoute) }
        )
    }
}
