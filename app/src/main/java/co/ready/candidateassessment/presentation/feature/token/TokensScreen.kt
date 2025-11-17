package co.ready.candidateassessment.presentation.feature.token

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ready.candidateassessment.R
import co.ready.candidateassessment.common.Outcome
import co.ready.candidateassessment.common.formatLargeNumber
import co.ready.candidateassessment.common.formatTimestamp
import co.ready.candidateassessment.presentation.design.ReadyTheme
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun TokensScreen(viewModel: TokensViewModel = hiltViewModel()) {
    val displayedTokens by viewModel.displayedTokens.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()

    Tokens(
        displayedTokens = displayedTokens,
        isLoading = isLoading,
        error = error,
        searchText = searchText,
        onRefresh = { viewModel.getTopTokens(forceRefresh = true) },
        onClearCache = { viewModel.clearCache() },
        onSearch = { viewModel.updateSearchText(it) },
        onRetryLoadTokens = { viewModel.retryLoadTokens() },
        onRetryLoadTokenBalance = { viewModel.retryTokenBalance(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tokens(
    displayedTokens: List<TokensViewModel.TopTokenViewState>,
    isLoading: Boolean,
    error: String?,
    searchText: String,
    onRetryLoadTokens: () -> Unit,
    onRetryLoadTokenBalance: (String) -> Unit,
    onRefresh: () -> Unit,
    onClearCache: () -> Unit,
    onSearch: (String) -> Unit
) {
    val hasLoadedTokens = !isLoading || displayedTokens.isNotEmpty()

    Scaffold(
        topBar = { TokensScreenTopBar(onClearCache = onClearCache) }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            isRefreshing = isLoading,
            onRefresh = { onRefresh() }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // search Bar
                TextField(
                    value = searchText,
                    onValueChange = onSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_for_your_tokens),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )

                // token List
                when {
                    isLoading && !hasLoadedTokens -> {
                        // initial loading state
                        TokensLoading()
                    }

                    error != null && !hasLoadedTokens -> {
                        // error card for when we DO NOT have cached data but have an error
                        TokenErrorCard(
                            error = error,
                            onRetryLoadTokens = onRetryLoadTokens
                        )
                    }

                    error != null && hasLoadedTokens -> {
                        // error banner for when we have cached data and an error
                        Column(modifier = Modifier.fillMaxSize()) {
                            TokenErrorBanner(onRetryLoadTokens = onRetryLoadTokens)

                            when {
                                displayedTokens.isEmpty() && searchText.isNotBlank() -> {
                                    TokenSearchNoResults()
                                }

                                searchText.isBlank() -> {
                                    TokenSearchEmptyState()
                                }

                                else -> {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        contentPadding = PaddingValues(vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(displayedTokens, key = { it.address }) { token ->
                                            TokenItem(
                                                token = token,
                                                onRetryBalance = onRetryLoadTokenBalance
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    displayedTokens.isEmpty() && searchText.isNotBlank() -> {
                        // empty state when search returns no results
                        TokenSearchNoResults()
                    }

                    searchText.isBlank() -> {
                        // empty state when user hasn't searched yet
                        TokenSearchEmptyState()
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedTokens, key = { it.address }) { token ->
                                TokenItem(
                                    token = token,
                                    onRetryBalance = onRetryLoadTokenBalance
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenSearchNoResults() {
    Box(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.no_tokens_found),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.try_searching_with_a_different_keyword),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TokenSearchEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "💎🤲",
                style = MaterialTheme.typography.displayMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.start_searching_for_your_tokens),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.enter_a_token_name_or_symbol_above_to_begin),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TokensLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.loading_tokens),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokensScreenTopBar(onClearCache: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.my_tokens),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = { onClearCache() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.clear_cache)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    )
}

@Composable
private fun TokenErrorBanner(onRetryLoadTokens: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.connection_error_showing_cached_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onRetryLoadTokens
            ) {
                Text(
                    text = stringResource(R.string.retry),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun TokenErrorCard(error: String?, onRetryLoadTokens: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.failed_to_load_tokens),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error ?: stringResource(R.string.unknown_error_occurred),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onRetryLoadTokens() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun TokenItem(token: TokensViewModel.TopTokenViewState, onRetryBalance: (String) -> Unit = {}) {
    val focusManager = LocalFocusManager.current
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable(interactionSource = null, indication = null) {
                focusManager.clearFocus()
                isExpanded = !isExpanded
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // token Logo and Info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(token.image)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.logo, token.name),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            },
                            error = {
                                // fallback to showing the first letter of the symbol
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = token.symbol.firstOrNull()?.toString()?.uppercase()
                                            ?: "?",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = token.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // balance
                Box(
                    contentAlignment = Alignment.CenterEnd
                ) {
                    when (token.balance) {
                        is Outcome.Success -> {
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Text(
                                        text = token.balance.data + " " + token.symbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        is Outcome.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        is Outcome.Error -> {
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = stringResource(R.string.tap_to_retry),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.clickable { onRetryBalance(token.address) }
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TokenDetailsCard(
                    token = token,
                    onRetryBalance = onRetryBalance
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun TokenDetailsCard(token: TokensViewModel.TopTokenViewState, onRetryBalance: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.details),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // price information
                if (token.priceRate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.price,
                                String.format(
                                    "%.4f",
                                    token.priceRate
                                ),
                                token.priceCurrency ?: ""
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (token.priceDiff != null) {
                            Text(
                                text = "${if (token.priceDiff >= 0) "+" else ""}${
                                    String.format(
                                        "%.2f",
                                        token.priceDiff
                                    )
                                }%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (token.priceDiff >= 0) {
                                    Color(0xFF4CAF50)
                                } else {
                                    Color(
                                        0xFFF44336
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // market cap
                if (token.marketCapUsd != null) {
                    Text(
                        text = stringResource(R.string.market_cap, formatLargeNumber(token.marketCapUsd)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // 24h Volume
                if (token.volume24h != null) {
                    Text(
                        text = stringResource(R.string._24h_volume, formatLargeNumber(token.volume24h)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // holders count
                if (token.holdersCount != null) {
                    Text(
                        text = stringResource(R.string.holders, String.format("%,d", token.holdersCount)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // contract address (shortened)
                Text(
                    text = stringResource(
                        R.string.contract,
                        token.address.take(10),
                        token.address.takeLast(
                            8
                        )
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.updated, formatTimestamp(token.lastUpdated)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onRetryBalance(token.address) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh_balance),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(text = stringResource(R.string.refresh_balance))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TokensScreenPreview() {
    ReadyTheme {
        Tokens(
            displayedTokens = listOf(
                TokensViewModel.TopTokenViewState(
                    address = "0x123",
                    name = "Ethereum",
                    symbol = "ETH",
                    decimals = 18,
                    image = "https://example.com/eth.png",
                    balance = Outcome.Success("1,234.56"),
                    lastUpdated = System.currentTimeMillis(),
                    priceRate = 2500.0,
                    priceCurrency = "USD",
                    priceDiff = 5.23,
                    marketCapUsd = 300_000_000_000.0,
                    volume24h = 15_000_000_000.0,
                    holdersCount = 1_234_567
                ),
                TokensViewModel.TopTokenViewState(
                    address = "0x456",
                    name = "USD Coin",
                    symbol = "USDC",
                    decimals = 6,
                    image = "https://example.com/usdc.png",
                    balance = Outcome.Loading,
                    lastUpdated = System.currentTimeMillis(),
                    priceRate = 1.0,
                    priceCurrency = "USD",
                    priceDiff = -0.02,
                    marketCapUsd = 25_000_000_000.0,
                    volume24h = 5_000_000_000.0,
                    holdersCount = 2_500_000
                ),
                TokensViewModel.TopTokenViewState(
                    address = "0x789",
                    name = "Wrapped Bitcoin",
                    symbol = "WBTC",
                    decimals = 8,
                    image = "https://example.com/wbtc.png",
                    balance = Outcome.Error("Failed to load"),
                    lastUpdated = System.currentTimeMillis(),
                    priceRate = 45000.0,
                    priceCurrency = "USD",
                    priceDiff = 3.15,
                    marketCapUsd = 8_000_000_000.0,
                    volume24h = 500_000_000.0,
                    holdersCount = 50_000
                )
            ),
            isLoading = false,
            error = null,
            searchText = "eth",
            onRetryLoadTokens = {},
            onRetryLoadTokenBalance = {},
            onRefresh = {},
            onClearCache = {},
            onSearch = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TokenItemPreview() {
    ReadyTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TokenItem(
                    token = TokensViewModel.TopTokenViewState(
                        address = "0x123",
                        name = "Ethereum",
                        symbol = "ETH",
                        decimals = 18,
                        image = "https://example.com/eth.png",
                        balance = Outcome.Success("1,234.56"),
                        lastUpdated = System.currentTimeMillis(),
                        priceRate = 2500.0,
                        priceCurrency = "USD",
                        priceDiff = 5.23,
                        marketCapUsd = 300_000_000_000.0,
                        volume24h = 15_000_000_000.0,
                        holdersCount = 1_234_567
                    )
                )
                TokenItem(
                    token = TokensViewModel.TopTokenViewState(
                        address = "0x456",
                        name = "USD Coin",
                        symbol = "USDC",
                        decimals = 6,
                        image = "https://example.com/usdc.png",
                        balance = Outcome.Loading,
                        lastUpdated = System.currentTimeMillis(),
                        priceRate = 1.0,
                        priceCurrency = "USD",
                        priceDiff = -0.02,
                        marketCapUsd = 25_000_000_000.0,
                        volume24h = 5_000_000_000.0,
                        holdersCount = 2_500_000
                    )
                )
                TokenItem(
                    token = TokensViewModel.TopTokenViewState(
                        address = "0x789",
                        name = "Wrapped Bitcoin",
                        symbol = "WBTC",
                        decimals = 8,
                        image = "https://example.com/wbtc.png",
                        balance = Outcome.Error("Failed to load"),
                        lastUpdated = System.currentTimeMillis(),
                        priceRate = 45000.0,
                        priceCurrency = "USD",
                        priceDiff = 3.15,
                        marketCapUsd = 8_000_000_000.0,
                        volume24h = 500_000_000.0,
                        holdersCount = 50_000
                    )
                )
            }
        }
    }
}
