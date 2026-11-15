package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.model.formatPrice
import com.example.ui.components.LiquidGlassMediaIconButton
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.MelvoreWordmark
import com.example.ui.components.ProductCard
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.MelvoreBrandFontFamily
import com.example.ui.viewmodel.MelvoreUiState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    uiState: MelvoreUiState,
    onRetry: () -> Unit,
    onOpenTopMenu: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onNavigateShop: (category: String, collection: String, query: String) -> Unit,
    onNavigateBrand: (Int) -> Unit,
    onNavigateCategories: () -> Unit,
    onNavigateTrack: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateBlog: () -> Unit,
    onNavigateContact: () -> Unit,
    onNavigateFaq: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val animationsEnabled = rememberAnimationsEnabled()

    if (uiState.isHomeLoading && uiState.products.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MelvoreLoadingState("Loading Melvore storefront…")
        }
        return
    }

    if (uiState.homeError != null && uiState.products.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            MelvoreErrorState(message = uiState.homeError, onRetry = onRetry)
        }
        return
    }

    val design = uiState.design
    val heroSlides = design.heroSlides
    var testimonialIndex by remember { mutableIntStateOf(0) }

    val routeFromCtaLink: (String, String) -> Unit = remember(
        onNavigateShop,
        onNavigateBrand,
        onNavigateCategories,
        onNavigateTrack,
        onNavigateAbout,
        onNavigateBlog,
        onNavigateContact,
        onNavigateFaq
    ) {
        { rawLink, _ ->
            val link = rawLink.trim()
            val lower = link.lowercase()
            when {
                lower.contains("categories") -> onNavigateCategories()
                lower.contains("about") -> onNavigateAbout()
                lower.contains("track") -> onNavigateTrack()
                lower.contains("blog") -> onNavigateBlog()
                lower.contains("contact") -> onNavigateContact()
                lower.contains("faq") -> onNavigateFaq()
                else -> {
                    val queryPart = link.substringAfter("?", "")
                    val params = queryPart.split("&").mapNotNull { kv ->
                        val parts = kv.split("=", limit = 2)
                        if (parts.size == 2) parts[0].lowercase() to parts[1] else null
                    }.toMap()
                    val brandIdParam = params["brand_id"]?.toIntOrNull() ?: 0
                    if (brandIdParam > 0) {
                        onNavigateBrand(brandIdParam)
                    } else {
                        val cat = params["category"]?.ifBlank { null } ?: "all"
                        val col = params["collection"].orEmpty()
                        val q = params["search"] ?: params["q"].orEmpty()
                        onNavigateShop(cat, col, q)
                    }
                }
            }
        }
    }

    LaunchedEffect(design.testimonials.size) {
        if (design.testimonials.size > 1) {
            while (true) {
                delay(5400)
                testimonialIndex = (testimonialIndex + 1) % design.testimonials.size
            }
        }
    }

    val runwayProducts = remember(uiState.products) { uiState.products.take(10) }
    val featuredProducts = remember(design.featuredProducts, uiState.products) {
        design.featuredProducts.ifEmpty { uiState.products }.take(6)
    }
    val importedOrArrivals = remember(uiState.products) {
        val imported = uiState.products.filter { it.isImported || it.originCountry.isNotBlank() }
        (imported.ifEmpty { uiState.products.drop(2) }).take(3)
    }

    val runwayListState = rememberLazyListState()
    val centeredDropIndex by remember {
        derivedStateOf {
            runwayListState.firstVisibleItemIndex +
                if (runwayListState.firstVisibleItemScrollOffset > 140) 1 else 0
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        if (uiState.homeError != null) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    MelvoreErrorState(message = uiState.homeError, onRetry = onRetry)
                }
            }
        }

        // 1. Full-Bleed Hero Slider + Dedicated Monochrome Brand Discovery Slider + Value Ribbon
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (heroSlides.isNotEmpty()) {
                    EditorialHeroSlider(
                        slides = heroSlides,
                        animationsEnabled = animationsEnabled,
                        onSlideCta = { slide -> routeFromCtaLink(slide.ctaLink, "") }
                    )
                }

                // Brand Discovery Slider directly below Hero Slider
                BrandDiscoverySlider(
                    brands = uiState.brands,
                    animationsEnabled = animationsEnabled,
                    onSelectBrand = { brand -> onNavigateBrand(brand.id) },
                    onBrowseAllBrands = { onNavigateShop("all", "", "") }
                )

                if (design.marqueeItems.isNotEmpty()) {
                    MonochromeValueRibbon(
                        items = design.marqueeItems,
                        onItemClick = { onNavigateShop("all", "", "") }
                    )
                }
            }
        }

        // 2. Editorial Directory Quick Bar
        if (uiState.categories.isNotEmpty() || uiState.collections.isNotEmpty()) {
            item {
                EditorialRevealSection(animationsEnabled = animationsEnabled, delayMillis = 40) {
                    EditorialQuickNavBar(
                        categories = uiState.categories,
                        collections = uiState.collections,
                        onOpenMegaMenu = onOpenTopMenu,
                        onSelectCategory = { slug -> onNavigateShop(slug, "", "") },
                        onSelectCollection = { slug -> onNavigateShop("all", slug, "") },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // 3. Asymmetrical Editorial Campaign Grid (banners table + spotlight piece)
        if (design.promoBanners.isNotEmpty()) {
            item {
                EditorialRevealSection(animationsEnabled = animationsEnabled, delayMillis = 80) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EditorialSectionHeader(
                            eyebrow = "SEASONAL CAMPAIGNS",
                            title = "Editorial Stories & Edits",
                            actionLabel = "Explore all →",
                            onAction = { onNavigateShop("all", "", "") }
                        )

                        AsymmetricalCampaignGrid(
                            banners = design.promoBanners,
                            spotlightProduct = featuredProducts.firstOrNull(),
                            currency = uiState.currency,
                            onBannerClick = { banner ->
                                routeFromCtaLink(banner.ctaLink, banner.filterTag)
                            },
                            onProductClick = onOpenProduct
                        )
                    }
                }
            }
        }

        // 4. Global Arrivals & Multi-Brand Showcase (varied editorial proportions)
        if (importedOrArrivals.isNotEmpty()) {
            item {
                EditorialRevealSection(animationsEnabled = animationsEnabled, delayMillis = 100) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EditorialSectionHeader(
                            eyebrow = "GLOBAL CURATION",
                            title = "Imported & New Arrivals",
                            actionLabel = "View arrivals →",
                            onAction = { onNavigateShop("all", "", "") }
                        )

                        val lead = importedOrArrivals[0]
                        Surface(
                            onClick = { onOpenProduct(lead) },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(176.dp)
                            ) {
                                MelvoreNetworkImage(
                                    url = lead.imageUrl,
                                    contentDescription = lead.name,
                                    modifier = Modifier
                                        .width(148.dp)
                                        .fillMaxSize()
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = (lead.brandName.ifBlank {
                                                lead.originCountry.ifBlank { lead.category }
                                            }).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp,
                                            color = ext.campaignCobalt,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = lead.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontFamily = MelvoreBrandFontFamily,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatPrice(lead.effectivePrice, uiState.currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "INSPECT →",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (importedOrArrivals.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                importedOrArrivals.drop(1).forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(3f / 3.7f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ext.darkEditorialBg)
                                            .clickable { onOpenProduct(item) }
                                    ) {
                                        MelvoreNetworkImage(
                                            url = item.imageUrl,
                                            contentDescription = item.name,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))
                                                    )
                                                )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = (item.brandName.ifBlank {
                                                    item.originCountry.ifBlank { item.category }
                                                }).uppercase(),
                                                color = Color.White.copy(alpha = 0.78f),
                                                style = MaterialTheme.typography.labelSmall,
                                                letterSpacing = 0.9.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatPrice(item.effectivePrice, uiState.currency),
                                                color = Color.White,
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                if (importedOrArrivals.size == 2) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 5. "In Motion" Runway Multi-Brand Carousel
        if (runwayProducts.isNotEmpty()) {
            item {
                EditorialRevealSection(animationsEnabled = animationsEnabled, delayMillis = 120) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditorialSectionHeader(
                            eyebrow = "RUNWAY LOOKBOOK",
                            title = "In Motion · Latest Silhouettes",
                            actionLabel = "Shop all →",
                            onAction = { onNavigateShop("all", "", "") },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        LazyRow(
                            state = runwayListState,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            itemsIndexed(runwayProducts, key = { _, p -> "drop_${p.id}" }) { index, product ->
                                RunwayLookbookCard(
                                    product = product,
                                    currency = uiState.currency,
                                    isFeaturedCenter = index == centeredDropIndex,
                                    animationsEnabled = animationsEnabled,
                                    onOpenProduct = { onOpenProduct(product) },
                                    onQuickAdd = { onQuickAdd(product) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Editorial Category Plates
        if (uiState.categories.isNotEmpty()) {
            item {
                EditorialRevealSection(animationsEnabled = animationsEnabled, delayMillis = 120) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EditorialSectionHeader(
                            eyebrow = "CURATED WARDROBE",
                            title = "Shop by Category",
                            actionLabel = "All categories →",
                            onAction = onNavigateCategories
                        )

                        BoxWithConstraints {
                            val cols = if (maxWidth >= 600.dp) 3 else 2
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.categories.take(6).chunked(cols).forEachIndexed { rowIdx, rowCats ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowCats.forEach { cat ->
                                            val tileHeight = if (rowIdx == 0) 174.dp else 144.dp
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(tileHeight)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(ext.darkEditorialBg)
                                                    .clickable { onNavigateShop(cat.slug, "", "") }
                                            ) {
                                                MelvoreNetworkImage(
                                                    url = cat.imageUrl,
                                                    contentDescription = cat.name,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))
                                                            )
                                                        )
                                                )
                                                if (cat.isFeatured) {
                                                    Surface(
                                                        shape = RoundedCornerShape(2.dp),
                                                        color = Color.White,
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(10.dp)
                                                    ) {
                                                        Text(
                                                            "EDIT",
                                                            color = Color.Black,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.9.sp,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(12.dp)
                                                ) {
                                                    Text(
                                                        cat.name,
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontFamily = MelvoreBrandFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        "${cat.itemCount} styles",
                                                        color = Color.White.copy(alpha = 0.78f),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                        repeat(cols - rowCats.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Featured Collections Runway
        if (uiState.collections.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "LOOKBOOK CHAPTERS",
                        title = "Featured Collections",
                        actionLabel = "Catalog →",
                        onAction = { onNavigateShop("all", "", "") },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(uiState.collections, key = { "col_${it.id}" }) { col ->
                            Box(
                                modifier = Modifier
                                    .width(288.dp)
                                    .height(194.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onNavigateShop("all", col.slug, "") }
                            ) {
                                MelvoreNetworkImage(
                                    url = col.bannerUrl,
                                    contentDescription = col.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.88f))
                                            )
                                        )
                                )
                                if (col.subtitle.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = Color.Black.copy(alpha = 0.82f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = col.subtitle.uppercase(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = col.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = MelvoreBrandFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = Color.White,
                                        contentColor = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Signature Multi-Brand Pieces Grid
        if (featuredProducts.isNotEmpty()) {
            item {
                EditorialSectionHeader(
                    eyebrow = "CURATED SELECTION",
                    title = "Signature Pieces",
                    actionLabel = "View all →",
                    onAction = { onNavigateShop("all", "", "") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(featuredProducts.chunked(2)) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { product ->
                        ProductCard(
                            product = product,
                            currency = uiState.currency,
                            isWishlisted = uiState.wishlistIds.contains(product.id),
                            onProductClick = { onOpenProduct(product) },
                            onWishlistClick = { onToggleWishlist(product.id) },
                            onAddToCartClick = { onQuickAdd(product) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // 9. Editorial Journal Stories
        if (uiState.blogs.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "EDITORIAL JOURNAL",
                        title = "Brand & Lookbook Stories",
                        actionLabel = "Read all →",
                        onAction = onNavigateBlog,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(uiState.blogs.take(5), key = { "story_${it.id}" }) { post ->
                            Surface(
                                onClick = onNavigateBlog,
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, ext.borderSubtle),
                                modifier = Modifier.width(244.dp)
                            ) {
                                Column {
                                    MelvoreNetworkImage(
                                        url = post.imageUrl,
                                        contentDescription = post.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(136.dp)
                                    )
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            post.category.uppercase(),
                                            color = ext.muted,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            post.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 10. Full-Width Monochrome House Manifesto & Client Voices
        if (design.siteStats.isNotEmpty() || design.testimonials.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = Color.Black
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MelvoreWordmark(fontSize = 18.sp, color = Color.White)
                                Text(
                                    "GLOBAL HOUSE",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.4.sp
                                )
                            }
                            Text(
                                "Our Story →",
                                color = Color.White.copy(alpha = 0.88f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.clickable(onClick = onNavigateAbout)
                            )
                        }

                        Text(
                            "Curated domestic and imported labels delivered across India with verified provenance.",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = MelvoreBrandFontFamily
                        )

                        if (design.siteStats.isNotEmpty()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                design.siteStats.take(3).forEach { stat ->
                                    Column {
                                        Text(stat.value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                                        Text(stat.label, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (design.testimonials.isNotEmpty()) {
                            val t = design.testimonials[testimonialIndex % design.testimonials.size]
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.FormatQuote, contentDescription = null, tint = Color.White.copy(alpha = 0.55f))
                                        if (design.testimonials.size > 1) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                LiquidGlassMediaIconButton(
                                                    onClick = { testimonialIndex = (testimonialIndex - 1 + design.testimonials.size) % design.testimonials.size },
                                                    contentDescription = "Previous review"
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                                LiquidGlassMediaIconButton(
                                                    onClick = { testimonialIndex = (testimonialIndex + 1) % design.testimonials.size },
                                                    contentDescription = "Next review"
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                    Text("“${t.content}”", color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyLarge)
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            if (!t.customerImage.isNullOrBlank()) {
                                                MelvoreNetworkImage(
                                                    url = t.customerImage,
                                                    contentDescription = t.customerName,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                )
                                            }
                                            Column {
                                                Text(t.customerName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                                Text(t.customerTitle, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Text("★".repeat(t.rating.coerceIn(1, 5)), color = ext.campaignSun, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 11. Feature Guarantee Grid
        if (design.featureTiles.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    design.featureTiles.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { tile ->
                                val icon = when {
                                    tile.icon.contains("truck") || tile.icon.contains("shipping") -> Icons.Outlined.LocalShipping
                                    tile.icon.contains("shield") -> Icons.Outlined.Shield
                                    tile.icon.contains("undo") || tile.icon.contains("rotate") -> Icons.Outlined.Refresh
                                    else -> Icons.Outlined.HeadsetMic
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, ext.borderSubtle),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(2.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(tile.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(tile.description, style = MaterialTheme.typography.bodySmall, color = ext.muted, maxLines = 3)
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialSectionHeader(
    eyebrow: String,
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.4.sp,
                color = ext.muted,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = MelvoreBrandFontFamily
            )
        }
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
