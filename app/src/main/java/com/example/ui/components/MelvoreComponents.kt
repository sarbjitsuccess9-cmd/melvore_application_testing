package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Product
import com.example.model.User
import com.example.model.formatPrice
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.MelvoreBrandFontFamily

@Composable
fun MelvoreWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFamily = MelvoreBrandFontFamily,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.4).sp
                )
            ) {
                append("Melv")
            }
            withStyle(
                SpanStyle(
                    fontFamily = MelvoreBrandFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            ) {
                append("ore")
            }
        },
        fontFamily = MelvoreBrandFontFamily,
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}

@Composable
fun MelvoreNetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null,
    @DrawableRes fallbackDrawable: Int? = null
) {
    val ext = LocalMelvoreColors.current
    if (url.isNullOrBlank()) {
        if (fallbackDrawable != null) {
            Image(
                painter = painterResource(fallbackDrawable),
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = colorFilter,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    contentDescription = contentDescription,
                    tint = ext.muted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        colorFilter = colorFilter,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        error = {
            if (fallbackDrawable != null) {
                Image(
                    painter = painterResource(fallbackDrawable),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ImageNotSupported,
                        contentDescription = contentDescription,
                        tint = ext.muted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun MelvoreLoadingState(
    message: String = "Syncing with Melvore servers…",
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = ext.muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MelvoreErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.crimson.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ext.crimson.copy(alpha = 0.12f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = ext.crimson,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = "Connection Issue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("error_retry_button")
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
fun LiquidGlassMediaIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.36f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun MelvoreTopBar(
    isDark: Boolean,
    isMenuOpen: Boolean = false,
    onMenuClick: () -> Unit = {},
    onLogoClick: () -> Unit,
    onTrackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onMenuClick,
                        shape = RoundedCornerShape(4.dp),
                        color = if (isMenuOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("top_menu_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = if (isMenuOpen) "Close navigation menu" else "Open navigation menu",
                                modifier = Modifier.size(19.dp),
                                tint = if (isMenuOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onLogoClick)
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .testTag("top_bar_logo")
                    ) {
                        MelvoreWordmark(fontSize = 25.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onToggleTheme,
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("top_theme_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = onTrackClick,
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("top_track_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.LocalShipping,
                                contentDescription = stringResource(R.string.cd_track_order),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = onSearchClick,
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("top_search_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.cd_search),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ext.borderSubtle)
            )
        }
    }
}

@Composable
fun MelvoreBottomBar(
    currentSection: String,
    user: User?,
    wishlistCount: Int,
    cartCount: Int,
    onHomeClick: () -> Unit,
    onShopClick: () -> Unit,
    onMeClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val activeColor = MaterialTheme.colorScheme.onBackground
    val inactiveColor = ext.muted
    val initials = user?.name
        ?.trim()
        ?.split(" ")
        ?.filter { it.isNotBlank() }
        ?.take(2)
        ?.joinToString("") { it.first().uppercaseChar().toString() }
        ?: ""

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(66.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavSlot(
                selected = currentSection == "home",
                label = "Home",
                onClick = onHomeClick,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_home")
            ) {
                Icon(
                    imageVector = if (currentSection == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(21.dp)
                )
            }

            BottomNavSlot(
                selected = currentSection == "shop",
                label = "Shop",
                onClick = onShopClick,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_shop")
            ) {
                Icon(
                    imageVector = if (currentSection == "shop") Icons.Filled.Storefront else Icons.Outlined.Storefront,
                    contentDescription = "Shop",
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onMeClick)
                    .testTag("nav_me")
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (currentSection == "profile") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, ext.borderSubtle),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .offset(y = (-4).dp)
                        .size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (initials.isNotEmpty()) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentSection == "profile") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = stringResource(R.string.cd_profile),
                                modifier = Modifier.size(19.dp),
                                tint = if (currentSection == "profile") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Text(
                    text = "Me",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (currentSection == "profile") activeColor else inactiveColor,
                    modifier = Modifier.offset(y = (-3).dp)
                )
            }

            BottomNavSlot(
                selected = currentSection == "wishlist",
                label = "Wishlist",
                onClick = onWishlistClick,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_wishlist")
            ) {
                BadgedBox(
                    badge = {
                        if (wishlistCount > 0) {
                            Badge(containerColor = ext.crimson, contentColor = Color.White) {
                                Text(wishlistCount.toString(), fontSize = 9.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentSection == "wishlist") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_wishlist),
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            BottomNavSlot(
                selected = false,
                label = "Cart",
                onClick = onCartClick,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_cart")
            ) {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(containerColor = ext.crimson, contentColor = Color.White) {
                                Text(cartCount.toString(), fontSize = 9.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = stringResource(R.string.cd_cart),
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavSlot(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (selected) activeColor else inactiveColor
        ) {
            icon()
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeColor else inactiveColor
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    currency: String,
    isWishlisted: Boolean,
    onProductClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val imageScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.985f
            isHovered -> 1.03f
            else -> 1.0f
        },
        animationSpec = tween(durationMillis = 220),
        label = "product_card_hover"
    )

    val badgeLabel = when (product.badge?.uppercase()) {
        "NEW" -> "NEW"
        "HOT" -> "POPULAR"
        "BESTSELLER" -> "BESTSELLER"
        "EXCLUSIVE" -> "ATELIER"
        "LUXURY" -> "LUXURY"
        "TREND" -> "TRENDING"
        else -> product.badge?.takeIf { it.isNotBlank() }?.uppercase()
    }
    val brandTag = product.brandName.takeIf { it.isNotBlank() }
        ?: if (product.isImported) product.originCountry.ifBlank { "IMPORTED" } else ""

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 3.6f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onProductClick
                    )
            ) {
                MelvoreNetworkImage(
                    url = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                        }
                )
                if (badgeLabel != null || product.hasDiscount) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = if (product.hasDiscount) ext.crimson else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (product.hasDiscount) "-${product.discountPercent}%" else badgeLabel!!,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                LiquidGlassMediaIconButton(
                    onClick = onWishlistClick,
                    contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .testTag("wishlist_btn_${product.id}")
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isWishlisted) ext.crimson else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (brandTag.isNotBlank()) brandTag.uppercase() else product.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (product.rating > 0.0) {
                        Text("·", color = ext.muted, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${"%.1f".format(product.rating)} ★",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = ext.muted
                        )
                    }
                }

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onProductClick)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = formatPrice(product.effectivePrice, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (product.hasDiscount) {
                            Text(
                                text = formatPrice(product.regularPrice, currency),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = ext.muted,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    Button(
                        onClick = onAddToCartClick,
                        enabled = product.inStock,
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("add_to_cart_${product.id}")
                    ) {
                        if (product.inStock) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = "Add ${product.name} to cart",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("Add", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Text("Sold Out", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
