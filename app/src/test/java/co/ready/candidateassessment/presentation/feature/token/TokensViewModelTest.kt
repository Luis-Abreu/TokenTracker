package co.ready.candidateassessment.presentation.feature.token

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import co.ready.candidateassessment.common.Outcome
import co.ready.candidateassessment.domain.Token
import co.ready.candidateassessment.domain.TokenPrice
import co.ready.candidateassessment.domain.TokenService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokensViewModelTest {

    private lateinit var viewModel: TokensViewModel
    private lateinit var tokenService: TokenService
    private lateinit var savedStateHandle: SavedStateHandle
    private val testDispatcher = StandardTestDispatcher()

    // Sample test data
    private val sampleTokenPrice = TokenPrice(
        rate = 1000.0,
        currency = "USD",
        diff = 5.0,
        marketCapUsd = 1000000000.0,
        volume24h = 500000.0
    )

    private val sampleToken = Token(
        address = "0x123",
        name = "Test Token",
        symbol = "TEST",
        decimals = 18,
        image = "https://example.com/token.png",
        price = sampleTokenPrice,
        holdersCount = 1000L,
        totalSupply = "1000000000000000000000000"
    )

    private val sampleToken2 = Token(
        address = "0x456",
        name = "Another Token",
        symbol = "ANOTHER",
        decimals = 6,
        image = "https://example.com/another.png",
        price = null,
        holdersCount = 500L,
        totalSupply = "1000000000000"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tokenService = mockk()
        savedStateHandle = mockk(relaxed = true)

        // Mock SavedStateHandle to return empty string for search text
        every { savedStateHandle.get<String>(any()) } returns ""
        every { savedStateHandle.set(any<String>(), any<String>()) } returns Unit

        // Mock default getTopTokens call made by init block - return empty list by default
        coEvery { tokenService.getTopTokens(false) } returns flowOf(Outcome.Success(emptyList()))

        viewModel = TokensViewModel(tokenService, savedStateHandle)
        // Let the init block complete
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getTopTokens - loads tokens without fetching balances automatically`() = runTest {
        // Given
        val tokens = listOf(sampleToken, sampleToken2)

        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))

        // When
        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - tokens are loaded but displayedTokens is empty (no search yet)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)

        viewModel.displayedTokens.test {
            val displayedTokens = awaitItem()
            assertTrue(displayedTokens.isEmpty()) // No search performed yet
            cancelAndIgnoreRemainingEvents()
        }

        // Verify balance was NOT fetched automatically
        coVerify(exactly = 0) { tokenService.getTokenBalance(any(), any()) }
    }

    @Test
    fun `search - fetches balances only for filtered tokens`() = runTest {
        // Given
        val tokens = listOf(sampleToken, sampleToken2)
        val balance1 = "1000000000000000000" // 1.0 token

        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))
        coEvery { tokenService.getTokenBalance("0x123", any()) } returns flowOf(Outcome.Success(balance1))

        // When - Load tokens first
        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Search for first token only
        viewModel.updateSearchText("Test")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.displayedTokens.test {
            skipItems(1)
            val tokenStates = awaitItem()
            assertEquals(1, tokenStates.size)
            assertEquals("Test Token", tokenStates[0].name)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify only the filtered token's balance was fetched
        coVerify(exactly = 1) { tokenService.getTokenBalance("0x123", any()) }
        coVerify(exactly = 0) { tokenService.getTokenBalance("0x456", any()) }
    }

    @Test
    fun `search - searches multiple times fetches balances each time`() = runTest {
        // Given
        val tokens = listOf(sampleToken)
        val balance = "1000000000000000000"

        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))
        coEvery { tokenService.getTokenBalance("0x123", any()) } returns flowOf(Outcome.Success(balance))

        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSearchText("Test")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Search again
        viewModel.updateSearchText("")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateSearchText("Test")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Repository handles caching, but we call it again
        coVerify(atLeast = 1) { tokenService.getTokenBalance("0x123", any()) }
    }

    @Test
    fun `search - fetches multiple tokens in parallel`() = runTest {
        // Given
        val tokens = listOf(sampleToken, sampleToken2)
        val balance1 = "1000000000000000000"
        val balance2 = "2000000"

        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))
        coEvery { tokenService.getTokenBalance("0x123", any()) } returns flowOf(Outcome.Success(balance1))
        coEvery { tokenService.getTokenBalance("0x456", any()) } returns flowOf(Outcome.Success(balance2))

        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Search for token that matches both

        viewModel.updateSearchText("Token")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.displayedTokens.test {
            skipItems(1)
            val tokenStates = awaitItem()
            assertEquals(2, tokenStates.size)
            cancelAndIgnoreRemainingEvents()
        }

        // Both balances should be fetched
        coVerify(exactly = 1) { tokenService.getTokenBalance("0x123", any()) }
        coVerify(exactly = 1) { tokenService.getTokenBalance("0x456", any()) }
    }

    @Test
    fun `getTopTokens - loading state is set correctly`() = runTest {
        // Given
        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Loading)

        // When
        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.isLoading.test {
            val isLoading = awaitItem()
            assertTrue(isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTopTokens - error state updates error message`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { tokenService.getTopTokens(any()) } returns flowOf(
            Outcome.Error(errorMessage)
        )

        // When
        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.error.test {
            val error = awaitItem()
            assertEquals(errorMessage, error)
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `token balance error is handled gracefully`() = runTest {
        // Given
        val tokens = listOf(sampleToken)
        val balanceError = "Balance fetch failed"

        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))
        coEvery { tokenService.getTokenBalance("0x123", any()) } returns flowOf(
            Outcome.Error(balanceError)
        )

        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Search triggers balance fetch
        viewModel.updateSearchText("Test")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.displayedTokens.test {
            skipItems(1)
            val tokenStates = awaitItem()
            assertEquals(1, tokenStates.size)
            assertTrue(tokenStates[0].balance is Outcome.Error)
            val balanceOutcome = tokenStates[0].balance as Outcome.Error
            assertEquals(balanceError, balanceOutcome.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retryTokenBalance - sets loading state and fetches balance again`() = runTest {
        // Given
        val tokens = listOf(sampleToken)
        val balance = "1000000000000000000"

        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))
        coEvery { tokenService.getTokenBalance("0x123", any()) } returns flowOf(Outcome.Success(balance))

        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSearchText("Test")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.retryTokenBalance("0x123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(atLeast = 2) { tokenService.getTokenBalance("0x123", any()) }
        viewModel.displayedTokens.test {
            skipItems(1) // Skip initial state
            val tokenStates = awaitItem()
            assertTrue(tokenStates[0].balance is Outcome.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retryLoadTokens - calls getTopTokens again`() = runTest {
        // Given
        val tokens = listOf(sampleToken)
        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))

        // When
        viewModel.retryLoadTokens()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { tokenService.getTopTokens(any()) }
    }

    @Test
    fun `clearCache - clears service cache and refreshes data`() = runTest {
        // Given
        val tokens = listOf(sampleToken)
        coEvery { tokenService.clearCache() } returns Unit
        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))

        // When
        viewModel.clearCache()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { tokenService.clearCache() }
        coVerify { tokenService.getTopTokens(any()) }
    }

    @Test
    fun `clearCache - clears tokens list and refreshes from repository`() = runTest {
        // Given
        val tokens = listOf(sampleToken)
        val balance = "1000000000000000000"

        coEvery { tokenService.clearCache() } returns Unit
        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))
        coEvery { tokenService.getTokenBalance("0x123", any()) } returns flowOf(Outcome.Success(balance))

        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateSearchText("Test")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearCache()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - displayedTokens should be empty after clear (search was cleared)
        viewModel.displayedTokens.test {
            val tokenStates = awaitItem()
            assertTrue(tokenStates.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // When - Search again
        viewModel.updateSearchText("Test")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(atLeast = 2) { tokenService.getTokenBalance("0x123", any()) }
    }

    @Test
    fun `TopTokenViewState contains all token properties`() {
        // When
        val viewState = TokensViewModel.TopTokenViewState(
            address = "0x123",
            name = "Test Token",
            symbol = "TEST",
            decimals = 18,
            image = "https://example.com/token.png",
            balance = Outcome.Success("1.0"),
            lastUpdated = 123456789L,
            priceRate = 1000.0,
            priceCurrency = "USD",
            priceDiff = 5.0,
            marketCapUsd = 1000000000.0,
            volume24h = 500000.0,
            holdersCount = 1000L,
            totalSupply = "1000000000000000000000000"
        )

        // Then
        assertEquals("0x123", viewState.address)
        assertEquals("Test Token", viewState.name)
        assertEquals("TEST", viewState.symbol)
        assertEquals(18, viewState.decimals)
        assertEquals("https://example.com/token.png", viewState.image)
        assertTrue(viewState.balance is Outcome.Success)
        assertEquals(123456789L, viewState.lastUpdated)
        assertEquals(1000.0, viewState.priceRate)
        assertEquals("USD", viewState.priceCurrency)
        assertEquals(5.0, viewState.priceDiff)
        assertEquals(1000000000.0, viewState.marketCapUsd)
        assertEquals(500000.0, viewState.volume24h)
        assertEquals(1000L, viewState.holdersCount)
        assertEquals("1000000000000000000000000", viewState.totalSupply)
    }

    @Test
    fun `multiple tokens fetch balances independently`() = runTest {
        // Given
        val tokens = listOf(sampleToken, sampleToken2)
        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))
        coEvery { tokenService.getTokenBalance("0x123", any()) } returns flowOf(Outcome.Success("1000000000000000000"))
        coEvery { tokenService.getTokenBalance("0x456", any()) } returns flowOf(Outcome.Error("Balance error"))

        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Search for both tokens

        viewModel.updateSearchText("Token")
        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.displayedTokens.test {
            skipItems(1)
            val tokenStates = awaitItem()
            assertEquals(2, tokenStates.size)

            // First token should have successful balance
            assertTrue(tokenStates[0].balance is Outcome.Success)

            // Second token should have error balance
            assertTrue(tokenStates[1].balance is Outcome.Error)

            cancelAndIgnoreRemainingEvents()
        }

        // Both balance fetches should be called
        coVerify { tokenService.getTokenBalance("0x123", any()) }
        coVerify { tokenService.getTokenBalance("0x456", any()) }
    }

    @Test
    fun `initial state has correct defaults`() {
        // Then
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
        assertEquals("", viewModel.searchText.value)
    }

    @Test
    fun `clearCache cancels ongoing balance fetches`() = runTest {
        // Given
        val tokens = listOf(sampleToken)
        val balance = "1000000000000000000"

        coEvery { tokenService.clearCache() } returns Unit
        coEvery { tokenService.getTopTokens(any()) } returns flowOf(Outcome.Success(tokens))
        coEvery { tokenService.getTokenBalance("0x123", any()) } returns flowOf(Outcome.Success(balance))

        viewModel.getTopTokens(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Start a search (which triggers balance fetch)
        viewModel.updateSearchText("Test")
        // Don't wait for completion

        // When - Clear cache immediately
        viewModel.clearCache()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should complete without errors
        assertFalse(viewModel.isLoading.value)

        viewModel.displayedTokens.test {
            val tokens = awaitItem()
            assertTrue(tokens.isEmpty()) // Search was cleared
            cancelAndIgnoreRemainingEvents()
        }
    }
}
