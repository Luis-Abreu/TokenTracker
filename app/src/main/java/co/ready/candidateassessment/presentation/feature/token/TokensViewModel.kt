package co.ready.candidateassessment.presentation.feature.token

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ready.candidateassessment.common.Outcome
import co.ready.candidateassessment.common.formatTokenBalance
import co.ready.candidateassessment.common.mapSuccess
import co.ready.candidateassessment.domain.TokenService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.map

@OptIn(FlowPreview::class)
@HiltViewModel
class TokensViewModel @Inject constructor(
    private val tokenService: TokenService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _tokens = MutableStateFlow<List<TopTokenViewState>>(emptyList())
    val tokens = _tokens.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _searchText = MutableStateFlow(savedStateHandle.get<String>(KEY_SEARCH_TEXT) ?: "")
    val searchText = _searchText.asStateFlow()

    private var balanceFetchJob: Job? = null
    private var hasInitiallyLoaded = false

    val displayedTokens = combine(_tokens, _searchText) { tokens, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            filterTokenList(query, tokens)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Load tokens once on initialization
        getTopTokens()

        viewModelScope.launch {
            _searchText
                .debounce(300) // 300ms debounce
                .distinctUntilChanged()
                .collect { query ->
                    // Persist search text to SavedStateHandle
                    savedStateHandle[KEY_SEARCH_TEXT] = query

                    if (query.isNotBlank()) {
                        val filteredTokens = _tokens.value.filter {
                            it.name.contains(query, ignoreCase = true) ||
                                it.symbol.contains(query, ignoreCase = true)
                        }
                        fetchBalancesForTokens(filteredTokens)
                    }
                }
        }
    }

    companion object {
        private const val KEY_SEARCH_TEXT = "search_text"
    }

    /**
     * Loads the top tokens from the service
     */
    fun getTopTokens(forceRefresh: Boolean = false) {
        // Skip if already loaded and not forcing a refresh
        if (hasInitiallyLoaded && !forceRefresh) {
            return
        }

        viewModelScope.launch {
            // reset state
            _isLoading.value = true
            _error.value = null

            tokenService.getTopTokens(forceRefresh).collect { tokensOutcome ->
                when (tokensOutcome) {
                    is Outcome.Success -> {
                        _isLoading.value = false
                        _error.value = null
                        hasInitiallyLoaded = true

                        _tokens.value = tokensOutcome.data.map { token ->
                            TopTokenViewState(
                                address = token.address,
                                name = token.name,
                                symbol = token.symbol,
                                decimals = token.decimals,
                                image = token.image,
                                balance = Outcome.Loading, // show as loading until searched
                                lastUpdated = System.currentTimeMillis(),
                                priceRate = token.price?.rate,
                                priceCurrency = token.price?.currency,
                                priceDiff = token.price?.diff,
                                marketCapUsd = token.price?.marketCapUsd,
                                volume24h = token.price?.volume24h,
                                holdersCount = token.holdersCount,
                                totalSupply = token.totalSupply
                            )
                        }

                        // if there's search text, fetch balances for the filtered tokens
                        if (_searchText.value.isNotEmpty()) {
                            fetchBalancesForTokens(
                                filterTokenList(_searchText.value, _tokens.value),
                                forceRefresh = forceRefresh
                            )
                        }
                    }
                    is Outcome.Error -> {
                        _isLoading.value = false
                        _error.value = tokensOutcome.message
                        Log.e("TokensViewModel", "Error loading tokens: ${tokensOutcome.message}")
                    }
                    Outcome.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    /**
     * Updates the search text, which will trigger debounced filtering and balance fetching
     */
    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    /**
     * Fetches balances for the given list of tokens
     */
    private fun fetchBalancesForTokens(tokens: List<TopTokenViewState>, forceRefresh: Boolean = false) {
        balanceFetchJob?.cancel()

        if (tokens.isEmpty()) return

        Log.d("TokensViewModel", "fetching balance for the following tokens: ${tokens.map { it.name }}")
        balanceFetchJob = viewModelScope.launch {
            tokens.forEach { token ->
                launch {
                    fetchTokenBalance(token.address, forceRefresh)
                }
            }
        }
    }

    private suspend fun fetchTokenBalance(tokenAddress: String, forceRefresh: Boolean = false) {
        tokenService.getTokenBalance(tokenAddress, forceRefresh)
            .collect { balanceOutcome ->
                val token = _tokens.value.find { it.address == tokenAddress } ?: return@collect

                val updatedBalance =
                    balanceOutcome.mapSuccess { rawBalance ->
                        formatTokenBalance(rawBalance, token.decimals)
                    }

                _tokens.value = _tokens.value.map { token ->
                    if (token.address == tokenAddress) {
                        token.copy(
                            balance = updatedBalance,
                            lastUpdated = System.currentTimeMillis()
                        )
                    } else {
                        token
                    }
                }
            }
    }

    fun retryTokenBalance(tokenAddress: String) {
        viewModelScope.launch {
            _tokens.value = _tokens.value.map { token ->
                if (token.address == tokenAddress) {
                    token.copy(balance = Outcome.Loading, lastUpdated = System.currentTimeMillis())
                } else {
                    token
                }
            }

            // fetch the balance again with force refresh
            fetchTokenBalance(tokenAddress, forceRefresh = true)
        }
    }

    fun retryLoadTokens() {
        getTopTokens(forceRefresh = true)
    }

    fun clearCache() {
        viewModelScope.launch {
            // cancel any ongoing balance fetches
            balanceFetchJob?.cancel()
            balanceFetchJob = null

            tokenService.clearCache()

            _tokens.value = emptyList()
            hasInitiallyLoaded = false

            // reload tokens
            getTopTokens()
        }
    }

    private fun filterTokenList(query: String, tokens: List<TopTokenViewState>): List<TopTokenViewState> =
        tokens.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.symbol.contains(query, ignoreCase = true)
        }

    data class TopTokenViewState(
        val address: String,
        val name: String,
        val symbol: String,
        val decimals: Int,
        val image: String,
        val balance: Outcome<String> = Outcome.Loading,
        val lastUpdated: Long = System.currentTimeMillis(),
        val priceRate: Double? = null,
        val priceCurrency: String? = null,
        val priceDiff: Double? = null,
        val marketCapUsd: Double? = null,
        val volume24h: Double? = null,
        val holdersCount: Long? = null,
        val totalSupply: String? = null
    )
}
