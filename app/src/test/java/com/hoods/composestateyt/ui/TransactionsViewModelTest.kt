package com.hoods.composestateyt.ui


import com.hoods.composestateyt.data.FakeTransactionRepository
import com.hoods.composestateyt.data.Transaction
import com.hoods.composestateyt.ui.transaction.TransactionsUiState
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

    @Test
    fun initialLoadPopulatesItemsAndSummary() = runTest(testDispatcher) {
        val repository = FakeTransactionRepository(delayMillis = 0L)
        val viewModel = TransactionsViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoading)
        assertTrue(state.items.isNotEmpty())
        assertEquals(20, state.items.size)
        assertTrue(state.totalCount > 20)
        assertTrue(state.hasMore)
    }

    @Test
    fun categoryFilterRequiresFullStateComparison() = runTest(testDispatcher) {
        val sampleList = listOf(
            Transaction(1, "Supermarket", "Groceries", -50.0, "2026-10-01"),
            Transaction(2, "Salary", "Income", 2000.0, "2026-10-02"),
        )
        val repository =
            FakeTransactionRepository(initialTransactions = sampleList, delayMillis = 0L)
        val viewModel = TransactionsViewModel(repository)
        advanceUntilIdle()

        viewModel.onCategorySelected("Groceries")
        advanceUntilIdle()

        //TODO PROBLEM 5: test needs full state
        val expectedFullState = TransactionsUiState(
            items = listOf(Transaction(1, "Supermarket", "Groceries", -50.0, "2026-10-01")),
            page = 0,
            pageSize = 20,
            hasMore = false,
            totalCount = 1,
            isInitialLoading = false,
            isLoadingMore = false,
            isRefreshing = false,
            errorMessage = null,
            loadMoreError = null,
            query = "",
            selectedCategory = "Groceries",
            typeFilter = "All",
            periodFilter = "All",
            availableCategories = listOf(
                "All",
                "Groceries",
                "Income",
                "Transport",
                "Utilities",
                "Food",
                "Health",
                "Entertainment",
                "Education"
            ),
            totalIncome = 0.0,
            totalExpenses = 50.0,
            balance = -50.0,
            isSummaryLoading = false,
            summaryError = null,
            isDeleting = false,
            lastDeletedId = null,
            lastDeletedTransaction = null,
            searchDebounceMillis = 300L,
        )

        assertEquals(expectedFullState, viewModel.uiState.value)
    }

    @Test
    fun deleteTransactionUpdatesStateAndRemovesItem() = runTest(testDispatcher) {
        val item = Transaction(1, "Supermarket", "Groceries", -50.0, "2026-10-01")
        val repository =
            FakeTransactionRepository(initialTransactions = listOf(item), delayMillis = 0L)
        val viewModel = TransactionsViewModel(repository)
        advanceUntilIdle()

        viewModel.onDeleteTransaction(item)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertEquals(1, state.lastDeletedId)
        assertEquals(item, state.lastDeletedTransaction)
    }

    @Test
    fun retryLoadsDataAfterError() = runTest(testDispatcher) {
        val repository = FakeTransactionRepository(delayMillis = 0L)
        repository.setFailNextCall(true)
        val viewModel = TransactionsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.onRetry()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.items.isNotEmpty())
    }
}
