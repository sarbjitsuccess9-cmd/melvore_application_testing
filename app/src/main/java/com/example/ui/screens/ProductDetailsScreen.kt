package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.example.model.Product
import com.example.model.formatPrice
import com.example.model.parseHexColor
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.ProductCard
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun ProductDetailsScreen(
    productId: Int,
    uiState: MelvoreUiState,
    onLoadDetails: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onAddToCart: (Product, String, String, Int) -> Unit,
    onBuyNow: (Product, String, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current

    LaunchedEffect(productId) {
        onLoadDetails(productId)
    }

    val product = uiState.selectedProduct?.takeIf { it.id == productId }
        ?: uiState.products.find { it.id == productId }
        ?: uiState.catalogProducts.find { it.id == productId }

    if (product == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isProductDetailsLoading -> MelvoreLoadingState("Loading product details…")
                uiState.productDetailsError != null -> MelvoreErrorState(
                    message = uiState.productDetailsError,
                    onRetry = { onLoadDetails(productId) }
                )
                else -> MelvoreErrorState(
                    message = "Product not found.",
                    onRetry = onBack
                )
            }
        }
        return
    }

    var selectedColor by remember(product.id, product.colors) {
        mutableStateOf(product.colors.firstOrNull()?.name ?: "")
    }
    var selectedSize by remember(product.id, product.sizes) {
        mutableStateOf(product.sizes.firstOrNull() ?: "")
    }
    var quantity by remember(product.id) { mutableIntStateOf(1) }

    val selectedVariation = remember(product.variations, selectedColor, selectedSize) {
        product.variations.find { v ->
            v.isActive &&
                (selectedColor.isBlank() || v.color.equals(selectedColor, ignoreCase = true) || v.color.isBlank()) &&
                (selectedSize.isBlank() || v.size.equals(selectedSize, ignoreCase = true) || v.size.isBlank())
        }
    }

    val effectivePrice = selectedVariation?.salePrice ?: selectedVariation?.regularPrice ?: product.effectivePrice
    val effectiveRegular = selectedVariation?.regularPrice ?: product.regularPrice
    val effectiveInStock = selectedVariation?.let { it.stockQuantity > 0 } ?: product.inStock

    val gallery = remember(product.id, product.imageUrl, product.galleryUrls, selectedVariation?.imageUrl) {
        (listOfNotNull(selectedVariation?.imageUrl, product.imageUrl) + product.galleryUrls)
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("") }
    }
    val pagerState = rememberPagerState(pageCount = { gallery.size })
    val isWishlisted = uiState.wishlistIds.contains(product.id)
    val related = remember(product.id, product.relatedProducts, uiState.products) {
        product.relatedProducts.ifEmpty {
            uiState.products.filter { it.id != product.id }.take(4)
        }
    }
    val plainDescription = remember(product.description) {
        HtmlCompat.fromHtml(product.description, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .trim()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack)
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Shop", modifier = Modifier.size(18.dp))
                    Text("Back to Shop", style = MaterialTheme.typography.labelLarge, color = ext.muted)
                }
                if (uiState.isProductDetailsLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Image Gallery Pager
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3.4f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    MelvoreNetworkImage(
                        url = gallery[page].takeIf { it.isNotBlank() },
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (gallery.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gallery.indices.forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .height(5.dp)
                                    .width(if (pagerState.currentPage == idx) 22.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (pagerState.currentPage == idx) Color.White else Color.White.copy(alpha = 0.45f))
                            )
                        }
                    }
                }
            }
        }

        // Purchase Controls Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(product.category, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        Text("·", color = ext.muted)
                        Text(
                            "${"%.1f".format(product.rating)} ★ (${product.reviewCount} reviews)",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = ext.muted
                        )
                        Text("·", color = ext.muted)
                        Text(
                            if (effectiveInStock) "In Stock" else "Out of Stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (effectiveInStock) ext.emerald else ext.crimson
                        )
                    }

                    Text(product.name, style = MaterialTheme.typography.headlineMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatPrice(effectivePrice, uiState.currency),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (effectiveRegular > effectivePrice) {
                            Text(
                                text = formatPrice(effectiveRegular, uiState.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = ext.muted,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Text(
                                text = "Save ${product.discountPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.emerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (plainDescription.isNotBlank()) {
                        Text(plainDescription, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                    }

                    if (product.colors.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Color: $selectedColor",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                product.colors.forEach { variant ->
                                    val active = selectedColor == variant.name
                                    Surface(
                                        onClick = { selectedColor = variant.name },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (active) 1.5.dp else 1.dp,
                                            if (active) MaterialTheme.colorScheme.onBackground else ext.borderSubtle
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(parseHexColor(variant.hex)))
                                                    .border(1.dp, ext.borderSubtle, CircleShape)
                                            )
                                            Text(variant.name, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (product.sizes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select Size", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                product.sizes.forEach { size ->
                                    val active = selectedSize == size
                                    Surface(
                                        onClick = { selectedSize = size },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (active) null else BorderStroke(1.dp, ext.borderSubtle),
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                size,
                                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.borderSubtle)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
                                IconButton(onClick = { if (quantity > 1) quantity-- }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    quantity.toString(),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { quantity++ }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onToggleWishlist(product.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isWishlisted) ext.crimson else ext.borderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isWishlisted) ext.crimson else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isWishlisted) "Saved in Wishlist" else "Add to Wishlist",
                                color = if (isWishlisted) ext.crimson else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onAddToCart(product, selectedColor, selectedSize, quantity) },
                            enabled = effectiveInStock,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("details_add_to_cart")
                        ) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (effectiveInStock) "Add to Cart" else "Out of Stock")
                        }

                        OutlinedButton(
                            onClick = { onBuyNow(product, selectedColor, selectedSize, quantity) },
                            enabled = effectiveInStock,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("details_buy_now")
                        ) {
                            Text("Buy Now")
                        }
                    }

                    HorizontalDivider(color = ext.borderSubtle)

                    val specLine = listOfNotNull(
                        product.sku.takeIf { it.isNotBlank() }?.let { "SKU: $it" },
                        product.dimensions.takeIf { it.isNotBlank() }?.let { "Fit: $it" },
                        product.weight?.let { "Weight: ${it}g" }
                    ).joinToString(" · ")
                    if (specLine.isNotBlank()) {
                        Text(specLine, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PerkTag(Icons.Outlined.LocalShipping, "Free > ₹999")
                        PerkTag(Icons.Outlined.VerifiedUser, "COD Ready")
                        PerkTag(Icons.Outlined.Refresh, "30d Returns")
                    }
                }
            }
        }

        // Verified Reviews
        if (product.reviews.isNotEmpty()) {
            item {
                Text("Customer Reviews (${product.reviews.size})", style = MaterialTheme.typography.headlineSmall)
            }
            items(product.reviews, key = { it.id }) { rev ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rev.userName, style = MaterialTheme.typography.titleMedium)
                            Text("★".repeat(rev.rating.coerceIn(1, 5)), color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelLarge)
                        }
                        if (rev.title.isNotBlank()) {
                            Text(rev.title, style = MaterialTheme.typography.labelLarge)
                        }
                        Text(rev.comment, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }
                }
            }
        }

        // Related Products
        if (related.isNotEmpty()) {
            item {
                Text("You may also like", style = MaterialTheme.typography.headlineSmall)
            }

            items(related.chunked(2)) { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { rel ->
                        ProductCard(
                            product = rel,
                            currency = uiState.currency,
                            isWishlisted = uiState.wishlistIds.contains(rel.id),
                            onProductClick = { onOpenProduct(rel) },
                            onWishlistClick = { onToggleWishlist(rel.id) },
                            onAddToCartClick = {
                                onAddToCart(rel, rel.colors.firstOrNull()?.name ?: "", rel.sizes.firstOrNull() ?: "", 1)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PerkTag(icon: ImageVector, label: String) {
    val ext = LocalMelvoreColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = ext.muted, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = ext.muted)
    }
}
