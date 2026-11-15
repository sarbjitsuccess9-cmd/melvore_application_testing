package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import com.example.model.Product
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.ProductCard
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.MelvoreUiState
import kotlinx.coroutines.delay

@Composable
fun ShopScreen(
    uiState: MelvoreUiState,
    initialCategory: String,
    initialCollection: String,
    initialQuery: String,
    initialBrandId: Int = 0,
    onFetchCatalog: (category: String, collection: String, search: String, sort: String, brandId: Int) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    var activeCategory by remember(initialCategory) { mutableStateOf(initialCategory.ifBlank { "all" }) }
    var activeCollection by remember(initialCollection) { mutableStateOf(initialCollection) }
    var activeBrandId by remember(initialBrandId) { mutableStateOf(initialBrandId) }
    var localQuery by remember(initialQuery) { mutableStateOf(initialQuery) }
    var sortBy by remember { mutableStateOf("featured") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(activeCategory, activeCollection, localQuery, sortBy, activeBrandId) {
        if (localQuery.isNotBlank()) delay(250)
        onFetchCatalog(activeCategory, activeCollection, localQuery, sortBy, activeBrandId)
    }

    val sortLabel = when (sortBy) {
        "price_asc" -> "Price: Low to High"
        "price_desc" -> "Price: High to Low"
        "rating" -> "Top Rated"
        "newest" -> "Newest"
        else -> "Featured"
    }

    val displayedProducts = uiState.catalogProducts

    val activeBrand = remember(uiState.brands, activeBrandId) {
        uiState.brands.find { it.id == activeBrandId }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            activeBrand != null -> activeBrand.name
                            activeCollection.isNotBlank() -> activeCollection.replace("-", " ").split(" ").joinToString(" ") { w ->
                                w.replaceFirstChar { it.uppercase() }
                            }
                            else -> "All Products"
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${displayedProducts.size} ${if (displayedProducts.size == 1) "item" else "items"} available",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = ext.muted
                    )
                }
                if (activeCollection.isNotBlank() || activeBrandId > 0) {
                    OutlinedButton(
                        onClick = {
                            activeCollection = ""
                            activeCategory = "all"
                            activeBrandId = 0
                        },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Clear filter", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = localQuery,
                    onValueChange = { localQuery = it },
                    placeholder = { Text("Search jackets, labels, silhouettes…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (localQuery.isNotEmpty()) {
                            IconButton(onClick = { localQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear filter")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_search_input")
                )

                if (uiState.brands.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryFilterChip("All Brands", activeBrandId == 0) { activeBrandId = 0 }
                        uiState.brands.forEach { b ->
                            CategoryFilterChip(b.name, activeBrandId == b.id) { activeBrandId = b.id }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryFilterChip("All", activeCategory == "all") { activeCategory = "all" }
                        uiState.categories.forEach { cat ->
                            CategoryFilterChip(cat.name, activeCategory == cat.slug) { activeCategory = cat.slug }
                        }
                    }

                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        Surface(
                            onClick = { sortMenuExpanded = true },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier.testTag("shop_sort_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Outlined.FilterList, contentDescription = "Sort", modifier = Modifier.size(16.dp))
                                Text(sortLabel, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            listOf(
                                "featured" to "Featured",
                                "newest" to "Newest",
                                "price_asc" to "Price: Low to High",
                                "price_desc" to "Price: High to Low",
                                "rating" to "Top Rated"
                            ).forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        sortBy = key
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isCatalogLoading) {
            item {
                MelvoreLoadingState("Fetching products from Melvore…")
            }
        } else if (uiState.catalogError != null) {
            item {
                MelvoreErrorState(
                    message = uiState.catalogError,
                    onRetry = { onFetchCatalog(activeCategory, activeCollection, localQuery, sortBy, activeBrandId) }
                )
            }
        } else if (displayedProducts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No matching products found", style = MaterialTheme.typography.titleMedium)
                        Text("Try clearing your search or switching filters.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        Button(
                            onClick = {
                                activeCategory = "all"
                                activeCollection = ""
                                activeBrandId = 0
                                localQuery = ""
                                sortBy = "featured"
                            },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text("Reset Filters")
                        }
                    }
                }
            }
        } else {
            items(displayedProducts.chunked(2)) { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, ext.borderSubtle)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else ext.muted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun CategoriesScreen(
    uiState: MelvoreUiState,
    onRetry: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current

    if (uiState.isHomeLoading && uiState.categories.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MelvoreLoadingState("Loading categories…")
        }
        return
    }

    if (uiState.homeError != null && uiState.categories.isEmpty()) {
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Categories", style = MaterialTheme.typography.headlineMedium)
                Text("Browse Melvore India collections by silhouette and fabric.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
            }
        }

        items(uiState.categories.chunked(2)) { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { cat ->
                    Surface(
                        onClick = { onSelectCategory(cat.slug) },
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!cat.imageUrl.isNullOrBlank()) {
                                MelvoreNetworkImage(
                                    url = cat.imageUrl,
                                    contentDescription = cat.name,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            cat.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Column {
                                Text(cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${cat.itemCount} styles", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = ext.muted)
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (uiState.collections.isNotEmpty()) {
            item {
                Text("Featured Edits", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
            }

            items(uiState.collections, key = { it.id }) { col ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSelectCollection(col.slug) }
                ) {
                    MelvoreNetworkImage(
                        url = col.bannerUrl,
                        contentDescription = col.name,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        if (col.subtitle.isNotBlank()) {
                            Text(col.subtitle, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(col.name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
