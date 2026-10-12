package com.hoods.composestateyt.ui.transaction

import com.hoods.composestateyt.data.Transaction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class HeaderState(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class FiltersState(
    val query: String = "",
    val availableCategories: ImmutableList<String> = persistentListOf("All", "Groceries", "Income", "Transport", "Utilities", "Food", "Health", "Entertainment", "Education"),
    val selectedCategory: String = "All",
    val typeFilter: String = "All",
    val periodFilter: String = "All",
)

sealed interface ListState {
    data object Loading : ListState
    data class Error(val message: String) : ListState
    data class Content(
        val items: ImmutableList<Transaction>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean,
        val loadMoreFailed: Boolean,
    ) : ListState
}

data class UndoDelete(
    val transaction: Transaction,
)

data class UserMessage(
    val id: Long,
    val text: String,
    val undo: UndoDelete? = null,
)


//TODO FIX 1: giant flat state
data class TransactionsUiState(
    val header: HeaderState = HeaderState(),
    val filters: FiltersState = FiltersState(),
    val list:ListState = ListState.Loading,
    val userMessage: UserMessage? = null,
)

