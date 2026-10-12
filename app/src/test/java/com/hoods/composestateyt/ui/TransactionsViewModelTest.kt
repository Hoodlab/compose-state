package com.hoods.composestateyt.ui


import app.cash.turbine.test
import com.hoods.composestateyt.data.FakeTransactionRepository
import com.hoods.composestateyt.data.Transaction
import com.hoods.composestateyt.ui.transaction.ListState
import com.hoods.composestateyt.ui.transaction.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    //TODO FIX 5: test needs full state
    fun filterChangeResetsTheList() = runTest(testDispatcher) {
        val sampleList = listOf(
            Transaction(1, "Supermarket", "Groceries", -50.0, "2026-10-01"),
            Transaction(2, "Salary", "Income", 2000.0, "2026-10-02"),
        )
        val repository = FakeTransactionRepository(initialTransactions = sampleList, delayMillis = 0L)
        val viewModel = TransactionsViewModel(repository)

        viewModel.uiState.test {
            advanceUntilIdle()
            var state = expectMostRecentItem()
            val initialContent = state.list as ListState.Content
            assertEquals(2, initialContent.items.size)

            viewModel.onCategorySelected("Groceries")
            advanceUntilIdle()

            state = expectMostRecentItem()
            assertEquals("Groceries", state.filters.selectedCategory)
            val filteredContent = state.list as ListState.Content
            assertEquals(1, filteredContent.items.size)
            assertEquals("Supermarket", filteredContent.items.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }



    @Test
    fun loadMoreAppendsItems() = runTest(testDispatcher) {
        val repository = FakeTransactionRepository(delayMillis = 0L)
        val viewModel = TransactionsViewModel(repository)

        viewModel.uiState.test {
            advanceUntilIdle()
            var state = expectMostRecentItem()
            val initialContent = state.list as ListState.Content
            assertEquals(20, initialContent.items.size)
            assertTrue(initialContent.hasMore)

            viewModel.onLoadMore()
            advanceUntilIdle()

            state = expectMostRecentItem()
            val updatedContent = state.list as ListState.Content
            assertEquals(40, updatedContent.items.size)
            assertFalse(updatedContent.isLoadingMore)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failureProducesExpectedListState() = runTest(testDispatcher) {
        val repository = FakeTransactionRepository(delayMillis = 0L)
        repository.setFailNextCall(true)
        val viewModel = TransactionsViewModel(repository)

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state.list is ListState.Error)
            assertEquals("Simulated network error", (state.list as ListState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retryRecoversFromFailure() = runTest(testDispatcher) {
        val repository = FakeTransactionRepository(delayMillis = 0L)
        repository.setFailNextCall(true)
        val viewModel = TransactionsViewModel(repository)

        viewModel.uiState.test {
            advanceUntilIdle()
            var state = expectMostRecentItem()
            assertTrue(state.list is ListState.Error)

            viewModel.onRetry()
            advanceUntilIdle()

            state = expectMostRecentItem()
            assertTrue(state.list is ListState.Content)
            assertEquals(20, (state.list as ListState.Content).items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteTransactionUpdatesListSection() = runTest(testDispatcher) {
        val item = Transaction(1, "Supermarket", "Groceries", -50.0, "2026-10-01")
        val repository = FakeTransactionRepository(initialTransactions = listOf(item), delayMillis = 0L)
        val viewModel = TransactionsViewModel(repository)

        viewModel.uiState.test {
            advanceUntilIdle()
            var state = expectMostRecentItem()
            val content = state.list as ListState.Content
            assertEquals(1, content.items.size)

            viewModel.onDeleteTransaction(item)
            advanceUntilIdle()

            state = expectMostRecentItem()
            val updatedContent = state.list as ListState.Content
            assertTrue(updatedContent.items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
