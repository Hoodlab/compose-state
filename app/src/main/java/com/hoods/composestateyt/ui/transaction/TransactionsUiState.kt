package com.hoods.composestateyt.ui.transaction

import com.hoods.composestateyt.data.Transaction


//TODO PROBLEM 1: giant flat state
data class TransactionsUiState(
    val items: List<Transaction> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 20,
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val query: String = "",
    val selectedCategory: String = "All",
    val typeFilter: String = "All",
    val periodFilter: String = "All",
    val availableCategories: List<String> = listOf("All", "Groceries", "Income", "Transport", "Utilities", "Food", "Health", "Entertainment", "Education"),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val isSummaryLoading: Boolean = false,
    val summaryError: String? = null,
    val isDeleting: Boolean = false,
    val lastDeletedId: Int? = null,
    val lastDeletedTransaction: Transaction? = null,
    val isFilterSheetOpen: Boolean = false,
    val isDetailDialogOpen: Boolean = false,
    val selectedTransactionId: Int? = null,
    val selectedTransaction: Transaction? = null,
    val scrollToTopRequested: Boolean = false,
    val showScrollToTopButton: Boolean = false,
    val firstVisibleItemIndex: Int = 0,
    val snackbarMessage: String? = null,
    val exportRequested: Boolean = false,
    val isExportSuccessDialogOpen: Boolean = false,
    val searchDebounceMillis: Long = 300L,
)
